package com.bmo.mennu.ui.card

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.bmo.mennu.data.NfcHardwareState
import com.bmo.mennu.data.model.RefeicaoServida
import com.bmo.mennu.nfc.MennuHostApduService
import com.bmo.mennu.ui.components.AppHeader
import com.bmo.mennu.ui.components.LastSyncedText
import com.bmo.mennu.ui.components.OfflineBanner
import com.bmo.mennu.ui.components.PullToRefreshContent
import com.bmo.mennu.ui.navigation.Screen
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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

private fun parseIsoToDate(iso: String?): Date? {
    if (iso.isNullOrBlank()) return null
    return try {
        val cleanedIso = iso.take(iso.length - 1) + "+0000"
        val withMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
        val withoutMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
        try {
            withMillis.parse(cleanedIso)
        } catch (e: ParseException) {
            withoutMillis.parse(cleanedIso)
        }
    } catch (e: Exception) {
        Log.e("CardScreen", "Falha ao parsear data: $iso", e)
        null
    }
}

// Dia relativo (Hoje/Ontem/dd/MM) pra linha de transação do novo design.
fun formatRelativeDay(iso: String?): String {
    val date = parseIsoToDate(iso) ?: return "Data indisponível"
    val target = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when {
        sameDay(target, today) -> "Hoje"
        sameDay(target, yesterday) -> "Ontem"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
}

fun formatHoraLocal(iso: String?): String {
    val date = parseIsoToDate(iso) ?: return "--:--"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

// APROXIMADO/MOCK — RefeicaoServida não carrega tipo de refeição (ver comentário no
// model RefeicaoServida.kt). Heurístico cosmético a partir do horário; não é dado
// oficial da API e não deve alimentar nenhuma lógica de negócio/contabilização.
fun inferMealTypeFromTime(iso: String?): String {
    val hour = parseIsoToDate(iso)?.let { Calendar.getInstance().apply { time = it }.get(Calendar.HOUR_OF_DAY) }
        ?: return "Refeição"
    return when {
        hour < 10 -> "Café da manhã"
        hour < 16 -> "Almoço"
        else -> "Jantar"
    }
}

private fun mealTypeIcon(mealType: String): ImageVector = when (mealType) {
    "Café da manhã" -> Icons.Filled.FreeBreakfast
    "Almoço" -> Icons.Filled.LunchDining
    "Jantar" -> Icons.Filled.DinnerDining
    else -> Icons.Filled.LunchDining
}

// Agrupa da direita pra esquerda: garante que o último grupo tenha sempre os últimos
// `size` caracteres reais, independente do comprimento total da string.
private fun chunkFromEnd(value: String, size: Int): List<String> =
    value.reversed().chunked(size).map { it.reversed() }.reversed()

// Mesma convenção de agrupamento de VirtualCard.kt (blocos de 4), mascarando tudo
// exceto os últimos 4 caracteres reais, estilo "•••• •••• •••• 4821".
fun maskMatricula(matricula: String?): String {
    val raw = matricula.orEmpty()
    if (raw.isBlank()) return "•••• •••• •••• ••••"
    val groups = chunkFromEnd(raw, 4)
    return groups.mapIndexed { index, group ->
        if (index == groups.lastIndex) group else "•".repeat(group.length)
    }.joinToString(" ")
}

@Composable
fun CardScreen(navController: NavHostController, viewModel: CardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val tapDetected by viewModel.tapDetected.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.errorMessageShown() // Clear the error message after showing
        }
    }

    LaunchedEffect(tapDetected) {
        if (tapDetected != null) {
            Toast.makeText(context, "Cartão lido com sucesso!", Toast.LENGTH_SHORT).show()
            viewModel.tapEventShown()
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
                modifier = Modifier.clickable {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
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
    val cartaoNaoProvisionado = state.user?.vCardId.isNullOrBlank()

    val emulationAtiva by viewModel.emulationAtiva.collectAsStateWithLifecycle()
    val nfcState by viewModel.nfcHardwareState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, cartaoNaoProvisionado) {
        // currentNfcState: leitura síncrona dentro deste efeito (o StateFlow do
        // ViewModel só reflete o valor novo na próxima recomposição, não é seguro
        // reler nfcState logo após viewModel.onNfcHardwareStateChanged(...)).
        var currentNfcState = NfcHardwareState.ATIVO

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            currentNfcState = NfcHardwareState.NAO_SUPORTADO
            viewModel.onNfcHardwareStateChanged(currentNfcState)
            return@DisposableEffect onDispose { }
        }
        if (cartaoNaoProvisionado) {
            return@DisposableEffect onDispose { }
        }
        val activity = context as? Activity
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val cardEmulation = if (activity != null && nfcAdapter != null) {
            runCatching { CardEmulation.getInstance(nfcAdapter) }.getOrNull()
        } else null

        fun refreshNfcState() {
            currentNfcState = when {
                nfcAdapter == null -> NfcHardwareState.NAO_SUPORTADO
                !nfcAdapter.isEnabled -> NfcHardwareState.DESLIGADO
                else -> NfcHardwareState.ATIVO
            }
            viewModel.onNfcHardwareStateChanged(currentNfcState)
        }
        refreshNfcState()

        if (activity == null || cardEmulation == null) {
            return@DisposableEffect onDispose { }
        }
        val componentName = ComponentName(context, MennuHostApduService::class.java)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    refreshNfcState()
                    if (currentNfcState == NfcHardwareState.ATIVO) {
                        runCatching { cardEmulation.setPreferredService(activity, componentName) }
                            .onSuccess { viewModel.onEmulationActiveChanged(true) }
                            .onFailure {
                                viewModel.onEmulationActiveChanged(false)
                                Log.w("CardScreen", "Falha ao definir serviço NFC preferencial", it)
                            }
                    } else {
                        viewModel.onEmulationActiveChanged(false)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    runCatching { cardEmulation.unsetPreferredService(activity) }
                        .onFailure { Log.w("CardScreen", "Falha ao remover serviço NFC preferencial", it) }
                    viewModel.onEmulationActiveChanged(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { cardEmulation.unsetPreferredService(activity) }
            viewModel.onEmulationActiveChanged(false)
        }
    }

    PullToRefreshContent(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshCardData() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            AppHeader(
                userName = nome,
                onAvatarClick = {
                    viewModel.onLogoutClicked()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )

            Column(modifier = Modifier.padding(16.dp)) {
                if (!isOnline) {
                    OfflineBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Meu Cartão",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Saldo de créditos e histórico de refeições",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LastSyncedText(lastSyncedAt = lastSyncedAt)
                Spacer(modifier = Modifier.height(20.dp))

            val planoInfo = state.planoInfo
            CartaoMennuCard(
                nomePlano = planoInfo?.nomePlano ?: "—",
                creditosDisponiveis = planoInfo?.creditosDisponiveis ?: 0,
                emulationAtiva = emulationAtiva,
                numeroMascarado = maskMatricula(state.user?.matricula),
                nomeTitular = nome,
                dataRenovacao = planoInfo?.dataRenovacaoExibicao ?: "—"
            )

            if (cartaoNaoProvisionado) {
                Spacer(modifier = Modifier.height(12.dp))
                CartaoNaoProvisionadoAviso()
            } else if (nfcState != NfcHardwareState.ATIVO) {
                Spacer(modifier = Modifier.height(12.dp))
                NfcDesabilitadoAviso(
                    state = nfcState,
                    onAbrirConfiguracoes = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    icon = Icons.AutoMirrored.Filled.CallMade,
                    iconTint = MaterialTheme.colorScheme.error,
                    label = "Consumidos",
                    value = state.refeicoesEsteMes?.toString() ?: "-",
                    caption = "créditos este mês",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    icon = Icons.AutoMirrored.Filled.CallReceived,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    label = "Disponível",
                    value = planoInfo?.creditosDisponiveis?.toString() ?: "-",
                    caption = "créditos restantes",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Histórico de transações",
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
                    items(state.historico, key = { it.id }) { refeicao ->
                        TransacaoItem(refeicao)
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
}

@Composable
private fun CartaoMennuCard(
    nomePlano: String,
    creditosDisponiveis: Int,
    emulationAtiva: Boolean,
    numeroMascarado: String,
    nomeTitular: String,
    dataRenovacao: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // Círculos decorativos translúcidos do mockup — atrás do conteúdo.
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-70).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.06f))
        )

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cartão Mennu",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Contactless,
                        contentDescription = "Pagamento por aproximação",
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (emulationAtiva) 1f else 0.4f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (emulationAtiva) "Ativo" else "Inativo",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (emulationAtiva) 0.85f else 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = nomePlano,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Saldo disponível",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$creditosDisponiveis",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "créditos",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(numeroMascarado, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = nomeTitular.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Renova em", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Text(dataRenovacao, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CartaoNaoProvisionadoAviso(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CreditCardOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Cartão NFC não configurado",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Seu cadastro ainda não tem uma credencial NFC vinculada. Procure o RH para habilitar o pagamento por aproximação.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NfcDesabilitadoAviso(
    state: NfcHardwareState,
    onAbrirConfiguracoes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clicavel = state == NfcHardwareState.DESLIGADO
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (clicavel) it.clickable { onAbrirConfiguracoes() } else it },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (state == NfcHardwareState.DESLIGADO) "NFC desativado" else "Sem suporte a NFC",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (state == NfcHardwareState.DESLIGADO)
                        "Ative o NFC nas configurações do aparelho para pagar por aproximação. Toque aqui para abrir as configurações."
                    else
                        "Este aparelho não tem suporte a pagamento por aproximação. Use o QR Code para registrar suas refeições.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TransacaoItem(refeicao: RefeicaoServida) {
    val mealType = inferMealTypeFromTime(refeicao.dataHora)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(mealTypeIcon(mealType), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(mealType, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${formatRelativeDay(refeicao.dataHora)} · ${formatHoraLocal(refeicao.dataHora)} · ${refeicao.unidadeNome ?: "Unidade desconhecida"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text("-1", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}
