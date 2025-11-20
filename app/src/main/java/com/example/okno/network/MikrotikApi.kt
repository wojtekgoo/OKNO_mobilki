package com.example.okno.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

/*
    MikroTikApi.kt to tzw. "kontrakt API" — miejsce, w którym opisujemy:
      • jakie endpointy HTTP istnieją na routerze MikroTik,
      • pod jakimi ścieżkami (URL) są dostępne,
      • jakie dane zwracają,
      • w jakich klasach (DTO) mają być one odwzorowane.

    Retrofit wykorzystuje tę definicję do automatycznego wygenerowania
    klienta HTTP. Dzięki temu w kodzie aplikacji możemy po prostu wywołać:
        val api = RetrofitProvider.api(...)
        val ifaces = api.listInterfaces()
    zamiast manualnie budować URL, otwierać połączenie HTTPS, parsować JSON itd.

    Ten plik nie zawierta logiki, jedynie 𝗱𝗲𝗳𝗶𝗻𝗶𝗰𝗷𝗲  co mozna pobrac z routera
    Dopiero Retrofit i OkHttp wykonują prawdziwe zapytania.


    𝗗𝗧𝗢 (𝗗𝗮𝘁𝗮 𝗧𝗿𝗮𝗻𝘀𝗳𝗲𝗿 𝗢𝗯𝗷𝗲𝗰𝘁)

    Router MikroTik zwraca JSON — a dane JSON są mapowane na Kotlin data class.
    Każdy DTO odzwierciedla strukturę JSON zwracaną przez router.

    @Json(name = "...") — mówi Moshi, z którego pola JSON ma korzystać.
    @JsonClass(generateAdapter = true) — prosi Moshi o automatyczny
    adapter JSON ↔ Kotlin
*/

interface MikroTikApi {

    // Pobiera zasoby systemowe routera
    // GET https://<router>/rest/system/resource
    @GET("rest/system/resource")
    suspend fun systemResource(): SystemResourceDto

    // Pobiera listę interfejsów
    // GET https://<router>/rest/interface
    @GET("rest/interface")
    suspend fun listInterfaces(): List<InterfaceDto>

    // Pobiera listę dzierżaw DHCP
    // GET https://<router>/rest/ip/dhcp-server/lease
    @GET("rest/ip/dhcp-server/lease")
    suspend fun listDhcpLeases(): List<LeaseDto>
}

/*
    DTO odpowiadający danych z /rest/system/resource
*/
@JsonClass(generateAdapter = true)
data class SystemResourceDto(
    @Json(name = "board-name") val boardName: String?,
    @Json(name = "version") val version: String?,
    @Json(name = "cpu-load") val cpuLoad: Int?,
    @Json(name = "uptime") val uptime: String?,

    // Mikrotik zwraca liczniki bajtów w formie String
    @Json(name = "rx-byte") val rxByte: String?,
    @Json(name = "tx-byte") val txByte: String?,

    @Json(name = "free-memory") val freeMemory: Long?,
    @Json(name = "total-memory") val totalMemory: Long?
)

/*
    DTO reprezentujący dane interfejsu z /rest/interface
*/
@JsonClass(generateAdapter = true)
data class InterfaceDto(
    @Json(name = "name") val name: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "running") val running: String?,

    // Liczniki bajtów — zwracane jako string
    @Json(name = "rx-byte") val rxByte: String?,
    @Json(name = "tx-byte") val txByte: String?
)

/*
    DTO reprezentujący dzierżawy DHCP z /rest/ip/dhcp-server/lease
*/
@JsonClass(generateAdapter = true)
data class LeaseDto(
    @Json(name = "address") val address: String?,
    @Json(name = "host-name") val hostName: String?
)
