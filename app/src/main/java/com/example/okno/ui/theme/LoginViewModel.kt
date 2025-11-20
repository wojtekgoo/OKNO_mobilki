package com.example.okno.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import com.example.okno.network.RetrofitProvider

/*
    LoginViewModel obsługuje proces logowania do routera:
      • na podstawie IP / loginu / hasła próbuje nawiązać połączenie REST/HTTPS,
      • wykonuje krótkie wywołanie testowe do endpointu /rest/system/resource,
      • ustawia stan logowania (loading / error / success),
      • nie przechowuje danych logowania — tylko weryfikuje.

    UI (LoginScreen) nie wykonuje połączeń sieciowych — zamiast tego
    "nasłuchuje" na zmiany stanu UI i reaguje (pokazuje błąd, ładuje dane,
    przechodzi dalej po sukcesie).

    ViewModel wykonuje logikę,
    UI wyświetla tylko rezultat zmian stanu.

    Stan ekranu logowania przechowywany jako jeden obiekt zamiast wielu zmiennych.
    Zalety:
      • łatwiejsze odtworzenie stanu,
      • mniejsza szansa na niespójność pól.
*/

data class LoginUiState(
    val loading: Boolean = false,  // trwa próba połączenia?
    val error: String? = null,     // komunikat błędu dla użytkownika
    val verified: Boolean = false  // czy logowanie powiodło się?
)

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    // MutableStateFlow = zmienny stan w ViewModel
    private val _ui = MutableStateFlow(LoginUiState())

    // UI widzi tylko nieedytowalny StateFlow (asStateFlow)
    val ui = _ui.asStateFlow()

    // reset do stanu początkowego — używane po wylogowaniu
    fun reset() { _ui.value = LoginUiState() }

    /*
        verify() — główna metoda logowania
        Nie zapisuje danych logowania — tylko sprawdza, czy router odpowiada.
    */
    fun verify(routerIp: String, username: String, password: String) {
        viewModelScope.launch {
            // pokaż spinner, wyczyść błędy
            _ui.value = LoginUiState(loading = true)

            try {
                // pobierz API skonfigurowane dla wybranego IP/loginu/hasła
                val api = RetrofitProvider.api(getApplication(), routerIp, username, password)

                // szybkie wywołanie diagnostyczne — jeśli działa, dane są poprawne
                withTimeout(6_000) { api.systemResource() }

                // sukces logowania
                _ui.value = LoginUiState(
                    loading = false,
                    error = null,
                    verified = true
                )

            } catch (e: UnknownHostException) {
                _ui.value = LoginUiState(error = "Nie można znaleźć hosta $routerIp")

            } catch (e: SocketTimeoutException) {
                _ui.value = LoginUiState(error = "Błąd. Łączenie z $routerIp trwało za długo")

            } catch (e: SSLHandshakeException) {
                _ui.value = LoginUiState(error = "Błąd TLS: certificate/hostname mismatch")

            } catch (e: HttpException) {
                // typowe kody HTTP MikroTika
                val msg = when (e.code()) {
                    401 -> "Zły użytkownik lub hasło"
                    404 -> "Nie połączono z protokołem REST (sprawdź czy REST jest włączony)"
                    else -> "HTTP ${e.code()}: ${e.message()}"
                }
                _ui.value = LoginUiState(error = msg)

            } catch (e: Exception) {
                // każde inne nieoczekiwane zdarzenie
                _ui.value = LoginUiState(error = e.message ?: "Nieoczekiwany błąd")
            }
        }
    }
}
