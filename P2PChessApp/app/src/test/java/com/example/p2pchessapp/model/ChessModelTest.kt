package com.example.p2pchessapp.model

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChessBoardTest {

    private lateinit var board: ChessBoard

    @Before
    fun setUp() {
        board = ChessBoard() // Resets board to initial state
    }

    @Test
    fun initialBoardSetup_CorrectPieces() {
        // Pawns
        for (col in 0..7) {
            assertEquals(PieceType.PAWN, board.getPieceAt(Square(1, col))?.type)
            assertEquals(PieceColor.BLACK, board.getPieceAt(Square(1, col))?.color)
            assertEquals(PieceType.PAWN, board.getPieceAt(Square(6, col))?.type)
            assertEquals(PieceColor.WHITE, board.getPieceAt(Square(6, col))?.color)
        }

        // Rooks
        assertEquals(PieceType.ROOK, board.getPieceAt(Square(0, 0))?.type)
        assertEquals(PieceType.ROOK, board.getPieceAt(Square(0, 7))?.type)
        assertEquals(PieceType.ROOK, board.getPieceAt(Square(7, 0))?.type)
        assertEquals(PieceType.ROOK, board.getPieceAt(Square(7, 7))?.type)
        assertEquals(PieceColor.BLACK, board.getPieceAt(Square(0,0))?.color)
        assertEquals(PieceColor.WHITE, board.getPieceAt(Square(7,0))?.color)


        // Knights
        assertEquals(PieceType.KNIGHT, board.getPieceAt(Square(0, 1))?.type)
        assertEquals(PieceType.KNIGHT, board.getPieceAt(Square(0, 6))?.type)
        assertEquals(PieceType.KNIGHT, board.getPieceAt(Square(7, 1))?.type)
        assertEquals(PieceType.KNIGHT, board.getPieceAt(Square(7, 6))?.type)

        // Bishops
        assertEquals(PieceType.BISHOP, board.getPieceAt(Square(0, 2))?.type)
        assertEquals(PieceType.BISHOP, board.getPieceAt(Square(0, 5))?.type)
        assertEquals(PieceType.BISHOP, board.getPieceAt(Square(7, 2))?.type)
        assertEquals(PieceType.BISHOP, board.getPieceAt(Square(7, 5))?.type)

        // Queens
        assertEquals(PieceType.QUEEN, board.getPieceAt(Square(0, 3))?.type)
        assertEquals(PieceType.QUEEN, board.getPieceAt(Square(7, 3))?.type)

        // Kings
        assertEquals(PieceType.KING, board.getPieceAt(Square(0, 4))?.type)
        assertEquals(PieceType.KING, board.getPieceAt(Square(7, 4))?.type)

        // Current player
        assertEquals(PieceColor.WHITE, board.currentPlayer)
        assertEquals(GameState.ONGOING, board.gameState)
    }

    @Test
    fun pawnMoves_WhitePawnInitial() {
        val pawnStart = Square(6, 4) // e2
        val oneStep = Square(5, 4)   // e3
        val twoSteps = Square(4, 4)  // e4

        assertTrue("Pawn one step", board.isValidMove(ChessMove(pawnStart, oneStep, board.getPieceAt(pawnStart)!!)))
        assertTrue("Pawn two steps", board.isValidMove(ChessMove(pawnStart, twoSteps, board.getPieceAt(pawnStart)!!)))

        // Move pawn one step then try two steps (should be invalid)
        board.makeMove(ChessMove.fromSimpleNotation("e2e3", board)!!)
        assertFalse("Pawn two steps after first move", board.isValidMove(ChessMove(Square(5,4), Square(3,4), board.getPieceAt(Square(5,4))!!)))
    }

    @Test
    fun pawnMoves_WhitePawnCapture() {
        // Setup: White pawn at e2, Black pawn at d3
        board.resetBoard() // Clear board
        val whitePawn = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        val blackPawn = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
        board.squares[6][4] = whitePawn // e2
        board.squares[5][3] = blackPawn // d3 (for capture by e2 pawn)
        board.currentPlayer = PieceColor.WHITE

        val captureMove = ChessMove(Square(6,4), Square(5,3), whitePawn, capturedPiece = blackPawn)
        assertTrue("White Pawn e2xd3 capture", board.isValidMove(captureMove))
        assertTrue(board.makeMove(captureMove))
        assertEquals(whitePawn, board.getPieceAt(Square(5,3)))
        assertNull(board.getPieceAt(Square(6,4)))
    }


    @Test
    fun pawnPromotion_WhitePawnToQueen() {
        board.resetBoard()
        val whitePawn = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        board.squares[1][4] = whitePawn // White pawn at e7
        board.currentPlayer = PieceColor.WHITE

        val promotionSquare = Square(0, 4) // e8
        val promotionMove = ChessMove(Square(1,4), promotionSquare, whitePawn, promotionTo = PieceType.QUEEN)

        assertTrue("Pawn promotion valid", board.isValidMove(promotionMove))
        assertTrue(board.makeMove(promotionMove))
        assertEquals(PieceType.QUEEN, board.getPieceAt(promotionSquare)?.type)
        assertEquals(PieceColor.WHITE, board.getPieceAt(promotionSquare)?.color)
    }

    @Test
    fun rookMoves_ValidAndInvalid() {
        val rookStart = Square(7, 0) // a1
        assertTrue(board.isValidMove(ChessMove.fromSimpleNotation("a1a3", board)!!)) // Valid move
        board.makeMove(ChessMove.fromSimpleNotation("a2a3", board)!!) // move pawn out of the way
        board.currentPlayer = PieceColor.WHITE // set back to white
        assertFalse("Rook cannot jump over piece (initial)", board.isValidMove(ChessMove.fromSimpleNotation("a1a4", board)!!))

        board.resetBoard()
        val whiteRook = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        board.squares[3][3] = whiteRook // Rook at d5
        board.currentPlayer = PieceColor.WHITE

        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(3,0), whiteRook))) // d5-d1
        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(0,3), whiteRook))) // d5-a5
        assertFalse(board.isValidMove(ChessMove(Square(3,3), Square(2,2), whiteRook))) // d5-c6 (invalid diagonal)
    }


    @Test
    fun knightMoves_Valid() {
        val knightStart = Square(7, 1) // b1
        assertTrue(board.isValidMove(ChessMove.fromSimpleNotation("b1c3", board)!!))
        assertTrue(board.isValidMove(ChessMove.fromSimpleNotation("b1a3", board)!!))
    }

    @Test
    fun bishopMoves_ValidAndInvalid() {
        // b1 bishop blocked by c2 pawn initially
        assertFalse(board.isValidMove(ChessMove.fromSimpleNotation("c1e3", board)!!))

        board.resetBoard()
        val whiteBishop = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        board.squares[3][3] = whiteBishop // Bishop at d5
        board.currentPlayer = PieceColor.WHITE

        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(1,1), whiteBishop))) // d5-b7
        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(5,5), whiteBishop))) // d5-f3
        assertFalse(board.isValidMove(ChessMove(Square(3,3), Square(3,4), whiteBishop))) // d5-e5 (invalid straight)
    }

    @Test
    fun queenMoves_Valid() {
        // d1 queen blocked by d2 pawn initially
        assertFalse(board.isValidMove(ChessMove.fromSimpleNotation("d1d4", board)!!))
        assertFalse(board.isValidMove(ChessMove.fromSimpleNotation("d1h5", board)!!))

        board.resetBoard()
        val whiteQueen = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
        board.squares[3][3] = whiteQueen // Queen at d5
        board.currentPlayer = PieceColor.WHITE

        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(3,0), whiteQueen))) // d5-d1 (straight)
        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(0,3), whiteQueen))) // d5-a5 (straight)
        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(1,1), whiteQueen))) // d5-b7 (diagonal)
        assertTrue(board.isValidMove(ChessMove(Square(3,3), Square(5,5), whiteQueen))) // d5-f3 (diagonal)
    }

    @Test
    fun kingMoves_ValidAndInvalid() {
        // e1 king cannot move initially due to being blocked or self-check (not implemented here for initial)
        // but can't move to e2 because of pawn
        assertFalse(board.isValidMove(ChessMove.fromSimpleNotation("e1e2", board)!!))

        // Test basic king move after clearing path
        board.makeMove(ChessMove.fromSimpleNotation("e2e4", board)!!) // White moves e4
        board.makeMove(ChessMove.fromSimpleNotation("e7e5", board)!!) // Black moves e5
        assertTrue(board.isValidMove(ChessMove.fromSimpleNotation("e1e2", board)!!)) // Now king can move to e2
    }

    @Test
    fun kingSideCastling_White_Valid() {
        // Clear path for castling: e1-h1 for King and Rook
        board.squares[7][5] = null // f1
        board.squares[7][6] = null // g1
        board.currentPlayer = PieceColor.WHITE

        val king = board.getPieceAt(Square(7,4))!!
        val castlingMove = ChessMove(Square(7,4), Square(7,6), king, isCastlingMove = true)

        assertTrue("Kingside Castling Pre-check", board.isValidMove(castlingMove, ignoreSelfCheck = false))
        assertTrue(board.makeMove(castlingMove))
        assertEquals(PieceType.KING, board.getPieceAt(Square(7,6))?.type)
        assertEquals(PieceType.ROOK, board.getPieceAt(Square(7,5))?.type)
        assertNull(board.getPieceAt(Square(7,4)))
        assertNull(board.getPieceAt(Square(7,7)))
    }

    @Test
    fun queenSideCastling_White_Valid() {
        board.squares[7][1] = null // b1
        board.squares[7][2] = null // c1
        board.squares[7][3] = null // d1
        board.currentPlayer = PieceColor.WHITE

        val king = board.getPieceAt(Square(7,4))!!
        val castlingMove = ChessMove(Square(7,4), Square(7,2), king, isCastlingMove = true)

        assertTrue("Queenside Castling Pre-check", board.isValidMove(castlingMove))
        assertTrue(board.makeMove(castlingMove))
        assertEquals(PieceType.KING, board.getPieceAt(Square(7,2))?.type)
        assertEquals(PieceType.ROOK, board.getPieceAt(Square(7,3))?.type)
    }

    @Test
    fun castling_Invalid_KingMoved() {
        board.squares[7][5] = null; board.squares[7][6] = null // Clear path
        board.makeMove(ChessMove.fromSimpleNotation("e1f1", board)!!) // Move King
        board.makeMove(ChessMove.fromSimpleNotation("e8f8", board)!!) // Dummy black move
        board.makeMove(ChessMove.fromSimpleNotation("f1e1", board)!!) // Move King back
        board.makeMove(ChessMove.fromSimpleNotation("f8e8", board)!!) // Dummy black move

        val king = board.getPieceAt(Square(7,4))!!
        val castlingMove = ChessMove(Square(7,4), Square(7,6), king, isCastlingMove = true)
        assertFalse("Cannot castle if king moved", board.isValidMove(castlingMove))
    }

    @Test
    fun castling_Invalid_RookMoved() {
        board.squares[7][5] = null; board.squares[7][6] = null // Clear path
        board.makeMove(ChessMove.fromSimpleNotation("h1g1", board)!!) // Move Rook
        board.makeMove(ChessMove.fromSimpleNotation("e7e6", board)!!) // Dummy black move
        board.makeMove(ChessMove.fromSimpleNotation("g1h1", board)!!) // Move Rook back
        board.makeMove(ChessMove.fromSimpleNotation("e6e5", board)!!) // Dummy black move

        val king = board.getPieceAt(Square(7,4))!!
        val castlingMove = ChessMove(Square(7,4), Square(7,6), king, isCastlingMove = true)
        assertFalse("Cannot castle if rook moved", board.isValidMove(castlingMove))
    }

    @Test
    fun castling_Invalid_PathBlocked() {
        // f1 is NOT null, path blocked
        board.squares[7][6] = null
        val king = board.getPieceAt(Square(7,4))!!
        val castlingMove = ChessMove(Square(7,4), Square(7,6), king, isCastlingMove = true)
        assertFalse("Cannot castle if path blocked", board.isValidMove(castlingMove))
    }

    @Test
    fun checkDetection_Simple() {
        board.resetBoard()
        board.squares[3][3] = ChessPiece(PieceType.ROOK, PieceColor.WHITE) // White Rook at d5
        board.squares[3][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)  // Black King at e5
        board.currentPlayer = PieceColor.WHITE // White's turn, but we check black king

        assertTrue("Black King should be in check", board.isKingInCheck(PieceColor.BLACK))
    }

    @Test
    fun selfCheck_InvalidMove() {
        board.resetBoard()
        // K . . .
        // . . . .
        // R . . . (White Rook attacking King's escape)
        // . k . . (Black King)
        board.squares[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE) // White King e1
        board.squares[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK) // Black Rook a8
        board.squares[2][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE) // White Rook a6 (protects a1)
        board.squares[0][4] = null // remove black king for this test
        board.currentPlayer = PieceColor.WHITE

        // Try to move King e1 to d1, which would be into check from a1 (a8 Rook)
        val illegalMove = ChessMove(Square(7,4), Square(7,3), board.getPieceAt(Square(7,4))!!)
        assertFalse("King cannot move into check", board.isValidMove(illegalMove))
    }


    @Test
    fun checkmate_FoolsMate() {
        // 1. f2f3 e7e5
        // 2. g2g4 d8h4#
        assertTrue(board.makeMove(ChessMove.fromSimpleNotation("f2f3", board)!!))
        assertTrue(board.makeMove(ChessMove.fromSimpleNotation("e7e5", board)!!))
        assertTrue(board.makeMove(ChessMove.fromSimpleNotation("g2g4", board)!!))

        val checkmateMove = ChessMove.fromSimpleNotation("d8h4", board)!!
        assertTrue("Queen Qh4# is a valid move pattern", board.isValidMove(checkmateMove, ignoreSelfCheck = true)) // Check if move itself is valid pattern

        assertTrue(board.makeMove(checkmateMove))
        assertEquals(GameState.CHECKMATE_BLACK_WINS, board.gameState)
        assertTrue("White King should be in check", board.isKingInCheck(PieceColor.WHITE))
        assertEquals(0, board.getAllLegalMovesForPlayer(PieceColor.WHITE).size) // No legal moves for white
    }

    @Test
    fun stalemate_BlockedKing() {
        board.resetBoard()
        // Setup:
        // Black King at h8 (0,7)
        // White Queen at g6 (2,6)
        // White King at f5 (3,5) (to prevent black king moving to g7 or h7)
        board.squares[0][7] = ChessPiece(PieceType.KING, PieceColor.BLACK) // Black King h8
        board.squares[2][6] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE) // White Queen g6
        board.squares[3][5] = ChessPiece(PieceType.KING, PieceColor.WHITE) // White King f5

        // Clear other pieces for simplicity
        for (r in 0..7) for (c in 0..7) if (!( (r==0 && c==7) || (r==2 && c==6) || (r==3 && c==5) )) board.squares[r][c] = null

        board.currentPlayer = PieceColor.BLACK
        board.updateGameState() // Manually trigger state update after setting up board

        assertFalse("Black King is not in check", board.isKingInCheck(PieceColor.BLACK))
        assertEquals("Black has no legal moves", 0, board.getAllLegalMovesForPlayer(PieceColor.BLACK).size)
        assertEquals(GameState.STALEMATE_DRAW, board.gameState)
    }

    @Test
    fun chessMoveNotation_toAndFrom() {
        val move1 = ChessMove(Square(6,4), Square(4,4), ChessPiece(PieceType.PAWN, PieceColor.WHITE)) // e2e4
        assertEquals("e2e4", move1.toSimpleNotation())
        val parsedMove1 = ChessMove.fromSimpleNotation("e2e4", board)
        assertNotNull(parsedMove1)
        assertEquals(move1.from, parsedMove1!!.from)
        assertEquals(move1.to, parsedMove1.to)

        val promotionMove = ChessMove(Square(1,0), Square(0,0), ChessPiece(PieceType.PAWN, PieceColor.WHITE), promotionTo = PieceType.QUEEN) // a7a8q
        assertEquals("a7a8q", promotionMove.toSimpleNotation())
        val parsedPromotion = ChessMove.fromSimpleNotation("a7a8q", board) // Need a board with a pawn on a7
        board.resetBoard()
        board.squares[1][0] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        assertNotNull(parsedPromotion)
        assertEquals(promotionMove.from, parsedPromotion!!.from)
        assertEquals(promotionMove.to, parsedPromotion.to)
        assertEquals(PieceType.QUEEN, parsedPromotion.promotionTo)
    }

    @Test
    fun copyBoard_IsDeepCopy() {
        board.makeMove(ChessMove.fromSimpleNotation("e2e4", board)!!)
        val copiedBoard = board.copy()

        // Ensure they are different instances but have same values initially
        assertNotSame(board, copiedBoard)
        assertEquals(board.getPieceAt(Square(4,4))?.type, copiedBoard.getPieceAt(Square(4,4))?.type)
        assertEquals(board.currentPlayer, copiedBoard.currentPlayer)

        // Modify original board, copied board should not change
        board.makeMove(ChessMove.fromSimpleNotation("e7e5", board)!!) // Black makes a move
        assertNotEquals(board.getPieceAt(Square(3,4))?.type, copiedBoard.getPieceAt(Square(3,4))?.type) // (4,4) was white pawn, e5 is (3,4) for black
        assertNotEquals(board.currentPlayer, copiedBoard.currentPlayer)

        // Modify copied board, original should not change
        copiedBoard.makeMove(ChessMove.fromSimpleNotation("d2d4", copiedBoard)!!) // White makes another move on copy
        assertNull(board.getPieceAt(Square(4,3))) // d4 on original should still be null if not moved
        assertNotNull(copiedBoard.getPieceAt(Square(4,3))) // d4 on copy should be the pawn
    }

    @Test
    fun enPassant_SetupAndExecution() {
        board.resetBoard()
        // 1. e2e4 (White)
        board.makeMove(ChessMove.fromSimpleNotation("e2e4", board)!!)
        // 2. a7a6 (Black - dummy move)
        board.makeMove(ChessMove.fromSimpleNotation("a7a6", board)!!)
        // 3. e4e5 (White - pawn now at e5)
        board.makeMove(ChessMove.fromSimpleNotation("e4e5", board)!!)
        // 4. d7d5 (Black - pawn advances two squares next to white e5 pawn)
        val blackPawnD5 = board.getPieceAt(Square(1,3))!! // d7 pawn
        board.makeMove(ChessMove(Square(1,3), Square(3,3), blackPawnD5)) // d7d5

        // White's turn. White pawn at e5 (row 3, col 4). Black pawn just moved to d5 (row 3, col 3).
        // White should be able to capture d5 pawn by moving e5xd6 (to row 2, col 3)
        val whitePawnE5 = board.getPieceAt(Square(3,4))!!
        val enPassantTargetSquare = Square(2,3) // d6

        // Check if en passant is a valid move
        val enPassantMove = ChessMove(Square(3,4), enPassantTargetSquare, whitePawnE5, isEnPassant = true, capturedPiece = board.getPieceAt(Square(3,3)))

        // Need to manually check getPossibleMovesForPiece because isValidMove itself calls it.
        val possibleMovesForE5Pawn = board.getPossibleMovesForPiece(Square(3,4))
        val foundEnPassant = possibleMovesForE5Pawn.any { it.to == enPassantTargetSquare && it.isEnPassant }
        assertTrue("En passant move e5xd6 should be in possible moves", foundEnPassant)

        assertTrue("En passant move e5xd6 should be valid", board.isValidMove(enPassantMove))

        // Make the en passant move
        assertTrue(board.makeMove(enPassantMove))

        // Assertions after en passant
        assertEquals(whitePawnE5, board.getPieceAt(enPassantTargetSquare)) // White pawn moved to d6
        assertNull(board.getPieceAt(Square(3,4))) // Original square e5 is empty
        assertNull(board.getPieceAt(Square(3,3))) // Captured black pawn at d5 is gone
    }
}

