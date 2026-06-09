package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.database.AppDatabase
import com.example.data.repository.DocumentRepository
import com.example.ui.screens.MainOfficeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OfficeViewModel
import com.example.ui.viewmodel.OfficeViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = DocumentRepository(database.documentDao())
    val officeViewModel: OfficeViewModel by viewModels {
      OfficeViewModelFactory(repository)
    }

    setContent {
      // Get system dark state initially and bind to settings
      val systemDark = isSystemInDarkTheme()
      var isDarkTheme by remember { mutableStateOf(systemDark) }

      MyApplicationTheme(
        darkTheme = isDarkTheme,
        dynamicColor = false // Force custom Office slate/indigo colors to stand out instead of dynamic wallpaper tints
      ) {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          MainOfficeScreen(
            viewModel = officeViewModel,
            isDarkTheme = isDarkTheme,
            onDarkThemeChange = { isDarkTheme = it }
          )
        }
      }
    }
  }
}

