package com.example.ruvirtual.ui.card

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale


fun formatIsoToLocal(iso: String?): String {
    return try {
        if (iso.isNullOrBlank()) return "Data indisponível"

        // Replace 'Z' (UTC) with '+0000' for SimpleDateFormat compatibility
        val cleanedIso = iso.take(iso.length - 1) + "+0000"

        val isoFormatWithMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
        val isoFormatWithoutMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())

        val date = try {
            isoFormatWithMillis.parse(cleanedIso)
        } catch (e: ParseException) {
            try {
                isoFormatWithoutMillis.parse(cleanedIso)
            } catch (e: ParseException) {
                Log.e("formatIsoToLocal", "Failed to parse date: $iso", e)
                null
            }
        }

        if (date == null) {
            Log.e("formatIsoToLocal", "Parsed date is null for: $iso")
            return iso ?: "Data inválida"
        }

        // Format to local date and time using device's default timezone
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        Log.e("formatIsoToLocal", "Exception during date formatting: $iso", e)
        iso ?: "Data inválida"
    }
}

@Composable
fun CardScreen(navController: NavHostController, viewModel: CardViewModel = hiltViewModel()) {
    val provisionResponse by viewModel.provisionResponse.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.errorMessageShown() // Clear the error message after showing
        }
    }


    // If provisionResponse is null AND it's not currently refreshing (initial load failed or no data)
    if (provisionResponse == null && !isRefreshing) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Erro: dados do usuário não carregados", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tentar Recarregar",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { viewModel.refreshCardData(true) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Voltar ao Login",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { navController.popBackStack("login", inclusive = false) }
            )
        }
        return
    }

    // If provisionResponse is null but still refreshing (initial load in progress)
    if (provisionResponse == null) { // This case handles when initial data is null but a refresh is in progress
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show a simple loading indicator or message during initial refresh
            Text("Carregando dados do cartão...", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
        }
        return
    }

    val nome = provisionResponse!!.nome ?: "Usuário Desconhecido"
    val matricula = provisionResponse!!.matricula ?: "Matrícula Não Informada"
    val creditos = provisionResponse!!.creditos ?: 0
    val transacoes = provisionResponse!!.transacoes ?: emptyList()

    var showNfcAnimation by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "NFC_Animation")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha_animation"
    )

    val onPayClick: () -> Unit = {
        showNfcAnimation = !showNfcAnimation
        // TODO: Implement actual payment logic here
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RU Card",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Sair da conta",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable {
                        navController.popBackStack("login", inclusive = false)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Olá, $nome!",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Seu cartão está pronto para uso",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Refeições Disponíveis e Dados do Usuário
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Refeições disponíveis",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                            val availableMeals = creditos.toInt()

                            Text(
                                text = "$availableMeals",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Ícone de cartão de refeição",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Matrícula",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                text = matricula,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Nome",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                text = nome,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Card 2: VirtualCard com Indicativo de Pagamento
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { onPayClick() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary // Using primaryDark from the new palette for the virtual card
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top row with chip and NFC icons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = "Chip do cartão",
                            tint = MaterialTheme.colorScheme.onPrimary, // Using onPrimaryDark for icons on the virtual card
                            modifier = Modifier.size(40.dp)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "Símbolo NFC",
                                tint = MaterialTheme.colorScheme.onPrimary, // Using onPrimaryDark for icons on the virtual card
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "NFC",
                                color = MaterialTheme.colorScheme.onPrimary, // Using onPrimaryDark for text on the virtual card
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Card number (matricula) and name
                    Column {
                        Text(
                            text = matricula,
                            color = MaterialTheme.colorScheme.onPrimary, // Using onPrimaryDark for text on the virtual card
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = nome.uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), // Using onPrimaryDark for text on the virtual card
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Payment indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showNfcAnimation) {
                            Text(
                                text = "• Toque para pagar",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = animatedAlpha), // Using onPrimaryDark for text on the virtual card
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "Toque no cartão para ativar pagamento",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), // Using onPrimaryDark for text on the virtual card
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Lista de Transações Recentes
            Text(
                text = "Transações recentes",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Verifica se há transações antes de exibir a LazyColumn
            if (transacoes.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(transacoes) { transaction ->
                        val isRecarga = transaction.valor >= 0
                        val iconTint = if (isRecarga) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        val iconBackground = if (isRecarga) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isRecarga) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = "Ícone de transação de ${transaction.tipo} ${transaction.local ?: ""}", // Updated
                                        tint = iconTint,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(iconBackground)
                                            .padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.size(16.dp))
                                    Column {
                                        Text(
                                            text = transaction.tipo, // Updated
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = formatIsoToLocal(transaction.data), // 'data' is correct
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Text(
                                    text = (if (isRecarga) "+ " else "- ") + "R$ %.2f".format(kotlin.math.abs(transaction.valor)), // Updated
                                    color = iconTint,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Nenhuma transação recente.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }


    }
}
