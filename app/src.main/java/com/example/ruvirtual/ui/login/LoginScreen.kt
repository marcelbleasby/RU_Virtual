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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.ruvirtual.data.model.LoginResult
import com.example.ruvirtual.ui.theme.RUVirtualTheme

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.colorResource
import androidx.compose.material3.MaterialTheme
import com.example.ruvirtual.R

@OptIn(ExperimentalMaterial3Api::class)
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
            if (it.matricula != null && it.nome != null) {
                navController.navigate("card")
                viewModel.onNavigated()
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onErrorMessageShown()
        }
    }
    RUVirtualTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = matricula,
                onValueChange = viewModel::onMatriculaChange,
                label = { Text(stringResource(R.string.label_matricula)) },
                isError = isMatriculaError,
                supportingText = {
                    if (isMatriculaError) {
                        Text(stringResource(R.string.error_matricula_empty), color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = senha,
                onValueChange = viewModel::onSenhaChange,
                label = { Text(stringResource(R.string.label_senha)) },
                visualTransformation = PasswordVisualTransformation(),
                isError = isSenhaError,
                supportingText = {
                    if (isSenhaError) {
                        Text(stringResource(R.string.error_senha_empty), color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = viewModel::onLoginClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.button_entrar), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
