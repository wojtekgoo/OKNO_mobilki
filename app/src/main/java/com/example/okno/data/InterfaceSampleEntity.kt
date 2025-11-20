package com.example.okno.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
    InterfaceSampleEntity jest encją Room — czyli strukturą danych, która
    reprezentuje jeden wiersz w tabeli bazy danych SQLite.

    Tabela = "interface_samples"

    Każdy rekord przechowuje jedną próbkę ruchu sieciowego z routera:
      - nazwę interfejsu (np. "ether1", "wlan1", "ap_salon")
      - liczniki RX i TX w bajtach
      - timestamp (moment pobrania próby, w milisekundach od epoki)

    Te dane są później wykorzystywane m.in. do wykresów:
      - widok „Samples” — pokazuje próbki w czasie
      - widok „Daily” — porównuje pierwszą i ostatnią próbkę w danym dniu

    Room na podstawie tej klasy automatycznie tworzy tabelę SQL
    oraz mapuje rekordy wiersz → obiekt Kotlin / obiekt Kotlin → wiersz.
*/

@Entity(tableName = "interface_samples") // nazwa tabeli SQLite w bazie Room
data class InterfaceSampleEntity(

    // unikalne ID rekordu — generowane automatycznie przez Room
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // nazwa interfejsu routera, np. "ether3", "pppoe-out", "internet"
    val name: String,

    // licznik odebranych danych (RX) w bajtach — wartość zagregowana od momentu uruchomienia routera
    // pobierana z routera, nie kasowana po odczycie
    val rxBytes: Long,

    // licznik wysłanych danych (TX) w bajtach
    val txBytes: Long,

    // timestamp próby w milisekundach (System.currentTimeMillis())
    // służy do rysowania wykresów i agregacji dziennej
    val ts: Long
)
