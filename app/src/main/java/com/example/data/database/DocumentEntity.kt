package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "writer" | "spreadsheet" | "presentation" | "pdf" | "scanned"
    val content: String, // Holds raw string, cell mapping, slide mapping, or OCR outputs
    val isFavorite: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val size: Long = 0
)
