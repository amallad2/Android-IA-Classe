package com.example.tictactoeollama.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class OllamaRepository {

    suspend fun getOllamaMove(
        baseUrl: String,
        model: String,
        board: List<String>,
        availableMoves: List<Int>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (availableMoves.isEmpty()) {
                return@withContext Result.failure(Exception("No available moves"))
            }

            val urlString = "$baseUrl/api/generate"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            // Calculate strategic hints for Ollama
            val winMoveForO = findWinMove(board, "O", availableMoves)
            val blockMoveForX = findWinMove(board, "X", availableMoves)

            val boardStr = buildString {
                append("Board layout (0-8):\n")
                append(" 0: ${board[0].ifEmpty { "." }} | 1: ${board[1].ifEmpty { "." }} | 2: ${board[2].ifEmpty { "." }}\n")
                append(" 3: ${board[3].ifEmpty { "." }} | 4: ${board[4].ifEmpty { "." }} | 5: ${board[5].ifEmpty { "." }}\n")
                append(" 6: ${board[6].ifEmpty { "." }} | 7: ${board[7].ifEmpty { "." }} | 8: ${board[8].ifEmpty { "." }}\n")
            }

            val hintStr = when {
                winMoveForO != null -> "CRITICAL HINT: You can WIN right now by choosing position $winMoveForO."
                blockMoveForX != null -> "CRITICAL HINT: Player 'X' will win on the next turn unless you BLOCK at position $blockMoveForX."
                4 in availableMoves -> "HINT: Position 4 (Center) is available and is the best strategic position."
                else -> "HINT: Prefer corner positions (0, 2, 6, 8) if available."
            }

            val prompt = """
                You are playing Tic-Tac-Toe as 'O'. The human is 'X'.
                Your objective is to play the optimal move to win or draw.

                $boardStr

                Available empty positions: ${availableMoves.joinToString(", ")}.

                $hintStr

                Rules:
                - Select the BEST position from the available empty positions: ${availableMoves.joinToString(", ")}.
                - Rule 1: If 'O' has two in a row, play the third to WIN.
                - Rule 2: If 'X' has two in a row, play the third to BLOCK 'X'.
                - Rule 3: Take center position 4 or a corner (0, 2, 6, 8).

                You must reply ONLY with valid JSON containing the chosen position:
                {"move": <number>}
            """.trimIndent()
            Log.d("TEST",prompt)

            val options = JSONObject().apply {
                put("temperature", 0.0)
            }

            val jsonInput = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
                put("stream", false)
                put("format", "json")
                put("options", options)
            }

            connection.outputStream.use { os ->
                val inputBytes = jsonInput.toString().toByteArray(Charsets.UTF_8)
                os.write(inputBytes, 0, inputBytes.size)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    StringBuilder().apply {
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            append(line)
                        }
                    }.toString()
                }

                val jsonResponse = JSONObject(responseString)
                val rawText = jsonResponse.optString("response", "").trim()

                val extractedMove = extractMoveFromResponse(rawText, availableMoves)
                if (extractedMove != null) {
                    Result.success(extractedMove)
                } else {
                    val bestFallback = getBestMove(board, availableMoves)
                    Result.success(bestFallback)
                }
            } else {
                val bestFallback = getBestMove(board, availableMoves)
                Result.success(bestFallback)
            }
        } catch (e: Exception) {
            val bestFallback = getBestMove(board, availableMoves)
            if (bestFallback in availableMoves) {
                Result.success(bestFallback)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun extractMoveFromResponse(rawText: String, availableMoves: List<Int>): Int? {
        // 1. Direct JSON parsing
        try {
            val jsonObj = JSONObject(rawText)
            if (jsonObj.has("move")) {
                val move = jsonObj.getInt("move")
                if (move in availableMoves) return move
            }
            if (jsonObj.has("position")) {
                val move = jsonObj.getInt("position")
                if (move in availableMoves) return move
            }
        } catch (_: Exception) { }

        // 2. Regex for JSON pattern `"move": 4`
        val jsonKeyRegex = Regex("\"(?:move|position|index)\"\\s*:\\s*(\\d+)")
        val match = jsonKeyRegex.find(rawText)
        if (match != null) {
            val move = match.groupValues[1].toIntOrNull()
            if (move != null && move in availableMoves) return move
        }

        // 3. Search digits from the end of the text
        val digitsRegex = Regex("\\d+")
        val digitMatches = digitsRegex.findAll(rawText).mapNotNull { it.value.toIntOrNull() }.toList()
        for (candidate in digitMatches.reversed()) {
            if (candidate in availableMoves) {
                return candidate
            }
        }

        return null
    }

    companion object {
        private val WIN_PATTERNS = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )

        fun findWinMove(board: List<String>, player: String, availableMoves: List<Int>): Int? {
            for (pattern in WIN_PATTERNS) {
                val playerMoves = pattern.count { board[it] == player }
                val emptyMoves = pattern.count { board[it].isEmpty() }
                if (playerMoves == 2 && emptyMoves == 1) {
                    val pos = pattern.first { board[it].isEmpty() }
                    if (pos in availableMoves) return pos
                }
            }
            return null
        }

        fun getBestMove(board: List<String>, availableMoves: List<Int>): Int {
            if (availableMoves.isEmpty()) return -1

            // 1. Win if possible
            val winMove = findWinMove(board, "O", availableMoves)
            if (winMove != null) return winMove

            // 2. Block X if needed
            val blockMove = findWinMove(board, "X", availableMoves)
            if (blockMove != null) return blockMove

            // 3. Take center (4)
            if (4 in availableMoves) return 4

            // 4. Take available corner
            val corners = listOf(0, 2, 6, 8).filter { it in availableMoves }
            if (corners.isNotEmpty()) return corners.first()

            // 5. Take available side
            val sides = listOf(1, 3, 5, 7).filter { it in availableMoves }
            if (sides.isNotEmpty()) return sides.first()

            return availableMoves.first()
        }
    }
}

