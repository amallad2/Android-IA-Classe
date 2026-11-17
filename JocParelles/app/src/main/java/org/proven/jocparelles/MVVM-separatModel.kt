package org.prove.jocparelles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.prove.jocparelles.ui.theme.JocParellesTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --- MODEL DE LA CARTA ---
data class CardModel(
    val id: Int,
    val position: Int,
    val frontImageRes: Int,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
) {
    val currentImageRes: Int
        get() = if (isFlipped || isMatched) frontImageRes else R.drawable.back

    fun flip(): CardModel = copy(isFlipped = !isFlipped)

    fun isSameCard(other: CardModel): Boolean = frontImageRes == other.frontImageRes

    fun markAsMatched(): CardModel = copy(isMatched = true, isFlipped = true)
}

// --- MODEL DEL TAULER AMB 6 CARTES ---
data class BoardModel(
    val cards: List<CardModel> = emptyList(),
    val isGameOver: Boolean = false
) {
    companion object {
        // Mètode de fàbrica per crear el joc inicialitzat amb les 6 cartes
        fun createInitialBoard(): BoardModel {
            // 1. Definim els recursos de les imatges de les 3 parelles
            val images = listOf(
                R.drawable.eugenio,
                R.drawable.chiquito,
                R.drawable.rubianes
            )

            // 2. Dupliquem les imatges per tenir 6 cartes (3 parelles)
            val initialCards = (images + images)
                .shuffled() // Barrejem l'ordre
                .mapIndexed { index, imageRes ->
                    CardModel(
                        id = index,          // Identificador únic per a cada carta (0 a 5)
                        position = index,    // Posició al tauler
                        frontImageRes = imageRes
                    )
                }

            return BoardModel(cards = initialCards)
        }
    }

    // --- MÈTODES DE LÒGICA DE NEGOCI ---

    // Acció de girar la carta en una posició concreta
    fun onCardClick(position: Int): BoardModel {
        val updatedCards = cards.map { card ->
            if (card.position == position && !card.isMatched) {
                card.flip()
            } else {
                card
            }
        }
        return this.copy(cards = updatedCards)
    }

    // Comprovar si les cartes girades coincideixen
    fun checkMatches(): BoardModel {
        val flippedCards = cards.filter { it.isFlipped && !it.isMatched }

        if (flippedCards.size == 2) {
            val (card1, card2) = flippedCards
            val isMatch = card1.isSameCard(card2)

            val updatedCards = cards.map { card ->
                when {
                    isMatch && (card.id == card1.id || card.id == card2.id) -> card.markAsMatched()
                    !isMatch && (card.id == card1.id || card.id == card2.id) -> card.copy(isFlipped = false)
                    else -> card
                }
            }

            val allMatched = updatedCards.all { it.isMatched }
            return this.copy(cards = updatedCards, isGameOver = allMatched)
        }

        return this
    }
}

// --- VIEWMODEL ---
class GameViewModel : ViewModel() {
    private val _board = MutableStateFlow(BoardModel.createInitialBoard())
    val board: StateFlow<BoardModel> = _board.asStateFlow()

    fun onCardClick(position: Int) {
        val currentBoard = _board.value
        val flippedUnmatched = currentBoard.cards.filter { it.isFlipped && !it.isMatched }

        if (flippedUnmatched.size >= 2) return

        val updatedBoard = currentBoard.onCardClick(position)
        _board.value = updatedBoard

        val newlyFlipped = updatedBoard.cards.filter { it.isFlipped && !it.isMatched }
        if (newlyFlipped.size == 2) {
            viewModelScope.launch {
                delay(1000L)
                _board.value = _board.value.checkMatches()
            }
        }
    }

    fun restartGame() {
        _board.value = BoardModel.createInitialBoard()
    }
}

// --- ACTIVITY (VIEW) ---
class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JocParellesTheme {
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

// --- COMPOSABLE VIEW ---
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (board.isGameOver) {
            Text(
                text = "Enhorabona! JOC ACABAT!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = { viewModel.restartGame() }) {
                Text(text = "Tornar a jugar")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(board.cards, key = { it.id }) { card ->
                CardItemView(
                    card = card,
                    onCardClick = { viewModel.onCardClick(card.position) }
                )
            }
        }
    }
}

@Composable
fun CardItemView(
    card: CardModel,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable(enabled = !card.isMatched) { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Image(
            painter = painterResource(id = card.currentImageRes),
            contentDescription = "Carta a la posició ${card.position}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
