package com.example.wifirsslogger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.room.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
// --- same imports, add a few more ---
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.components.YAxis

class MainActivity : ComponentActivity() {
    private lateinit var wifiManager: WifiManager
    private val rssiDao by lazy { RssiDatabase.getDatabase(applicationContext).rssiDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "scan") {
                composable("scan") {
                    ScanScreen(navController, wifiManager, rssiDao)
                }
                composable("savedData") {
                    SavedDataScreen(navController, rssiDao)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController, wifiManager: WifiManager, rssiDao: RssiDao) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedLocation by remember { mutableStateOf("") }  // Initially empty
    val locations = listOf("Location 1", "Location 2", "Location 3")

    var wifiScanResults by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var wifiCount by remember { mutableStateOf(0) }
    var showReadyToScanText by remember { mutableStateOf(false) }  // new

    fun scanWifi(location: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Location permission required for Wi-Fi scanning", Toast.LENGTH_SHORT).show()
            return
        }

        wifiScanResults = emptyList()

        if (wifiManager.isWifiEnabled) {
            val success = wifiManager.startScan()
            if (success) {
                val scanResults = wifiManager.scanResults
                var rssiList = scanResults.map { it.level }

                if (rssiList.size >= 100) {
                    rssiList = rssiList.take(100)
                } else {
                    val lastValue = rssiList.lastOrNull() ?: -100
                    rssiList = rssiList + List(100 - rssiList.size) { lastValue }
                }
                wifiScanResults = scanResults.take(100)
                wifiCount = wifiScanResults.size

                coroutineScope.launch {
                    wifiScanResults.forEach { result ->
                        rssiDao.insertRssiData(
                            RssiData(
                                location = location,
                                rssiValue = result.level,
                                ssid = result.SSID
                            )
                        )
                    }
                    Toast.makeText(context, "Saved 100 values for $location!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "WiFi scan failed.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Wi-Fi is not enabled", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi RSS Logger") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Select Location", style = MaterialTheme.typography.titleMedium)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                locations.forEach { location ->
                    Button(
                        onClick = {
                            selectedLocation = location
                            showReadyToScanText = true  // set to true when location selected
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (location == selectedLocation) Color(0xFFBBDEFB) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(location)
                    }
                }
            }

            if (showReadyToScanText && selectedLocation.isNotEmpty()) {
                Text(
                    text = "Ready to scan at $selectedLocation",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            if (showReadyToScanText) {  // show scan button only after location selected
                Button(
                    onClick = { scanWifi(selectedLocation) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Scan and Save RSSI")
                }
            }

            Button(
                onClick = { navController.navigate("savedData") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Saved Data")
            }

            if (wifiScanResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Live Wi-Fi Scan Results", style = MaterialTheme.typography.titleMedium)

                wifiScanResults.forEach { result ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("SSID: ${result.SSID}")
                            Text("RSSI: ${result.level} dBm")
                            Text("Frequency: ${result.frequency} MHz")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Number of Wi-Fi networks found in $selectedLocation: $wifiCount",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDataScreen(navController: NavController, rssiDao: RssiDao) {
    val coroutineScope = rememberCoroutineScope()
    var savedData by remember { mutableStateOf<List<RssiData>>(emptyList()) }

    LaunchedEffect(true) {
        coroutineScope.launch {
            savedData = rssiDao.getAllData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved RSSI Data") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back to Scan")
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            rssiDao.deleteAllData()
                            savedData = emptyList()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Clear Data")
                }
            }

            if (savedData.isNotEmpty()) {
                val groupedData = savedData.groupBy { it.location }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "RSSI Range Comparison",
                    style = MaterialTheme.typography.headlineSmall
                )

                Divider(color = Color.Gray, thickness = 1.dp)

                AndroidView(
                    factory = { context ->
                        BarChart(context).apply {
                            val colors = listOf(
                                android.graphics.Color.RED,
                                android.graphics.Color.GREEN,
                                android.graphics.Color.BLUE
                            )

                            val entries = groupedData.entries.mapIndexed { index, (location, data) ->
                                val avgRssi = data.map { it.rssiValue }.average().toFloat()
                                BarEntry(index.toFloat(), avgRssi)
                            }

                            val dataSet = BarDataSet(entries, "Average RSSI per Location").apply {
                                setColors(colors)
                                valueTextSize = 12f
                            }

                            val barData = BarData(dataSet)
                            this.data = barData

                            val xAxis = this.xAxis
                            xAxis.valueFormatter = IndexAxisValueFormatter(groupedData.keys.toList())
                            xAxis.position = XAxis.XAxisPosition.BOTTOM
                            xAxis.granularity = 1f
                            xAxis.setDrawGridLines(false)
                            xAxis.textSize = 12f

                            val yAxisLeft = this.axisLeft
                            yAxisLeft.axisMinimum = -100f
                            yAxisLeft.axisMaximum = 0f
                            yAxisLeft.setDrawGridLines(true)
                            yAxisLeft.textSize = 12f

                            this.axisRight.isEnabled = false
                            this.description.isEnabled = false
                            this.legend.isEnabled = false

                            this.setFitBars(true)
                            this.animateY(1000)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .horizontalScroll(rememberScrollState())
                )

                Spacer(modifier = Modifier.height(16.dp))

                groupedData.entries.forEach { (location, data) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = location,
                                style = MaterialTheme.typography.titleMedium
                            )

                            val maxRssi = data.maxOf { it.rssiValue }
                            val minRssi = data.minOf { it.rssiValue }
                            val avgRssi = data.map { it.rssiValue }.average().toFloat()
                            val wifiCount = data.size
                            val minRssiWifi = data.minByOrNull { it.rssiValue }
                            val maxRssiWifi = data.maxByOrNull { it.rssiValue }

                            Text(
                                "$wifiCount Wi-Fi Networks Found",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Text(
                                "Max RSSI: ${maxRssiWifi?.rssiValue} dBm (SSID: ${maxRssiWifi?.ssid ?: "Unknown"})"
                            )
                            Text(
                                "Min RSSI: ${minRssiWifi?.rssiValue} dBm (SSID: ${minRssiWifi?.ssid ?: "Unknown"})"
                            )
                            Text(
                                "Average RSSI: ${avgRssi.toInt()} dBm"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            AndroidView(
                                factory = { context ->
                                    LineChart(context).apply {
                                        val entries = data.mapIndexed { index, rssi ->
                                            Entry(index.toFloat(), rssi.rssiValue.toFloat())
                                        }

                                        val dataSet = LineDataSet(entries, "RSSI at $location").apply {
                                            color = android.graphics.Color.BLUE
                                            valueTextSize = 10f
                                            lineWidth = 2f
                                            setDrawCircles(true)
                                            setCircleColor(android.graphics.Color.RED)
                                        }

                                        val lineData = LineData(dataSet)
                                        this.data = lineData

                                        val xAxis = this.xAxis
                                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                                        xAxis.setDrawGridLines(true)
                                        xAxis.labelRotationAngle = 90f
                                        xAxis.textSize = 10f
                                        xAxis.granularity = 1f
                                        xAxis.valueFormatter = IndexAxisValueFormatter(data.map { it.ssid })

                                        val yAxisLeft = this.axisLeft
                                        yAxisLeft.axisMinimum = -100f
                                        yAxisLeft.axisMaximum = 0f
                                        yAxisLeft.textSize = 10f

                                        this.axisRight.isEnabled = false
                                        this.description.isEnabled = false
                                        this.legend.isEnabled = false

                                        this.setDragEnabled(true)
                                        this.setScaleEnabled(true)
                                        this.setPinchZoom(true)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No saved data found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        }
    }
}

@Entity(tableName = "rssi_data")
data class RssiData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val location: String,
    val rssiValue: Int,
    val ssid: String // Add the ssid property
)

@Dao
interface RssiDao {
    @Insert
    suspend fun insertRssiData(rssiData: RssiData)

    @Query("SELECT * FROM rssi_data ORDER BY id DESC")
    suspend fun getAllData(): List<RssiData>

    @Query("SELECT * FROM rssi_data WHERE location = :location ORDER BY id DESC")
    suspend fun getRssiDataForLocation(location: String): List<RssiData>

    @Query("DELETE FROM rssi_data")
    suspend fun deleteAllData()
}


@Database(entities = [RssiData::class], version = 2) // Update version here
abstract class RssiDatabase : RoomDatabase() {
    abstract fun rssiDao(): RssiDao

    companion object {
        @Volatile
        private var INSTANCE: RssiDatabase? = null

        fun getDatabase(context: Context): RssiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RssiDatabase::class.java,
                    "rssi_database"
                ).fallbackToDestructiveMigration() // Allow Room to handle migrations
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}