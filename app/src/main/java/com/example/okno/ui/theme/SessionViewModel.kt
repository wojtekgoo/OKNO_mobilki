package com.example.okno.ui.theme

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
    SessionViewModel przechowuje informacje o bieżącej sesji użytkownika:
      • adres routera (IP lub IP:port),
      • nazwę użytkownika,
      • hasło.

    To jest "centrum pamięci sesji" aplikacji. Nie komunikuje się z API
    ani z bazą danych — jedynie przechowuje dane potrzebne innym ViewModelom
    do wykonywania zapytań.

    Dzięki użyciu StateFlow dane są natychmiast widoczne dla UI oraz
    dla innych ekranów.


    Login → zapis danych do SessionViewModel → Dashboard / Graph mogą na tej podstawie pobierać dane z routera.

    Nie przechowujemy hasła w pamięci trwałej — tylko w pamięci aplikacji
    (zostaje wyczyszczone po „Clear Session”).
*/

class SessionViewModel : ViewModel() {

    // dane sesji
    private val _routerIp = MutableStateFlow("")
    private val _username = MutableStateFlow<String?>(null)
    private val _password = MutableStateFlow<String?>(null)

    val routerIp = _routerIp.asStateFlow()
    val username = _username.asStateFlow()
    val password = _password.asStateFlow()

    // ============================
    // 𝗨𝘀𝘁𝗮𝘄 𝗮𝗱𝗿𝗲𝘀 𝗿𝗼𝘂𝘁𝗲𝗿𝗮
    // ============================
    fun setRouterIp(routerIp: String) {
        _routerIp.value = routerIp
    }

    // Ustaw dane do logowania (po poprawnym połączeniu)
    fun setCredentials(user: String, pass: String) {
        _username.value = user
        _password.value = pass
    }

    // Zakoncz sesje (wywoływane przy „Clear Session”)
    // Czyści wszystkie dane -> aplikacja wraca do trybu logowania
    fun clearSession() {
        _routerIp.value = ""
        _username.value = null
        _password.value = null
    }

    // Sprawdz czy sesja jest wciaz aktywna
    // przydatne do nawigacji po starcie aplikacji
    fun isLoggedIn(): Boolean =
        _username.value != null && _password.value != null
}
