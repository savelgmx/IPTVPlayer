package com.portfolio.iptvplayer.domain.model

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val groupTitle: String?,
    val streamUrl: String
)
