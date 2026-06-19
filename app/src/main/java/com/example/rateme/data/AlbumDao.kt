package com.example.rateme.data

import androidx.room.*
import com.example.rateme.data.model.Album
import com.example.rateme.data.model.Artist
import com.example.rateme.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Insert
    suspend fun insertArtist(artist: Artist): Long

    @Insert
    suspend fun insertAlbum(album: Album): Long

    @Insert
    suspend fun insertSong(song: Song)

    @Update
    suspend fun updateAlbum(album: Album)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Query("UPDATE songs SET rating = :score WHERE id = :songId")
    suspend fun updateRating(songId: Long, score: Int)

    @Transaction
    @Query("SELECT * FROM albums ORDER BY title")
    fun getAllAlbumsWithSongs(): Flow<List<AlbumWithArtistAndSongs>>

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun getAlbumWithSongs(albumId: Long): Flow<AlbumWithArtistAndSongs?>

    @Query("SELECT AVG(CAST(rating AS REAL)) FROM songs WHERE albumId = :albumId AND rating IS NOT NULL")
    suspend fun getAverageRating(albumId: Long): Double?
}

data class AlbumWithArtistAndSongs(
    @Embedded val album: Album,
    @Relation(parentColumn = "artistId", entityColumn = "id")
    val artist: Artist,
    @Relation(parentColumn = "id", entityColumn = "albumId")
    val songs: List<Song>
)