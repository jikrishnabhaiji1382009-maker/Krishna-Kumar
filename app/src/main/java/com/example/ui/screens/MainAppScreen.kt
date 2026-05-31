package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.MovieEntity
import com.example.ui.MovieViewModel
import com.example.ui.components.FreeVideoPlayer

enum class AppTab {
    HOME, SEARCH, WATCHLIST, CINEBOT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(AppTab.HOME) }
    
    // ViewModel states
    val allMovies by viewModel.allMoviesList.collectAsStateWithLifecycle()
    val watchlistMovies by viewModel.watchlistMoviesList.collectAsStateWithLifecycle()
    val filteredMovies by viewModel.filteredMovies.collectAsStateWithLifecycle()
    val trendingMovies by viewModel.trendingMovies.collectAsStateWithLifecycle()
    val newReleases by viewModel.newReleaseMovies.collectAsStateWithLifecycle()
    val actionMovies by viewModel.actionMovies.collectAsStateWithLifecycle()
    val scifiMovies by viewModel.dramaOrSciFiMovies.collectAsStateWithLifecycle()
    
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val activeMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // Video playback active flag
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoTitle by remember { mutableStateOf("") }

    // Display user messages when status changes
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF090A0E), // Cinematic deep dark background
        bottomBar = {
            if (activeVideoUrl == null) {
                FreeFlixBottomBar(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views switcher
            when (activeTab) {
                AppTab.HOME -> {
                    HomeScreenLayout(
                        trendingMovies = trendingMovies,
                        newReleases = newReleases,
                        actionMovies = actionMovies,
                        scifiMovies = scifiMovies,
                        isRefreshing = isRefreshing,
                        onMovieClick = { viewModel.selectMovie(it) },
                        onRefreshTrigger = { viewModel.refreshNewReleases() }
                    )
                }
                AppTab.SEARCH -> {
                    SearchScreenLayout(
                        searchQuery = searchQuery,
                        selectedGenre = selectedGenre,
                        filteredMovies = filteredMovies,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onGenreSelected = { viewModel.setGenreFilter(it) },
                        onMovieClick = { viewModel.selectMovie(it) }
                    )
                }
                AppTab.WATCHLIST -> {
                    WatchlistScreenLayout(
                        watchlistMovies = watchlistMovies,
                        onMovieClick = { viewModel.selectMovie(it) }
                    )
                }
                AppTab.CINEBOT -> {
                    CineBotScreenLayout(
                        viewModel = viewModel
                    )
                }
            }

            // High-fidelity Movie Details Overlay dialog
            activeMovie?.let { movie ->
                MovieDetailsSheet(
                    movie = movie,
                    viewModel = viewModel,
                    onClose = { viewModel.selectMovie(null) },
                    onPlayClick = {
                        activeVideoUrl = movie.videoUrl
                        activeVideoTitle = movie.title
                        viewModel.selectMovie(null) // dismiss sheet
                    }
                )
            }

            // Deep level media streaming player
            activeVideoUrl?.let { url ->
                FreeVideoPlayer(
                    videoUrl = url,
                    title = activeVideoTitle,
                    onClose = { activeVideoUrl = null }
                )
            }
        }
    }
}

@Composable
fun FreeFlixBottomBar(
    activeTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = Color(0xFF0F1015),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = activeTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFE50914),
                selectedTextColor = Color(0xFFE50914),
                unselectedIconColor = Color(0xFF7F818A),
                unselectedTextColor = Color(0xFF7F818A),
                indicatorColor = Color(0xFFE50914).copy(alpha = 0.08f)
            ),
            modifier = Modifier.testTag("nav_home")
        )
        NavigationBarItem(
            selected = activeTab == AppTab.SEARCH,
            onClick = { onTabSelected(AppTab.SEARCH) },
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFE50914),
                selectedTextColor = Color(0xFFE50914),
                unselectedIconColor = Color(0xFF7F818A),
                unselectedTextColor = Color(0xFF7F818A),
                indicatorColor = Color(0xFFE50914).copy(alpha = 0.08f)
            ),
            modifier = Modifier.testTag("nav_search")
        )
        NavigationBarItem(
            selected = activeTab == AppTab.WATCHLIST,
            onClick = { onTabSelected(AppTab.WATCHLIST) },
            icon = { Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Watchlist") },
            label = { Text("My List", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFE50914),
                selectedTextColor = Color(0xFFE50914),
                unselectedIconColor = Color(0xFF7F818A),
                unselectedTextColor = Color(0xFF7F818A),
                indicatorColor = Color(0xFFE50914).copy(alpha = 0.08f)
            ),
            modifier = Modifier.testTag("nav_watchlist")
        )
        NavigationBarItem(
            selected = activeTab == AppTab.CINEBOT,
            onClick = { onTabSelected(AppTab.CINEBOT) },
            icon = { Icon(imageVector = Icons.Default.MovieFilter, contentDescription = "AI CineBot") },
            label = { Text("AI CineBot", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFE50914),
                selectedTextColor = Color(0xFFE50914),
                unselectedIconColor = Color(0xFF7F818A),
                unselectedTextColor = Color(0xFF7F818A),
                indicatorColor = Color(0xFFE50914).copy(alpha = 0.08f)
            ),
            modifier = Modifier.testTag("nav_cinebot")
        )
    }
}

