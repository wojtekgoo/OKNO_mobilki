package com.example.okno.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState

/*
    GraphScreen jest ekranem, który odpowiada za:

      • wybór interfejsu sieciowego,
      • załadowanie próbek dla wybranego interfejsu z bazy danych,
      • wyświetlenie wykresu (GraphCard) na podstawie danych z GraphViewModel,
      • przełączanie jasnego/ciemnego motywu,
      • powrót do ekranu Dashboard.

    Sam wykres nie jest tu rysowany — robi to GraphCard.
    GraphScreen jest "kontrolerem widoku", który:
      - pobiera listę interfejsów od MainViewModel,
      - pozwala użytkownikowi wybrać interfejs z listy,
      - przekazuje decyzję do GraphViewModel (loadSamples),
      - wyświetla GraphCard.

    Przeplyw danych wyglada nastepujaco:
       Router → Retrofit → DB (GraphRepository) → GraphViewModel → GraphScreen → GraphCard
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    sessionVM: SessionViewModel,         // dane logowania i aktywny router
    mainVM: MainViewModel,               // lista interfejsów pobrana z API
    graphVM: GraphViewModel,             // odpowiada za pobieranie próbek z DB
    onBack: () -> Unit,                  // callback – powrót do Dashboard
    onToggleTheme: () -> Unit,           // callback – przełącz jasny/ciemny motyw
    isDark: Boolean
) {
    // Lista interfejsów pobrana wcześniej przez Dashboard (Show Interfaces)
    val interfaces by mainVM.interfaces.collectAsState()

    // Dane logowania / routera
    val routerIp = sessionVM.routerIp.value ?: ""
    val username = sessionVM.username.value
    val password = sessionVM.password.value

    // Aktualnie wybrany interfejs
    var selectedIface by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) } // czy menu rozwijane jest otwarte

    // Kiedy pierwszy raz pojawią się interfejsy — automatycznie wybierz pierwszy
    // i załaduj próbki dla wykresu, żeby użytkownik od razu coś widział.
    LaunchedEffect(interfaces) {
        if (selectedIface == null && interfaces.isNotEmpty()) {
            selectedIface = interfaces.first()
            if (routerIp.isNotBlank() && username != null && password != null) {
                graphVM.loadSamples(routerIp, selectedIface!!)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Wykres ruchu") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Powrót") }
                },
                actions = {
                    // przełączenie motywu
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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Jeśli użytkownik wszedł tu bez wcześniejszego kliknięcia
            // "Show Interfaces" na Dashboardzie — nie ma danych
            if (interfaces.isEmpty()) {
                Text("Brak załadowanych interfejsów. Wróć i pokaż interfejsy, żeby wyświetlić wykres.")
            } else {
                // Rozwijane menu wyboru interfejsu
                Box {
                    Button(onClick = { expanded = true }) {
                        Text(selectedIface ?: "Wybierz interfejs")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        interfaces.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedIface = name
                                    expanded = false
                                    // po wyborze interfejsu pobieramy jego próbki
                                    if (routerIp.isNotBlank() && username != null && password != null) {
                                        graphVM.loadSamples(routerIp, name)
                                    }
                                }
                            )
                        }
                    }
                }

                // wyświetla wykres
                GraphCard(graphVM = graphVM)
            }
        }
    }
}
