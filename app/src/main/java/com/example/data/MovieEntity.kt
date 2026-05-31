package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "Movie", "TV Series", "Special"
    val posterUrl: String,
    val backdropUrl: String,
    val description: String,
    val rating: String,
    val releaseYear: String,
    val durationOrEpisodes: String,
    val videoUrl: String,
    val genre: String,
    val cast: String,
    val director: String,
    val isNewRelease: Boolean,
    val isTrending: Boolean,
    val isBookmarked: Boolean = false,
    val orderIndex: Int = 0 // For preserving list order
)
