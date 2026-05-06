package com.ecobook.data

import android.content.Context
import android.net.Uri
import com.ecobook.api.MaterialApiService
import com.ecobook.dto.ApiErrorResponseDTO
import com.ecobook.dto.CreateMaterialRequestDTO
import com.ecobook.dto.GeminiResponseDTO
import com.ecobook.dto.MaterialDTO
import com.ecobook.material.ImageCompressionHelper
import com.ecobook.material.ImagePickerHelper
import com.ecobook.material.ImageSource
import com.ecobook.material.SelectedImageUiModel
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

@Singleton
class MaterialRepository @Inject constructor(
    private val materialApiService: MaterialApiService,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {

    fun describeImage(uri: Uri, source: ImageSource): SelectedImageUiModel {
        return ImagePickerHelper.describeImage(context, uri, source)
    }

    suspend fun previewImage(image: SelectedImageUiModel): GeminiResponseDTO = withContext(Dispatchers.IO) {
        val preparedImage = ImageCompressionHelper.prepareForUpload(context, image.uri, image.fileName)
        val requestBody = preparedImage.bytes.toRequestBody(preparedImage.mimeType.toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", preparedImage.fileName, requestBody)
        requireBody(materialApiService.previewMaterial(filePart))
    }

    suspend fun createMaterial(request: CreateMaterialRequestDTO): MaterialDTO {
        return requireBody(materialApiService.createMaterial(request))
    }

    private fun <T> requireBody(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body() ?: throw ApiException(response.code(), "Resposta vazia do servidor")
        }

        val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { payload ->
            runCatching { gson.fromJson(payload, ApiErrorResponseDTO::class.java) }.getOrNull()
        }

        throw ApiException(
            statusCode = response.code(),
            message = error?.message ?: "Falha ao processar a requisicao",
            fieldErrors = error?.fieldErrors ?: emptyMap()
        )
    }
}
