package com.example.okno.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.okno.network.RetrofitProvider
import com.example.okno.repo.MikroTikRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/*
    MainViewModel jest głównym ViewModelem ekranu Dashboard.
    Odpowiada za:

      • pobieranie danych z routera (system resources, interfejsy, DHCP leases),
      • przechowywanie aktualnego statusu (tekst u góry ekranu),
      • przechowywanie list:
          - interfejsów (nazwy),
          - dzierżaw DHCP (address + host-name),
          - zasobów systemowych (sformatowany tekst),
      • obsługę „toggles” — Show/Hide Interfaces, Show/Hide Leases, Show/Hide System Resources,
      • czyszczenie stanu po "Clear Session".

    Jest to warstwa pośrednia między:
      Router (API) ↔ RetrofitProvider + MikroTikRepository ↔ MainViewModel ↔ DashboardScreen (UI)
*/

class MainViewModel(app: Application) : AndroidViewModel(app) {

    // 𝗧𝗲𝗸𝘀𝘁 𝘀𝘁𝗮𝘁𝘂𝘀𝘂 (u góry ekranu)
    private val _status = MutableStateFlow("--")
    val status = _status.asStateFlow()

    // 𝗟𝗶𝘀𝘁𝗮 𝗶𝗻𝘁𝗲𝗿𝗳𝗲𝗷𝘀𝗼́𝘄 (same nazwy)
    private val _interfaces = MutableStateFlow<List<String>>(emptyList())
    val interfaces = _interfaces.asStateFlow()

    // 𝗟𝗶𝘀𝘁𝗮 𝗱𝘇𝗶𝗲𝗿𝘇̇𝗮𝘄 𝗗𝗛𝗖𝗣 – każda jako para (address, host-name)
    private val _leases = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val leases = _leases.asStateFlow()

    // 𝗭𝗮𝘀𝗼𝗯𝘆 𝘀𝘆𝘀𝘁𝗲𝗺𝗼𝘄𝗲 – sformatowany tekst, lub null jeśli ukryte
    private val _systemResources = MutableStateFlow<String?>(null)
    val systemResources = _systemResources.asStateFlow()

    /*
        connectAndFetch():
        Starsza/ogólna funkcja – łączy się z routerem, pobiera system/resource
        oraz listę interfejsów i aktualizuje status + listę interfejsów.
        W praktyce korzystasz teraz z bardziej rozbitych metod (toggleX),
        ale ta metoda dobrze pokazuje podstawową logikę.
     */
    fun connectAndFetch(routerIp: String, username: String, password: String) {
        _status.value = "Łączę…"
        viewModelScope.launch {
            try {
                val api = RetrofitProvider.api(getApplication(), routerIp, username, password)
                val repo = MikroTikRepository(api)

                val sys = repo.loadSystem()
                val ifaces = repo.loadInterfaces()

                _status.value = "Router v${sys.version} CPU ${sys.cpuLoad}% Uptime ${sys.uptime}"
                _interfaces.value = ifaces.map { it.name ?: "(nieznany)" }
            } catch (e: Exception) {
                _status.value = "Błąd: ${e.message}"
                _interfaces.value = emptyList()
            }
        }
    }

