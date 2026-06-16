package com.uade.sensores.ui.screen

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uade.sensores.sensor.AcelerometroReader
import com.uade.sensores.ui.theme.SensoresTheme
import com.uade.sensores.ui.viewmodel.MainViewModel
import com.uade.sensores.data.local.AppDatabase
import com.uade.sensores.data.repository.MeasurementRepositoryImpl
import com.uade.sensores.data.remote.RetrofitClient

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

        // Sensor lifecycle-aware: "listen alone" thanks to the addObserver.
        // Control Inversion: Activity do not call to start/stop manually,
        // reader react to the lifecycle itself.
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

    // Life Cyclo Logs for study the behavior
    override fun onStart()   { super.onStart();   Log.d("CYCLO", "onStart ► it is VISIBLE") }
    override fun onResume()  { super.onResume();  Log.d("CYCLO", "onResume ► have the FOCUS") }
    override fun onPause()   { super.onPause();   Log.d("CYCLO", "onPause ► lost focus (SOFT)") }
    override fun onStop()    { super.onStop();    Log.d("CYCLO", "onStop ► its not visible") }
    override fun onRestart() { super.onRestart(); Log.d("CYCLO", "onRestart ► return to the second plane") }
    override fun onDestroy() { super.onDestroy(); Log.d("CYCLO", "onDestroy ► DIE (to the RAM never again)") }
}

@Composable
fun ScreenSensor(
    viewModel: MainViewModel,
    onFinishClick: () -> Unit
) {
    // Compose observed the ViewModel state
    // Survived the rotation because the ViewModel
    val measure by viewModel.measure

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "X: %.2f".format(measure.x),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Y: %.2f".format(measure.y),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Z: %.2f".format(measure.z),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "%.1f".format(measure.fuerzaG),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Button(onClick = onFinishClick) {
            Text("Close app (finish)")
        }
    }
}