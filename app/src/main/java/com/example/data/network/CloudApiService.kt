package com.example.data.network

import com.example.data.database.DocumentEntity
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val status: String,
    val message: String,
    val syncedCount: Int,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class CloudStatusResponse(
    val online: Boolean,
    val activeEditors: Int,
    val driveState: String
)

interface CloudApiService {
    @POST("api/office/backup")
    suspend fun backupDocuments(
        @Body documents: List<DocumentEntity>,
        @Query("drive") drive: String
    ): Response<SyncResponse>

    @GET("api/office/status")
    suspend fun checkCloudStatus(
        @Query("drive") drive: String
    ): Response<CloudStatusResponse>
}
