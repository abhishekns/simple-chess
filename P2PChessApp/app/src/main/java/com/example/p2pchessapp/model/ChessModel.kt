package com.example.p2pchessapp.model

// Represents the type of chess piece
enum class PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

// Represents the color of the piece
enum class PieceColor { WHITE, BLACK }

// Represents a single chess piece
data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    var hasMoved: Boolean = false // Important for castling and pawn's first move
)

// Represents a square on the chessboard
// Row and col are 0-indexed (0-7)
data class Square(val row: Int, val col: Int) {
    override fun toString(): String {
        return "${'a' + col}${8 - row}" // e.g., a8, h1
    }
}

// ----------------------
// Helper extensions / utils
// ----------------------

/** Returns a new square offset from the current one (row/col). */
operator fun Square.plus(offset: Pair<Int, Int>): Square =
    Square(row + offset.first, col + offset.second)

/** Board‑bounds check (0‒7). */
private fun isInBounds(row: Int, col: Int): Boolean = row in 0..7 && col in 0..7

// Represents a chess move
data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: ChessPiece, // The piece that moved
    val capturedPiece: ChessPiece? = null,
    val promotionTo: PieceType? = null, // For pawn promotion
    val isCastlingMove: Boolean = false,
    val isEnPassant: Boolean = false
) {
    override fun toString(): String {
        return "${piece.type} from $from to $to" +
               (if (capturedPiece != null) " captures ${capturedPiece.type}" else "") +
               (if (promotionTo != null) " promotes to $promotionTo" else "") +
               (if (isCastlingMove) " (castling)" else "") +
               (if (isEnPassant) " (en passant)" else "")
    }

    // Simple algebraic notation (e.g., "e2e4", "e7e8q" for promotion)
    fun toSimpleNotation(): String {
        return "${from.toString()}${to.toString()}${promotionTo?.name?.first()?.lowercaseChar() ?: ""}"
    }

    companion object {
        // Create a ChessMove from simple algebraic notation
        fun fromSimpleNotation(notation: String, board: ChessBoard): ChessMove? {
            if (notation.length < 4 || notation.length > 5) return null

            val fromColChar = notation[0]
            val fromRowChar = notation[1]
            val toColChar = notation[2]
            val toRowChar = notation[3]

            val fromCol = fromColChar - 'a'
            val fromRow = 8 - (fromRowChar - '0')
            val toCol = toColChar - 'a'
            val toRow = 8 - (toRowChar - '0')

            if (fromCol !in 0..7 || fromRow !in 0..7 || toCol !in 0..7 || toRow !in 0..7) return null

            val fromSq = Square(fromRow, fromCol)
            val toSq = Square(toRow, toCol)
            val piece = board.getPieceAt(fromSq) ?: return null // Piece must exist at source

            var promotion: PieceType? = null
            if (notation.length == 5) {
                promotion = when (notation[4]) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> null
                }
            }
            // Captured piece, castling, en passant would need more context or board state analysis here
            // This basic version primarily handles coordinates and promotion type.
            return ChessMove(fromSq, toSq, piece, promotionTo = promotion)
        }
    }
}


// Represents the game state
enum class GameState {
    ONGOING,
    CHECK, // Current player is in check
    CHECKMATE_WHITE_WINS,
    CHECKMATE_BLACK_WINS,
    STALEMATE_DRAW,
    DRAW_AGREED,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_THREEFOLD_REPETITION // More complex to implement fully
}


public class ChessBoard {
    // Board: 8x8 grid, null if square is empty, ChessPiece if occupied
    val squares: Array<Array<ChessPiece?>> = Array(8) { Array(8) { null } }
    var currentPlayer: PieceColor = PieceColor.WHITE
    var gameState: GameState = GameState.ONGOING
    private var lastMove: ChessMove? = null // For en passant
    private val moveHistory: MutableList<ChessMove> = mutableListOf()

    // King positions, tracked for efficient check detection
    private var whiteKingPos: Square = Square(7, 4)
    private var blackKingPos: Square = Square(0, 4)

    private fun findKingPosition(color: PieceColor): Square {
        for (r in 0..7) for (c in 0..7) {
            val p = squares[r][c]
            if (p?.type == PieceType.KING && p.color == color) {
                return Square(r, c)
            }
        }
        return if (color == PieceColor.WHITE) whiteKingPos else blackKingPos
    }


    init {
        resetBoard()
    }

