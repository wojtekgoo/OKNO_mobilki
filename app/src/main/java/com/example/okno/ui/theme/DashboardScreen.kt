package com.example.okno.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import com.example.okno.network.RetrofitProvider

/*
    DashboardScreen to główny ekran aplikacji po zalogowaniu do routera.

    Na tym ekranie użytkownik może:
      • zobaczyć status i komunikaty aplikacji,
      • przełączać widoczność:
          - zasobów systemowych routera (CPU, RAM, uptime),
          - listy interfejsów,
          - listy dzierżaw DHCP (IP + host-name),
      • przejść do ekranu wykresów ("Graph"),
      • przełączyć motyw (Dark / Light),
      • wyczyścić sesję i wrócić do ekranu logowania ("Clear Session").

    Ekran opiera się o:
      • SessionViewModel  – przechowuje aktualne dane sesji (IP, login, hasło)
      • MainViewModel     – pobiera dane z API (interfejsy, leases, system resources)
      • GraphViewModel    – wykresy i próbki ruchu (obsługiwane na innym ekranie)
      • Scaffold + Column + przyciski + karty (Compose UI)

    Jest to typowy ekran „menu + dane”, oparty o Jetpack Compose.
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    sessionVM: SessionViewModel,
    mainVM: MainViewModel,
    graphVM: GraphViewModel,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit,
    onShowGraph: () -> Unit,
    isDark: Boolean
) {
    // 𝗦𝘁𝗮𝗻 𝗼𝗯𝘀𝗲𝗿𝘄𝗼𝘄𝗮𝗻𝘆 𝗽𝗿𝘇𝗲𝘇 𝗨𝗜 (Flow → State)
    val status by mainVM.status.collectAsState()
    val ifaces by mainVM.interfaces.collectAsState()
    val leases by mainVM.leases.collectAsState()

    // Proste flagi: czy pokazujemy interfejsy / leases
    val interfacesShown = ifaces.isNotEmpty()
    val leasesShown = leases.isNotEmpty()

    val systemResources by mainVM.systemResources.collectAsState()
    val sysShown = systemResources != null

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            // Górny pasek z tytułem i przyciskami akcji (theme + clear session)
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Panel Routera",
                        fontSize = 20.sp
                    )
                },

                actions = {
                    // Przycisk zmiany motywu (Dark/Light)
                    TextButton(onClick = onToggleTheme) {
                        Text(
                            if (isDark) "Jasny" else "Ciemny",
                            fontSize = 15.sp
                        )
                    }

                    // Przycisk "Clear Session" — czyści stan i wraca do logowania
                    TextButton(
                        onClick = {
                            mainVM.reset()              // czyści dane z MainViewModel
                            RetrofitProvider.clear()    // czyści cache klienta Retrofit/API
                            sessionVM.clearSession()    // usuwa IP, login, hasło
                            onLogout()                  // nawigacja do ekranu logowania
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    {
                        Text(
                            "Zakończ sesję",
                            fontSize = 15.sp
                        )
                    }
                }
            )
        }
    ) { inner ->
        // Cały Dashboard jest przewijalny w pionie
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(scrollState)    // cały ekran można przewinąć w dół
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        )
        {
            // =========================================================================
            // 𝗣𝗿𝘇𝘆𝗰𝗶𝘀𝗸: 𝗦𝘆𝘀𝘁𝗲𝗺 𝗿𝗲𝘀𝗼𝘂𝗿𝗰𝗲𝘀
            // =========================================================================
            Button(
                onClick = {
                    mainVM.toggleSystemResources(
                        sessionVM.routerIp.value!!,
                        sessionVM.username.value!!,
                        sessionVM.password.value!!
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sysShown)
                        MaterialTheme.colorScheme.error       // czerwony przycisk gdy już pokazujemy (hide)
                    else
                        MaterialTheme.colorScheme.primary,    // niebieski gdy możemy "Show"
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (sysShown) "Ukryj zasoby systemowe" else "Pokaż zasoby systemowe")
            }

            // =========================================================================
            // 𝗣𝗿𝘇𝘆𝗰𝗶𝘀𝗸: 𝗜𝗻𝘁𝗲𝗿𝗳𝗮𝗰𝗲𝘀
            // =========================================================================
            Button(
                onClick = {
                    mainVM.toggleInterfaces(
                        sessionVM.routerIp.value!!,
                        sessionVM.username.value!!,
                        sessionVM.password.value!!
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (interfacesShown)
                        MaterialTheme.colorScheme.error          // czerwony, gdy kliknięcie = hide
                    else
                        MaterialTheme.colorScheme.primary,       // niebieski, gdy kliknięcie = show
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (interfacesShown) "Ukryj interfejsy" else "Pokaż interfejsy")
            }

            // =========================================================================
            // 𝗣𝗿𝘇𝘆𝗰𝗶𝘀𝗸: 𝗗𝗛𝗖𝗣 𝗟𝗲𝗮𝘀𝗲𝘀 (IP addresses)
            // =========================================================================
            Button(
                onClick = {
                    mainVM.toggleLeases(
                        sessionVM.routerIp.value,
                        sessionVM.username.value!!,
                        sessionVM.password.value!!
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (leasesShown)
                        MaterialTheme.colorScheme.error          // czerwony, gdy ukrywamy
                    else
                        MaterialTheme.colorScheme.primary,       // niebieski, gdy pokazujemy
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (leasesShown) "Ukryj adresy IP" else "Pokaż adresy IP")
            }

            // =========================================================================
            // 𝗣𝗿𝘇𝘆𝗰𝗶𝘀𝗸: 𝗚𝗿𝗮𝗽𝗵 (przejście do ekranu wykresów)
            // =========================================================================
            Button(
                onClick = onShowGraph,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wykres")
            }

            Divider()

            // =========================================================================
            // 𝗦𝘁𝗮𝘁𝘂𝘀 𝗮𝗽𝗹𝗶𝗸𝗮𝗰𝗷𝗶
            // =========================================================================
            Text("Status:", style = MaterialTheme.typography.titleMedium)
            Text(status, color = MaterialTheme.colorScheme.primary)

            // =========================================================================
            // 𝗦𝘆𝘀𝘁𝗲𝗺 𝗿𝗲𝘀𝗼𝘂𝗿𝗰𝗲𝘀 (CPU, RAM, uptime) – tylko gdy sysShown == true
            // =========================================================================
            if (sysShown && systemResources != null) {
                Spacer(Modifier.height(8.dp))
                Text("System resources:", style = MaterialTheme.typography.titleMedium)
                Text(systemResources!!, style = MaterialTheme.typography.bodyMedium)
            }

            // =========================================================================
            // 𝗟𝗶𝘀𝘁𝗮 𝗶𝗻𝘁𝗲𝗿𝗳𝗲𝗷𝘀𝗼́𝘄 – tylko gdy coś zostało pobrane
            // =========================================================================
            if (interfacesShown) {
                Text("Interfejsy:", style = MaterialTheme.typography.titleMedium)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ifaces.forEach { name ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "• $name",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 𝗟𝗶𝘀𝘁𝗮 𝗗𝗛𝗖𝗣 𝗟𝗲𝗮𝘀𝗲𝘀 – IP + host-name
            // =========================================================================
            if (leasesShown) {
                Text("Przydzielone adresy IP:", style = MaterialTheme.typography.titleMedium)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    leases.forEach { (addr, host) ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "$addr — $host",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // W razie potrzeby można tu odkomentować GraphCard()
            // aby wykres był częścią tego samego ekranu, zamiast osobnego.
            // GraphCard(graphVM = graphVM)
        }
    }
}
