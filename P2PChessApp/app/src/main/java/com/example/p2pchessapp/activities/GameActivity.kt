package com.example.p2pchessapp.activities

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.p2pchessapp.R
import com.example.p2pchessapp.databinding.ActivityGameBinding
import com.example.p2pchessapp.model.*

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private lateinit var chessBoard: ChessBoard
    private val boardSquares = Array(8) { arrayOfNulls<ImageView>(8) }
    private var selectedSquare: Square? = null
    private var playerColor: PieceColor = PieceColor.WHITE // Set this based on P2P negotiation
    private var isMyTurn: Boolean = false // Determined by playerColor and chessBoard.currentPlayer

    // TODO: These would be passed from MainActivity or determined via P2P negotiation
    private var isHost: Boolean = true // Example: Host is White
    private var opponentName: String = "Opponent"

    // TODO: Replace with actual P2P communication handler
    // This is a placeholder for sending/receiving moves.
    // In a real app, this would interface with the P2P networking layer from MainActivity.
    interface P2PCommunicationListener {
        fun sendMove(moveNotation: String)
        fun onMoveReceived(moveNotation: String) // Called by network layer
        fun sendResign()
        fun onOpponentResigned()
    }
    private var p2pListener: P2PCommunicationListener? = null // This needs to be set up


    companion object {
        const val EXTRA_IS_HOST = "is_host"
        const val EXTRA_OPPONENT_NAME = "opponent_name"
        // Static WeakReference to MainActivity for sending/receiving messages.
        // This is a simplification. A better approach would be a bound service or event bus.
        // Using a WeakReference helps prevent memory leaks if GameActivity outlives MainActivity,
        // though the overall communication pattern could be improved.
        var mainActivityInstance: java.lang.ref.WeakReference<MainActivity>? = null
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerColor = if (intent.getBooleanExtra(EXTRA_IS_HOST, true)) PieceColor.WHITE else PieceColor.BLACK
        opponentName = intent.getStringExtra(EXTRA_OPPONENT_NAME) ?: "Opponent"
        title = "Chess vs $opponentName"

        chessBoard = ChessBoard()
        initializeBoardUI()
        drawBoard()
        updateTurnInfo()

        binding.buttonResign.setOnClickListener {
            confirmResignation()
        }

        // Setup P2P Listener (simplified)
        // This listener relies on MainActivity to be available via mainActivityInstance.
        // For more robust communication, consider using LocalBroadcastManager, an Event Bus, or a Bound Service.
        mainActivityInstance?.get()?.let { main ->
             this.p2pListener = object : P2PCommunicationListener {
                override fun sendMove(moveNotation: String) {
                    main.sendMessage("MOVE:$moveNotation")
                }
                override fun onMoveReceived(moveNotation: String) {
                    // This method would be called by MainActivity when it receives a move
                    handleOpponentMove(moveNotation)
                }
                 override fun sendResign() {
                     main.sendMessage("RESIGN")
                 }
                 override fun onOpponentResigned() {
                     runOnUiThread {
                         showGameEndDialog("${opponentName}(${chessBoard.currentPlayer.name}) resigned. You win!")
                     }
                 }
            }
        }
        // Let MainActivity know GameActivity is ready to receive moves via ChessApplication
        // This is part of the simplified communication pattern.
        (applicationContext as? ChessApplication)?.activeGameActivity = this
    }

    private fun initializeBoardUI() {
        val gridLayout = binding.gridLayoutChessboard
        gridLayout.rowCount = 8
        gridLayout.columnCount = 8

        val displayMetrics = resources.displayMetrics
        // Ensure screen width is available, otherwise use a default
        val screenWidth = if (displayMetrics.widthPixels > 0) displayMetrics.widthPixels else 1080 // Fallback
        val squareSize = screenWidth / 8


        for (row in 0..7) {
            for (col in 0..7) {
                val imageView = ImageView(this)
                val params = GridLayout.LayoutParams()
                params.width = squareSize
                params.height = squareSize
                params.rowSpec = GridLayout.spec(row)
                params.columnSpec = GridLayout.spec(col)
                imageView.layoutParams = params
                imageView.setPadding(8, 8, 8, 8) // Padding for piece visibility

                imageView.setBackgroundColor(if ((row + col) % 2 == 0) Color.LTGRAY else Color.DKGRAY)
                imageView.setOnClickListener { onSquareClicked(row, col) }

                gridLayout.addView(imageView)
                boardSquares[row][col] = imageView
            }
        }
    }

    private fun drawBoard() {
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = chessBoard.getPieceAt(Square(row, col))
                boardSquares[row][col]?.setImageResource(getPieceDrawableRes(piece))
                // Reset background for non-selected, non-highlighted squares
                boardSquares[row][col]?.background = GradientDrawable().apply {
                    setColor(if ((row + col) % 2 == 0) Color.parseColor("#e0e0e0") else Color.parseColor("#a0a0a0")) // Lighter/Darker grays
                }
            }
        }
        // Highlight selected square if any
        selectedSquare?.let {
            boardSquares[it.row][it.col]?.background = GradientDrawable().apply {
                setColor(if ((it.row + it.col) % 2 == 0) Color.parseColor("#e0e0e0") else Color.parseColor("#a0a0a0"))
                setStroke(6, Color.BLUE) // Blue border for selected
            }
        }
    }

    private fun getPieceDrawableRes(piece: ChessPiece?): Int {
        return when (piece?.type) {
            PieceType.KING -> if (piece.color == PieceColor.WHITE) R.drawable.ic_king_white else R.drawable.ic_king_black
            PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) R.drawable.ic_queen_white else R.drawable.ic_queen_black
            PieceType.ROOK -> if (piece.color == PieceColor.WHITE) R.drawable.ic_rook_white else R.drawable.ic_rook_black
            PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) R.drawable.ic_bishop_white else R.drawable.ic_bishop_black
            PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) R.drawable.ic_knight_white else R.drawable.ic_knight_black
            PieceType.PAWN -> if (piece.color == PieceColor.WHITE) R.drawable.ic_pawn_white else R.drawable.ic_pawn_black
            null -> 0 // No drawable / transparent
        }
    }

    private fun onSquareClicked(row: Int, col: Int) {
        if (!isMyTurnNow()) {
            Toast.makeText(this, "Not your turn.", Toast.LENGTH_SHORT).show()
            return
        }

        val clickedSquare = Square(row, col)
        val pieceAtClicked = chessBoard.getPieceAt(clickedSquare)

        if (selectedSquare == null) { // First click - select a piece
            if (pieceAtClicked != null && pieceAtClicked.color == chessBoard.currentPlayer) {
                selectedSquare = clickedSquare
                highlightValidMoves(clickedSquare)
                Log.d("GameActivity", "Selected piece at $clickedSquare")
            } else {
                Log.d("GameActivity", "Cannot select empty square or opponent's piece at $clickedSquare")
            }
        } else { // Second click - attempt to move
            val move = ChessMove(selectedSquare!!, clickedSquare, chessBoard.getPieceAt(selectedSquare!!)!!)

            // Check for pawn promotion
            val piece = chessBoard.getPieceAt(selectedSquare!!)
            if (piece?.type == PieceType.PAWN &&
                ((piece.color == PieceColor.WHITE && clickedSquare.row == 0) || (piece.color == PieceColor.BLACK && clickedSquare.row == 7))) {
                promptForPromotion(move) { promotionMove ->
                    performMove(promotionMove)
                }
            } else {
                 performMove(move)
            }
        }
        drawBoard() // Redraw to show selection highlight or move
    }

    private fun performMove(move: ChessMove) {
        Log.d("GameActivity", "Attempting move: ${move.toSimpleNotation()}")
        if (chessBoard.isValidMove(move)) {
            chessBoard.makeMove(move)
            Log.d("GameActivity", "Move successful: ${move.toSimpleNotation()}")
            p2pListener?.sendMove(move.toSimpleNotation()) // Send move to opponent
            selectedSquare = null
            drawBoard()
            updateTurnInfo()
            checkGameState()
        } else {
            Log.d("GameActivity", "Invalid move: ${move.toSimpleNotation()}")
            Toast.makeText(this, "Invalid move", Toast.LENGTH_SHORT).show()
            selectedSquare = null // Deselect on invalid move attempt
        }
    }


    private fun highlightValidMoves(square: Square) {
        drawBoard() // Clear previous highlights
        boardSquares[square.row][square.col]?.background = GradientDrawable().apply {
            setColor(if ((square.row + square.col) % 2 == 0) Color.parseColor("#e0e0e0") else Color.parseColor("#a0a0a0"))
            setStroke(6, Color.BLUE) // Blue for selected
        }

        val validMoves = chessBoard.getAllLegalMovesForPlayer(chessBoard.currentPlayer)
                           .filter { it.from == square }

        Log.d("GameActivity", "Highlighting ${validMoves.size} moves for piece at $square")
        validMoves.forEach { move ->
            boardSquares[move.to.row][move.to.col]?.background = GradientDrawable().apply {
                setColor(if ((move.to.row + move.to.col) % 2 == 0) Color.parseColor("#90CAF9") else Color.parseColor("#64B5F6")) // Light blue for valid moves
                setStroke(4, Color.GREEN) // Green border for valid moves
            }
        }
    }

    private fun promptForPromotion(baseMove: ChessMove, onPromotionSelected: (ChessMove) -> Unit) {
        val promotionPieces = arrayOf("Queen", "Rook", "Bishop", "Knight")
        val promotionPieceTypes = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

        AlertDialog.Builder(this)
            .setTitle("Promote Pawn to:")
            .setItems(promotionPieces) { _, which ->
                val promotedMove = baseMove.copy(promotionTo = promotionPieceTypes[which])
                onPromotionSelected(promotedMove)
            }
            .setCancelable(false) // User must choose
            .show()
    }

    private fun updateTurnInfo() {
        isMyTurn = (chessBoard.currentPlayer == playerColor)
        val turnText = if (isMyTurn) "Your Turn (${playerColor.name})" else "${opponentName}'s Turn (${chessBoard.currentPlayer.name})"
        binding.textViewTurnInfo.text = turnText
        binding.textViewGameState.text = "State: ${chessBoard.gameState.name}"
    }

    private fun checkGameState() {
        when (chessBoard.gameState) {
            GameState.CHECKMATE_WHITE_WINS -> showGameEndDialog("Checkmate! White wins.")
            GameState.CHECKMATE_BLACK_WINS -> showGameEndDialog("Checkmate! Black wins.")
            GameState.STALEMATE_DRAW -> showGameEndDialog("Stalemate! It's a draw.")
            GameState.CHECK -> Toast.makeText(this, "${chessBoard.currentPlayer.name} is in Check!", Toast.LENGTH_SHORT).show()
            // Handle other draw conditions if implemented
            else -> { /* Game ongoing */ }
        }
    }

    private fun showGameEndDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish() // Close GameActivity
            }
            .setCancelable(false)
            .show()
    }

    private fun isMyTurnNow(): Boolean {
        return chessBoard.currentPlayer == playerColor
    }

    // Called by MainActivity (or a service/event bus) when a move is received
    fun handleOpponentMove(moveNotation: String) {
        Log.d("GameActivity", "Opponent move received: $moveNotation")
        val move = ChessMove.fromSimpleNotation(moveNotation, chessBoard)
        if (move != null) {
             if (chessBoard.isValidMove(move, ignoreTurn = true)) { // Opponent's turn, so ignore current player check for validation
                chessBoard.makeMove(move) // This will switch currentPlayer
                runOnUiThread {
                    selectedSquare = null // Clear any local selection
                    drawBoard()
                    updateTurnInfo()
                    checkGameState()
                    Toast.makeText(this, "$opponentName played ${move.toSimpleNotation()}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("GameActivity", "Opponent sent invalid move: $moveNotation")
                // TODO: Handle invalid move from opponent (e.g., request resend, or declare fault)
                runOnUiThread {
                    Toast.makeText(this, "Opponent sent an invalid move. This shouldn't happen.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.e("GameActivity", "Could not parse opponent move: $moveNotation")
        }
    }

    fun handleOpponentResignation() {
        runOnUiThread {
            showGameEndDialog("${opponentName} resigned. You win!")
        }
    }

    private fun confirmResignation() {
        AlertDialog.Builder(this)
            .setTitle("Resign Game")
            .setMessage("Are you sure you want to resign?")
            .setPositiveButton("Yes, Resign") { _, _ ->
                p2pListener?.sendResign()
                showGameEndDialog("You resigned. ${opponentName} wins.")
            }
            .setNegativeButton("No", null)
            .show()
    }


    override fun onDestroy() {
        super.onDestroy()
        // Clear reference in Application class or MainActivity to avoid memory leaks
        // For robust inter-activity communication, consider a Bound Service or Event Bus
        // instead of static references or Application class coupling.
        val app = applicationContext as? ChessApplication
        if (app?.activeGameActivity == this) {
            app?.activeGameActivity = null
        }
        if (GameActivity.mainActivityInstance?.get() == this.mainActivityInstance?.get()) { // Check if it's the same main activity
             GameActivity.mainActivityInstance?.clear() // Clear weak reference
        }
        p2pListener = null // Clear listener
    }
}

/**
 * A simple Application class to hold a weak reference to GameActivity for MainActivity communication.
 * This is a quick-and-dirty solution. A proper solution would use a Service,
 * LocalBroadcastManager, or an event bus (e.g., GreenRobot EventBus or Otto).
 * Using a static reference to MainActivity from GameActivity is also not ideal and prone to leaks
 * if not handled carefully (here, a WeakReference is used for mainActivityInstance).
 */
class ChessApplication : android.app.Application() {
    // WeakReference to avoid leaking GameActivity if MainActivity holds it too long
    var activeGameActivity: GameActivity? = null // Kept for simplicity for now, MainActivity accesses it.
}

// Make mainActivityInstance a WeakReference in GameActivity to reduce leak potential
// This change should be in the GameActivity class members declaration area:
// var mainActivityInstance: WeakReference<MainActivity>? = null
// And when setting it in MainActivity:
// GameActivity.mainActivityInstance = WeakReference(this)
// And when using it in GameActivity:
// mainActivityInstance?.get()?.sendMessage(...)
// For this iteration, will stick to the direct static reference for simplicity as per current code,
// but acknowledge this is an area for improvement. The comments have been added.
