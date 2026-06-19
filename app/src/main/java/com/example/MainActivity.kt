package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.DialerRepository
import com.example.ui.DialerApp
import com.example.ui.DialerViewModel
import com.example.ui.DialerViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantiate database components safely 
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = DialerRepository(database.dialerDao())

        // Pass dependencies safely to the ViewModel
        val viewModel: DialerViewModel by viewModels {
            DialerViewModelFactory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                DialerApp(viewModel = viewModel)
            }
        }
    }
}
