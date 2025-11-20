package com.example.okno.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/*
    LoginScreen jest pierwszym ekranem aplikacji. Umożliwia użytkownikowi:
       • wprowadzenie IP/portu routera,
       • podanie loginu i hasła,
       • uruchomienie procesu weryfikacji połączenia (SSL + Basic Auth + REST API).

    Ekran NIE kontaktuje się bezpośrednio z routerem — deleguje zadanie do LoginViewModel.
    ViewModel zwraca stan (loading, success, error), a ekran reaguje na niego
    i w razie sukcesu wywołuje onVerified(), co uruchamia przejście do DashboardScreen.
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    loginVM: LoginViewModel,
    onVerified: (ip: String, user: String, pass: String) -> Unit,  // callback do MainActivity
    onToggleTheme: () -> Unit,                                      // przełącznik Dark/Light
    isDark: Boolean                                                 // aktualny stan motywu
) {
    /*
        Pola formularza — przechowywane w pamięci Compose przez remember,
        ponieważ to UI-state, nie stan aplikacyjny.
    */
    var routerIp by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // stan aplikacyjny z ViewModel — (loading, verified, error)
    val ui by loginVM.ui.collectAsState()

    /*
        Jeśli ViewModel ustawi ui.verified = true, to automatycznie wykonujemy
        callback onVerified(), który przenosi użytkownika do DashboardScreen.
    */
    LaunchedEffect(ui.verified) {
        if (ui.verified) onVerified(routerIp, user, pass)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Logowanie do routera") },
                actions = {
                    TextButton(onClick = onToggleTheme) {
                        Text(if (isDark) "Jasny" else "Ciemny")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Pole IP routera
            OutlinedTextField(
                value = routerIp,
                onValueChange = { routerIp = it },
                label = { Text("Adres IP routera") },
                modifier = Modifier.fillMaxWidth()
            )

            // Pole loginu
            OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Login") },
                modifier = Modifier.fillMaxWidth()
            )

            // Pole hasła + przycisk show/hide
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Hasło") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = null)
                    }
                }
            )

            // Przycisk logowania
            Button(
                onClick = { loginVM.verify(routerIp, user, pass) },
                enabled = !ui.loading && user.isNotBlank() && pass.isNotBlank() && routerIp.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (ui.loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Łączę…")
                } else {
                    Text("Połącz")
                }
            }

            // Komunikat błędu (jeśli logowanie niepowiodło się)
            if (ui.error != null) {
                Text(
                    text = ui.error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
