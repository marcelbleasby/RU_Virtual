package com.bmo.mennu.ui.sessao

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bmo.mennu.data.Sessao
import com.bmo.mennu.data.model.Contexto

@Composable
fun ContextoScreen(sessao: Sessao, carregando: Boolean, erro: String?,
                   selecionar: (Contexto) -> Unit, atualizar: () -> Unit, sair: () -> Unit,
                   voltar: (() -> Unit)? = null) {
    BackHandler(enabled = voltar != null && !carregando) { voltar?.invoke() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Sua unidade", style = MaterialTheme.typography.headlineMedium)
        if (carregando || (!sessao.validada && erro == null)) {
            CircularProgressIndicator()
            Text("Validando sua sessão…")
        } else {
            Text(if (sessao.contextos.isEmpty()) "Nenhuma unidade disponível para sua conta. Atualize ou entre em contato com a instituição."
                else "Escolha onde deseja consultar suas refeições.")
            sessao.contextos.forEach { contexto ->
                Button(onClick = { selecionar(contexto) }, enabled = !carregando, modifier = Modifier.fillMaxWidth()) {
                    Text("${contexto.empresaNome ?: contexto.empresaId} · ${contexto.unidadeNome ?: contexto.unidadeId}")
                }
            }
        }
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = atualizar, enabled = !carregando) { Text("Atualizar") }
        voltar?.let { TextButton(onClick = it, enabled = !carregando) { Text("Voltar") } }
        TextButton(onClick = sair, enabled = !carregando) { Text("Sair") }
    }
}
