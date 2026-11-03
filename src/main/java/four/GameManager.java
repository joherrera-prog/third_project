package four;

/**
 * Stores the game state and applies the Connect Four rules.
 *
 * <p>This class does not create buttons, change colors, or display windows.
 * It only works with pieces, coordinates, turns, and game results.</p>
 *
 * <p>Board coordinates are zero-based. Row zero is the bottom row.
 * Column zero represents A, and column six represents G.</p>
 *
 * <p>A rejected move does not change the board, the current player,
 * the move counter, or the game result.</p>
 */
public final class GameManager {

    /** The board height: six rows, with indices from zero to five. */
    public static final int ROWS = 6;

    /** The board width: seven columns, with indices from zero to six. */
    public static final int COLUMNS = 7;

    /** The number of consecutive matching pieces required to win. */
    private static final int CONNECT_LENGTH = 4;

    /** The character used to represent an unoccupied board position. */
    private static final char EMPTY = ' ';

    /**
     * The row and column changes used to visit neighboring positions.
     *
     * <p>Each pair represents a direction, not a board position.
     * The first number changes the row, and the second changes the
     * column whenever we advance one step.</p>
     *
     * <p>For example, {0, 1} means that the row stays the same while
     * the column increases by one.</p>
     */
    private static final int[][] DIRECTIONS = {
            {0, 1}, // Move right: keep the row and increase the column.
            {1, 0}, // Move up: increase the row and keep the column.
            {1, 1}, // Move diagonally up and right.
            {1, -1} // Move diagonally up and left.
    };

    /**
     * The pieces currently stored in the board.
     *
     * <p>Each position contains X, O, or an empty space.
     * For example, board[0][2] contains the piece at C1.</p>
     *
     * <p>The final reference always identifies the same board array.
     * Its individual positions can still be modified.</p>
     */
    private final char[][] board = new char[ROWS][COLUMNS];

    /**
     * The player whose piece will be placed by the next accepted move.
     *
     * <p>During victory detection, this still identifies the player
     * who just moved. After the game ends, it keeps that player.</p>
     */
    private char turn;

    /** Whether a victory or a draw has blocked further moves. */
    private boolean gameFinished;

    /**
     * The number of pieces successfully placed on the board.
     *
     * <p>Rejected moves do not increase this counter.
     * Forty-two accepted moves fill the entire board.</p>
     */
    private int movesPlayed;

    /**
     * The coordinates of one winning line, or an empty array.
     *
     * <p>Each inner array stores a position as {row, column}.
     * These pairs identify positions, unlike the direction pairs.</p>
     *
     * <p>For example, {0, 2} identifies C1.
     * Four pairs are stored only after a winning line is verified.</p>
     */
    private int[][] winningCells;

    /**
     * Creates an empty game with X playing first.
     */
    public GameManager() {
        // A new game and a reset game need the same initial state.
        reset();
    }

    /**
     * Removes all pieces and restores the initial game state.
     */
    public void reset() {
        // Visit every row of the existing board.
        for (int rowIndex = 0; rowIndex < ROWS; rowIndex++) {

            // Visit every column within this row.
            for (int colIndex = 0; colIndex < COLUMNS; colIndex++) {

                // Replace the previous piece with an empty space.
                board[rowIndex][colIndex] = EMPTY;
            }
        }

        // X always starts a new game.
        turn = 'X';

        // The new game accepts moves.
        gameFinished = false;

        // No pieces have been placed in this game.
        movesPlayed = 0;

        // Create an array containing zero coordinate pairs.
        // This does not represent a winning line filled with zeros.
        // There are no winning positions stored at all.
        winningCells = new int[0][2];
    }

