package com.example.comforterproject.model

import com.google.gson.annotations.SerializedName

data class BannerResponse(
    val status: Boolean,
    val response: List<BannerItem>
)

data class BannerItem(
    val id: Int,
    val heading: String?,
    @SerializedName("image_name_main")
    val imageNameMain: String?
)