// ---------------- HOME VIEW ----------------
@Composable
fun HomeScreenLayout(
    trendingMovies: List<MovieEntity>,
    newReleases: List<MovieEntity>,
    actionMovies: List<MovieEntity>,
    scifiMovies: List<MovieEntity>,
    isRefreshing: Boolean,
    onMovieClick: (MovieEntity) -> Unit,
    onRefreshTrigger: () -> Unit
) {
    val topHero = trendingMovies.firstOrNull() ?: newReleases.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
    ) {
        // App Header Brand Icon
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "FREEFLIX",
                    color = Color(0xFFE50914),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Curation auto trigger
                    IconButton(
                        onClick = onRefreshTrigger,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF14151C), shape = CircleShape)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = Color(0xFFE50914),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Auto Curation Update",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Simple User profile icon placeholder
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF00bcd4), shape = RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hero Spotlight Banner
        if (topHero != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clickable { onMovieClick(topHero) }
                ) {
                    AsyncImage(
                        model = topHero.backdropUrl,
                        contentDescription = topHero.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Visual Dark Gradient Shadow Shield to fade screen bottom
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF090A0E).copy(alpha = 0.5f),
                                        Color(0xFF090A0E)
                                    )
                                )
                            )
                    )

                    // Hero Labels & Info Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE50914)
                        ) {
                            Text(
                                text = "TRENDING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = topHero.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${topHero.genre} • Rating: ${topHero.rating}",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = { onMovieClick(topHero) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.width(140.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch Trailer",
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Free", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = { onMovieClick(topHero) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2129)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.width(140.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Info State", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Category Lists
        item {
            MovieRowSection(
                title = "New Releases (Auto-Updating)",
                movies = newReleases,
                onMovieSelected = onMovieClick
            )
        }

        item {
            MovieRowSection(
                title = "Trending Hot Movies",
                movies = trendingMovies,
                onMovieSelected = onMovieClick
            )
        }

        item {
            MovieRowSection(
                title = "Action & High Voltage Cyberpunk",
                movies = actionMovies,
                onMovieSelected = onMovieClick
            )
        }

        item {
            MovieRowSection(
                title = "Sci-Fi, Space & Mind-Benders",
                movies = scifiMovies,
                onMovieSelected = onMovieClick
            )
        }

        // Free License disclaimer footer
        item {
            Spacer(modifier = Modifier.height(30.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FreeFlix Streaming Engine",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No Subscriptions • No Ads • Open Source Catalogs",
                    color = Color.DarkGray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun MovieRowSection(
    title: String,
    movies: List<MovieEntity>,
    onMovieSelected: (MovieEntity) -> Unit
) {
    if (movies.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { onMovieSelected(movie) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF14151C))
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Stream badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (movie.type == "Movie") "HD" else "SERIES",
                                color = Color.White,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = movie.title,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------- SEARCH VIEW ----------------
@Composable
fun SearchScreenLayout(
    searchQuery: String,
    selectedGenre: String,
    filteredMovies: List<MovieEntity>,
    onQueryChange: (String) -> Unit,
    onGenreSelected: (String) -> Unit,
    onMovieClick: (MovieEntity) -> Unit
) {
    val genresList = listOf("All", "Sci-Fi", "Action", "Cyberpunk", "Drama", "Suspense", "Thriller")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Explore & Search",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.statusBarsPadding()
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search title, genre, stars, director...", color = Color.Gray, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.LightGray,
                focusedBorderColor = Color(0xFFE50914),
                unfocusedBorderColor = Color(0xFF2C3240),
                focusedContainerColor = Color(0xFF14151C),
                unfocusedContainerColor = Color(0xFF14151C)
            ),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Genre filter row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genresList) { genre ->
                val isSelected = genre == selectedGenre
                Box(
                    modifier = Modifier
                        .clickable { onGenreSelected(genre) }
                        .background(
                            color = if (isSelected) Color(0xFFE50914) else Color(0xFF1C1E26),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = genre,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Results List
        if (filteredMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "No results",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No movies match your criteria.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1.0f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredMovies, key = { it.id }) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14151C), RoundedCornerShape(6.dp))
                            .clickable { onMovieClick(movie) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            modifier = Modifier
                                .width(60.dp)
                                .height(90.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = movie.genre,
                                color = Color(0xFFE50914),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = movie.description,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
                        }

                        IconButton(onClick = { onMovieClick(movie) }) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play Stream",
                                tint = Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- WATCHLIST VIEW ----------------
@Composable
fun WatchlistScreenLayout(
    watchlistMovies: List<MovieEntity>,
    onMovieClick: (MovieEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "My Watchlist",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.statusBarsPadding()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (watchlistMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Empty Watchlist",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Your Watchlist is Empty",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Save your favorite movies and web series to access them free, anytime without advertisements.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1.0f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(watchlistMovies, key = { it.id }) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14151C), RoundedCornerShape(6.dp))
                            .clickable { onMovieClick(movie) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            modifier = Modifier
                                .width(55.dp)
                                .height(80.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${movie.type} • ${movie.genre}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Rating: ${movie.rating}",
                                color = Color(0xFFFFB300),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(onClick = { onMovieClick(movie) }) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Play",
                                tint = Color(0xFFE50914)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CINEBOT VIEW ----------------
@Composable
fun CineBotScreenLayout(
    viewModel: MovieViewModel
) {
    val context = LocalContext.current
    val query by viewModel.chatbotQuery.collectAsStateWithLifecycle()
    val rawRecommendation by viewModel.aiRecommendation.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingRecommendation.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.Default.MovieFilter,
                contentDescription = "CineBot icon",
                tint = Color(0xFFE50914),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI CineBot Mood-Streamer",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tell the AI what you feel like watching, and our servers will immediately suggest matching FreeFlix blockbusters!",
            color = Color.Gray,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // CineBot prompt entry
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateChatQuery(it) },
                placeholder = { Text("e.g. hack cyber or heroic space journey", color = Color.DarkGray, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("cinebot_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = Color(0xFFE50914),
                    unfocusedBorderColor = Color(0xFF2C3240),
                    focusedContainerColor = Color(0xFF14151C),
                    unfocusedContainerColor = Color(0xFF14151C)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = { viewModel.requestAIRecommendation() },
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFE50914), shape = RoundedCornerShape(8.dp)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Query AI", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendation Text Result
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
                .background(Color(0xFF14151C), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFF2C3240), RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (rawRecommendation == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = "Bot chat",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Awaken CineBot Curation",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type your personalized request above to explore! We'll search and analyze trends to highlight what fits best.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                SelectionContainer {
                    Text(
                        text = rawRecommendation!!,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ---------------- MOVIE DETAILS MODAL ----------------
@Composable
fun MovieDetailsSheet(
    movie: MovieEntity,
    viewModel: MovieViewModel,
    onClose: () -> Unit,
    onPlayClick: () -> Unit
) {
    val watchlist by viewModel.watchlistEntities.collectAsStateWithLifecycle()
    val isAlreadyAdded = watchlist.any { it.id == movie.id }
    
    val aiReview by viewModel.activeMovieReview.collectAsStateWithLifecycle()
    val isLoadingReview by viewModel.isLoadingReview.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onClose), // Click background to close
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(Color(0xFF14151C), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .border(1.dp, Color(0xFF2C3240), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clickable(enabled = false) {} // block click propagation
                .verticalScroll(rememberScrollState())
        ) {
            // Header backdrop with poster overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = movie.backdropUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Overlay shadow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF14151C))
                            )
                        )
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Detail", tint = Color.White)
                }
            }

            // Title and basic markers
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = movie.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = movie.releaseYear,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color.DarkGray
                    ) {
                        Text(
                            text = if (movie.type == "Movie") "MOVIE" else "SHOW SERIES",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = movie.durationOrEpisodes,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = "rating", tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = movie.rating,
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Watch Button
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("watch_now_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Watch", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WATCH FREE (NO ADS)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Add to my watchlist button
                OutlinedButton(
                    onClick = { viewModel.toggleWatchlist(movie.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF2E323E)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(
                        imageVector = if (isAlreadyAdded) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Watchlist toggle",
                        tint = if (isAlreadyAdded) Color.Green else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAlreadyAdded) "My Watchlist Saved" else "Add to My Watchlist",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description plot
                Text(
                    text = movie.description,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Additional details (genre, cast, director)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.background(Color(0xFF1E2129), RoundedCornerShape(6.dp)).padding(12.dp).fillMaxWidth()
                ) {
                    Row {
                        Text("Genre: ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(movie.genre, color = Color.LightGray, fontSize = 11.sp)
                    }
                    Row {
                        Text("Cast: ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(movie.cast, color = Color.LightGray, fontSize = 11.sp)
                    }
                    Row {
                        Text("Director: ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(movie.director, color = Color.LightGray, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic AI Critics Reviews Section (using Gemini)
                Text(
                    text = "AI Critic Smart-Review",
                    fontSize = 13.sp,
                    color = Color(0xFFE50914),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1D24), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    if (isLoadingReview) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("CineBot reviewing...", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        Text(
                            text = aiReview ?: "An outstanding cinematic achievement featuring powerful delivery and visual masterpiece direction.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
