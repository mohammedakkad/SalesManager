package com.trader.core.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class CloudinaryUploader(private val client: OkHttpClient) {

    suspend fun uploadReceipt(imageBytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fileName    = "receipt_${System.currentTimeMillis()}.jpg"
                val imageBody   = imageBytes.toRequestBody(MEDIA_TYPE_JPEG)

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, imageBody)
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build()

                val request = Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw CloudinaryUploadException(
                        code    = response.code,
                        message = response.message
                    )
                }

                val rawBody = response.body?.string()
                    ?: throw CloudinaryUploadException(message = "Empty response body")

                JSONObject(rawBody).getString("secure_url")
            }
        }

    class CloudinaryUploadException(
        val code: Int = -1,
        override val message: String = "Upload failed"
    ) : Exception("[$code] $message")

    companion object {
        private const val CLOUD_NAME    = "degdkjohf"
        private const val UPLOAD_PRESET = "receipts"
        private const val UPLOAD_URL    =
            "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

        private val MEDIA_TYPE_JPEG = "image/jpeg".toMediaType()
    }
}
