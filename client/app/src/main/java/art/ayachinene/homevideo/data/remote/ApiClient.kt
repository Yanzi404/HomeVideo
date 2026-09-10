package art.ayachinene.homevideo.data.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var currentBaseUrl = "http://192.168.31.73:8080/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var retrofit: Retrofit = buildRetrofit(currentBaseUrl)

    var apiService: ApiService = retrofit.create(ApiService::class.java)
        private set

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setLenient().create()
                )
            )
            .build()
    }

    @Synchronized
    fun updateBaseUrl(ip: String) {
        val url = "http://$ip:8080/"
        if (url == currentBaseUrl) return
        currentBaseUrl = url
        retrofit = buildRetrofit(url)
        apiService = retrofit.create(ApiService::class.java)
    }

    fun getBaseUrl(): String = currentBaseUrl
}
