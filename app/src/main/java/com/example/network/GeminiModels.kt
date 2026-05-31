package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

@JsonClass(generateAdapter = true)
data class NetworkMovie(
    val id: String,
    val title: String,
    val type: String, // Movie or TV Series
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
    val isTrending: Boolean
)
