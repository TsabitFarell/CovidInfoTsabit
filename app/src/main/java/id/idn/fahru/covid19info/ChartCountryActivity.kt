package id.idn.fahru.covid19info

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import id.idn.fahru.covid19info.databinding.ActivityChartCountryBinding
import id.idn.fahru.covid19info.pojo.CountriesItem
import id.idn.fahru.covid19info.pojo.ResponseCountry
import id.idn.fahru.covid19info.retrofit.CovidInterface
import id.idn.fahru.covid19info.retrofit.RetrofitService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChartCountryActivity : AppCompatActivity() {
    // data yang akan diterima dari MainActivity
    private lateinit var dataCountry: CountriesItem

    // buat variabel binding untuk ViewBinding
    private lateinit var binding: ActivityChartCountryBinding

    // buat variabel untuk menyimpan nama sumbu x
    private val dayCases = mutableListOf<String>()

    // buat variabel untuk menyimpan data kematian, sembuh, aktif, dan terkonfirmasi
    private val dataConfirmed = mutableListOf<BarEntry>()
    private val dataDeath = mutableListOf<BarEntry>()
    private val dataRecovered = mutableListOf<BarEntry>()
    private val dataActive = mutableListOf<BarEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // deklarasikan inflater dan binding
        val inflater = layoutInflater
        binding = ActivityChartCountryBinding.inflate(inflater)

        // ubah setContentView menggunakan binding.root
        setContentView(binding.root)

        // dapatkan data country dari parcelable
        dataCountry = intent.getParcelableExtra("DATA_COUNTRY") as CountriesItem

        // memasukkan data country ke dalam viewBinding
        binding.run {
            txtNewConfirmedCurrent.text = dataCountry.newConfirmed.toString()
            txtNewDeathsCurrent.text = dataCountry.newDeaths.toString()
            txtNewRecoveredCurrent.text = dataCountry.newRecovered.toString()
            txtTotalConfirmedCurrent.text = dataCountry.totalConfirmed.toString()
            txtTotalDeathsCurrent.text = dataCountry.totalDeaths.toString()
            txtTotalRecoveredCurrent.text = dataCountry.totalRecovered.toString()
            txtCurrent.text = dataCountry.countryCode
            txtCountryChart.text = dataCountry.country

            // gunakan glide untuk menambah bendera
            Glide.with(root)
                .load("https://www.countryflags.io/${dataCountry.countryCode}/flat/64.png") // load diisi oleh link dari bendera
                .into(imgFlagChart) // into diisi oleh imageView tujuan
        }

        // setelah membuat getCountryData, maka panggil fungsi teresebut di onCreate
        // cek dulu apakah slug country ada, jika ada baru deh panggil fungsi
        dataCountry.slug?.let {slug ->
            getCountryData(slug)
        }

    }

    // buat fungsi getCountryData untuk mendapatkan data covid19 berdasarkan nama negara
    private fun getCountryData(countryName : String) {
        // panggil retrofit Interface (CovidInterface)
        val retrofit = RetrofitService.buildService(CovidInterface::class.java)

        // membuat variabel format tanggal dari JSON
        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:MM:SS'Z'", Locale.getDefault())
        // membuat variabel format output tanggal yang bisa dimengerti manusia
        val outputDataFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        // buat Android Coroutines
        lifecycleScope.launch {
            // buat variabel countryData yang berisi dataCovid sesuai nama negara
            val countryData = retrofit.getCountryData(countryName)
            // jika dataCountry sukses diambil oleh retrofit
            if (countryData.isSuccessful) {
                // buat variabel berisi data tersebut
                val dataCovid = countryData.body() as List<ResponseCountry>

                // lakukan perulangan item dari dataCovid
                dataCovid.forEachIndexed { index, responseCountry ->
                    val barConfirmed = BarEntry(index.toFloat(), responseCountry.Confirmed?.toFloat() ?: 0f)
                    val barDeath = BarEntry(index.toFloat(), responseCountry.Deaths?.toFloat() ?: 0f)
                    val barRecovered = BarEntry(index.toFloat(), responseCountry.Recovered?.toFloat() ?: 0f)
                    val barActive = BarEntry(index.toFloat(), responseCountry.Active?.toFloat() ?: 0f)

                    // tambahkan data bar di atas ke dalam dataConfrimed dll
                    dataConfirmed.add(barConfirmed)
                    dataDeath.add(barDeath)
                    dataRecovered.add(barRecovered)
                    dataActive.add(barActive)

                    // jika ada tanggal / Date item
                    responseCountry.Date?.let {itemDate ->
                        // parse tanggal dan ubah ke bentuk yang telah diformat sesuai format output
                        val date = inputDateFormat.parse(itemDate)
                        val formattedDate = outputDataFormat.format(date)
                        // tambahkan tanggal yang telah diformat ke dalam dayCases
                        dayCases.add(formattedDate)

                    }

                }

                binding.chartView.axisLeft.axisMinimum = 0f
                val labelSumbuX = binding.chartView.xAxis
                labelSumbuX.run {
                    valueFormatter = IndexAxisValueFormatter(dayCases)
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setCenterAxisLabels(true)
                    isGranularityEnabled = true
                }

                // adanya keterangan warna dan legenda
                val barDataConfirmed = BarDataSet(dataConfirmed, "Confirmed")
                val barDataRecovered = BarDataSet(dataRecovered, "Recovered")
                val barDataDeath = BarDataSet(dataDeath, "Death")
                val barDataActive = BarDataSet(dataActive, "Active")

                // buat variabel data berisi semua barData
                val dataChart =
                    BarData(barDataConfirmed, barDataRecovered, barDataDeath, barDataActive)

                barDataConfirmed.setColors(Color.parseColor("#F44336"))
                barDataRecovered.setColors(Color.parseColor("#FFEB3B"))
                barDataDeath.setColors(Color.parseColor("#03DAC5"))
                barDataActive.setColors(Color.parseColor("#2196F3"))

                // buat variabel berisi spasi
                val barSpace = 0.02f
                val groupSpace = 0.3f
                val groupCount = 4f

                // modifikasi chartView programmatically
                binding.chartView.run {
                    // tambahkan dataChart kedalam chartView
                    data = dataChart
                    // invalidate untuk mengganti data sebelumnya (jika ada) dengan data yang baru
                    invalidate()
                    setNoDataTextColor(R.color.dkgrey)
                    // ChartView bisa zoom
                    setTouchEnabled(true)
                    description.isEnabled = false
                    xAxis.axisMinimum = 0f
                    setVisibleXRangeMaximum(0f + barData.getGroupWidth(groupSpace, barSpace) * groupCount)
                    groupBars(0f, groupSpace, barSpace)
                }
            }
        }
    }
}