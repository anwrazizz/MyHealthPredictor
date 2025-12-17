package com.example.myhealthpredictor.Prediction

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
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

class PredictionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PredictionHistoryDao
    val allPredictions: LiveData<List<PredictionHistory>>

    private val _predictionResult = MutableLiveData<String>()
    val predictionResult: LiveData<String> = _predictionResult

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // Label kelas obesitas
    private val obesityClasses = arrayOf(
        "Berat Badan Kurang",
        "Berat Badan Normal",
        "Kelebihan Berat Badan Tingkat I",
        "Kelebihan Berat Badan Tingkat II",
        "Obesitas Tipe I",
        "Obesitas Tipe II",
        "Obesitas Tipe III"
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = database.predictionHistoryDao()
        allPredictions = repository.getAllPredictionHistory().asLiveData()

        // Inisialisasi ONNX Runtime
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ortEnv = OrtEnvironment.getEnvironment()
                val modelBytes = application.assets.open("obesity_model.onnx").readBytes()

                // Validasi file bukan placeholder
                val contentString = String(modelBytes.take(100).toByteArray())
                if (contentString.contains("PLACEHOLDER", ignoreCase = true)) {
                    Log.e("PredictionViewModel", "ERROR: File obesity_model.onnx masih placeholder!")
                    return@launch
                }

                ortSession = ortEnv?.createSession(modelBytes)

                // Log info model
                ortSession?.let { session ->
                    Log.d("ONNX", "✓ Model berhasil dimuat")
                    Log.d("ONNX", "Jumlah Input: ${session.numInputs}")
                    session.inputInfo.forEach { (name, info) ->
                        Log.d("ONNX", "Input: $name -> $info")
                    }
                    Log.d("ONNX", "Jumlah Output: ${session.numOutputs}")
                    session.outputInfo.forEach { (name, info) ->
                        Log.d("ONNX", "Output: $name -> $info")
                    }
                }
            } catch (e: Exception) {
                Log.e("ONNX", "Gagal memuat model: ${e.message}", e)
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
            val result = if (ortSession != null && ortEnv != null) {
                withContext(Dispatchers.Default) {
                    runOnnxInference(
                        gender, age, height, weight, familyHistory, favc, fcvc, ncp,
                        caec, smoke, ch2o, scc, faf, tue, calc, mtrans
                    )
                }
            } else {
                Log.w("ONNX", "Model belum siap, menggunakan fallback BMI")
                calculateBmiFallback(weight, height)
            }

            _predictionResult.value = result

            // Simpan ke database
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

    private fun runOnnxInference(
        gender: String, age: Int, height: Float, weight: Float,
        familyHistory: Boolean, favc: Boolean, fcvc: Int, ncp: Int,
        caec: String, smoke: Boolean, ch2o: Int, scc: Boolean,
        faf: Int, tue: Int, calc: String, mtrans: String
    ): String {
        val tensors = mutableListOf<OnnxTensor>()
        var result: OrtSession.Result? = null

        return try {
            val env = ortEnv!!

            // Hitung BMI (tinggi dalam meter)
            val heightInMeters = if (height > 3.0f) height / 100f else height
            val bmi = weight / (heightInMeters * heightInMeters)

            // Buat input tensors sesuai model ONNX
            val inputs = mutableMapOf<String, OnnxTensor>()

            // Float inputs [1,1]
            tensors.add(OnnxTensor.createTensor(env, arrayOf(floatArrayOf(age.toFloat()))).also {
                inputs["Age"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(floatArrayOf(bmi))).also {
                inputs["BMI"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(floatArrayOf(fcvc.toFloat()))).also {
                inputs["FCVC"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(floatArrayOf(ncp.toFloat()))).also {
                inputs["NCP"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(floatArrayOf(ch2o.toFloat()))).also {
                inputs["CH2O"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(floatArrayOf(faf.toFloat()))).also {
                inputs["FAF"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(floatArrayOf(tue.toFloat()))).also {
                inputs["TUE"] = it
            })

            // String inputs [1,1]
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(gender))).also {
                inputs["Gender"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(calc))).also {
                inputs["CALC"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(if (favc) "yes" else "no"))).also {
                inputs["FAVC"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(if (scc) "yes" else "no"))).also {
                inputs["SCC"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(if (smoke) "yes" else "no"))).also {
                inputs["SMOKE"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(if (familyHistory) "yes" else "no"))).also {
                inputs["family_history_with_overweight"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(caec))).also {
                inputs["CAEC"] = it
            })
            tensors.add(OnnxTensor.createTensor(env, arrayOf(arrayOf(mtrans))).also {
                inputs["MTRANS"] = it
            })

            Log.d("ONNX", "Menjalankan inferensi dengan ${inputs.size} inputs")

            // Run model
            result = ortSession!!.run(inputs)

            // Ambil output (index prediksi)
            val outputTensor = result[0] as OnnxTensor
            val predictedIndex = when (val value = outputTensor.value) {
                is LongArray -> value[0].toInt()
                is Array<*> -> (value as Array<Long>)[0].toInt()
                else -> {
                    Log.w("ONNX", "Tipe output tidak dikenali: ${value?.javaClass}")
                    0
                }
            }

            // Map ke label
            if (predictedIndex in obesityClasses.indices) {
                obesityClasses[predictedIndex]
            } else {
                "Unknown Class ($predictedIndex)"
            }

        } catch (e: Exception) {
            Log.e("ONNX", "Error saat inferensi: ${e.message}", e)
            calculateBmiFallback(weight, height)
        } finally {
            result?.close()
            tensors.forEach { it.close() }
        }
    }

    private fun calculateBmiFallback(weight: Float, height: Float): String {
        val h = if (height > 3.0f) height / 100f else height
        val bmi = weight / (h * h)
        return when {
            bmi < 18.5 -> "Berat Badan Kurang"
            bmi < 25.0 -> "Berat Badan Normal"
            bmi < 27.0 -> "Kelebihan Berat Badan Tingkat I"
            bmi < 30.0 -> "Kelebihan Berat Badan Tingkat II"
            bmi < 35.0 -> "Obesitas Tipe I"
            bmi < 40.0 -> "Obesitas Tipe II"
            else -> "Obesitas Tipe III"
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (e: Exception) {
            Log.e("ONNX", "Error menutup ONNX: ${e.message}")
        }
    }
}