package com.bmo.mennu.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.bmo.mennu.presentation.viewmodel.MainViewModel
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(LocalContext.current)
            )
            val vCardId by viewModel.vCardId.collectAsState()
            val refeicoesMes by viewModel.refeicoesMes.collectAsState()
            WearApp(vCardId = vCardId, refeicoesMes = refeicoesMes)
        }
    }
}

@Composable
fun WearApp(vCardId: String?, refeicoesMes: Int?) {
    val context = LocalContext.current
    val remoteActivityHelper = RemoteActivityHelper(context)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (refeicoesMes != null || vCardId != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (refeicoesMes != null) {
                    Text(
                        text = "Refeições este mês:",
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$refeicoesMes",
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (vCardId != null) {
                    Text(
                        text = "VCard ID:",
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = vCardId,
                        style = MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Por favor cadastre no app primeiro",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                        nodes.firstOrNull()?.let { node ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                addCategory(Intent.CATEGORY_LAUNCHER)
                                setClassName(
                                    "com.bmo.mennu",
                                    "com.bmo.mennu.MainActivity"
                                )
                            }
                            remoteActivityHelper.startRemoteActivity(intent, node.id)
                        }
                    }
                }) {
                    Text(text = "Abra o app no seu smartphone")
                }
            }
        }
    }
}
