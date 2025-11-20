package com.example.okno.repo

import com.example.okno.network.MikroTikApi

/*
    MikroTikRepository to prosta warstwa pośrednia między ViewModelami
    (które sterują logiką ekranu) a warstwą sieciową (Retrofit / MikroTikApi).

    Repository:
      - nie rysuje interfejsu,
      - nie zapisuje danych do bazy,
      - nie przechowuje stanu aplikacji.

    Repository:
      - pobiera dane z API
      - udostępnia je ViewModelom
      - ukrywa przed ViewModelami szczegóły Retrofit, endpointów i URL

    Dzięki temu ViewModel może używać repozytorium w sposób deklaratywny:
        val sys = repository.loadSystem()
        val ifaces = repository.loadInterfaces()
    bez znajomości jak działa Retrofit, SSL ani autoryzacja.

    Architektura aplikacji:

        UI ↔ ViewModel ↔ Repository ↔ Retrofit/Network
                               ↑
                          Router MikroTik
*/

class MikroTikRepository(private val api: MikroTikApi) {

    /*
        loadSystem()
        — pobiera zasoby systemowe routera
        — np. board-name, cpu-load, uptime, free-memory itd.
        — API: GET /rest/system/resource
     */
    suspend fun loadSystem() = api.systemResource()

    /*
        loadInterfaces()
        — pobiera listę interfejsów sieciowych routera oraz ich bieżące liczniki RX/TX
        — API: GET /rest/interface
     */
    suspend fun loadInterfaces() = api.listInterfaces()
}
