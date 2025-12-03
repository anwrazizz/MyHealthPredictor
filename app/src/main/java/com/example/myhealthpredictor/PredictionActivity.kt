package com.example.myhealthpredictor

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText

class PredictionActivity : AppCompatActivity() {

    private lateinit var viewModel: PredictionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        viewModel = ViewModelProvider(this)[PredictionViewModel::class.java]

        val btnBack = findViewById<ImageView>(R.id.btn_back)

        val rgGender = findViewById<RadioGroup>(R.id.rg_gender)
        val etAge = findViewById<TextInputEditText>(R.id.et_age)
        val etHeight = findViewById<TextInputEditText>(R.id.et_height)
        val etWeight = findViewById<TextInputEditText>(R.id.et_weight)
        val cbFamilyHistory = findViewById<CheckBox>(R.id.cb_family_history)
        val cbFavc = findViewById<CheckBox>(R.id.cb_favc)
        val spFcvc = findViewById<Spinner>(R.id.sp_fcvc)
        val spNcp = findViewById<Spinner>(R.id.sp_ncp)
        val spCaec = findViewById<Spinner>(R.id.sp_caec)
        val cbSmoke = findViewById<CheckBox>(R.id.cb_smoke)
        val spCh2o = findViewById<Spinner>(R.id.sp_ch2o)
        val cbScc = findViewById<CheckBox>(R.id.cb_scc)
        val spFaf = findViewById<Spinner>(R.id.sp_faf)
        val spTue = findViewById<Spinner>(R.id.sp_tue)
        val spCalc = findViewById<Spinner>(R.id.sp_calc)
        val spMtrans = findViewById<Spinner>(R.id.sp_mtrans)
        val btnPredict = findViewById<Button>(R.id.btn_predict)

        btnBack.setOnClickListener { finish() }

        viewModel.predictionResult.observe(this) { result ->
            val intent = Intent(this, PredictionResultActivity::class.java)
            intent.putExtra(PredictionResultActivity.EXTRA_RESULT, result)
            startActivity(intent)
        }

        btnPredict.setOnClickListener {
            val genderId = rgGender.checkedRadioButtonId
            if (genderId == -1) {
                Toast.makeText(this, "Mohon pilih jenis kelamin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val gender = if (genderId == R.id.rb_male) "Male" else "Female"
            
            val ageStr = etAge.text.toString()
            val heightStr = etHeight.text.toString()
            val weightStr = etWeight.text.toString()

            if (ageStr.isNotEmpty() && heightStr.isNotEmpty() && weightStr.isNotEmpty()) {
                val age = ageStr.toInt()
                val height = heightStr.toFloat() / 100 // Convert cm to m if needed
                val weight = weightStr.toFloat()
                
                val fcvc = spFcvc.selectedItemPosition + 1
                val ncp = spNcp.selectedItemPosition + 1
                val caec = spCaec.selectedItem.toString()
                val ch2o = spCh2o.selectedItemPosition + 1
                val faf = spFaf.selectedItemPosition
                val tue = spTue.selectedItemPosition
                val calc = spCalc.selectedItem.toString()
                val mtrans = spMtrans.selectedItem.toString()

                viewModel.predict(
                    gender, age, height, weight,
                    cbFamilyHistory.isChecked, cbFavc.isChecked, fcvc, ncp,
                    caec, cbSmoke.isChecked, ch2o, cbScc.isChecked,
                    faf, tue, calc, mtrans
                )
            } else {
                Toast.makeText(this, "Mohon lengkapi data utama (Usia, Tinggi, Berat)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
