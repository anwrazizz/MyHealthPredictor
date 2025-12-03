package com.example.myhealthpredictor

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.util.Collections

class PredictionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PredictionHistoryDao
    val allPredictions: LiveData<List<PredictionHistory>>
    
    private val _predictionResult = MutableLiveData<String>()
    val predictionResult: LiveData<String> = _predictionResult

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = database.predictionHistoryDao()
        allPredictions = repository.getAllPredictionHistory().asLiveData()
        
        // Inisialisasi ONNX Runtime
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ortEnv = OrtEnvironment.getEnvironment()
                // Baca model dari assets
                val modelBytes = application.assets.open("obesity_model.onnx").readBytes()
                ortSession = ortEnv?.createSession(modelBytes)
                Log.d("PredictionViewModel", "Model ONNX berhasil dimuat.")
            } catch (e: Exception) {
                Log.e("PredictionViewModel", "Gagal memuat model ONNX: ${e.message}")
            }
        }
    }

    fun predict(
        gender: String, age: Int, height: Float, weight: Float,
        familyHistory: Boolean, favc: Boolean, fcvc: Int, ncp: Int,
        caec: String, smoke: Boolean, ch2o: Int, scc: Boolean,
        faf: Int, tue: Int, calc: String, mtrans: String
    ) {
        viewModelScope.launch {
            var result = ""
            
            if (ortSession != null && ortEnv != null) {
                result = withContext(Dispatchers.Default) {
                    try {
                        // 1. Siapkan Input Float Array [1, 17]
                        // Sesuaikan urutan fitur dengan model Python Anda (misal Scikit-Learn export)
                        val inputData = FloatArray(17)
                        
                        inputData[0] = if (gender == "Male") 1f else 0f
                        inputData[1] = age.toFloat()
                        inputData[2] = height
                        inputData[3] = weight
                        inputData[4] = if (familyHistory) 1f else 0f
                        inputData[5] = if (favc) 1f else 0f
                        inputData[6] = fcvc.toFloat()
                        inputData[7] = ncp.toFloat()
                        inputData[8] = when(caec) { "No" -> 0f; "Sometimes" -> 1f; "Frequently" -> 2f; else -> 3f }
                        inputData[9] = if (smoke) 1f else 0f
                        inputData[10] = ch2o.toFloat()
                        inputData[11] = if (scc) 1f else 0f
                        inputData[12] = faf.toFloat()
                        inputData[13] = tue.toFloat()
                        inputData[14] = when(calc) { "I do not drink" -> 0f; "Sometimes" -> 1f; "Frequently" -> 2f; else -> 3f }
                        inputData[15] = when(mtrans) { 
                            "Automobile" -> 0f; "Motorbike" -> 1f; "Bike" -> 2f; 
                            "Public Transportation" -> 3f; else -> 4f 
                        }
                        inputData[16] = 0f // Bias/Dummy jika dibutuhkan model, sesuaikan
                        
                        // Buat Tensor [1, 17]
                        val shape = longArrayOf(1, 17)
                        val tensor = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(inputData), shape)
                        
                        // Jalankan Inferensi
                        // Sesuaikan "float_input" dengan nama input layer model ONNX Anda (cek via Netron.app)
                        // Jika model Scikit-learn, biasanya input bernama "float_input" atau "X"
                        val inputName = ortSession!!.inputNames.iterator().next() 
                        val inputs = Collections.singletonMap(inputName, tensor)
                        
                        val results = ortSession!!.run(inputs)
                        
                        // Ambil Output
                        // Scikit-learn biasanya output: [0] = Label (Int/Long), [1] = Probabilities
                        // Jika model NN (PyTorch/TF), biasanya output: [1, 7] probabilities
                        
                        val outputTensor = results[0] as OnnxTensor
                        val outputValue = outputTensor.value as? LongArray 
                                          ?: (outputTensor.value as? Array<Long>)?.toLongArray()
                        
                        // Mapping Hasil Prediksi (Asumsi output adalah Index Kelas 0-6)
                        val predictedIndex = outputValue?.get(0)?.toInt() ?: 0
                        
                        val classes = arrayOf(
                            "Underweight", "Normal Weight", 
                            "Overweight Level I", "Overweight Level II", 
                            "Obesity Type I", "Obesity Type II", "Obesity Type III"
                        )
                        classes.getOrElse(predictedIndex) { "Unknown" }
                        
                    } catch (e: Exception) {
                        Log.e("PredictionViewModel", "Error ONNX Inference: ${e.message}")
                        calculateBmiBased(weight, height)
                    }
                }
            } else {
                // Fallback Manual
                result = calculateBmiBased(weight, height)
            }
            
            _predictionResult.value = result

            // Simpan Riwayat
            val history = PredictionHistory(
                gender = gender, age = age, height = height, weight = weight,
                familyHistoryWithOverweight = familyHistory, favc = favc, fcvc = fcvc,
                ncp = ncp, caec = caec, smoke = smoke, ch2o = ch2o, scc = scc,
                faf = faf, tue = tue, calc = calc, mtrans = mtrans,
                nobeyerere = result, date = System.currentTimeMillis()
            )
            repository.insert(history)
        }
    }

    private fun calculateBmiBased(weight: Float, height: Float): String {
        val h = if (height > 3.0) height / 100 else height 
        val bmi = weight / (h * h)
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal Weight"
            bmi < 30.0 -> "Overweight Level I"
            bmi < 35.0 -> "Overweight Level II"
            bmi < 40.0 -> "Obesity Type I"
            bmi < 50.0 -> "Obesity Type II"
            else -> "Obesity Type III"
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (e: Exception) {
            Log.e("PredictionViewModel", "Error closing ONNX resources: ${e.message}")
        }
    }
}
