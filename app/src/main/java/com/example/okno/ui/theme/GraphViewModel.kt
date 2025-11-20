package com.example.okno.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.okno.repo.GraphRepository   // repo odczytuje i zapisuje dane do Room

/*
    GraphViewModel jest warstwą logiki ekranów wykresów.
    Jego zadania:
      • pobieranie próbek z bazy Room dla wybranego interfejsu,
      • publikowanie danych do UI w postaci StateFlow (obserwowalne przez Compose),
      • wywoływanie rejestracji nowych próbek (captureNow),
      • przechowywanie wybranego routera (opcjonalnie, dla wygody).

    Jest to łącznik między:
      UI (GraphScreen + GraphCard)
      i
      Repository (GraphRepository → Room → DB z próbkami interfejsów).
*/

class GraphViewModel(app: Application) : AndroidViewModel(app) {

    /*
        Prosta struktura na potrzeby UI (po odczycie z DB).
        Dzięki niej UI nie musi używać encji Room ani DTO — dostaje gotowe wartości.
    */
    data class SamplePoint(
        val ts: Long,   // timestamp (epoch millis)
        val rx: Long,   // łączny licznik RX bajtów zwrócony przez router
        val tx: Long    // łączny licznik TX bajtów zwrócony przez router
    )

    /*
        Strumień z próbkami do wykresu.
        UI (GraphCard) subskrybuje StateFlow i będzie automatycznie odświeżane.
    */
    private val _samples = MutableStateFlow<List<SamplePoint>>(emptyList())
    val samples: StateFlow<List<SamplePoint>> = _samples.asStateFlow()

    // Repository odpowiedzialny za komunikację z DB i rejestrowanie próbek
    private val repo = GraphRepository(app)

    // Aktualny router — pozwala UI lub innym VM łatwo odczytać jaki router jest podpięty
    private val _routerIp = MutableStateFlow<String?>(null)
    val routerIp: StateFlow<String?> = _routerIp.asStateFlow()

    fun setRouterIp(ip: String) {
        _routerIp.value = ip
    }

    /**
     * captureNow — pobiera próbkę z API Routera
     * (pełną listę interfejsów wraz z licznikami RX/TX) i zapisuje to do DB.
     *
     * To NIE aktualizuje wykresu — tylko zwiększa liczbę rekordów w DB.
     * Sam wykres odświeży się dopiero gdy loadSamples() odbierze nowe dane z DB.
     */
    fun captureNow(routerIp: String, user: String, pass: String) {
        viewModelScope.launch {
            repo.captureInterfacesSample(routerIp, user, pass)
        }
    }

    /**
     * loadSamples — pobiera dane z DB dla konkretnego interfejsu (np. "ether1" lub "internet").
     *
     * repo.samplesFlow() zwraca Flow<List<InterfacePoint>>, a Flow emituje wartość za każdym
     * razem, gdy do bazy dopisywana jest nowa próbka.
     *
     * Dzięki temu wykres działa „na żywo” — bez ręcznego odświeżania.
     */
    fun loadSamples(routerIp: String, ifaceName: String) {
        viewModelScope.launch {
            // collect = rejestruje zmiany strumienia z DB
            repo.samplesFlow(routerIp, ifaceName).collect { list ->
                _samples.value = list.map { p ->
                    // Mapuje dane Room do odpowiedniej struktury dla UI
                    SamplePoint(
                        ts = p.ts,
                        rx = p.rxBytes,
                        tx = p.txBytes
                    )
                }
            }
        }
    }
}
