package com.example.tictactoeollama.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tictactoeollama.data.OllamaRepository
import com.example.tictactoeollama.model.GameState
import com.example.tictactoeollama.model.GameUiState
import com.example.tictactoeollama.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TicTacToeViewModel : ViewModel() {

    private val repository = OllamaRepository()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun updateConfig(url: String, model: String) {
        _uiState.update {
            it.copy(
                ollamaUrl = url.trim(),
                modelName = model.trim()
            )
        }
    }

    fun onCellClicked(index: Int) {
        val currentState = _uiState.value
        // If game is not playing, or cell is not empty, or it's Ollama's turn, ignore click
        if (currentState.gameState != GameState.PLAYING ||
            currentState.board[index].isNotEmpty() ||
            currentState.currentTurn != Player.X ||
            currentState.isOllamaThinking
        ) {
            return
        }

        // Human (X) makes move
        val newBoard = currentState.board.toMutableList()
        newBoard[index] = "X"

        val winState = checkGameWin(newBoard)
        if (winState != GameState.PLAYING) {
            updateAfterGameEnd(newBoard, winState)
            return
        }

        // Switch turn to Ollama (O)
        _uiState.update {
            it.copy(
                board = newBoard,
                currentTurn = Player.O,
                statusMessage = "Ollama (${it.modelName}) está pensando...",
                isOllamaThinking = true
            )
        }

        // Trigger Ollama move
        triggerOllamaTurn(newBoard)
    }

    private fun triggerOllamaTurn(currentBoard: List<String>) {
        val state = _uiState.value
        val availableMoves = currentBoard.indices.filter { currentBoard[it].isEmpty() }

        if (availableMoves.isEmpty()) {
            _uiState.update {
                it.copy(
                    gameState = GameState.DRAW,
                    drawScore = it.drawScore + 1,
                    statusMessage = "Empate!",
                    isOllamaThinking = false
                )
            }
            return
        }

        viewModelScope.launch {
            val result = repository.getOllamaMove(
                baseUrl = state.ollamaUrl,
                model = state.modelName,
                board = currentBoard,
                availableMoves = availableMoves
            )

            val chosenMove = result.getOrElse {
                // Fallback: pick optimal strategic move if Ollama connection fails or errors
                OllamaRepository.getBestMove(currentBoard, availableMoves)
            }

            val updatedBoard = currentBoard.toMutableList()
            updatedBoard[chosenMove] = "O"

            val winState = checkGameWin(updatedBoard)
            if (winState != GameState.PLAYING) {
                updateAfterGameEnd(updatedBoard, winState)
            } else {
                _uiState.update {
                    it.copy(
                        board = updatedBoard,
                        currentTurn = Player.X,
                        statusMessage = "Tu turno (X)",
                        isOllamaThinking = false
                    )
                }
            }
        }
    }

    fun resetGame() {
        _uiState.update {
            it.copy(
                board = List(9) { "" },
                currentTurn = Player.X,
                gameState = GameState.PLAYING,
                statusMessage = "Tu turno (X)",
                isOllamaThinking = false
            )
        }
    }

    private fun updateAfterGameEnd(board: List<String>, gameState: GameState) {
        _uiState.update {
            when (gameState) {
                GameState.X_WON -> it.copy(
                    board = board,
                    gameState = gameState,
                    humanScore = it.humanScore + 1,
                    statusMessage = "¡Has ganado (X)!",
                    isOllamaThinking = false
                )
                GameState.O_WON -> it.copy(
                    board = board,
                    gameState = gameState,
                    ollamaScore = it.ollamaScore + 1,
                    statusMessage = "¡Ollama (${it.modelName}) ha ganado (O)!",
                    isOllamaThinking = false
                )
                GameState.DRAW -> it.copy(
                    board = board,
                    gameState = gameState,
                    drawScore = it.drawScore + 1,
                    statusMessage = "¡Empate!",
                    isOllamaThinking = false
                )
                else -> it
            }
        }
    }

    private fun checkGameWin(board: List<String>): GameState {
        val winPatterns = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6)
        )

        for (pattern in winPatterns) {
            val a = board[pattern[0]]
            val b = board[pattern[1]]
            val c = board[pattern[2]]
            if (a.isNotEmpty() && a == b && b == c) {
                return if (a == "X") GameState.X_WON else GameState.O_WON
            }
        }

        if (board.all { it.isNotEmpty() }) {
            return GameState.DRAW
        }

        return GameState.PLAYING
    }
}
