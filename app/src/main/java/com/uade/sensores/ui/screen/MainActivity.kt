package com.uade.sensores.ui.screen

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uade.sensores.data.local.database.AppDatabase
import com.uade.sensores.data.remote.client.RetrofitClient
import com.uade.sensores.data.repository.MeasurementRepositoryImpl
import com.uade.sensores.sensor.AcelerometroReader
import com.uade.sensores.ui.theme.SensoresTheme
import com.uade.sensores.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = MeasurementRepositoryImpl(
            dao = db.measurementDao(),
            api = RetrofitClient.api
        )
        MainViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CYCLE", "onCreate ► la Activity BORN (una sola vez)")

        // Sensor lifecycle-aware: "listen alone" thanks to addObserver.
        // Inversion of Control: Activity doesn't call start/stop manually,
        // the reader reacts to the lifecycle by itself.
        val reader = AcelerometroReader(this) { measure ->
            viewModel.onNewMeasure(measure)
        }
        lifecycle.addObserver(reader)

        setContent {
            SensoresTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScreenSensor(
                        viewModel = viewModel,
                        onFinishClick = { finish() }
                    )
                }
            }
        }
    }

    // Life Cycle Logs to study the behavior
    override fun onStart()   { super.onStart();   Log.d("CYCLE", "onStart ► it is VISIBLE") }
    override fun onResume()  { super.onResume();  Log.d("CYCLE", "onResume ► has the FOCUS") }
    override fun onPause()   { super.onPause();   Log.d("CYCLE", "onPause ► lost focus (SOFT)") }
    override fun onStop()    { super.onStop();    Log.d("CYCLE", "onStop ► not visible anymore") }
    override fun onRestart() { super.onRestart(); Log.d("CYCLE", "onRestart ► returning from background") }
    override fun onDestroy() { super.onDestroy(); Log.d("CYCLE", "onDestroy ► DIES (to RAM never again)") }
}

@Composable
fun ScreenSensor(
    viewModel: MainViewModel,
    onFinishClick: () -> Unit
) {
    val measure by viewModel.measure
    val historical by viewModel.historical.collectAsState()
    val total by viewModel.total.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // === Live sensor read ===
        Text("X: %.2f".format(measure.x), style = MaterialTheme.typography.bodyLarge)
        Text("Y: %.2f".format(measure.y), style = MaterialTheme.typography.bodyLarge)
        Text("Z: %.2f".format(measure.z), style = MaterialTheme.typography.bodyLarge)
        Text(
            "%.1f G".format(measure.fuerzaG),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // === Action buttons ===
        Button(
            onClick = { viewModel.syncWithBackend() },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Sync with Supabase")
        }

        Button(
            onClick = onFinishClick,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Close app (finish)")
        }

        // === Persisted history from Room (also receives what comes from Supabase) ===
        Text(
            "History: $total stored measurements",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(historical) { m ->
                Text(
                    text = "G=%.1f | x=%.2f y=%.2f z=%.2f".format(m.fuerzaG, m.x, m.y, m.z),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}