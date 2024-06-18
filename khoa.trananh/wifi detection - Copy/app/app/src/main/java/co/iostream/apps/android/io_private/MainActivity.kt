package co.iostream.apps.android.io_private

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import co.iostream.apps.android.io_private.ui.theme.IOTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = IOConfig.DATA_STORE_NAME)
val LocalNavController = compositionLocalOf<NavHostController> {
    error("No LocalNavController provided")
}

internal fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}
fun <T> DataStore<Preferences>.getValueFlow(
    key: Preferences.Key<T>, defaultValue: T
): Flow<Any?> {
    return this.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        preferences[key] ?: defaultValue
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private var scanResults by mutableStateOf<List<ScanResult>>(emptyList())
    private var scanStatus by mutableStateOf("")

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothScanReceiver: BroadcastReceiver
    private var bluetoothDevices by mutableStateOf<List<BluetoothDevice>>(emptyList())
    private var bluetoothScanStatus by mutableStateOf("")

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    finishAndRemoveTask()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            finish() // Exit the activity if Bluetooth is not supported
        }

        // Request storage permission for devices running Android R and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val getPermissionIntent = Intent()
                getPermissionIntent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startForResult.launch(getPermissionIntent)
            }
        } else {
            // Request storage permission for devices below Android R
            val storageRequestCode = 3655
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    storageRequestCode
                )
            }
        }

        // Initialize WiFiManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Initialize BluetoothManager and start Bluetooth scan if enabled
        if (bluetoothAdapter.isEnabled) {
            startBluetoothScan()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startForResult.launch(enableBtIntent)
        }

        // Request WiFi and Bluetooth permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_WIFI_PERMISSION
            )
        } else {
            startWifiScan()
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                REQUEST_BLUETOOTH_PERMISSION
            )
        } else {
            startBluetoothScan()
        }

        // Set content using Compose
        enableEdgeToEdge()
        setContent {
            IOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(text = scanStatus, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn {
                            items(scanResults) { result ->
                                val distance = calculateDistance(
                                    result.level.toDouble(),
                                    result.frequency.toDouble()
                                )
                                Text(
                                    text = "SSID: ${result.SSID}, BSSID: ${result.BSSID}, Signal Level: ${result.level}, Distance: ${
                                        "%.2f".format(
                                            distance
                                        )
                                    } meters",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Initialize MobileAds and configure test device ids
        MobileAds.initialize(this) {}
        val testDeviceIds = listOf(
            "F9F813CC38E15FF9383417B304B4D3F5",
        )
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WIFI_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startWifiScan()
                } else {
                    Toast.makeText(this, "WiFi permission denied", Toast.LENGTH_SHORT).show()
                    scanStatus = "WiFi permission denied"
                }
            }
            REQUEST_BLUETOOTH_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBluetoothScan()
                } else {
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
                    bluetoothScanStatus = "Bluetooth permission denied"
                }
            }
        }
    }

    private fun startWifiScan() {
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val success =
                    intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false) ?: false
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            scanFailure()
        } else {
            scanStatus = "Scanning WiFi..."
        }
    }

    private fun scanSuccess() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scanResults = wifiManager.scanResults
            scanStatus = "Scan Success"
            Toast.makeText(this, "Scan Success", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scanFailure() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scanResults = emptyList() // Clear previous results
            scanStatus = "Scan Failed"
            Toast.makeText(this, "Scan Failure", Toast.LENGTH_SHORT).show()
        }
    }
    private fun startBluetoothScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                REQUEST_BLUETOOTH_PERMISSION
            )
        } else {
            bluetoothScanReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device =
                                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            device?.let {
                                bluetoothDevices = bluetoothDevices + it
                            }
                        }
                    }
                }
            }
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(bluetoothScanReceiver, filter)

            val success = bluetoothAdapter.startDiscovery()
            if (!success) {
                bluetoothScanStatus = "Bluetooth Scan Failed"
            } else {
                bluetoothScanStatus = "Scanning Bluetooth..."
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Unregister receivers to avoid memory leaks
        unregisterReceiver(wifiScanReceiver)
        unregisterReceiver(bluetoothScanReceiver)
    }

    override fun attachBaseContext(newBase: Context) {
        val systemLang = newBase.resources.configuration.locales.get(0)

        val sharedPreferences = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString(
            "SelectedLanguage", systemLang.language
        )
        val locale = Locale(language!!)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    fun calculateDistance(signalLevelInDb: Double, freqInMHz: Double): Double {
        val exp = (27.55 - (20 * log10(freqInMHz)) + abs(signalLevelInDb)) / 20.0
        return 10.0.pow(exp)
    }

    companion object {
        private const val REQUEST_WIFI_PERMISSION = 1
        private const val REQUEST_BLUETOOTH_PERMISSION = 2
    }
}
