package com.example.okno.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/*
    Ten plik zawiera "DAO" (Data Access Object) dla bazy danych Room,
    odpowiedzialny za komunikację między aplikacją a SQLite.

    - DAO jest interfejsem, w którym deklarujemy zapytania SQL.
    - Room automatycznie generuje implementację tych metod w czasie działania aplikacji.

    W tej aplikacji DAO odpowiada za zapis i odczyt próbek ruchu
    (RX/TX bytes) dla poszczególnych interfejsów sieciowych routera.

    Główne zadania GraphDao:
    --------------------------------------------------------
    • zapisywanie pobranych próbek ruchu
    • dostarczanie listy dostępnych interfejsów pobranych do bazy
    • zwracanie próbek w kolejności czasowej dla wybranego interfejsu
    • pobieranie próbek dla danego dnia (potrzebne do logiki "pierwsza + ostatnia próbka")
    • usuwanie wybranych rekordów przy kontrolowaniu rozmiaru bazy

    Uwaga:
    Nowa logika zarządzania bazą nie używa już prostego limitu 100 rekordów.
    Zamiast tego GraphRepository dba o to, aby:
       — dla każdego dnia i interfejsu zawsze zachować pierwszą i ostatnią próbkę dnia,
       — oraz maksymalną liczbę zapisów w środku dnia (np. do 10 na dzień).
    Dzięki temu dzienne sumy (delta = last - first) są zawsze poprawne, a baza nie rośnie bez końca.
*/

@Dao
interface GraphDao {

    /*  Wstawianie wielu próbek jednocześnie.
        IGNORE oznacza, że Room pominie duplikaty, zamiast rzucać wyjątek.
    */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<InterfaceSampleEntity>)


    /*
        Pobieranie próbek tylko dla danego interfejsu w obrębie jednego dnia.

        • iface     – nazwa interfejsu (ether1, internet, wlan1 itp.)
        • dayStart  – początek dnia w ms (00:00:00)
        • dayEnd    – koniec dnia w ms (23:59:59)

        Zwracane rekordy są posortowane chronologicznie.
        GraphRepository wykorzystuje to, aby zachować:
           — pierwszą próbkę dnia
           — ostatnią próbkę dnia
           — oraz przyciąć nadmiarowe rekordy ze środka dnia.
    */
    @Query("""
        SELECT * FROM interface_samples
        WHERE name = :iface
          AND ts BETWEEN :dayStart AND :dayEnd
        ORDER BY ts
    """)
    suspend fun samplesForDay(
        iface: String,
        dayStart: Long,
        dayEnd: Long
    ): List<InterfaceSampleEntity>


    /*
        Usuwanie rekordów o podanych ID — używane przy kontroli liczby
        próbek w jednym dniu (trimming).
    */
    @Query("DELETE FROM interface_samples WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)


    /*
        Zwraca listę unikalnych nazw interfejsów dostępnych w bazie.
        Flow oznacza, że UI otrzyma aktualizację automatycznie
        po każdej zmianie w bazie.
    */
    @Query("SELECT DISTINCT name FROM interface_samples ORDER BY name")
    fun distinctInterfaceNames(): Flow<List<String>>


    /*
        Zwraca wszystkie próbki danego interfejsu, posortowane chronologicznie.
        Flow -> wykres aktualizuje się w czasie rzeczywistym po zapisaniu nowych danych.
    */
    @Query("""
        SELECT ts, rxBytes, txBytes FROM interface_samples
        WHERE name = :iface
        ORDER BY ts
    """)
    fun samplesFor(iface: String): Flow<List<InterfacePoint>>

    /*
        Projekcja danych pobieranych z zapytania powyżej
        (nie zawiera ID — tylko pola potrzebne do wykresów).
    */
    data class InterfacePoint(
        val ts: Long,
        val rxBytes: Long,
        val txBytes: Long
    )
}
