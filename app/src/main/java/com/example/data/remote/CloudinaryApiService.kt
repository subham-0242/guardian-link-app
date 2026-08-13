package com.example.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CloudinaryApiService {

    @Multipart
    @POST("v1_1/{cloud_name}/{resource_type}/upload")
    suspend fun uploadMedia(
        @Path("cloud_name") cloudName: String,
        @Path("resource_type") resourceType: String, // "video", "image", "raw", "auto"
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part("timestamp") timestamp: RequestBody? = null,
        @Part("api_key") apiKey: RequestBody? = null,
        @Part("signature") signature: RequestBody? = null,
        @Part("folder") folder: RequestBody? = null,
        @Part("tags") tags: RequestBody? = null
    ): Response<CloudinaryUploadResponse>
}
