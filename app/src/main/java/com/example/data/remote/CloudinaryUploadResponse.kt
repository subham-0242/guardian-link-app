package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudinaryUploadResponse(
    @Json(name = "public_id") val publicId: String? = null,
    @Json(name = "version") val version: Long? = null,
    @Json(name = "signature") val signature: String? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "format") val format: String? = null,
    @Json(name = "resource_type") val resourceType: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "bytes") val bytes: Long? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "secure_url") val secureUrl: String? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "original_filename") val originalFilename: String? = null,
    @Json(name = "error") val error: CloudinaryErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class CloudinaryErrorDetail(
    @Json(name = "message") val message: String? = null
)
