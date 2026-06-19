package com.example.rateme.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albums",
    foreignKeys = [ForeignKey(
        entity = Artist::class,
        parentColumns = ["id"],
        childColumns = ["artistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("artistId")]
)
data class Album(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artistId: Long,
    val coverUrl: String? = null
)