package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.MovieRepository
import com.example.ui.MovieViewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Database, Repository, and ViewModel instantiation
        val database = AppDatabase.getDatabase(this)
        val repository = MovieRepository(this, database.movieDao())
        val factory = MovieViewModel.Factory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[MovieViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