    /*
        loadSystemResources():
        Pobiera szczegółowe informacje o zasobach systemowych routera:
           - board name
           - wersję RouterOS
           - wykorzystanie CPU
           - uptime
           - pamięć używana / całkowita
        Formatuje je do czytelnego tekstu i zapisuje w _systemResources.
     */
    fun loadSystemResources(routerIp: String, username: String, password: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitProvider.api(getApplication(), routerIp, username, password)
                val res = api.systemResource()

                val boardName = res.boardName ?: "nieznany"
                val version = res.version ?: "nieznany"
                val cpu = res.cpuLoad ?: 0
                val uptime = res.uptime ?: "nieznany"
                val totalMem = res.totalMemory ?: 0L
                val freeMem = res.freeMemory ?: 0L
                val usedMem = (totalMem - freeMem).coerceAtLeast(0L)

                val text = buildString {
                    append("Board name: $boardName\n")
                    append("Wersja: $version\n")
                    append("CPU: $cpu%\n")
                    append("Uptime: $uptime\n")
                    append("Pamięć: ${usedMem / (1024 * 1024)} / ${totalMem / (1024 * 1024)} MB wykorzystane")
                }

                _systemResources.value = text
                _status.value = "Zasoby systemowe wczytane"
            } catch (e: Exception) {
                _systemResources.value = null
                _status.value = "Błąd wczytywania zasobów systemowych routera: ${e.message}"
            }
        }
    }

    /*
        fetchInterfaces():
        Pobiera pełną listę interfejsów z /rest/interface i aktualizuje _interfaces.
        W razie potrzeby możemy tu debugować też liczniki RX/TX.
     */
    private fun fetchInterfaces(routerIp: String, username: String, password: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitProvider.api(getApplication(), routerIp, username, password)
                val ifaces = api.listInterfaces() // pełne DTO interfejsu

                // Przykładowy kod do debugowania wybranego interfejsu:
                // ifaces.firstOrNull { it.name == "internet" }?.let { iface ->
                //     println("DEBUG: interface 'internet' >> rx-byte=${iface.rxByte}, tx-byte=${iface.txByte}")
                // }

                _interfaces.value = ifaces.map { it.name ?: "(nieznany)" }
                _status.value = "Interfejsy: ${_interfaces.value.size}"
            } catch (e: Exception) {
                _status.value = "Błąd wczytywania interfejsów: ${e.message}"
                _interfaces.value = emptyList()
            }
        }
    }

    /*
        toggleSystemResources():
        Jeśli dane są już w _systemResources → ukryj (wyczyść).
        Jeśli nie ma danych → pobierz z routera.
     */
    fun toggleSystemResources(routerIp: String, username: String, password: String) {
        if (_systemResources.value != null) {
            _systemResources.value = null
            _status.value = "Zasoby systemowe ukryte"
        } else {
            loadSystemResources(routerIp, username, password)
        }
    }

    /*
        toggleInterfaces():
        Jeśli lista interfejsów nie jest pusta → ukryj.
        Jeśli pusta → pobierz z routera.
     */
    fun toggleInterfaces(routerIp: String, username: String, password: String) {
        if (_interfaces.value.isNotEmpty()) {
            _interfaces.value = emptyList()
            _status.value = "Interfejsy ukryte"
        } else {
            fetchInterfaces(routerIp, username, password)
        }
    }

    /*
        fetchLeases():
        Pobiera listę dzierżaw DHCP (adres IP + host-name) i mapuje je
        na pary (address, hostName), które są łatwe do wyświetlenia w UI.
     */
    fun fetchLeases(routerIp: String, username: String, password: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitProvider.api(getApplication(), routerIp, username, password)
                val items = api.listDhcpLeases()
                _leases.value = items.map {
                    val addr = it.address ?: "(brak adresu)"
                    val host = it.hostName?.takeIf { h -> h.isNotBlank() } ?: "(brak nawy hosta)"
                    addr to host
                }
                // dodatkowa informacja w statusie
                _status.value = "Adresy IP: ${_leases.value.size}"
            } catch (e: Exception) {
                _status.value = "Błąd wczytywania dresów IP: ${e.message}"
                _leases.value = emptyList()
            }
        }
    }

    /*
        toggleLeases():
        Jeśli lista dzierżaw nie jest pusta → ukryj.
        Jeśli pusta → pobierz z routera.
     */
    fun toggleLeases(routerIp: String, username: String, password: String) {
        if (_leases.value.isNotEmpty()) {
            _leases.value = emptyList()
            _status.value = "Adresy IP ukryte"
        } else {
            fetchLeases(routerIp, username, password)
        }
    }

    /*
        reset():
        Wywoływane po "Clear Session" — czyści cały stan, tak jakby aplikacja
        dopiero co się uruchomiła.
     */
    fun reset() {
        _status.value = "--"
        _interfaces.value = emptyList()
        _leases.value = emptyList()
        _systemResources.value = null
    }
}
