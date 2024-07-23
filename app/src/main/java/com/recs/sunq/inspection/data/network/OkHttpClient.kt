package com.recs.sunq.androidapp.data.network

import android.content.Context
import com.recs.sunq.inspection.R
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

fun createOkHttpClientWithPemCertificate(context: Context): OkHttpClient {
    val certificateFactory = CertificateFactory.getInstance("X.509")

    // res/raw 폴더에 있는 .pem 인증서 파일을 로드합니다.
    val inputStream: InputStream = context.resources.openRawResource(R.raw.cert2)
    val certificate = certificateFactory.generateCertificate(inputStream)
    inputStream.close()

    // 키스토어를 생성하고 인증서를 추가합니다.
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        setCertificateEntry("ca", certificate)
    }

    // TrustManagerFactory를 사용하여 신뢰할 수 있는 인증서 목록을 초기화합니다.
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(keyStore)
    }

    // SSL 컨텍스트를 생성하고 초기화합니다.
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, trustManagerFactory.trustManagers, null)
    }

    // 로깅 인터셉터를 생성합니다.
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 최신 TLS 버전을 사용하도록 설정합니다.
    val connectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3) // TLS 1.2와 1.3 모두 지원
        .build()

    // OkHttp 클라이언트를 생성하고 SSL 컨텍스트로 구성된 SSLSocketFactory와 로깅 인터셉터, TLS 설정을 추가합니다.
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true } // 이 줄을 추가하여 호스트 이름 검증을 무시합니다.
        .addInterceptor(loggingInterceptor) // 모든 빌드에서 로깅 인터셉터를 추가합니다.
        .connectionSpecs(listOf(connectionSpec))
        .build()
}