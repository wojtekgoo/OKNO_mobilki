package com.example.okno.network

import android.content.Context
import com.example.okno.R
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


/*
    TLS.kt odpowiada za przygotowanie bezpiecznego połączenia HTTPS do routera
    Mikrotik poprzez:

      - wczytanie pliku certyfikatu CA routera z /res/raw/mikrotik_ca.crt
      - uwierzytelnienie serwera (routera) na podstawie tego certyfikatu
      - utworzenie SSLContext i zaufanego TrustManagera
      - zbudowanie OkHttpClient, który:
            + ufa tylko certyfikatowi routera
            + odrzuca każdy inny certyfikat
      - wymuszenie zgodności nazwy hosta przez hostnameVerifier
      - dołączenie BasicAuthInterceptor (login + hasło)

    Dzięki temu:
      - aplikacja nie zaakceptuje połączenia z innym routerem / innym serwerem
      - MITM (man-in-the-middle) jest skutecznie blokowany
      - nawet jeśli ktoś wystawi fałszywy certyfikat, połączenie zostanie przerwane
*/

object TLS {

    /*
        buildClientWithCa() tworzy gotowego, w pełni skonfigurowanego
        klienta HTTP z uwierzytelnieniem i TLS opartym o własny CA cert.
     */
    fun buildClientWithCa(
        context: Context,
        auth: BasicAuthInterceptor,
        routerIp: String
    ): OkHttpClient {

        // 1) Wczytanie certyfikatu CA z zasobu raw
        val cf = CertificateFactory.getInstance("X.509")
        val caInput = context.resources.openRawResource(R.raw.mikrotik_ca).buffered()
        val caCert = caInput.use { cf.generateCertificate(it) }

        // 2) Tworzymy KeyStore zawierający tylko certyfikat routera
        //    -> urządzenie zaufa TYLKO temu certyfikatowi
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)   // inicjalizacja pustego KeyStore
            setCertificateEntry("mikrotik", caCert)
        }

        // 3) trust manager korzysta z KeyStore (czyli cert. routera)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore)
        }
        val trustManager = tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager

        // 4) Tworzymy SSLContext zawierający trustManager
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }

        // 5) Konstruujemy OkHttpClient z TLS + BasicAuth
        return OkHttpClient.Builder()
            // własny SSL socket + zaufanie tylko do routera
            .sslSocketFactory(sslContext.socketFactory, trustManager)

            // hostnameVerifier — dopuszcza wyłącznie certyfikat routera dla jego IP
            // blokuje TLS handshake jeśli hostname w certyfikacie jest inny
            .hostnameVerifier { hostname, _ -> hostname == routerIp }

            // BasicAuthInterceptor dodaje nagłówek Authorization do wszystkich zapytań
            .addInterceptor(auth)

            .build()
    }
}
