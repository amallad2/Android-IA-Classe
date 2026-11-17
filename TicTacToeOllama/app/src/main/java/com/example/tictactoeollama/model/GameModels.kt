package com.example.tictactoeollama.model

enum class Player {
    X, // Human
    O  // Ollama
}

enum class GameState {
    PLAYING,
    X_WON,
    O_WON,
    DRAW
}

data class GameUiState(
    val board: List<String> = List(9) { "" },
    val currentTurn: Player = Player.X,
    val gameState: GameState = GameState.PLAYING,
    val humanScore: Int = 0,
    val ollamaScore: Int = 0,
    val drawScore: Int = 0,
    val isOllamaThinking: Boolean = false,
    val ollamaUrl: String = "http://10.0.2.2:11434",
    val modelName: String = "llama3.2",
    val statusMessage: String = "Tu turno (X)"
)
