package com.example.ruvirtual.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.ruvirtual.data.UserDataHolder // Adicionar import para UserDataHolder
import com.example.ruvirtual.data.model.ProvisionResponse // Esta importação está sendo usada agora internamente pelo UserDataHolder
import com.example.ruvirtual.data.model.LoginResult // Assumindo que LoginResult está neste pacote
import com.example.ruvirtual.ui.theme.RUVirtualTheme // Importar seu tema

// Importe stringResource e colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
// Se você definiu cores no tema M3:
import androidx.compose.material3.MaterialTheme
import com.example.ruvirtual.R // Importa a classe R para acessar recursos

@OptIn(ExperimentalMaterial3Api::class) // Adicionado para resolver o aviso da API experimental
@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = hiltViewModel()) {
    val matricula by viewModel.matricula.collectAsState()
    val senha by viewModel.senha.collectAsState()
    val loginResult by viewModel.loginResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isMatriculaError by viewModel.isMatriculaError.collectAsState()
    val isSenhaError by viewModel.isSenhaError.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(loginResult) {
        loginResult?.let {
            // VERIFICAR ESTA LÓGICA DE NAVEGAÇÃO E SUCESSO
            // Conforme discutido anteriormente, CardScreen agora busca os dados do UserDataHolder.
            // A navegação deve ser simplesmente para a rota "card", e a condição de sucesso
            // deve ser baseada na presença de dados críticos (ex: matricula não nula).
            if (it.matricula != null && it.nome != null) { // Indicador de login bem-sucedido
                // Certifique-se de que o UserDataHolder foi preenchido pelo ViewModel aqui ou antes.
                // Exemplo: viewModel.saveLoginData(it)
                // Se o ViewModel já salva em UserDataHolder e 'it' é apenas uma confirmação
                navController.navigate("card") // Rota simplificada e correta
                viewModel.onNavigated() // Reset the navigation trigger
            } else {
                // Se loginResult não é nulo mas indica falha, talvez você queira mostrar um Toast genérico aqui
                // se o ViewModel não tiver setado um errorMessage específico.
                // Isso pode ser uma falha interna, não um erro do formulário.
                if (errorMessage == null) { // Evitar sobreposição se já houver um erro específico
                    // Toast.makeText(context, "Falha na autenticação. Tente novamente.", Toast.LENGTH_LONG).show() // String hardcoded
                }
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onErrorMessageShown() // Clear the error message after showing
        }
    }
    RUVirtualTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background), // Usar cor do tema
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = matricula,
                onValueChange = viewModel::onMatriculaChange,
                label = { Text(stringResource(R.string.label_matricula)) }, // String hardcoded
                isError = isMatriculaError,
                supportingText = {
                    if (isMatriculaError) {
                        Text(stringResource(R.string.error_matricula_empty), color = MaterialTheme.colorScheme.error) // String hardcoded
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp)) // Dimensão hardcoded
            OutlinedTextField(
                value = senha,
                onValueChange = viewModel::onSenhaChange,
                label = { Text(stringResource(R.string.label_senha)) }, // String hardcoded
                visualTransformation = PasswordVisualTransformation(),
                isError = isSenhaError,
                supportingText = {
                    if (isSenhaError) {
                        Text(stringResource(R.string.error_senha_empty), color = MaterialTheme.colorScheme.error) // String hardcoded
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp)) // Dimensão hardcoded
            Button(
                onClick = viewModel::onLoginClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary // Usar cor primária do tema
                )
            ) {
                Text(stringResource(R.string.button_entrar), color = MaterialTheme.colorScheme.onPrimary) // String hardcoded
            }
        }
    }
}