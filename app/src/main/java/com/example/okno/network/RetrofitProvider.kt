package com.example.okno.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/*
    RetrofitProvider jest centralnym punktem dostarczania obiektu API
    (czyli instancji MikroTikApi), który służy do wykonywania zapytań
    HTTPS do routera MikroTik.

    Ten obiekt odpowiada za:
      - zbudowanie Retrofit + OkHttp z poprawną konfiguracją
      - ustawienie certyfikatu CA (przez TLS.buildClientWithCa)
      - dodanie BasicAuthInterceptor (autoryzacja login + hasło)
      - cache'owanie (ponowne używanie) instancji API
        + żeby uniknąć ponownego łączenia i handshake SSL
        + żeby logowanie przebiegało szybko po pierwszym razie

    Ponieważ zmienić mogą się:
        router IP    (np. 192.168.88.1 ➜ 91.227.0.7:30071)
        username     (np. admin ➜ guest)
        password
    RetrofitProvider porównuje te parametry i tworzy nową instancję tylko wtedy,
    gdy naprawdę jest potrzebna. W przeciwnym razie zwraca poprzednią instancję.

    Dzięki temu aplikacja:
      - autoryzuje się raz
      - zachowuje stałe połączenie HTTPS
      - nie tworzy niepotrzebnie nowych klientów
*/

object RetrofitProvider {

    @Volatile private var api: MikroTikApi? = null
    private var lastKey: String? = null   // przechowuje "ip|user|pass"
    private var lastBaseUrl: String? = null

    /*
        𝗚𝗹𝗼́𝘄𝗻𝗮 𝗳𝘂𝗻𝗸𝗰𝗷𝗮:
        Zwraca gotowy do użycia obiekt MikroTikApi (Retrofit interface).
        Automatycznie wykonuje:
            • poprawne zbudowanie URL (obsługuje IP i IP:port)
            • autoryzację BasicAuth
            • weryfikację certyfikatu CA routera
            • cache'owanie instancji (tworzy nową tylko jeśli potrzebne)
    */
    fun api(
        context: Context,
        routerIp: String,
        username: String,
        password: String
    ): MikroTikApi
    {
        // obsługa routera z portem: np. "91.227.0.7:30071"
        val (host, port) = splitHostPort(routerIp)
        val baseUrl = if (port != null)
            "https://$host:$port/"
        else
            "https://$host/"

        // klucz identyfikujący połączenie — jeśli inny, budujemy nowe API
        val key = "$baseUrl|$username|$password"

        return synchronized(this) {
            // jeżeli pierwszy raz albo zmienił się router/login/hasło → budujemy nowe API
            if (api == null || lastKey != key) {
                val client = TLS.buildClientWithCa(
                    context = context,
                    auth = BasicAuthInterceptor(username, password),
                    host
                )

                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)                               // np. https://91.227.0.7:30071/
                    .client(client)                                 // OkHttp z BasicAuth + CA cert
                    .addConverterFactory(MoshiConverterFactory.create()) // JSON ↔ Kotlin
                    .build()

                api = retrofit.create(MikroTikApi::class.java)
                lastKey = key
            }

            api!!
        }
    }

    /*
        Czyścimy obiekt API po kliknięciu "Clear Session".
        Dzięki temu kolejne logowanie z nowymi danymi wymusi stworzenie nowego klienta HTTPS.
     */
    fun clear() = synchronized(this) {
        api = null
        lastKey = null
    }

    /*
        Pomocnicza funkcja:
        Rozdziela zapis "host:port" na parę (host, port)
        Przykład:
            "91.227.0.7:30071" → ("91.227.0.7", 30071)
            "192.168.88.1"     → ("192.168.88.1", null)
     */
    private fun splitHostPort(ip: String): Pair<String, Int?> {
        val parts = ip.split(":")
        return if (parts.size == 2) {
            val host = parts[0]
            val port = parts[1].toIntOrNull()
            host to port
        } else {
            ip to null
        }
    }

    /*
        (Stara wersja, pozostawiona jako zapasowa)
        Gdybyśmy nie wspierali routera na porcie publicznym, ta funkcja byłaby używana.
        Obecnie obsługę portu przejął splitHostPort().
     */
    private fun build(context: Context, routerIp: String, user: String, pass: String): MikroTikApi
    {
        val client: OkHttpClient = TLS.buildClientWithCa(
            context,
            BasicAuthInterceptor(user, pass),
            routerIp = routerIp
        )

        val retrofit = Retrofit.Builder()
            .baseUrl("https://${routerIp.trimEnd('/')}/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()

        return retrofit.create(MikroTikApi::class.java)
    }
}