    /**
     * Clears the board of all pieces without placing a new starting setup.
     * Current player, game state and history are also reset.
     */
    fun clearBoard() {
        for (r in 0..7) for (c in 0..7) squares[r][c] = null
        currentPlayer = PieceColor.WHITE
        gameState = GameState.ONGOING
        lastMove = null
        moveHistory.clear()
        whiteKingPos = Square(7, 4)
        blackKingPos = Square(0, 4)
    }

    fun resetBoard() {
        for (r in 0..7) for (c in 0..7) squares[r][c] = null

        // Pawns
        for (c in 0..7) {
            squares[1][c] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
            squares[6][c] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        }

        // Rooks
        squares[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        squares[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        squares[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        squares[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)

        // Knights
        squares[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        squares[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        squares[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        squares[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)

        // Bishops
        squares[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        squares[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        squares[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        squares[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)

        // Queens
        squares[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
        squares[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)

        // Kings
        squares[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
        whiteKingPos = Square(7, 4)
        squares[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
        blackKingPos = Square(0, 4)

        currentPlayer = PieceColor.WHITE
        gameState = GameState.ONGOING
        lastMove = null
        moveHistory.clear()
    }

    fun getPieceAt(square: Square): ChessPiece? {
        if (!isValidSquare(square.row, square.col)) return null
        return squares[square.row][square.col]
    }

    private fun setPieceAt(square: Square, piece: ChessPiece?) {
        squares[square.row][square.col] = piece
        if (piece?.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) whiteKingPos = square
            else blackKingPos = square
        }
    }

    private fun isValidSquare(row: Int, col: Int): Boolean = row in 0..7 && col in 0..7

    fun makeMove(move: ChessMove): Boolean {
        val pieceToMove = getPieceAt(move.from)

        // Basic validation: piece exists, belongs to current player, valid destination
        if (pieceToMove == null || pieceToMove.color != currentPlayer) return false
        if (!isValidMove(move)) return false // This will also check for self-check

        // Perform the move
        val captured = getPieceAt(move.to) // Could be different from move.capturedPiece if not pre-validated
        setPieceAt(move.to, pieceToMove)
        setPieceAt(move.from, null)
        pieceToMove.hasMoved = true

        // Handle castling: move the rook
        if (move.isCastlingMove) {
            when (move.to.col) {
                6 -> { // Kingside
                    val rook = getPieceAt(Square(move.from.row, 7))
                    setPieceAt(Square(move.from.row, 5), rook)
                    setPieceAt(Square(move.from.row, 7), null)
                    rook?.hasMoved = true
                }
                2 -> { // Queenside
                    val rook = getPieceAt(Square(move.from.row, 0))
                    setPieceAt(Square(move.from.row, 3), rook)
                    setPieceAt(Square(move.from.row, 0), null)
                    rook?.hasMoved = true
                }
            }
        }

        // Handle en passant capture
        if (move.isEnPassant) {
            val capturedPawnRow = if (pieceToMove.color == PieceColor.WHITE) move.to.row + 1 else move.to.row - 1
            setPieceAt(Square(capturedPawnRow, move.to.col), null)
        }

        // Handle pawn promotion
        if (move.promotionTo != null && pieceToMove.type == PieceType.PAWN) {
            if ((pieceToMove.color == PieceColor.WHITE && move.to.row == 0) ||
                (pieceToMove.color == PieceColor.BLACK && move.to.row == 7)) {
                setPieceAt(move.to, ChessPiece(move.promotionTo, pieceToMove.color, true))
            }
        }

        // Update king position if king moved
        if (pieceToMove.type == PieceType.KING) {
            if (pieceToMove.color == PieceColor.WHITE) whiteKingPos = move.to
            else blackKingPos = move.to
        }

        // Switch player
        currentPlayer = if (currentPlayer == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        lastMove = move
        moveHistory.add(move)

        // Update game state (check, checkmate, stalemate)
        updateGameState()

        return true
    }

    fun isValidMove(move: ChessMove, ignoreTurn: Boolean = false, ignoreSelfCheck: Boolean = false): Boolean {
        val piece = getPieceAt(move.from) ?: return false
        if (!ignoreTurn && piece.color != currentPlayer) return false
        if (move.from == move.to) return false // Cannot move to the same square

        val targetPiece = getPieceAt(move.to)
        if (targetPiece != null && targetPiece.color == piece.color) return false // Cannot capture own piece

        // Validate piece-specific rules
        // Use full move generation here so normal non-capturing moves are allowed
        val validPieceMoves = getPossibleMovesForPiece(move.from) // Get all pseudo-legal moves
        if (!validPieceMoves.any { it.from == move.from && it.to == move.to }) { // Check if the proposed move is among them
             // Special check for promotion part of the move object, as getPossibleMovesForPiece might generate multiple promotions
            if (move.promotionTo != null && piece.type == PieceType.PAWN) {
                 if (!validPieceMoves.any { it.from == move.from && it.to == move.to && it.promotionTo == move.promotionTo }) return false
            } else if (move.isCastlingMove){
                 if (!validPieceMoves.any {it.from == move.from && it.to == move.to && it.isCastlingMove}) return false
            }
            else {
                return false
            }
        }


        // Check if the move puts the current player's king in check
        if (!ignoreSelfCheck) {
            // Simulate the move
            val originalPieceAtTo = getPieceAt(move.to)
            val originalPieceAtFrom = getPieceAt(move.from) // Should be 'piece'
            val originalKingPos = findKingPosition(piece.color)

            setPieceAt(move.to, piece)
            setPieceAt(move.from, null)
            if (piece.type == PieceType.KING) { // Update king position for the simulation
                if (piece.color == PieceColor.WHITE) whiteKingPos = move.to
                else blackKingPos = move.to
            }

            val kingInCheckAfterMove = isKingInCheck(piece.color)

            // Revert the simulated move
            setPieceAt(move.from, originalPieceAtFrom)
            setPieceAt(move.to, originalPieceAtTo)
            if (piece.type == PieceType.KING) { // Restore king position
                 if (piece.color == PieceColor.WHITE) whiteKingPos = originalKingPos
                 else blackKingPos = originalKingPos
            }

            if (kingInCheckAfterMove) return false
        }

        return true
    }


    public fun getPossibleMovesForPiece(square: Square, forAttackOnly: Boolean = false): List<ChessMove> {
        val piece = getPieceAt(square) ?: return emptyList()
        val moves = mutableListOf<ChessMove>()
        when (piece.type) {
            PieceType.PAWN -> moves.addAll(getPawnMoves(square, piece, forAttackOnly))
            PieceType.ROOK -> moves.addAll(getSlidingMoves(square, piece, listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)))
            PieceType.BISHOP -> moves.addAll(getSlidingMoves(square, piece, listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)))
            PieceType.QUEEN -> moves.addAll(getSlidingMoves(square, piece, listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0, 1 to 1, 1 to -1, -1 to 1, -1 to -1)))
            PieceType.KNIGHT -> moves.addAll(getKnightMoves(square, piece))
            PieceType.KING -> moves.addAll(getKingMoves(square, piece, forAttackOnly))
        }
        return moves
    }

    private fun doesMoveLeaveKingSafe(move: ChessMove, piece: ChessPiece): Boolean {
        // remember original pieces
        val captured = getPieceAt(move.to)
        val originalKingPos =
            if (piece.type == PieceType.KING) move.from else findKingPosition(piece.color)

        // play the move in-place
        setPieceAt(move.to, piece)
        setPieceAt(move.from, null)
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) whiteKingPos = move.to else blackKingPos = move.to
        }

        val stillSafe = !isKingInCheck(piece.color)

        // rollback
        setPieceAt(move.from, piece)
        setPieceAt(move.to, captured)
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) whiteKingPos = originalKingPos else blackKingPos = originalKingPos
        }
        return stillSafe
    }


    fun getAllLegalMovesForPlayer(pieceColor: PieceColor): List<ChessMove> {
        val allMoves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = getPieceAt(Square(r, c))
                if (piece != null && piece.color == pieceColor) {
                    val pseudoMoves = getPossibleMovesForPiece(Square(r, c))
                    for (m in pseudoMoves) {
                        if (isValidMove(m, ignoreTurn = true)) {
                            allMoves.add(m)
                        }
                    }
                }
            }
        }
        return allMoves
    }


    private fun getPawnMoves(square: Square, piece: ChessPiece, forAttackOnly: Boolean = false): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val direction = if (piece.color == PieceColor.WHITE) -1 else 1
        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1

        val front = square + Pair(direction, 0)
        if (!forAttackOnly && isInBounds(front.row, front.col) && getPieceAt(front) == null) {
            moves.add(ChessMove(square, front, piece))
            val doubleFront = square + Pair(2 * direction, 0)
            if (square.row == startRow && getPieceAt(doubleFront) == null) {
                moves.add(ChessMove(square, doubleFront, piece))
            }
        }

        val attackOffsets = listOf(Pair(direction, -1), Pair(direction, 1))
        for (offset in attackOffsets) {
            val target = square + offset
            if (isInBounds(target.row, target.col)) {
                val targetPiece = getPieceAt(target)
                if (targetPiece != null && targetPiece.color != piece.color) {
                    moves.add(ChessMove(square, target, piece, capturedPiece = targetPiece))
                } else if (!forAttackOnly && canEnPassant(square, target)) {
                    moves.add(ChessMove(square, target, piece, isEnPassant = true))
                }
            }
        }
        return moves
    }

    private fun isPromotionSquare(square: Square, color: PieceColor): Boolean {
        return (color == PieceColor.WHITE && square.row == 0) || (color == PieceColor.BLACK && square.row == 7)
    }

    private fun addPromotionMoves(moves: MutableList<ChessMove>, from: Square, to: Square, piece: ChessPiece, capturedPiece: ChessPiece? = null) {
        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { promoType ->
            moves.add(ChessMove(from, to, piece, capturedPiece, promotionTo = promoType))
        }
    }

    private fun canEnPassant(pawnSquare: Square, targetSquare: Square): Boolean {
        lastMove ?: return false
        val lastMovedPiece = getPieceAt(lastMove!!.to) ?: return false // Should be the piece that just moved

        // Check if the last move was a two-step pawn advance by the opponent
        if (lastMovedPiece.type == PieceType.PAWN &&
            lastMovedPiece.color != currentPlayer && // Opponent's pawn
            kotlin.math.abs(lastMove!!.from.row - lastMove!!.to.row) == 2 && // Two-step advance
            lastMove!!.to.col == targetSquare.col && // Same column as target capture square
            kotlin.math.abs(lastMove!!.to.row - pawnSquare.row) == 0) { // Pawn is adjacent horizontally to the moved pawn
            // Target square for en passant must be one step diagonally forward for the current pawn
            val expectedRow = pawnSquare.row + (if (currentPlayer == PieceColor.WHITE) -1 else 1)
            return targetSquare.row == expectedRow
        }
        return false
    }


    private fun getSlidingMoves(square: Square, piece: ChessPiece, directions: List<Pair<Int, Int>>): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for ((dr, dc) in directions) {
            for (i in 1..7) {
                val nextRow = square.row + dr * i
                val nextCol = square.col + dc * i
                if (!isValidSquare(nextRow, nextCol)) break

                val targetSquare = Square(nextRow, nextCol)
                val targetPiece = getPieceAt(targetSquare)

                if (targetPiece == null) {
                    moves.add(ChessMove(square, targetSquare, piece))
                } else {
                    if (targetPiece.color != piece.color) { // Capture
                        moves.add(ChessMove(square, targetSquare, piece, capturedPiece = targetPiece))
                    }
                    break // Blocked by a piece
                }
            }
        }
        return moves
    }

    private fun getKnightMoves(square: Square, piece: ChessPiece): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val knightMoves = listOf(
            -2 to -1, -2 to 1, // Up L
            -1 to -2, -1 to 2, // Left L
            1 to -2, 1 to 2,   // Right L
            2 to -1, 2 to 1    // Down L
        )
        for ((dr, dc) in knightMoves) {
            val nextRow = square.row + dr
            val nextCol = square.col + dc
            if (isValidSquare(nextRow, nextCol)) {
                val targetSquare = Square(nextRow, nextCol)
                val targetPiece = getPieceAt(targetSquare)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    moves.add(ChessMove(square, targetSquare, piece, capturedPiece = targetPiece))
                }
            }
        }
        return moves
    }

    private fun getKingMoves(square: Square, piece: ChessPiece, forAttackOnly: Boolean = false): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        val kingOffsets = listOf(
            -1 to -1, -1 to 0, -1 to 1,
            0 to -1,           0 to 1,
            1 to -1, 1 to 0, 1 to 1
        )
        for ((dr, dc) in kingOffsets) {
            val nextRow = square.row + dr
            val nextCol = square.col + dc
            if (isValidSquare(nextRow, nextCol)) {
                val targetSquare = Square(nextRow, nextCol)
                val targetPiece = getPieceAt(targetSquare)
                if (targetPiece == null || targetPiece.color != piece.color) {
                    // Check if this move would put king in check (done by isValidMove later)
                    moves.add(ChessMove(square, targetSquare, piece, capturedPiece = targetPiece))
                }
            }
        }
        if (forAttackOnly) {
            // Add castling moves if applicable
            return moves
        }
        // Castling (Kingside and Queenside)
        if (!piece.hasMoved && !isKingInCheck(piece.color)) {
            // Kingside
            val kingsideRookSquare = Square(square.row, 7)
            val kingsideRook = getPieceAt(kingsideRookSquare)
            if (kingsideRook?.type == PieceType.ROOK && !kingsideRook.hasMoved &&
                getPieceAt(Square(square.row, 5)) == null && getPieceAt(Square(square.row, 6)) == null &&
                !isSquareAttacked(Square(square.row, 5), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE) &&
                !isSquareAttacked(Square(square.row, 6), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)) {
                moves.add(ChessMove(square, Square(square.row, 6), piece, isCastlingMove = true))
            }

            // Queenside
            val queensideRookSquare = Square(square.row, 0)
            val queensideRook = getPieceAt(queensideRookSquare)
            if (queensideRook?.type == PieceType.ROOK && !queensideRook.hasMoved &&
                getPieceAt(Square(square.row, 1)) == null && getPieceAt(Square(square.row, 2)) == null && getPieceAt(Square(square.row, 3)) == null &&
                 !isSquareAttacked(Square(square.row, 2), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE) &&
                !isSquareAttacked(Square(square.row, 3), if (piece.color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)) {
                 moves.add(ChessMove(square, Square(square.row, 2), piece, isCastlingMove = true))
            }
        }
        return moves
    }

    fun isKingInCheck(kingColor: PieceColor): Boolean {
        val kingPos = findKingPosition(kingColor)
        return isSquareAttacked(kingPos, if (kingColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE)
    }

    // Checks if a square is attacked by the opponent
    private fun isSquareAttacked(square: Square, byColor: PieceColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val attacker = getPieceAt(Square(r, c)) ?: continue
                if (attacker.color != byColor) continue
                val moves = getPossibleMovesForPiece(Square(r, c), forAttackOnly = true)
                if (moves.any { it.to == square }) return true
            }
        }
        return false
    }

    public fun updateGameState() {
        val kingInCheck = isKingInCheck(currentPlayer)
        val hasLegalMoves = getAllLegalMovesForPlayer(currentPlayer).isNotEmpty()

        if (kingInCheck) {
            gameState = if (!hasLegalMoves) {
                if (currentPlayer == PieceColor.WHITE) GameState.CHECKMATE_BLACK_WINS else GameState.CHECKMATE_WHITE_WINS
            } else {
                GameState.CHECK
            }
        } else { // Not in check
            gameState = if (!hasLegalMoves) {
                GameState.STALEMATE_DRAW
            } else {
                GameState.ONGOING
            }
        }
        // TODO: Add other draw conditions (insufficient material, threefold repetition)
    }

    // Creates a deep copy of the board state
    fun copy(): ChessBoard {
        val newBoard = ChessBoard()
        for (r in 0..7) {
            for (c in 0..7) {
                newBoard.squares[r][c] = this.squares[r][c]?.copy() // Assuming ChessPiece is a data class
            }
        }
        newBoard.currentPlayer = this.currentPlayer
        newBoard.gameState = this.gameState
        newBoard.lastMove = this.lastMove // Shallow copy for lastMove is fine as ChessMove is data class
        newBoard.whiteKingPos = this.whiteKingPos
        newBoard.blackKingPos = this.blackKingPos
        newBoard.moveHistory.addAll(this.moveHistory)
        return newBoard
    }

    // For debugging or simple display
    fun printBoard() {
        println("  a b c d e f g h")
        for (r in 0..7) {
            print("${8 - r} ")
            for (c in 0..7) {
                val piece = squares[r][c]
                print(when (piece?.type) {
                    PieceType.KING -> if (piece.color == PieceColor.WHITE) "K" else "k"
                    PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "Q" else "q"
                    PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "R" else "r"
                    PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "B" else "b"
                    PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "N" else "n"
                    PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "P" else "p"
                    null -> "."
                } + " ")
            }
            println()
        }
        println("Current player: $currentPlayer, State: $gameState")
    }
}
