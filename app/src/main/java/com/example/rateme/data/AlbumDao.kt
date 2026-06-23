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
    @Query("SELECT * FROM albums ORDER BY id DESC")
    fun getAllAlbumsWithSongs(): Flow<List<AlbumWithArtistAndSongs>>

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun getAlbumWithSongs(albumId: Long): Flow<AlbumWithArtistAndSongs?>

    @Query("""
        SELECT a.id, a.title, a.artistId, a.coverUrl, a.year, 
               AVG(CAST(s.rating AS REAL)) as avgRating, 
               ar.name as artistName
        FROM albums a 
        INNER JOIN songs s ON s.albumId = a.id 
        INNER JOIN artists ar ON ar.id = a.artistId
        WHERE s.rating IS NOT NULL 
        GROUP BY a.id 
        ORDER BY avgRating DESC
    """)
    fun getAlbumsByRating(): Flow<List<AlbumWithAvgRating>>

    @Transaction
    @Query("""
        SELECT * FROM albums 
        WHERE id IN (SELECT DISTINCT albumId FROM songs WHERE rating IS NOT NULL)
        ORDER BY id DESC
    """)
    fun getRatedAlbums(): Flow<List<AlbumWithArtistAndSongs>>

    @Query("SELECT COUNT(*) FROM albums WHERE title = :title AND artistId = :artistId")
    suspend fun albumExists(title: String, artistId: Long): Int

    @Query("SELECT * FROM artists WHERE name = :name LIMIT 1")
    suspend fun getArtistByName(name: String): Artist?
}

data class AlbumWithArtistAndSongs(
    @Embedded val album: Album,
    @Relation(parentColumn = "artistId", entityColumn = "id")
    val artist: Artist,
    @Relation(parentColumn = "id", entityColumn = "albumId")
    val songs: List<Song>
)

data class AlbumWithAvgRating(
    val id: Long,
    val title: String,
    val artistId: Long,
    val coverUrl: String?,
    val year: String?,
    val avgRating: Double,
    val artistName: String
)