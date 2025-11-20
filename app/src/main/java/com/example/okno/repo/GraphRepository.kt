package com.example.okno.repo

import android.content.Context
import com.example.okno.data.GraphDb
import com.example.okno.data.InterfaceSampleEntity
import com.example.okno.network.RetrofitProvider

/*
    GraphRepository jest częścią warstwy "repozytoriów" aplikacji.

    Jego zadaniem jest obsługa logiki danych — UI nie powinien wiedzieć:
      • jak wyglądają zapytania HTTP (Retrofit)
      • jak działa szyfrowanie TLS
      • jak wygląda baza SQL lub zapytania Room

    Repozytorium pobiera dane z routera → przetwarza je → zapisuje do SQLite →
    a UI/ViewModel dostaje tylko gotowe dane.

    W tej aplikacji GraphRepository odpowiada za:
    --------------------------------------------------------
    ✔ pobieranie z API routera informacji o interfejsach (liczniki RX/TX)
    ✔ zapis nowych próbek do bazy danych (każda próbka = jeden czas pomiaru)
    ✔ inteligentne przycinanie historii w bazie:
          • dla każdego dnia i każdego interfejsu
          • zachowujemy ZAWSZE pierwszą i ostatnią próbkę dnia
          • w środku można mieć maks. 10 próbek dziennie
          • dzięki temu wykres dzienny (delta = last - first) jest zawsze poprawny

    Dzięki takiemu podejściu:
      — baza nie rośnie bez końca
      — dzienne wykresy pozostają dokładne
      — próbki z nocy/mniej aktywnych godzin nie nadpisują najważniejszych danych dnia
*/

class GraphRepository(private val appContext: Context) {

    /*
        captureInterfacesSample():
        ----------------------------------------
        • pobiera z routera aktualny stan wszystkich interfejsów
        • zamienia DTO z API na encje bazy danych
        • zapisuje próbki do odpowiedniej bazy SQLite (baza zależna od routera!)
        • wykonuje "smart trimming" — utrzymuje do 10 próbek / dzień / interfejs
    */
    suspend fun captureInterfacesSample(routerIp: String, user: String, pass: String) {
        // 1) Pobranie danych z routera (REST API → Retrofit)
        val api = RetrofitProvider.api(appContext, routerIp, user, pass)
        val list = api.listInterfaces()

        val now = System.currentTimeMillis()

        // 2) Konwersja DTO → encje bazy
        val rows = list.mapNotNull { dto ->
            val name = dto.name ?: return@mapNotNull null
            val rx = dto.rxByte?.toLongOrNull() ?: 0L
            val tx = dto.txByte?.toLongOrNull() ?: 0L

            InterfaceSampleEntity(
                name = name,
                rxBytes = rx,
                txBytes = tx,
                ts = now
            )
        }

        if (rows.isEmpty()) return // brak danych = nic do roboty

        // 3) Wyliczenie zakresu [start, end] bieżącego dnia (do trimowania dziennego)
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 24L * 60L * 60L * 1000L - 1L

        // 4) Wybór odpowiedniej bazy danych dla routera
        //    (każdy router ma własny plik .db)
        val db = GraphDb.getForRouter(appContext, routerIp)
        val dao = db.dao()

        // 5) Zapis nowych próbek do bazy
        dao.insertAll(rows)

        /*
            6) TRIMOWANIE HISTORII:

            – Pobieramy próbki TYLKO dla dzisiejszego dnia i danego interfejsu
            – Maksymalnie 10 rekordów na dzień

            Aby nie zepsuć obliczeń delta dla wykresu dziennego:
               • zachowujemy 1. próbkę dnia (najwcześniejszą)
               • zachowujemy ostatnią próbkę dnia (najpóźniejszą)
               • w środku zostaje max 8 rekordów

            Nadmiarowe rekordy usuwamy ZAWSZE z "środka dnia", a NIE z początku,
            dzięki czemu logika "last - first" zawsze działa prawidłowo.
        */
        val maxPerDay = 10
        val ifaceNames = rows.map { it.name }.distinct()

        ifaceNames.forEach { iface ->
            // wszystkie dzisiejsze rekordy tego interfejsu
            val dayRows = dao.samplesForDay(iface, dayStart, dayEnd)

            if (dayRows.size > maxPerDay) {
                val first = dayRows.first()
                val last = dayRows.last()

                // middle = wszystko między pierwszym a ostatnim
                val middle = dayRows.drop(1).dropLast(1)

                // ile "środkowych" może zostać?
                val allowedMiddle = (maxPerDay - 2).coerceAtLeast(0)

                if (middle.size > allowedMiddle) {
                    // usuwamy *najstarsze spośród middle*, NIE first / last
                    val toDeleteCount = middle.size - allowedMiddle
                    val toDelete = middle.take(toDeleteCount)
                    dao.deleteByIds(toDelete.map { it.id })
                }
            }
        }
    }

    /*
        Zwraca listę nazw interfejsów jako Flow,
        dzięki czemu UI automatycznie aktualizuje się po zmianie bazy.
    */
    fun interfaceNamesFlow(routerIp: String) =
        GraphDb.getForRouter(appContext, routerIp).dao().distinctInterfaceNames()

    /*
        Zwraca próbki RX/TX wybranego interfejsu w kolejności czasowej.
        Flow → wykres uaktualnia się sam po dopisaniu nowych rekordów.
    */
    fun samplesFlow(routerIp: String, name: String) =
        GraphDb.getForRouter(appContext, routerIp).dao().samplesFor(name)
}