class ChessMoveTest {
    @Test
    fun toString_FormatsCorrectly() {
        val piece = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        val from = Square(6,0) // a2
        val to = Square(4,0)   // a4
        val move = ChessMove(from, to, piece)
        assertEquals("PAWN from a2 to a4", move.toString())

        val capture = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        val moveWithCapture = ChessMove(from, to, piece, capturedPiece = capture)
        assertEquals("PAWN from a2 to a4 captures ROOK", moveWithCapture.toString())

        val promotionMove = ChessMove(from, Square(0,0), piece, promotionTo = PieceType.QUEEN) // a7a8Q
        assertEquals("PAWN from a2 to a8 promotes to QUEEN", promotionMove.toString())

        val castlingMove = ChessMove(Square(7,4), Square(7,6), ChessPiece(PieceType.KING, PieceColor.WHITE), isCastlingMove = true)
        assertEquals("KING from e1 to g1 (castling)", castlingMove.toString())
    }
}

class SquareTest {
    @Test
    fun toString_FormatsToAlgebraic() {
        assertEquals("a8", Square(0,0).toString())
        assertEquals("h8", Square(0,7).toString())
        assertEquals("a1", Square(7,0).toString())
        assertEquals("h1", Square(7,7).toString())
        assertEquals("e4", Square(4,4).toString())
    }
}
