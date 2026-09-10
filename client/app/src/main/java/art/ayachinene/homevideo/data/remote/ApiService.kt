package art.ayachinene.homevideo.data.remote

import art.ayachinene.homevideo.data.model.DirectoryItem
import art.ayachinene.homevideo.data.model.DirectoryResponse
import art.ayachinene.homevideo.data.model.HistoryItem
import art.ayachinene.homevideo.data.model.LibraryResponse
import art.ayachinene.homevideo.data.model.VideoInfo
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @GET("api/health")
    suspend fun health(): Map<String, String>

    @GET("api/files")
    suspend fun listFiles(
        @Query("path") path: String = "/",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("sort") sort: String = "name",
        @Query("order") order: String = "asc"
    ): DirectoryResponse

    @GET("api/files/search")
    suspend fun searchFiles(
        @Query("keyword") keyword: String
    ): List<DirectoryItem>

    @GET("api/files/library")
    suspend fun getLibrary(): LibraryResponse

    @GET("api/videos/info")
    suspend fun getVideoInfo(
        @Query("path") path: String
    ): VideoInfo

    @Streaming
    @GET("api/videos/stream")
    suspend fun streamVideo(@Query("path") path: String): ResponseBody

    @GET("api/videos/thumbnail")
    suspend fun getThumbnail(@Query("path") path: String): ResponseBody

    @GET("api/videos/subtitles")
    suspend fun getSubtitle(
        @Query("path") path: String,
        @Query("name") name: String
    ): ResponseBody

    @GET("api/history")
    suspend fun getHistory(): List<HistoryItem>

    @POST("api/history")
    suspend fun saveHistory(@Body body: Map<String, Any>): HistoryItem

    @DELETE("api/history")
    suspend fun deleteHistory(@Query("videoPath") videoPath: String? = null)
}
