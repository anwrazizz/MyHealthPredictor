package com.example.myhealthpredictor.Prediction

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.Result
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

    /**
     * Perbaikan predict:
     * - Mengirim input sesuai model: beberapa tensor float [1,1], beberapa tensor string [1,1]
     * - Menghitung BMI karena model menerima BMI (bukan height & weight terpisah)
     * - Menutup semua OnnxTensor dan Result setelah penggunaan
     */
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
                    // Siapkan koleksi tensor agar bisa ditutup di finally
                    val createdTensors = mutableListOf<OnnxTensor>()
                    var runResult: Result? = null
                    try {
                        val env = ortEnv!!

                        // Hitung BMI (model menerima BMI)
                        val h = if (height > 3.0f) height / 100f else height
                        val bmiValue = weight / (h * h)

                        // Buat map input sesuai nama yang tertera di model (case-sensitive)
                        val inputs = mutableMapOf<String, OnnxTensor>()

                        // FLOAT inputs -> bentuk [1,1] menggunakan arrayOf(floatArrayOf(...))
                        val ageTensor = OnnxTensor.createTensor(env, arrayOf(floatArrayOf(age.toFloat())))
                        inputs["Age"] = ageTensor
                        createdTensors.add(ageTensor)

                        val bmiTensor = OnnxTensor.createTensor(env, arrayOf(floatArrayOf(bmiValue)))
                        inputs["BMI"] = bmiTensor
                        createdTensors.add(bmiTensor)

                        val fcvcTensor = OnnxTensor.createTensor(env, arrayOf(floatArrayOf(fcvc.toFloat())))
                        inputs["FCVC"] = fcvcTensor
                        createdTensors.add(fcvcTensor)

                        val ncpTensor = OnnxTensor.createTensor(env, arrayOf(floatArrayOf(ncp.toFloat())))
                        inputs["NCP"] = ncpTensor
                        createdTensors.add(ncpTensor)

                        val ch2oTensor = OnnxTensor.createTensor(env, arrayOf(floatArrayOf(ch2o.toFloat())))
                        inputs["CH2O"] = ch2oTensor
                        createdTensors.add(ch2oTensor)

                        val fafTensor = OnnxTensor.createTensor(env, arrayOf(floatArrayOf(faf.toFloat())))
                        inputs["FAF"] = fafTensor
                        createdTensors.add(fafTensor)

                        val tueTensor = OnnxTensor.createTensor(env, arrayOf(floatArrayOf(tue.toFloat())))
                        inputs["TUE"] = tueTensor
                        createdTensors.add(tueTensor)

                        // STRING inputs -> bentuk [1,1] menggunakan arrayOf(arrayOf(string))
                        val genderTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(gender)))
                        inputs["Gender"] = genderTensor
                        createdTensors.add(genderTensor)

                        val calcTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(calc)))
                        inputs["CALC"] = calcTensor
                        createdTensors.add(calcTensor)

                        // FAVC, SCC, SMOKE, family_history_with_overweight are strings in model dataset ("yes"/"no")
                        val favcStr = if (favc) "yes" else "no"
                        val favcTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(favcStr)))
                        inputs["FAVC"] = favcTensor
                        createdTensors.add(favcTensor)

                        val sccStr = if (scc) "yes" else "no"
                        val sccTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(sccStr)))
                        inputs["SCC"] = sccTensor
                        createdTensors.add(sccTensor)

                        val smokeStr = if (smoke) "yes" else "no"
                        val smokeTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(smokeStr)))
                        inputs["SMOKE"] = smokeTensor
                        createdTensors.add(smokeTensor)

                        val familyHistoryStr = if (familyHistory) "yes" else "no"
                        val famTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(familyHistoryStr)))
                        inputs["family_history_with_overweight"] = famTensor
                        createdTensors.add(famTensor)

                        // CAEC and MTRANS as-is from dataset (strings)
                        val caecTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(caec)))
                        inputs["CAEC"] = caecTensor
                        createdTensors.add(caecTensor)

                        val mtransTensor = OnnxTensor.createTensor(env, arrayOf(arrayOf(mtrans)))
                        inputs["MTRANS"] = mtransTensor
                        createdTensors.add(mtransTensor)

                        // Debug: Log nama input yang dikirim (opsional)
                        Log.d("PredictionViewModel", "Menjalankan inferensi dengan inputs: ${inputs.keys}")

                        // RUN INFERENCE
                        runResult = ortSession!!.run(inputs)

                        // Ambil output_label (output pertama sesuai model)
                        // output_label bertipe INT64 (node info menunjukkan INT64)
                        val out0 = runResult[0]
                        val predictedIndex: Int = try {
                            val outTensor = out0 as OnnxTensor
                            // ONNX Runtime for Java dapat mengembalikan LongArray atau Array<Long>
                            val value = outTensor.value
                            when (value) {
                                is LongArray -> value[0].toInt()
                                is Array<*> -> {
                                    // bisa jadi Array<Long>
                                    val arr = value as Array<Long>
                                    arr[0].toInt()
                                }
                                else -> {
                                    Log.w("PredictionViewModel", "Tipe output tidak dikenali: ${value?.javaClass}")
                                    0
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("PredictionViewModel", "Gagal membaca output_label: ${e.message}")
                            0
                        }

                        // Mapping index ke label yang lebih manusiawi
                        val classes = arrayOf(
                            "Underweight",           // 0
                            "Normal Weight",         // 1
                            "Overweight Level I",    // 2
                            "Overweight Level II",   // 3
                            "Obesity Type I",        // 4
                            "Obesity Type II",       // 5
                            "Obesity Type III"       // 6
                        )

                        val predictedLabel = if (predictedIndex in classes.indices) {
                            classes[predictedIndex]
                        } else {
                            "Unknown Class ($predictedIndex)"
                        }

                        predictedLabel

                    } catch (e: Exception) {
                        Log.e("PredictionViewModel", "ERROR SAAT PREDIKSI ONNX: ${e.message}")
                        e.printStackTrace()
                        // fallback jika ada kegagalan inferensi
                        calculateBmiBased(weight, height)
                    } finally {
                        // Tutup run result dan semua tensor yang dibuat
                        try {
                            runResult?.close()
                        } catch (e: Exception) {
                            Log.w("PredictionViewModel", "Gagal menutup runResult: ${e.message}")
                        }
                        for (t in createdTensors) {
                            try {
                                t.close()
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
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