    /**
     * Attempts to place the current player's piece in one column.
     *
     * <p>An accepted move applies gravity, checks for a victory or
     * a draw, and changes the turn if the game can continue.</p>
     *
     * @param columnLetter the requested column, from A to G
     * @return the occupied row index, or -1 if the move is rejected
     */
    public int dropPiece(final char columnLetter) {
        // Reject finished games and letters outside the board.
        if (gameFinished || columnLetter < 'A' || columnLetter > 'G') {
            return -1;
        }

        // Convert the visual letter into an array index.
        // 'A' - 'A' gives 0; 'B' - 'A' gives 1; and so on.
        final int colIndex = columnLetter - 'A';

        // Start at the bottom and search upward in this column.
        for (int rowIndex = 0; rowIndex < ROWS; rowIndex++) {

            // Gravity places the piece in the first empty position.
            if (board[rowIndex][colIndex] == EMPTY) {

                // Store the current player's symbol in the model.
                board[rowIndex][colIndex] = turn;

                // Count only this successfully accepted move.
                movesPlayed++;

                // A newly created victory must include this piece.
                // Check before changing turn, while it still identifies
                // the player whose piece we have just placed.
                findWinningCells(rowIndex, colIndex);

                // A winning line ends the game.
                // A full board also ends it, producing a draw
                // when there is no winning line.
                gameFinished = hasWinner()
                        || movesPlayed == ROWS * COLUMNS;

                // Change players only if another move is allowed.
                if (!gameFinished) {
                    turn = turn == 'X' ? 'O' : 'X';
                }

                // Exit the entire method after placing one piece.
                // Row zero is valid, so failure uses -1 instead.
                return rowIndex;
            }
        }

        // We reach this point only if every position was occupied.
        // The placement block never ran, so no state was changed.
        return -1;
    }

    /**
     * Searches for a winning line containing the latest piece.
     *
     * <p>For each direction, the latest piece can be the first,
     * second, third, or fourth position of a possible winning line.
     * The offset selects each of those possibilities.</p>
     *
     * @param rowIndex the row where the latest piece was placed
     * @param colIndex the column where the latest piece was placed
     */
    private void findWinningCells(final int rowIndex,
                                  final int colIndex) {
        // Select one direction pair at a time.
        for (int[] direction : DIRECTIONS) {

            // Read how much the row changes with each forward step.
            final int rowStep = direction[0];

            // Read how much the column changes with each forward step.
            final int colStep = direction[1];

            // Try four possible starting positions.
            // Offset 0: the latest piece starts the candidate line.
            // Offset 1: one position comes before the latest piece.
            // Offset 2: two positions come before the latest piece.
            // Offset 3: three positions come before the latest piece.
            for (int offset = 0; offset < CONNECT_LENGTH; offset++) {

                // Move backward from the latest piece to find
                // the starting row of this candidate line.
                final int startRow = rowIndex - offset * rowStep;

                // Apply the same backward movement to the column.
                final int startCol = colIndex - offset * colStep;

                // The starting position may be outside the board.
                // isWinningLine safely rejects such candidates.
                if (isWinningLine(startRow, startCol,
                        rowStep, colStep)) {

                    // All four pieces have already been checked.
                    // Now remember their coordinates for the view.
                    saveWinningCells(startRow, startCol,
                            rowStep, colStep);

                    // One winning line is enough to finish the game.
                    return;
                }
            }
        }

        // If no candidate wins, the stored coordinates stay empty.
    }

    /**
     * Checks whether a candidate contains four matching pieces.
     *
     * <p>The candidate is defined by a starting position and a
     * direction. Each inspected position must belong to the board
     * and contain the symbol of the player who just moved.</p>
     *
     * @param row the starting row of the candidate line
     * @param col the starting column of the candidate line
     * @param rowStep the row change for each forward step
     * @param colStep the column change for each forward step
     * @return whether all four positions contain the player's piece
     */
    private boolean isWinningLine(final int row, final int col,
                                  final int rowStep,
                                  final int colStep) {
        // Inspect four positions: steps 0, 1, 2, and 3.
        // Step zero inspects the starting position itself.
        for (int step = 0; step < CONNECT_LENGTH; step++) {

            // Position = starting position + steps * direction.
            // Multiplication represents repeated movement.
            final int currentRow = row + step * rowStep;
            final int currentCol = col + step * colStep;

            // Reject a position outside the board or a different piece.
            //
            // The || operator evaluates the left condition first.
            // If the position is outside, the left condition is true
            // and Java does not access the board on the right.
            //
            // Otherwise, read the position and compare its piece
            // with the player whose move we are checking.
            if (!isInsideBoard(currentRow, currentCol)
                    || board[currentRow][currentCol] != turn) {

                // One invalid position disproves this candidate.
                // Other candidates may still produce a victory.
                return false;
            }
        }

        // This point is reached only after all four positions pass.
        // No position was outside, empty, or occupied by the opponent.
        return true;
    }

