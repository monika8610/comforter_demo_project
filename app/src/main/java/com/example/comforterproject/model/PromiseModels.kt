package com.example.comforterproject.model

import com.google.gson.annotations.SerializedName

data class PromiseResponse(
    val status: Boolean,
    val response: List<PromiseItem>
)

data class PromiseItem(
    val id: Int,
    val schedule: String?,
    @SerializedName("image_name_large")
    val imageNameLarge: String?,
    @SerializedName("image_name_thumb")
    val imageNameThumb: String?
)
