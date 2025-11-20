package com.example.okno.ui.theme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    /*
        Jeśli ViewModel byłby potrzebny na poziomie Activity, można go trzymać tutaj,
        ale w tej aplikacji każdy ekran tworzy swoje ViewModel-e wewnątrz Compose.
    */
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
            setContent { … }
            ----------------
            Uruchamia środowisko Jetpack Compose — od tej chwili ekran buduje się
            z funkcji @Composable, a nie z XML.
        */
        setContent {

            // ViewModel odpowiedzialny za motyw (jasny/ciemny)
            val themeVM: ThemeViewModel = viewModel()
            val forceDark = themeVM.forceDark.collectAsState().value

            // Globalny kontener stylów / kolorów aplikacji
            OKNOTheme(forceDark = forceDark) {

                /*
                    Kontroler nawigacji — zarządza przełączaniem między ekranami.
                    Tu zamiast wielu aktywności jest jedna Activity + wiele ekranów Compose.
                */
                val navController = rememberNavController()

                // ViewModel-e dostępne globalnie dla nawigacji
                val sessionVM: SessionViewModel = viewModel()  // przechowuje IP + login + hasło
                val mainVM: MainViewModel = viewModel()        // logika pobierania danych z routera
                val loginVM: LoginViewModel = viewModel()      // obsługa logowania i błędów
                val graphVM: GraphViewModel = viewModel()      // obsługa lokalnej bazy i wykresów

                /*
                    NavHost — mapa ekranów aplikacji:
                    - "login"     — pierwszy ekran (chyba że istnieją zapisane dane sesji)
                    - "dashboard" — główny ekran z przyciskami
                    - "graph"     — ekran wykresów
                */
                NavHost(
                    navController = navController,
                    startDestination =
                        if (sessionVM.username.value != null && sessionVM.password.value != null)
                            "dashboard"
                        else
                            "login"
                ) {

                    /*
                        *********************
                        𝗘𝗞𝗥𝗔𝗡 𝗟𝗢𝗚𝗜𝗡
                        *********************
                    */
                    composable("login") {
                        LoginScreen(
                            loginVM = loginVM,

                            // Callback wywoływany dopiero po poprawnym zalogowaniu
                            onVerified = { ip, user, pass ->
                                // zapisujemy dane sesji
                                sessionVM.setRouterIp(ip)
                                sessionVM.setCredentials(user, pass)

                                // inicjalizacja systemu wykresów / lokalnej bazy
                                graphVM.setRouterIp(ip)
                                graphVM.captureNow(ip, user, pass)            // pierwszy zapis
                                graphVM.loadSamples(ip, "ether3")             // optional

                                // przejdź do dashboardu i usuń login z historii
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }

                                // wyczyść stan błędów
                                loginVM.reset()
                            },

                            onToggleTheme = { themeVM.toggle() },
                            isDark = forceDark ?: true
                        )
                    }

                    /*
                        **************************
                        𝗘𝗞𝗥𝗔𝗡 𝗗𝗔𝗦𝗛𝗕𝗢𝗔𝗥𝗗
                        — przyciski: interfejsy, IP, system, graf
                        **************************
                    */
                    composable("dashboard") {
                        DashboardScreen(
                            sessionVM = sessionVM,
                            mainVM = mainVM,
                            graphVM = graphVM,

                            // Wylogowanie
                            onLogout = {
                                sessionVM.clearSession()
                                navController.navigate("login") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            },

                            onToggleTheme = { themeVM.toggle() },
                            isDark = forceDark ?: false,

                            // przejście do ekranu wykresów
                            onShowGraph = {
                                navController.navigate("graph")
                            }
                        )
                    }

                    /*
                        ************************
                        𝗘𝗞𝗥𝗔𝗡 𝗪𝗬𝗞𝗥𝗘𝗦𝗢́𝗪
                        ************************
                    */
                    composable("graph") {
                        GraphScreen(
                            sessionVM = sessionVM,
                            mainVM = mainVM,
                            graphVM = graphVM,

                            // powrót do dashboardu
                            onBack = { navController.popBackStack() },

                            onToggleTheme = { themeVM.toggle() },
                            isDark = forceDark ?: true
                        )
                    }
                }
            }
        }
    }
}