    /**
     * Checks whether a position belongs to the board.
     *
     * @param row the row index to validate
     * @param col the column index to validate
     * @return whether both indices are within their valid ranges
     */
    private boolean isInsideBoard(final int row, final int col) {
        // Valid rows: 0 through 5.
        // Valid columns: 0 through 6.
        // All four conditions must be true.
        return row >= 0 && row < ROWS
                && col >= 0 && col < COLUMNS;
    }

    /**
     * Stores the coordinates of an already verified winning line.
     *
     * <p>This method does not check the pieces.
     * It visits the same positions that isWinningLine verified
     * and records their coordinates for highlighting.</p>
     *
     * @param row the starting row of the winning line
     * @param col the starting column of the winning line
     * @param rowStep the row change for each forward step
     * @param colStep the column change for each forward step
     */
    private void saveWinningCells(final int row, final int col,
                                  final int rowStep,
                                  final int colStep) {
        // Allocate four coordinate pairs, each containing two integers.
        // Allocation initially fills their values with zero.
        // The loop below replaces them with the actual coordinates.
        winningCells = new int[CONNECT_LENGTH][2];

        // Store the coordinates of each of the four winning positions.
        for (int step = 0; step < CONNECT_LENGTH; step++) {

            // step selects which coordinate pair we are filling.
            // Index 0 inside that pair stores the board row.
            winningCells[step][0] = row + step * rowStep;

            // Index 1 inside that same pair stores the board column.
            winningCells[step][1] = col + step * colStep;
        }
    }

    /**
     * Returns the player associated with the current turn.
     *
     * <p>After a continuing move, this is the next player.
     * After a finished game, this is the player who moved last.
     * A finished game may be a victory or a draw.</p>
     *
     * @return X or O
     */
    public char getTurn() {
        return turn;
    }

    /**
     * Reads one board position without exposing the board array.
     *
     * @param rowIndex a valid row index, from zero to five
     * @param colIndex a valid column index, from zero to six
     * @return X, O, or an empty space
     */
    public char getPieceAt(final int rowIndex, final int colIndex) {
        // Return one character instead of the mutable board array.
        return board[rowIndex][colIndex];
    }

    /**
     * Reports whether the game has ended.
     *
     * @return whether a victory or a draw blocks further moves
     */
    public boolean isGameFinished() {
        return gameFinished;
    }

    /**
     * Reports whether a verified winning line has been stored.
     *
     * <p>This method does not search the board again.
     * It relies on the rule that only a verified victory causes
     * four coordinate pairs to be stored.</p>
     *
     * @return whether a winning line has been found
     */
    public boolean hasWinner() {
        // An empty array has length zero.
        // A saved winning line contains four coordinate pairs.
        return winningCells.length == CONNECT_LENGTH;
    }

    /**
     * Returns an independent copy of the winning coordinates.
     *
     * <p>Copying each inner pair prevents callers from changing
     * the coordinates stored inside this model.</p>
     *
     * @return four coordinate pairs, or an empty array
     */
    public int[][] getWinningCells() {
        // Create separate storage with the same number of pairs.
        // Before a victory, this creates an empty array.
        final int[][] copy = new int[winningCells.length][2];

        // Visit each stored winning coordinate pair.
        for (int index = 0; index < winningCells.length; index++) {

            // Copy the row value into the new pair.
            copy[index][0] = winningCells[index][0];

            // Copy the column value into the new pair.
            copy[index][1] = winningCells[index][1];
        }

        // The caller receives the copy instead of the internal array.
        // Changing the returned pairs cannot change winningCells.
        return copy;
    }
}
