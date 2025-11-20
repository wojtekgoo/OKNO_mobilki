package com.example.okno.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/*
    GraphDb jest klasą reprezentującą bazę danych Room w aplikacji.

    RoomDatabase to wysokopoziomowa warstwa nad SQLite.
    DAO (GraphDao) zapewnia API do wykonywania zapytań SQL.

    W tej aplikacji GraphDb przechowuje próbkowane dane ruchu (RX/TX bytes)
    dla interfejsów routera. Każdy router ma swoją własną bazę danych,
    aby oddzielić historię ruchu per router/IP.

    Najważniejsze zadania GraphDb:
      - przechowywanie tabeli interface_samples
      - zapewnianie dostępu do DAO
      - utrzymywanie osobnych baz danych dla różnych routerów (różne pliki .db)
      - budowanie instancji DB tylko raz (singleton), aby uniknąć wycieków pamięci
*/

@Database(
    entities = [InterfaceSampleEntity::class], // lista tabel w DB
    version = 1,                               // wersja DB (używane przy migracjach)
    exportSchema = false                       // nie zapisujemy schematu do pliku
)
abstract class GraphDb : RoomDatabase() {

    // Udostępniamy DAO, dzięki któremu ViewModel/Repository mogą wykonywać zapytania
    abstract fun dao(): GraphDao

    companion object {
        // Mapa instancji DB — osobna baza danych dla każdego routera
        // klucz = nazwa pliku DB, wartość = GraphDb
        @Volatile
        private var instances: MutableMap<String, GraphDb> = mutableMapOf()

        /*
            Pobiera lub tworzy nowa instancję GraphDb dla konkretnego routera.
            Każdy router ma własną osobną bazę danych.
            Np:
              router "192.168.88.1" -> baza "192_168_88_1.db"
         */
        fun getForRouter(context: Context, routerIp: String): GraphDb {
            val key = routerIp.trim()
            val fileName = ipToDbName(key)   // np. "192_168_88_1.db"

            return synchronized(this) {
                // Jeśli baza dla danego routera już istnieje, użyj jej
                instances[fileName] ?: Room.databaseBuilder(
                    context.applicationContext,
                    GraphDb::class.java,
                    fileName
                ).build().also { db ->
                    // Zapisz w mapie, aby następnym razem nie tworzyć nowej instancji
                    instances[fileName] = db
                }
            }
        }

        /*
            Konwersja adresu w formacie IP/host:port do bezpiecznej nazwy pliku DB.
            192.168.88.1      → 192_168_88_1.db
            91.227.0.7:30071  → 91_227_0_7_30071.db

            Kropki i dwukropki są zamieniane na podkreślenia zeby uniknąć
            nieprawidłowych znaków w nazwie pliku.
         */
        private fun ipToDbName(ip: String): String {
            val sanitized = ip
                .replace('.', '_')
                .replace(':', '_')
            return "${sanitized}.db"
        }
    }
}