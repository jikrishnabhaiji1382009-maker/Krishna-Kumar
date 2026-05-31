package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.network.*
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MovieRepository(private val context: Context, private val movieDao: MovieDao) {

    val allMovies: Flow<List<MovieEntity>> = movieDao.getAllMovies()
    val watchlist: Flow<List<WatchlistEntity>> = movieDao.getWatchlist()

    // Seeds default movies if database is empty
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val currentMovies = allMovies.first()
        if (currentMovies.isEmpty()) {
            Log.d("MovieRepository", "Seeding initial movies...")
            val seeds = getSeedMovies()
            movieDao.insertMovies(seeds)
        }
    }

    // Dynamic AI fetch to ADD and UPDATE latest release movies and web series automatically!
    suspend fun fetchLatestNewReleasesAsync(): Result<Unit> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("MovieRepository", "Gemini API Key is missing or default placeholder.")
            return@withContext Result.failure(Exception("Gemini API key is not configured in AI Studio Secrets!"))
        }

        val prompt = """
            You are a premium movie and series curation engine. Generate a JSON list of exactly 8 highly anticipated or recently released blockbuster movies and top-rated television web series of late 2024, 2025, or 2026.
            
            Return ONLY a valid JSON array of objects. Do not include any HTML markdown backticks or explanations.
            Each object MUST strictly have these details:
            - id: String (unique identifier, e.g., "new_m1", "new_s2")
            - title: String (real title of the movie or web series)
            - type: String (either "Movie" or "TV Series")
            - posterUrl: String (active, high-quality, relevant search terms Unsplash image links of portrait size - e.g., https://images.unsplash.com/photo-X?q=80&w=400&fit=crop)
            - backdropUrl: String (active, wide landscape Unsplash image links - e.g. https://images.unsplash.com/photo-Y?q=80&w=800&fit=crop)
            - description: String (compelling cinematic plot summary without spoilers)
            - rating: String (realistic rating like "8.7/10" or "9.1/10")
            - releaseYear: String (must be "2024", "2025", or "2026")
            - durationOrEpisodes: String (e.g. "2h 15m" or "Season 1 (8 Episodes)")
            - videoUrl: String (MUST be an active stream link, select from these fast public test MP4 links: 
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", 
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", 
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
            - genre: String (e.g. "Sci-Fi & Cyberpunk", "Action, Thriller", "Mystery & Drama")
            - cast: String (comma-separated lead actors)
            - director: String (lead directors or creators)
            - isNewRelease: Boolean (MUST be true since these are latest releases)
            - isTrending: Boolean (true or false)
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.85f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Received empty response from Gemini curation endpoint."))
            }

            // Clean json text from markdown formatting in case the model ignored responseMimeType instructions
            val cleanedJsonText = cleanRawJsonText(jsonText)

            Log.d("MovieRepository", "Gemini response text: $cleanedJsonText")

            val moshi = RetrofitClient.genericMoshi
            val listMyType = Types.newParameterizedType(List::class.java, NetworkMovie::class.java)
            val adapter = moshi.adapter<List<NetworkMovie>>(listMyType)

            val parsedNetworkMovies = adapter.fromJson(cleanedJsonText)
            if (parsedNetworkMovies.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("Failed to decode JSON representation into Movies list."))
            }

            // Map network movies to Room movie entities
            val movieEntities = parsedNetworkMovies.mapIndexed { index, netMovie ->
                MovieEntity(
                    id = netMovie.id,
                    title = netMovie.title,
                    type = netMovie.type,
                    posterUrl = netMovie.posterUrl,
                    backdropUrl = netMovie.backdropUrl,
                    description = netMovie.description,
                    rating = netMovie.rating,
                    releaseYear = netMovie.releaseYear,
                    durationOrEpisodes = netMovie.durationOrEpisodes,
                    videoUrl = netMovie.videoUrl,
                    genre = netMovie.genre,
                    cast = netMovie.cast,
                    director = netMovie.director,
                    isNewRelease = netMovie.isNewRelease,
                    isTrending = netMovie.isTrending,
                    orderIndex = -100 + index // Make sure these newly fetched items appear first at the top of lists!
                )
            }

            // Update local SQLite db
            movieDao.insertMovies(movieEntities)
            Log.d("MovieRepository", "Successfully inserted ${movieEntities.size} auto-curated movies into local database.")
            return@withContext Result.success(Unit)

        } catch (e: Exception) {
            Log.e("MovieRepository", "Error on AI Movie Curation", e)
            return@withContext Result.failure(e)
        }
    }

    // Clean any markdown formatting wrap like ```json ... ```
    private fun cleanRawJsonText(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter("```")
            if (text.startsWith("json")) {
                text = text.substringAfter("json")
            }
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        return text.trim()
    }

    // Dynamic AI Smart AI Review Generator using Gemini!
    suspend fun generateShortAIReview(movieTitle: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key not configured. (Configure Gemini Secrets in AI Studio to load dynamic AI critic reviews!)"
        }

        val prompt = "Write a highly professional, short critical review (1-2 sentences) in the style of Rotten Tomatoes or Roger Ebert for the movie: '$movieTitle'. Keep it engaging and artistic."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Review unavailable."
        } catch (e: Exception) {
            "Critical review: Intricate performances and pacing make this a remarkable watch."
        }
    }

    // Watchlist Persistence Methods
    suspend fun addToWatchlist(id: String) {
        movieDao.addToWatchlist(WatchlistEntity(id))
    }

    suspend fun removeFromWatchlist(id: String) {
        movieDao.removeFromWatchlist(id)
    }

    suspend fun isBookmarked(id: String): Boolean {
        return movieDao.isBookmarked(id)
    }

    fun observeIsBookmarked(id: String): Flow<Boolean> {
        return movieDao.observeIsBookmarked(id)
    }

    // Static high-quality initial seed data for movies and series
    private fun getSeedMovies(): List<MovieEntity> {
        return listOf(
            MovieEntity(
                id = "m1",
                title = "Cosmic Odyssey: Infinity",
                type = "Movie",
                posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=400&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=800&fit=crop",
                description = "When a mysterious spatial node opens near Jupiter, a team of specialized deep-space explorers must cross the galactic event horizon, risking everything to find humanity's new anchor.",
                rating = "8.9/10",
                releaseYear = "2024",
                durationOrEpisodes = "2h 42m",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                genre = "Sci-Fi & Adventure",
                cast = "Mathew McConnel, Anne Hathaway, Jessica Chaste",
                director = "Christopher Nolan style",
                isNewRelease = true,
                isTrending = true,
                orderIndex = 1
            ),
            MovieEntity(
                id = "m2",
                title = "Slate: Out of Shadows",
                type = "Movie",
                posterUrl = "https://images.unsplash.com/photo-1511556532299-8f662fc26c06?q=80&w=400&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?q=80&w=800&fit=crop",
                description = "An undercover agent operating in a near-future cyberpunk metropolis gets framed for high-level digital sabotage. Armed with smart tech, he hunts down the shadow conspiracy behind the slate.",
                rating = "8.4/10",
                releaseYear = "2024",
                durationOrEpisodes = "2h 14m",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                genre = "Action & Cyberpunk",
                cast = "Keanu Reeves, Carrie-Anne Moss",
                director = "Wachowski crew",
                isNewRelease = true,
                isTrending = true,
                orderIndex = 2
            ),
            MovieEntity(
                id = "m3",
                title = "Mind Over Matter",
                type = "Movie",
                posterUrl = "https://images.unsplash.com/photo-1507413245164-6160d8298b31?q=80&w=400&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?q=80&w=800&fit=crop",
                description = "In a psychological paradigm shifting experiment, a neuroscientist discovers how to materialize dreams into palpable energy, only to unlock an untamable anomaly within her mind.",
                rating = "7.8/10",
                releaseYear = "2023",
                durationOrEpisodes = "1h 56m",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                genre = "Drama & Sci-Fi",
                cast = "Florence Pugh, Cillian Murphy",
                director = "Danny Boyle",
                isNewRelease = false,
                isTrending = true,
                orderIndex = 3
            ),
            MovieEntity(
                id = "m4",
                title = "Echoes of the Night",
                type = "Movie",
                posterUrl = "https://images.unsplash.com/photo-1509248961158-e54f6934749c?q=80&w=400&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1502082553048-f009c37129b9?q=80&w=800&fit=crop",
                description = "A quiet architectural research project in a remote, misty pine valley turns into an intense psychological struggle when a sound specialist uncovers rhythmic ultrasound signals.",
                rating = "8.1/10",
                releaseYear = "2024",
                durationOrEpisodes = "2h 02m",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                genre = "Suspense & Thriller",
                cast = "Anya Taylor-Joy, Nicholas Hoult",
                director = "Robert Eggers",
                isNewRelease = true,
                isTrending = false,
                orderIndex = 4
            ),
            MovieEntity(
                id = "m5",
                title = "The Neon Grid: Tokyo",
                type = "TV Series",
                posterUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=400&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=800&fit=crop",
                description = "In the dense alleyways of Neo-Tokyo, rival synthetic underground hacker crews race to build the ultimate autonomous core AI, while avoiding ruthless robotic authorities.",
                rating = "9.1/10",
                releaseYear = "2024",
                durationOrEpisodes = "Season 1 (10 Episodes)",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                genre = "Cyberpunk Series",
                cast = "Hiroyuki Sanada, Rinko Kikuchi",
                director = "Shinichiro Watanabe stylings",
                isNewRelease = true,
                isTrending = true,
                orderIndex = 5
            ),
            MovieEntity(
                id = "m6",
                title = "Midnight Protocol",
                type = "TV Series",
                posterUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?q=80&w=400&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1504384308090-c894fdcc538d?q=80&w=800&fit=crop",
                description = "When a massive cyber breach triggers a countdown on national systems, a localized team of cybersecurity analysts must race against time to execute the ultimate decryption protocols.",
                rating = "8.6/10",
                releaseYear = "2023",
                durationOrEpisodes = "Season 2 (8 Episodes)",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                genre = "Tech & Suspense",
                cast = "Rami Malek, Christian Slater",
                director = "Sam Esmail",
                isNewRelease = false,
                isTrending = true,
                orderIndex = 6
            ),
            MovieEntity(
                id = "m7",
                title = "Quantum Horizon",
                type = "TV Series",
                posterUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=400&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=800&fit=crop",
                description = "An intricate scientific series exploring chronological displacement portals, following researchers as they establish bridges to multiple alternative historical realities.",
                rating = "8.8/10",
                releaseYear = "2024",
                durationOrEpisodes = "Season 1 (6 Episodes)",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                genre = "Mind-Bending TV",
                cast = "Benedict Cumberbatch, Elizabeth Olsen",
                director = "Alex Garland",
                isNewRelease = true,
                isTrending = false,
                orderIndex = 7
            )
        )
    }
}
