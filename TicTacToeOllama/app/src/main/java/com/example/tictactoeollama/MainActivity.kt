package com.example.tictactoeollama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tictactoeollama.ui.GameScreen
import com.example.tictactoeollama.ui.theme.TicTacToeOllamaTheme
import com.example.tictactoeollama.viewmodel.TicTacToeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicTacToeOllamaTheme {
                val viewModel: TicTacToeViewModel = viewModel()
                GameScreen(viewModel = viewModel)
            }
        }
    }
}
