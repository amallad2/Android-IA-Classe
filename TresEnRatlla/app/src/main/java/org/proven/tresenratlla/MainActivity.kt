package org.proven.tresenratlla

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.proven.tresenratlla.ui.theme.TresEnRatllaTheme


// ==========================================
// 1. MODEL (LÒGICA DE NEGOCI I DADES)
// ==========================================

/**
 * Representa una casella individual del tauler de 3x3.
 * @param id Identificador únic de la casella (0 a 8).
 * @param position Posició dins del tauler (0 a 8).
 * @param symbol Símbol ocupant: "X", "O" o buit ("").
 */
data class CellModel(
    val id: Int,
    val position: Int,
    val symbol: String = ""
) {
    val isEmpty: Boolean
        get() = symbol.isEmpty()
}

/**
 * Representa l'estat global del tauler de joc i encapsula la lògica pura.
 */
data class BoardModel(
    val cells: List<CellModel> = emptyList(),
    val currentTurn: String = "X",
    val winner: String? = null,
    val isDraw: Boolean = false,
    val isGameOver: Boolean = false
) {
    companion object {
        /**
         * Mètode de fàbrica per crear el tauler inicial net amb 9 caselles (3x3).
         */
        fun createInitialBoard(): BoardModel {
            val initialCells = (0..8).map { index ->
                CellModel(id = index, position = index, symbol = "")
            }
            return BoardModel(cells = initialCells, currentTurn = "X")
        }
    }

    /**
     * Executa el moviment a la posició indicada i retorna un nou BoardModel immutablement[cite: 1, 2].
     */
    fun makeMove(position: Int): BoardModel {
        // Si la casella no està buida o el joc ha acabat, ignorem el moviment
        if (isGameOver || !cells[position].isEmpty) {
            return this
        }

        // Col·loquem el símbol del jugador actual a la casella indicada
        val updatedCells = cells.map { cell ->
            if (cell.position == position) {
                cell.copy(symbol = currentTurn)
            } else {
                cell
            }
        }

        // Creem la còpia temporal amb les caselles actualitzades
        val boardWithMove = this.copy(cells = updatedCells)

        // Comprovem l'estat de victòria o empat
        return boardWithMove.checkWinner()
    }

    /**
     * Comprova si hi ha un guanyador o si hi ha un empat al tauler.
     */
    fun checkWinner(): BoardModel {
        // Combinacions guanyadores (3 horitzontals, 3 verticals, 2 diagonals)
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Files
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columnes
            listOf(0, 4, 8), listOf(2, 4, 6)                  // Diagonals
        )

        for (pattern in winPatterns) {
            val (a, b, c) = pattern
            val symbolA = cells[a].symbol
            val symbolB = cells[b].symbol
            val symbolC = cells[c].symbol

            if (symbolA.isNotEmpty() && symbolA == symbolB && symbolB == symbolC) {
                return this.copy(
                    winner = symbolA,
                    isGameOver = true
                )
            }
        }

        // Comprovació d'empat (totes les caselles plenes sense guanyador)
        val allFull = cells.all { !it.isEmpty }
        if (allFull) {
            return this.copy(
                isDraw = true,
                isGameOver = true
            )
        }

        // Si la partida continua, canviem el torn al següent jugador
        val nextTurn = if (currentTurn == "X") "O" else "X"
        return this.copy(currentTurn = nextTurn)
    }
}

// ==========================================
// 2. VIEWMODEL (GESTIÓ DE L'ESTAT DE LA UI)
// ==========================================

class GameViewModel : ViewModel() {
    // Estat privat MutableStateFlow[cite: 1, 2, 3]
    private val _board = MutableStateFlow(BoardModel.createInitialBoard())
    // Estat públic de només lectura exposed a la Vista com a StateFlow[cite: 1, 2, 3]
    val board: StateFlow<BoardModel> = _board.asStateFlow()

    /**
     * Acció llançada des de la vista en pulsar una casella del tauler.
     */
    fun onCellClick(position: Int) {
        _board.value = _board.value.makeMove(position)
    }

    /**
     * Reinicia el tauler a l'estat inicial.
     */
    fun restartGame() {
        _board.value = BoardModel.createInitialBoard()
    }
}

// ==========================================
// 3. VIEW (ACTIVITY I COMPOSABLES)
// ==========================================

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TresEnRatllaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // Observació de l'estat reactiu amb collectAsState()[cite: 1, 2, 3]
    val board by viewModel.board.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tres en Ratlla (MVVM)",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Indicadors visuals de torn, victòria o empat
        StatusBanner(board = board)

        Spacer(modifier = Modifier.height(24.dp))

        // Tauler de joc de 3x3
        BoardGrid(
            board = board,
            onCellClick = { position -> viewModel.onCellClick(position) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.restartGame() },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text(
                text = if (board.isGameOver) "Tornar a jugar" else "Reiniciar partida",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun StatusBanner(board: BoardModel) {
    val statusText = when {
        board.winner != null -> "🎉 Guanyador: Jugador ${board.winner}!"
        board.isDraw -> "🤝 Empat! Cap jugador guanya."
        else -> "Torn del jugador: ${board.currentTurn}"
    }

    val backgroundColor = when {
        board.winner != null -> MaterialTheme.colorScheme.primaryContainer
        board.isDraw -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BoardGrid(
    board: BoardModel,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Manté el tauler com un quadrat perfecte
    ) {
        items(board.cells, key = { it.id }) { cell ->
            CellItemView(
                cell = cell,
                enabled = !board.isGameOver && cell.isEmpty,
                onCellClick = { onCellClick(cell.position) }
            )
        }
    }
}

@Composable
fun CellItemView(
    cell: CellModel,
    enabled: Boolean,
    onCellClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val symbolColor = when (cell.symbol) {
        "X" -> MaterialTheme.colorScheme.primary
        "O" -> MaterialTheme.colorScheme.error
        else -> Color.Unspecified
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clickable(enabled = enabled) { onCellClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cell.symbol,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = symbolColor
            )
        }
    }
}