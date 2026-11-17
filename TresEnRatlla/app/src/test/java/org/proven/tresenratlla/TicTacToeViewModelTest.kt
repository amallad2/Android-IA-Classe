package org.proven.tresenratlla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.proven.tresenratlla.model.Player

class TicTacToeViewModelTest {

    private lateinit var viewModel: TicTacToeViewModel

    @Before
    fun setUp() {
        viewModel = TicTacToeViewModel()
    }

    @Test
    fun `initial state is empty board with Player X active`() {
        val state = viewModel.uiState.value

        assertEquals(List(9) { null }, state.board)
        assertEquals(Player.X, state.currentPlayer)
        assertNull(state.winner)
        assertFalse(state.isDraw)
        assertEquals(0, state.scoreX)
        assertEquals(0, state.scoreO)
    }

    @Test
    fun `clicking empty cell updates board and switches player`() {
        viewModel.onCellClicked(0)

        val state = viewModel.uiState.value
        assertEquals(Player.X, state.board[0])
        assertEquals(Player.O, state.currentPlayer)
    }

    @Test
    fun `clicking occupied cell does nothing`() {
        viewModel.onCellClicked(0) // X
        viewModel.onCellClicked(0) // Attempt O on same cell

        val state = viewModel.uiState.value
        assertEquals(Player.X, state.board[0])
        assertEquals(Player.O, state.currentPlayer)
    }

    @Test
    fun `winning horizontal row sets winner and increments score`() {
        // X X X on row 0
        // X: 0, O: 3, X: 1, O: 4, X: 2
        viewModel.onCellClicked(0) // X
        viewModel.onCellClicked(3) // O
        viewModel.onCellClicked(1) // X
        viewModel.onCellClicked(4) // O
        viewModel.onCellClicked(2) // X wins!

        val state = viewModel.uiState.value
        assertEquals(Player.X, state.winner)
        assertEquals(listOf(0, 1, 2), state.winningLine)
        assertEquals(1, state.scoreX)
        assertEquals(0, state.scoreO)
        assertTrue(state.isGameOver)
    }

    @Test
    fun `draw game sets isDraw flag`() {
        // X O X
        // X O O
        // O X X
        val moves = listOf(0, 1, 2, 4, 3, 5, 7, 6, 8)
        // 0:X, 1:O, 2:X, 4:O, 3:X, 5:O, 7:X, 6:O, 8:X
        moves.forEach { viewModel.onCellClicked(it) }

        val state = viewModel.uiState.value
        assertTrue(state.isDraw)
        assertNull(state.winner)
        assertTrue(state.isGameOver)
    }

    @Test
    fun `onResetGame clears board but keeps score`() {
        // X wins
        viewModel.onCellClicked(0) // X
        viewModel.onCellClicked(3) // O
        viewModel.onCellClicked(1) // X
        viewModel.onCellClicked(4) // O
        viewModel.onCellClicked(2) // X wins!

        viewModel.onResetGame()

        val state = viewModel.uiState.value
        assertEquals(List(9) { null }, state.board)
        assertNull(state.winner)
        assertEquals(Player.X, state.currentPlayer)
        assertEquals(1, state.scoreX) // Score retained!
    }

    @Test
    fun `onResetScores resets score and board`() {
        viewModel.onCellClicked(0) // X
        viewModel.onCellClicked(3) // O
        viewModel.onCellClicked(1) // X
        viewModel.onCellClicked(4) // O
        viewModel.onCellClicked(2) // X wins!

        viewModel.onResetScores()

        val state = viewModel.uiState.value
        assertEquals(0, state.scoreX)
        assertEquals(0, state.scoreO)
        assertEquals(List(9) { null }, state.board)
    }
}
