package com.example.ruvirtual.ui.card

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruvirtual.ui.theme.VirtualCardBackground
import kotlinx.coroutines.launch

@Composable
fun VirtualCard(nome: String, matricula: String, modifier: Modifier = Modifier) {
    val formattedMatricula = matricula.chunked(4).joinToString(" ")
    var animationPlayed by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(-300f) }
    val scope = rememberCoroutineScope()
    var hapticEngine by remember { mutableStateOf<VibrationEffect?>(null) }
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        val numberOfPulses = 3
        val pulseDuration = 50L
        val spaceBetweenPulses = 100L
        val maxAmplitude = 255
        val timings = LongArray(numberOfPulses * 2)
        val amplitudes = IntArray(numberOfPulses * 2)
        for (i in 0 until numberOfPulses) {
            val amplitude = (maxAmplitude * (i + 1) / numberOfPulses)
            timings[i * 2] = spaceBetweenPulses
            timings[i * 2 + 1] = pulseDuration
            amplitudes[i * 2] = 0
            amplitudes[i * 2 + 1] = amplitude
        }
        hapticEngine = VibrationEffect.createWaveform(timings, amplitudes, -1)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VirtualCardBackground) // Updated to use custom color
            .padding(20.dp)
            .offset(x = offsetX.value.dp)
            .clickable {
                if (!animationPlayed) {
                    scope.launch {
                        offsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 300)
                        )
                    }
                    hapticEngine?.let {
                        val vibrator =
                            context.getSystemService(Vibrator::class.java)
                        vibrator.vibrate(it)
                    }
                    animationPlayed = true
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip do cartão
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = "Chip do cartão",
                    tint = MaterialTheme.colorScheme.onPrimary, // Updated to use theme color
                    modifier = Modifier.size(50.dp)
                )
                // Ícone de pagamento sem contato
                Icon(
                    imageVector = Icons.Filled.Nfc,
                    contentDescription = "Pagamento sem contato",
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), // Updated to use theme color
                    modifier = Modifier.size(32.dp)
                )
            }

            Column {
                Text(
                    text = formattedMatricula,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), // Updated to use theme color
                    fontSize = 22.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nome.uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary, // Updated to use theme color
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
