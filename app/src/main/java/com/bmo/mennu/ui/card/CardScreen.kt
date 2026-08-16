package com.bmo.mennu.ui.card

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
import androidx.navigation.NavHostController
import com.bmo.mennu.data.model.RefeicaoServida
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.errorMessageShown() // Clear the error message after showing
        }
    }

    // Se não há estado carregado e não está atualizando: sessão ausente/expirada.
    if (uiState == null && !isRefreshing) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Erro: dados do usuário não carregados", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
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

    if (uiState == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Carregando dados do cartão...", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val state = uiState!!
    val nome = state.user?.nome ?: "Usuário Desconhecido"
    val identificador = state.user?.matricula ?: state.user?.email ?: "-"

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
                    style = MaterialTheme.typography.headlineSmall
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Sair da conta",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable {
                        viewModel.onLogoutClicked()
                        navController.popBackStack("login", inclusive = false)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Olá, $nome!",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Seu cartão está pronto para uso",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Consumo do período e dados do usuário
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
                                text = "Refeições este mês",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (state.consumoIndisponivel) {
                                Text(
                                    text = "Indisponível",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            } else {
                                Text(
                                    text = "${state.refeicoesEsteMes ?: 0}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.displayMedium
                                )
                            }
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = identificador,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Nome",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = nome,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyLarge,
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
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Card number (identificador) and name
                    Column {
                        Text(
                            text = identificador,
                            color = MaterialTheme.colorScheme.onPrimary, // Using onPrimaryDark for text on the virtual card
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = nome.uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), // Using onPrimaryDark for text on the virtual card
                            style = MaterialTheme.typography.bodyLarge
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
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                text = "Toque no cartão para ativar pagamento",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), // Using onPrimaryDark for text on the virtual card
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Histórico de refeições servidas
            Text(
                text = "Refeições recentes",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (state.consumoIndisponivel) {
                Text(
                    text = "Histórico indisponível para esta conta.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else if (state.historico.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(state.historico) { refeicao ->
                        RefeicaoServidaItem(refeicao)
                    }
                }
            } else {
                Text(
                    text = "Nenhuma refeição registrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RefeicaoServidaItem(refeicao: RefeicaoServida) {
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
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Refeição servida em ${refeicao.unidadeNome ?: "unidade desconhecida"}",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        text = refeicao.unidadeNome ?: "Refeição",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = formatIsoToLocal(refeicao.dataHora),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (refeicao.manual) {
                Text(
                    text = "Manual",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
