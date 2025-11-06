package com.example.ruvirtual.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruvirtual.ui.theme.VirtualCardBackground // Import your custom color

@Composable
fun VirtualCard(nome: String, matricula: String, modifier: Modifier = Modifier) {
    val formattedMatricula = matricula.chunked(4).joinToString(" ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(VirtualCardBackground) // Updated to use custom color
            .padding(20.dp)
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
