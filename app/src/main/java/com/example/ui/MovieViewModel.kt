package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MovieEntity
import com.example.data.MovieRepository
import com.example.data.WatchlistEntity
import com.example.network.GenerateContentRequest
import com.example.network.Content
import com.example.network.Part
import com.example.network.GenerationConfig
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MovieViewModel(
    application: Application,
    private val repository: MovieRepository
) : AndroidViewModel(application) {

    // Refresh and status flags
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    // Active screen selection or route state
    private val _selectedMovie = MutableStateFlow<MovieEntity?>(null)
    val selectedMovie = _selectedMovie.asStateFlow()

    // Movie list and Watchlist flows from cache/Room DB
    val allMoviesList: StateFlow<List<MovieEntity>> = repository.allMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistEntities: StateFlow<List<WatchlistEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live calculations of watchlist movies
    val watchlistMoviesList: StateFlow<List<MovieEntity>> = combine(allMoviesList, watchlistEntities) { movies, watchlist ->
        val watchlistIds = watchlist.map { it.id }.toSet()
        movies.filter { it.id in watchlistIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query inputs
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre = _selectedGenre.asStateFlow()

    val filteredMovies: StateFlow<List<MovieEntity>> = combine(allMoviesList, searchQuery, selectedGenre) { movies, query, genre ->
        movies.filter { movie ->
            val matchQuery = query.isBlank() || 
                    movie.title.contains(query, ignoreCase = true) || 
                    movie.genre.contains(query, ignoreCase = true) ||
                    movie.cast.contains(query, ignoreCase = true) ||
                    movie.director.contains(query, ignoreCase = true) ||
                    movie.description.contains(query, ignoreCase = true)

            val matchGenre = genre == "All" || movie.genre.contains(genre, ignoreCase = true)

            matchQuery && matchGenre
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Movie category groupings
    val trendingMovies: StateFlow<List<MovieEntity>> = allMoviesList.map { list ->
        list.filter { it.isTrending }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val newReleaseMovies: StateFlow<List<MovieEntity>> = allMoviesList.map { list ->
        list.filter { it.isNewRelease }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionMovies: StateFlow<List<MovieEntity>> = allMoviesList.map { list ->
        list.filter { it.genre.contains("Action", ignoreCase = true) || it.genre.contains("Cyberpunk", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dramaOrSciFiMovies: StateFlow<List<MovieEntity>> = allMoviesList.map { list ->
        list.filter { it.genre.contains("Sci-Fi", ignoreCase = true) || it.genre.contains("Suspense", ignoreCase = true) || it.genre.contains("Mind", ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic AI smart content details
    private val _activeMovieReview = MutableStateFlow<String?>(null)
    val activeMovieReview = _activeMovieReview.asStateFlow()

    private val _isLoadingReview = MutableStateFlow(false)
    val isLoadingReview = _isLoadingReview.asStateFlow()

    // Smart AI recommendation agent state ("Mood Stream")
    private val _aiRecommendation = MutableStateFlow<String?>(null)
    val aiRecommendation = _aiRecommendation.asStateFlow()

    private val _isLoadingRecommendation = MutableStateFlow(false)
    val isLoadingRecommendation = _isLoadingRecommendation.asStateFlow()

    private val _chatbotQuery = MutableStateFlow("")
    val chatbotQuery = _chatbotQuery.asStateFlow()

    init {
        // Run seed check first
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Toggle items in watchlist
    fun toggleWatchlist(movieId: String) {
        viewModelScope.launch {
            val list = watchlistEntities.value
            val isAdded = list.any { it.id == movieId }
            if (isAdded) {
                repository.removeFromWatchlist(movieId)
                _statusMessage.value = "Removed from My Watchlist"
            } else {
                repository.addToWatchlist(movieId)
                _statusMessage.value = "Added to My Watchlist"
            }
        }
    }

    // Selects movie and loads real-time dynamic AI critical reviews using Gemini!
    fun selectMovie(movie: MovieEntity?) {
        _selectedMovie.value = movie
        _activeMovieReview.value = null
        if (movie != null) {
            viewModelScope.launch {
                _isLoadingReview.value = true
                try {
                    val review = repository.generateShortAIReview(movie.title)
                    _activeMovieReview.value = review
                } catch (e: Exception) {
                    _activeMovieReview.value = "An immersive, cinematic story driven by strong direction and solid chemistry."
                } finally {
                    _isLoadingReview.value = false
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenreFilter(genre: String) {
        _selectedGenre.value = genre
    }

    fun updateChatQuery(query: String) {
        _chatbotQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Fetches and adds the newest releases dynamically using Gemini
    fun refreshNewReleases() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _statusMessage.value = "Tuning into FreeFlix live curation engines..."
            
            val result = repository.fetchLatestNewReleasesAsync()
            _isRefreshing.value = false
            
            if (result.isSuccess) {
                _statusMessage.value = "FreeFlix Live updated: Blockbusters & premium series loaded successfully!"
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown Connection Error"
                _statusMessage.value = "Streaming seed backup catalog. (To active active-curations, register your Gemini API key inside AI Studio secrets!)"
                Log.e("MovieViewModel", "Refresh Error: $errorMsg")
            }
        }
    }

    // AI Recommendation Conversation (Using current movies metadata and context)
    fun requestAIRecommendation() {
        val query = chatbotQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoadingRecommendation.value = true
            _aiRecommendation.value = "Analyzing cinematic databases and searching for matching trends..."

            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                _aiRecommendation.value = "Recommendation Engine Restricted: To explore personal mood recommendations, please register your Gemini API Key in the AI Studio secrets panel! Based on your query: '$query', we highly recommend watching '${trendingMovies.value.firstOrNull()?.title ?: "Cosmic Odyssey"}' from our free streaming catalog!"
                _isLoadingRecommendation.value = false
                return@launch
            }

            // Summarize current movies list for Gemini model context
            val moviesContext = allMoviesList.value.joinToString("\n") { 
                "- ${it.title} (${it.type}): Genre: ${it.genre}, Release Year: ${it.releaseYear}, Cast: ${it.cast}, Plot: ${it.description}, StreamID: ${it.id}"
            }

            val prompt = """
                You are FreeFlix AI CineBot, a master film critic and entertainment companion. The user has given this mood, interest or request: "$query".
                
                Analyze the following available FreeFlix movies catalog and recommend the best matching titles (recommend up to 2 items). Explain EXACTLY why they fit the user's specific mood using an enthusiastic, engaging, cinematic style.
                
                If nothing fits perfectly, suggest what matches best or curates a new concept that fits their mood, explaining that they can trigger 'Live Curation' to automatically download and watch fresh content!
                
                Available Catalog Context:
                $moviesContext
                
                Format your response using clean, brief paragraphs. Always highlight recommended titles in bold, and refer to them with their exact catalog title.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(temperature = 0.75f)
            )

            try {
                val response = RetrofitClient.service.generateContent(apiKey, request)
                _aiRecommendation.value = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    ?: "FreeFlix assistant is tuning its antennas. Please try asking again shortly!"
            } catch (e: Exception) {
                _aiRecommendation.value = "Error compiling recommendation: ${e.message}. FreeFlix backing is offline."
            } finally {
                _isLoadingRecommendation.value = false
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: MovieRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MovieViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MovieViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
