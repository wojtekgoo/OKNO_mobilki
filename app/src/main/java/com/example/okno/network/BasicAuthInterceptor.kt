package com.example.okno.network

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

/*
    Klasa jest częścią warstwy sieciowej aplikacji i odpowiada za dodawanie nagłówka
    HTTP "Authorization" do każdego zapytania wysyłanego przez Retrofit / OkHttp do routera.

    Router MikroTik wymaga autoryzacji typu Basic Authentication (login + hasło).
    Ten interceptor automatycznie koduje te dane w formacie:
        Authorization: Basic base64(login:hasło)

    Dzięki temu:
      - każdy request zawiera poprawne dane logowania,
      - inne części aplikacji nie muszą dodawać nagłówków ręcznie,
      - użytkownik wpisuje login i hasło tylko raz.

    Interceptor podłącza się do klienta HTTP (OkHttpClient) i modyfikuje
    każde żądanie zanim zostanie wysłane do sieci
*/

class BasicAuthInterceptor(
    username: String,
    password: String
) : Interceptor {

    // Nagłówek HTTP Authorization tworzony raz na podstawie loginu i hasła
    // Credentials.basic() generuje poprawny format Base64 — np. "Basic YWRtaW46cGFzcw=="
    private val header = Credentials.basic(username, password)

    // intercept() jest wywoływany automatycznie dla kazdego requestu wychodzącego
    override fun intercept(chain: Interceptor.Chain): Response {

        // Tworzymy nowy request na podstawie istniejącego
        // i dokładamy nagłówek autoryzacyjny
        val req = chain.request().newBuilder()
            .header("Authorization", header)   // ← dodanie Basic Auth header
            .build()

        // przekazujemy request dalej przez pipeline sieciowy OkHttp
        return chain.proceed(req)
    }
}
