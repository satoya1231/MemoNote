package com.example.memonote

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val deletedAt: Long? = null
)
