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
                
                // Cek apakah file masih placeholder
                val contentString = String(modelBytes.take(50).toByteArray())
                if (contentString.contains("PLACEHOLDER")) {
                    Log.e("PredictionViewModel", "CRITICAL: File obesity_model.onnx masih file placeholder! Mohon timpa dengan file model asli Anda.")
                    return@launch
                }

                ortSession = ortEnv?.createSession(modelBytes)
                
                // --- DEBUGGING INFO MODEL ---
                // Ini akan mencetak info apa yang diharapkan model ke Logcat
                ortSession?.let { session ->
                    Log.d("PredictionViewModel", "Model berhasil dimuat!")
                    Log.d("PredictionViewModel", "Jumlah Input yang diminta model: ${session.numInputs}")
                    for ((name, info) in session.inputInfo) {
                        Log.d("PredictionViewModel", "Input '$name': Info=$info")
                    }
                    Log.d("PredictionViewModel", "Jumlah Output model: ${session.numOutputs}")
                    for ((name, info) in session.outputInfo) {
                        Log.d("PredictionViewModel", "Output '$name': Info=$info")
                    }
                }
                // -----------------------------

            } catch (e: Exception) {
                Log.e("PredictionViewModel", "Gagal memuat model ONNX: ${e.message}")
                e.printStackTrace()
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
                        // 1. Siapkan Input Float Array [1, 16]
                        // PENTING: Pastikan model Python Anda dilatih dengan 16 fitur ini secara berurutan!
                        val inputData = FloatArray(16)
                        
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
                        
                        // Ambil nama input pertama dari model
                        val inputName = ortSession!!.inputNames.iterator().next()
                        
                        // Buat Tensor
                        // Kita coba buat Tensor Float [1, 16]
                        val shape = longArrayOf(1, 16)
                        val tensor = OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(inputData), shape)
                        
                        val inputs = Collections.singletonMap(inputName, tensor)
                        
                        // JALANKAN INFERENSI
                        val results = ortSession!!.run(inputs)
                        
                        // AMBIL HASIL
                        // Cek tipe output pertama
                        val outputData = results[0]
                        
                        var predictedIndex = 0
                        
                        // Logika penanganan output Scikit-Learn vs Deep Learning biasa
                        if (outputData.info.toString().contains("INT64") || outputData.info.toString().contains("Long")) {
                             // Scikit-Learn biasanya mengembalikan Label Kelas langsung (Misal: angka 0-6 atau String) di output ke-0
                             val outputTensor = outputData as OnnxTensor
                             val outputValue = outputTensor.value as? LongArray 
                                          ?: (outputTensor.value as? Array<Long>)?.toLongArray()
                             predictedIndex = outputValue?.get(0)?.toInt() ?: 0
                             
                        } else if (outputData.info.toString().contains("FLOAT")) {
                            // Model Deep Learning biasanya mengembalikan Probabilitas [0.1, 0.8, 0.05...]
                            // Kita harus cari nilai max (ArgMax)
                            val outputTensor = outputData as OnnxTensor
                            val floatArray = outputTensor.floatBuffer.array() // asumsi flat array
                            // Cari index dengan nilai terbesar
                            var maxVal = -Float.MAX_VALUE
                            for (i in floatArray.indices) {
                                if (floatArray[i] > maxVal) {
                                    maxVal = floatArray[i]
                                    predictedIndex = i
                                }
                            }
                        }
                        
                        val classes = arrayOf(
                            "Underweight", "Normal Weight", 
                            "Overweight Level I", "Overweight Level II", 
                            "Obesity Type I", "Obesity Type II", "Obesity Type III"
                        )
                        
                        // Safety check index
                        if (predictedIndex in classes.indices) {
                             classes[predictedIndex]
                        } else {
                             "Unknown Class ($predictedIndex)"
                        }
                        
                    } catch (e: Exception) {
                        Log.e("PredictionViewModel", "ERROR SAAT PREDIKSI ONNX: ${e.message}")
                        e.printStackTrace() // Ini akan mencetak detail error shape mismatch di logcat
                        calculateBmiBased(weight, height)
                    }
                }
            } else {
                Log.w("PredictionViewModel", "Model belum siap, menggunakan BMI.")
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
