package com.example.soundmeter

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.soundmeter.ui.theme.SoundMeterTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check microphone permission and request
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
        }

        setContent {
            SoundMeterTheme {
                SoundMeterScreen()
            }
        }
    }
}

// Function to capture and process audio
fun startRecording(context: Context, updateDbLevel: (Float) -> Unit, isRecordingState: () -> Boolean) {

    //context: access system services
    // heck if permission is granted and if not request
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            (context as ComponentActivity),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            200
        )
        return //if permission is not granted exit recording
    }

    //got help from google and ai...
    //the microphone takes 44,100 samples per second
    val sampleRate = 44100
    val bufferSize = AudioRecord.getMinBufferSize(
        //Records in one channel and meaning each sample takes 16 bits for higher accuracy
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    )

    //How much memory do I need to safely store audio data before processing it?
    // gets the answer from amdroid
    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
        return // exit if buffer size is invalid and bad value
    }

    //AudioRecord object
    val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC, // use device’s mic
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,  //one channel format
        AudioFormat.ENCODING_PCM_16BIT, //sample encoding
        bufferSize
    )

    val buffer = ShortArray(bufferSize)

    audioRecord.startRecording()

    //capture audio and update dB level continuously
    CoroutineScope(Dispatchers.Default).launch {
        while (isRecordingState()) {
            val readSize = audioRecord.read(buffer, 0, bufferSize)
            if (readSize > 0) {
                val amplitude = buffer.maxOrNull() ?: 1
                val decibels = 20 * kotlin.math.log10(amplitude.toDouble().coerceAtLeast(1.0))
                updateDbLevel(decibels.toFloat()) // Update UI
            }
            delay(200) // Update every 200ms
        }
    }
}



@Composable
fun SoundMeterScreen() {
    var isRecording by remember { mutableStateOf(false) }
    var dbLevel by remember { mutableStateOf(0f) }

    val context = LocalContext.current

    LaunchedEffect(isRecording) {
        if (isRecording) {
            startRecording(context, { newDbLevel -> dbLevel = newDbLevel }, { isRecording })
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        //display db by rounding to closest int to show in neat verion //original value has too much decimal points
        Text(text = "Sound Level: ${dbLevel.toInt()} dB", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(10.dp))

        //a bar to visualize db level
        LinearProgressIndicator(
            progress = {
                (dbLevel / 150f).coerceIn(0f, 1f) // Normalize between 0 - 1
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .padding(8.dp),
            color = when {
                //dynamically change color based on dB level
                dbLevel < 40 -> Color.Green
                dbLevel < 70 -> Color.Yellow
                else -> Color.Red
            },
            trackColor = Color(0xFFE0E0E0), // Background color

        )


        if(dbLevel.toInt()>70){
            Text(
                text = "Dangerous Level", style = MaterialTheme.typography.bodyLarge,
                color = Color.Red
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { isRecording = true },
            enabled = !isRecording,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            Text(text = "Check Decibel")
        }

        Spacer(modifier = Modifier.height(5.dp))

        Button(
            onClick = { isRecording = false },
            enabled = isRecording,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)

        ) {
            Text(text = "Stop")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SoundMeterTheme {
    }
}