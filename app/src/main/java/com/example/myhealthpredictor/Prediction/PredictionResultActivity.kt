package com.example.myhealthpredictor.Prediction

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myhealthpredictor.R

class PredictionResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESULT = "extra_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction_result)

        val resultTextView = findViewById<TextView>(R.id.tv_result_title)
        val healthAdviceTextView = findViewById<TextView>(R.id.tv_health_advice)
        val restartButton = findViewById<Button>(R.id.btn_restart)
        val finishButton = findViewById<Button>(R.id.btn_finish)

        val result = intent.getStringExtra(EXTRA_RESULT) ?: "Tidak Diketahui"

        // Format hasil (ganti spasi dengan newline untuk tampilan lebih baik)
        val formattedResult = result.replace(" ", "\n")
        resultTextView.text = formattedResult

        // Tampilkan saran kesehatan berdasarkan hasil
        val healthAdvice = getHealthAdvice(result)
        healthAdviceTextView.text = healthAdvice

        // Mulai baru -> buat PredictionActivity baru yang fresh (form kosong)
        restartButton.setOnClickListener {
            val intent = Intent(this, PredictionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Selesai -> kembali ke dashboard utama
        finishButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun getHealthAdvice(obesityLevel: String): String {
        return when (obesityLevel) {
            "Berat Badan Kurang" -> """
Berdasarkan analisis, berat badan Anda saat ini berada di bawah kategori normal. Kondisi ini dapat meningkatkan risiko kekurangan nutrisi dan menurunkan sistem kekebalan tubuh.

Kami merekomendasikan Anda untuk meningkatkan asupan kalori harian secara bertahap sebanyak 300-500 kalori lebih banyak dari kebutuhan normal. Fokuskan pada makanan tinggi protein seperti telur, ayam, ikan, dan kacang-kacangan untuk membantu pembentukan massa otot. Konsumsi makanan dalam porsi kecil namun lebih sering, sekitar 5-6 kali sehari, dapat membantu meningkatkan berat badan secara sehat.

Kombinasikan pola makan dengan latihan beban ringan untuk membangun massa otot, bukan hanya menambah lemak tubuh. Konsultasi dengan ahli gizi profesional sangat disarankan untuk mendapatkan program penambahan berat badan yang tepat dan memastikan tidak ada masalah metabolisme yang mendasari kondisi Anda.
            """.trimIndent()

            "Berat Badan Normal" -> """
Selamat! Berat badan Anda berada dalam kategori ideal. Kondisi ini menunjukkan keseimbangan yang baik antara tinggi dan berat badan Anda, yang berkontribusi pada kesehatan optimal dan mengurangi risiko berbagai penyakit kronis.

Untuk mempertahankan kondisi ini, kami sarankan Anda melanjutkan pola makan seimbang dengan mengonsumsi berbagai jenis makanan bergizi. Pastikan menu harian Anda mencakup minimal 5 porsi sayur dan buah, protein berkualitas, karbohidrat kompleks, dan lemak sehat. Aktivitas fisik rutin selama 150 menit per minggu, seperti jalan cepat, jogging, atau bersepeda, akan membantu menjaga kebugaran dan metabolisme tubuh.

Jangan lupakan pentingnya tidur yang cukup (7-8 jam per malam), hidrasi yang baik (minimal 8 gelas air per hari), dan manajemen stres yang efektif. Pemeriksaan kesehatan berkala setiap tahun tetap penting untuk memantau kondisi kesehatan secara menyeluruh.
            """.trimIndent()

            "Kelebihan Berat Badan Tingkat I" -> """
Hasil analisis menunjukkan Anda mengalami kelebihan berat badan tingkat ringan. Meskipun belum mencapai kategori obesitas, kondisi ini perlu mendapat perhatian karena dapat meningkatkan risiko berbagai masalah kesehatan seperti diabetes tipe 2, tekanan darah tinggi, dan penyakit kardiovaskular.

Langkah pertama yang dapat Anda lakukan adalah mengurangi asupan kalori harian secara bertahap, sekitar 300-500 kalori per hari. Fokuslah pada peningkatan konsumsi sayuran, buah-buahan, dan protein rendah lemak, sambil mengurangi makanan tinggi gula, lemak jenuh, dan gorengan. Perbanyak aktivitas fisik dengan olahraga kardio seperti jalan cepat atau jogging selama 30-45 menit, minimal 5 kali seminggu.

Perubahan kecil dalam kebiasaan sehari-hari dapat memberikan dampak besar. Misalnya, mengganti camilan dengan buah segar, mengurangi porsi nasi dan memperbanyak sayur, serta menghindari makan 2-3 jam sebelum tidur. Target penurunan berat badan yang sehat adalah 0.5-1 kg per minggu. Catat asupan makanan harian Anda untuk membantu mengontrol kalori dengan lebih baik.
            """.trimIndent()

            "Kelebihan Berat Badan Tingkat II" -> """
Anda saat ini berada dalam kategori kelebihan berat badan tingkat sedang. Kondisi ini memerlukan perhatian serius karena risiko kesehatan yang terkait semakin meningkat, termasuk diabetes, penyakit jantung, dan gangguan metabolik lainnya.

Kami sangat merekomendasikan Anda untuk berkonsultasi dengan dokter dan ahli gizi profesional untuk mendapatkan program penurunan berat badan yang terstruktur dan aman. Program yang tepat biasanya melibatkan pengurangan kalori sekitar 500-750 kalori per hari, dikombinasikan dengan olahraga teratur minimal 45-60 menit, 5-6 kali seminggu. Kombinasi latihan kardio seperti jogging atau renang dengan latihan kekuatan akan memberikan hasil optimal.

Penting untuk melakukan pemeriksaan kesehatan berkala, termasuk cek kadar gula darah, kolesterol, dan tekanan darah. Strategi sederhana seperti minum 2 gelas air putih sebelum makan dan menggunakan piring lebih kecil dapat membantu mengontrol porsi secara otomatis. Hindari diet ekstrem yang menjanjikan hasil cepat, karena dapat berbahaya bagi kesehatan. Target realistis adalah menurunkan 5-10% berat badan dalam 3-6 bulan dengan pendekatan yang berkelanjutan.
            """.trimIndent()

            "Obesitas Tipe I" -> """
Hasil analisis menunjukkan Anda berada dalam kategori obesitas tipe 1. Ini adalah kondisi medis serius yang memerlukan intervensi profesional segera. Obesitas pada tingkat ini secara signifikan meningkatkan risiko berbagai komplikasi kesehatan seperti diabetes tipe 2, hipertensi, sleep apnea, dan penyakit kardiovaskular.

Kami sangat menganjurkan Anda untuk segera berkonsultasi dengan dokter untuk evaluasi kesehatan menyeluruh, termasuk pemeriksaan tekanan darah, gula darah puasa, dan profil lipid lengkap. Program penurunan berat badan harus dilakukan di bawah supervisi medis dengan diet rendah kalori seimbang (1200-1500 kalori per hari) yang disesuaikan dengan kondisi kesehatan Anda. Olahraga harus dimulai dari intensitas ringan dan ditingkatkan secara bertahap untuk menghindari cedera.

Aspek psikologis juga sangat penting dalam perjalanan penurunan berat badan. Konseling dengan psikolog atau terapis perilaku dapat membantu mengidentifikasi dan mengubah pola makan emosional serta membangun kebiasaan hidup sehat jangka panjang. Dukungan dari keluarga dan orang terdekat akan sangat membantu kesuksesan program. Jangan pernah mencoba diet ekstrem atau obat penurun berat badan tanpa pengawasan dokter. Target awal yang realistis adalah menurunkan 5-10% berat badan dalam 6 bulan pertama.
            """.trimIndent()

            "Obesitas Tipe II" -> """
Anda didiagnosis mengalami obesitas tipe 2, yang merupakan kondisi kesehatan serius yang memerlukan tindakan medis segera dan komprehensif. Pada tingkat ini, risiko komplikasi kesehatan yang mengancam jiwa meningkat drastis, termasuk penyakit jantung koroner, stroke, diabetes parah, gagal ginjal, dan berbagai jenis kanker.

Konsultasi dengan dokter spesialis penyakit dalam atau spesialis obesitas adalah langkah wajib yang harus segera Anda ambil. Pemeriksaan komplikasi menyeluruh mencakup evaluasi fungsi jantung, diabetes, liver, dan ginjal perlu dilakukan. Program penurunan berat badan intensif dengan tim medis multidisiplin yang terdiri dari dokter, ahli gizi, dan psikolog akan memberikan pendekatan terbaik untuk kondisi Anda.

Dalam beberapa kasus, dokter mungkin merekomendasikan diet sangat rendah kalori (VLCD) di bawah pengawasan ketat, terapi obat anti-obesitas yang diresepkan, atau bahkan evaluasi untuk operasi bariatrik jika metode konvensional tidak memberikan hasil yang diharapkan. Terapi perilaku kognitif dan konseling psikologis profesional sangat penting untuk mengatasi aspek emosional dan behavioral dari obesitas.

Aktivitas fisik harus disesuaikan dengan kondisi kesehatan Anda untuk menghindari cedera atau komplikasi. Monitoring kesehatan secara ketat setiap bulan, termasuk pemeriksaan laboratorium dan EKG, diperlukan untuk memastikan keamanan dan efektivitas program. Libatkan keluarga dalam program ini karena dukungan emosional sangat krusial untuk kesuksesan jangka panjang.
            """.trimIndent()

            "Obesitas Tipe III" -> """
Ini adalah kondisi darurat medis. Anda didiagnosis dengan obesitas tipe 3, yang juga dikenal sebagai obesitas morbid atau obesitas ekstrem. Kondisi ini merupakan ancaman serius bagi kehidupan Anda dengan risiko sangat tinggi terhadap serangan jantung mendadak, stroke, kematian dini, dan berbagai komplikasi fatal lainnya.

Anda harus segera mendapatkan perhatian medis dari dokter spesialis obesitas dan penyakit dalam. Jangan menunda, karena setiap hari yang berlalu meningkatkan risiko komplikasi serius. Pemeriksaan komprehensif menyeluruh terhadap fungsi jantung, paru-paru, dan organ vital lainnya harus segera dilakukan untuk mengevaluasi sejauh mana dampak obesitas terhadap tubuh Anda.

Program medis intensif dengan tim multidisiplin yang terdiri dari dokter spesialis, ahli gizi klinis, dan psikolog medis adalah keharusan mutlak. Operasi bariatrik seperti gastric bypass atau sleeve gastrectomy harus dipertimbangkan dengan sangat serius karena seringkali merupakan solusi paling efektif untuk obesitas pada tingkat ini. Diet ketat di bawah pengawasan ketat ahli gizi dan dokter spesialis, terapi obat anti-obesitas, serta konseling psikologi intensif akan menjadi bagian integral dari program Anda.

Aktivitas fisik harus dimulai dengan sangat hati-hati dari intensitas paling ringan untuk menghindari cedera atau komplikasi kardiovaskular. Dalam beberapa kasus, rawat inap mungkin diperlukan untuk stabilisasi kondisi. Komplikasi yang mungkin sudah terjadi seperti diabetes parah, gagal jantung, atau sleep apnea berat perlu penanganan segera.

Yang terpenting, pahami bahwa ini bukan sekadar masalah penampilan atau kenyamanan—ini tentang keselamatan hidup Anda. Dukungan penuh dari keluarga dan komunitas sangat krusial. Komitmen total terhadap perubahan gaya hidup jangka panjang adalah satu-satunya cara untuk menyelamatkan dan meningkatkan kualitas hidup Anda.
            """.trimIndent()

            else -> """
Terima kasih telah menggunakan sistem prediksi kesehatan kami. Untuk mendapatkan rekomendasi yang lebih spesifik dan personal, kami sangat menyarankan Anda untuk berkonsultasi langsung dengan dokter atau ahli gizi profesional.

Pemeriksaan kesehatan menyeluruh akan membantu mengidentifikasi kondisi kesehatan Anda secara lebih akurat dan memberikan panduan yang sesuai dengan kebutuhan individual Anda. Sementara itu, terapkan prinsip dasar hidup sehat: konsumsi makanan bergizi seimbang, lakukan olahraga teratur minimal 30 menit setiap hari, tidur cukup, kelola stres dengan baik, dan monitor berat badan Anda secara berkala.

Ingatlah bahwa kesehatan adalah investasi jangka panjang yang memerlukan komitmen konsisten. Perubahan kecil yang dilakukan secara rutin akan memberikan dampak besar pada kualitas hidup Anda di masa depan.
            """.trimIndent()
        }
    }
}