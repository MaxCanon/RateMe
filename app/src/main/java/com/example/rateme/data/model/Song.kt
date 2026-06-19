package com.example.rateme.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    foreignKeys = [ForeignKey(
        entity = Album::class,
        parentColumns = ["id"],
        childColumns = ["albumId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("albumId")]
)
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val albumId: Long,
    val trackNumber: Int,
    val rating: Int? = null,
    val previewUrl: String? = null
)