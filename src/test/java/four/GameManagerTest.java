// Pure JUnit testing for game logic, winning, etc, different scenarios

package four;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the observable behavior of the Connect Four model.
 *
 */
final class GameManagerTest {

    /**
     * Verifies the initial state of a newly constructed game.
     *
     * <p>The board must be empty, X must play first, and the game
     * must not be finished or have a winner.</p>
     */
    @Test
    void startsWithAnEmptyBoardAndXTurn() {
        // Constructing the model also initializes its game state.
        final GameManager game = new GameManager();

        // The second argument is the actual value returned by the model.
        assertEquals('X', game.getTurn());

        // A new game must allow players to make moves.
        assertFalse(game.isGameFinished());

        // No player can have won before any pieces are placed.
        assertFalse(game.hasWinner());

        // getWinningCells returns an array of coordinate pairs.
        // Its length must be zero because no winning line exists.
        assertEquals(0, game.getWinningCells().length);

        // This is our own helper method, defined near the file's end.
        // It checks that all 42 board positions contain a space.
        assertEmpty(game);
    }

    /**
     * Verifies gravity and turn changes after accepted moves.
     *
     * <p>Two pieces placed in the same column must occupy consecutive
     * positions from the bottom upward.</p>
     */
    @Test
    void appliesGravityAndAlternatesTurns() {
        // Start with an empty board and X ready to play.
        final GameManager game = new GameManager();

        // dropPiece is executed while evaluating this assertion.
        // The first piece in column A must land at row index zero.
        assertEquals(0, game.dropPiece('A'));

        // Column A has index zero.
        // The bottom position must now contain the first player's X.
        assertEquals('X', game.getPieceAt(0, 0));

        // The accepted move must hand the next turn to O.
        assertEquals('O', game.getTurn());

        // Place another piece in the same column.
        // Row zero is occupied, so this piece must land at row one.
        assertEquals(1, game.dropPiece('A'));

        // The second player placed O above the existing X.
        assertEquals('O', game.getPieceAt(1, 0));

        // After O's accepted move, X must play next.
        assertEquals('X', game.getTurn());
    }

    /**
     * Verifies that invalid column letters are rejected.
     *
     * <p>The same test is executed separately for each supplied
     * character. Every execution creates a fresh game.</p>
     *
     * @param column the invalid column supplied by ValueSource
     */
    @ParameterizedTest // this is like doing 4 different tests
    @ValueSource(chars = {'@', 'H', 'a', '\0'}) // \0 us null
    void rejectsInvalidColumns(final char column) {
        // @ParameterizedTest allows this method to receive test data.
        //
        // @ValueSource provides four separate executions:
        // '@' comes before 'A'.
        // 'H' comes after 'G'.
        // 'a' is lowercase and is not an accepted column.
        // '\0' is the null character, not an empty space.

        // ARRANGE:
        final GameManager game = new GameManager();

        // Save a representation of the observable state.
        // This is a String describing the game, not another game object.
        final String before = snapshot(game);

        // An invalid column must produce the rejection result.
        assertEquals(-1, game.dropPiece(column));

        // The board, turn, result, and winning coordinates must match
        // the values captured before the rejected move.
        assertEquals(before, snapshot(game));
    }

    /**
     * Verifies that a full column rejects additional pieces.
     *
     * <p>The rejection must preserve the current turn, allowing the
     * same player to choose another column.</p>
     */
    @Test
    void rejectsAFullColumnWithoutChangingTurn() {
        final GameManager game = new GameManager();

        // Make six accepted moves in column A.
        // The pieces alternate: X, O, X, O, X, O.
        // Therefore, this column fills without a vertical victory.
        play(game, "AAAAAA");

        // Capture the state after the column becomes full.
        final String before = snapshot(game);

        // A seventh piece cannot fit in this column.
        assertEquals(-1, game.dropPiece('A'));

        // Rejecting the move must leave the observable state unchanged.
        assertEquals(before, snapshot(game));

        // The player can retry in a different column.
        // Column B is empty, so the piece must land at row zero.
        assertEquals(0, game.dropPiece('B'));

        // Six accepted moves return the turn to X.
        // The rejected seventh attempt must not consume X's turn.
        assertEquals('X', game.getPieceAt(0, 1));
    }

    /**
     * Verifies winning lines for both players and all four directions.
     *
     * <p>Additional cases check a victory completed in the middle of
     * a line and a move that connects five pieces at once.</p>
     *
     * @param sequence the sequence of requested column letters
     * @param winner the expected winning player
     * @param rowStep the expected row change between winning positions
     * @param colStep the expected column change between positions
     */
    @ParameterizedTest
    @CsvSource({ // Coma Separated Values = Csv
            "AGBGCFD, X, 0, 1", // case 1 Horizontal victory for X
            "ABABABA, X, 1, 0", // Vertical victory for X
            "ABBCGCCDGDFDD, X, 1, 1", // Diagonal victory up and right for X
            "GFFEA EEDADBDD, X, 1, -1", // Diagonal victory  up and left for X
            "GAGBFCFD, O, 0, 1", // Horizontal victory for O
            "AGBGDFC, X, 0, 1", // Horizontal victory completed by filling a
            // middle gap
            "AGBGDFEFC, X, 0, 1" // case 7 A move connecting five X pieces,
            // storing one line of four
    })
    void detectsWinningLines(final String sequence, final char winner,
                             final int rowStep, final int colStep) {
        // Each CsvSource row provides one independent test execution.
        // Its four values are passed to this method's four parameters.

        final GameManager game = new GameManager();

        // Remove the readability space used in one supplied sequence.
        // Each remaining character represents one requested column.
        final String moves = sequence.replace(" ", "");

        // This variable will store the row returned by the latest move.
        // -1 is an initial placeholder before any move is processed.
        int lastRow = -1;

        // Process the sequence one character at a time.
        for (int index = 0; index < moves.length(); index++) {

            // The game must still be active before the next move.
            // Otherwise, a victory occurred earlier than intended.
            // The message appears if this assertion fails.
            assertFalse(game.isGameFinished(), "No premature victory");

            // charAt(index) obtains the column letter for this move.
            // Save the actual row where this piece lands.
            lastRow = game.dropPiece(moves.charAt(index));

            // Every move in this prepared sequence must be accepted.
            assertNotEquals(-1, lastRow);
        }

        // The final move must complete a winning line.
        assertTrue(game.hasWinner());

        // A victory must also mark the game as finished.
        assertTrue(game.isGameFinished());

        // The model keeps the last player's turn when the game ends.
        // For these cases, that player must be the expected winner.
        assertEquals(winner, game.getTurn());

        // Obtain the coordinates selected by the model.
        // Each nested pair contains {row, column}.
        final int[][] cells = game.getWinningCells();

        // Even when five pieces connect, the model stores four
        // positions representing one complete winning line.
        assertEquals(4, cells.length);

        // We will verify that the stored line includes the latest piece.
        boolean containsLastMove = false;

        // Read the final column letter and convert it into an index.
        // For example, 'D' - 'A' gives column index 3.
        final int lastColumn =
                moves.charAt(moves.length() - 1) - 'A';

        // Inspect every coordinate pair returned by the model.
        for (int index = 0; index < cells.length; index++) {

            // Index zero inside the pair stores its board row.
            final int row = cells[index][0];

            // Index one inside the pair stores its board column.
            final int column = cells[index][1];

            // The board position must contain the winner's piece.
            // A stored coordinate alone is not enough to prove a win.
            assertEquals(winner, game.getPieceAt(row, column));

            // Check whether this coordinate is the latest move.

            // Boolean |= keeps the flag true once a match is found.
            // It becomes true if either:
            // - an earlier coordinate already matched, or
            // - this coordinate matches both the latest row and column.
            containsLastMove |= row == lastRow
                    && column == lastColumn;

            // The first coordinate has no previous coordinate.
            // Starting with the second, compare neighboring pairs.
            if (index > 0) {

                // The difference between neighboring rows must match
                // the expected direction's row increment.
                assertEquals(rowStep, row - cells[index - 1][0]);

                // The difference between neighboring columns must match
                // the expected direction's column increment.
                assertEquals(colStep,
                        column - cells[index - 1][1]);
            }
        }

        // A newly completed winning line must include the latest piece.
        assertTrue(containsLastMove);
    }

    /**
     * Verifies that incomplete lines do not produce a victory.
     *
     * @param moves a sequence that must leave the game unfinished
     */
    @ParameterizedTest
    @ValueSource(strings = {"AGBGC", "AGBGDF", "ABABAB"})
    void doesNotReportIncompleteLines(final String moves) {
        // Each supplied String produces a separate execution.

        // AGBGC: X has only three consecutive horizontal pieces.
        // AGBGDF: X has horizontal pieces with a gap between them.
        // ABABAB: X has only three consecutive vertical pieces.
        final GameManager game = new GameManager();

        // Play every supplied move using the model's public method.
        play(game, moves);

        // None of the sequences completes four matching pieces.
        assertFalse(game.hasWinner());

        // These sequences also leave empty positions on the board.
        // Therefore, the game must remain active.
        assertFalse(game.isGameFinished());
    }

    /**
     * Verifies that a victory blocks further moves in every column.
     */
    @Test
    void rejectsMovesAfterVictory() {

        final GameManager game = new GameManager();

        // This sequence ends with a horizontal victory for X.
        play(game, "AGBGCFD");

        // Capture the finished game's observable state.
        final String before = snapshot(game);

        // Try another move in every valid column, from A through G.
        for (char column = 'A'; column <= 'G'; column++) {

            // A finished game must reject every attempted move.
            assertEquals(-1, game.dropPiece(column));

            // Each rejection must preserve the completed game.
            assertEquals(before, snapshot(game));
        }
    }

    /**
     * Verifies that a full board without a winner produces a draw.
     *
     * <p>The prepared sequence fills all 42 positions through legal
     * moves without completing a winning line.</p>
     */
    @Test
    void detectsADrawAndRejectsFurtherMoves() {

        final GameManager game = new GameManager();

        // Execute a prepared full-board sequence.
        // The play helper verifies that each move is accepted.
        play(game, "FFGGCEGFEFDCAECGEGCGABEADEFDCCDDAFDAABBBBB");

        // A full board must end the game.
        assertTrue(game.isGameFinished());

        // A draw must not be reported as a victory.
        assertFalse(game.hasWinner());

        // No winning coordinates should have been stored.
        assertEquals(0, game.getWinningCells().length);

        // Check every position to verify that the board really is full.
        for (int row = 0; row < GameManager.ROWS; row++) {
            for (int col = 0; col < GameManager.COLUMNS; col++) {

                // A full board cannot contain an empty space.
                assertNotEquals(' ', game.getPieceAt(row, col));
            }
        }

        // Capture the completed draw before attempting another move.
        final String before = snapshot(game);

        // A draw must block further moves just like a victory.
        assertEquals(-1, game.dropPiece('A'));

        // The rejected move must not change the finished game.
        assertEquals(before, snapshot(game));
    }

    /**
     * Verifies that reset restores the game after a victory.
     *
     * <p>A second complete game also checks that the accepted-move
     * counter was reset, even though that counter is private.</p>
     */
    @Test
    void resetsAllStateAndAllowsAnotherCompleteGame() {
        final GameManager game = new GameManager();

        // Finish the first game with a victory.
        play(game, "AGBGCFD");

        // Reuse the same model object to start over.
        game.reset();

        // All pieces from the first game must be removed.
        assertEmpty(game);

        // X must play first again.
        assertEquals('X', game.getTurn());

        // The previous victory must no longer exist.
        assertFalse(game.hasWinner());

        // The reset game must accept new moves.
        assertFalse(game.isGameFinished());

        // The previous winning coordinates must also be removed.
        assertEquals(0, game.getWinningCells().length);

        // Play a second game that fills the entire board.

        // If movesPlayed still included moves from the first game,
        // the model could declare the board full too early.
        // The helper would then detect a rejected move.
        play(game, "FFGGCEGFEFDCAECGEGCGABEADEFDCCDDAFDAABBBBB");

        // The second game must finish normally as a draw.
        assertTrue(game.isGameFinished());
        assertFalse(game.hasWinner());
    }

    /**
     * Verifies that callers cannot modify stored winning coordinates.
     *
     * <p>The returned array must contain independent coordinate pairs,
     * rather than references to the model's internal pairs.</p>
     */
    @Test
    void protectsWinningCoordinatesWithADeepCopy() {

        final GameManager game = new GameManager();

        // Create a victory so four winning coordinates are available.
        play(game, "AGBGCFD");

        // Capture the model's state before modifying the returned data.
        final String before = snapshot(game);

        // Receive the coordinate array returned to the caller.
        // final prevents reassigning copy, but its contents can change.
        final int[][] copy = game.getWinningCells();

        // Modify the row value inside the first returned pair.
        // A shallow outer-array copy would not protect against this.
        copy[0][0] = 99;

        // Replace the second returned pair with another array.
        // This also checks that the returned outer array is separate.
        copy[1] = new int[] {99, 99};

        // The model must still contain its original coordinates.
        // Changes to the caller's copy must not affect the game.
        assertEquals(before, snapshot(game));
    }

    /**
     * Plays a sequence of moves and verifies that all are accepted.
     *
     * <p>This is a helper method, not an independent JUnit test.
     * Other test methods call it to prepare their game situations.</p>
     *
     * @param game the model that receives the moves
     * @param moves the requested columns in execution order
     */
    private static void play(final GameManager game,
                             final String moves) {
        // Convert a String such as "ABC" into the characters
        // 'A', 'B', and 'C', then process them in that order.
        for (char column : moves.toCharArray()) {

            // Each character requests one move.
            // If any move is rejected, the prepared sequence fails.
            assertNotEquals(-1, game.dropPiece(column));
        }
    }

    /**
     * Verifies that every board position contains an empty space.
     *
     * <p>The helper can be reused after construction and after reset.
     * It inspects the board through getPieceAt instead of accessing
     * the private board field.</p>
     *
     * @param game the model whose board must be empty
     */
    private static void assertEmpty(final GameManager game) {
        // Visit row indices zero through five.
        for (int row = 0; row < GameManager.ROWS; row++) {

            // Within each row, visit column indices zero through six.
            for (int col = 0; col < GameManager.COLUMNS; col++) {

                // The first argument is the expected empty character.
                // The second is the actual content of this position.
                assertEquals(' ', game.getPieceAt(row, col));
            }
        }
    }

    /**
     * Builds a stable representation of the model's observable state.
     *
     * <p>The result includes all board pieces, the current turn,
     * the finished flag, and the winning coordinates.</p>
     *
     * <p>This helper lets tests compare the observable state before
     * and after an operation that should not change it.
     * It does not capture the private move counter directly.</p>
     *
     * @param game the model to inspect
     * @return a String representing its observable state
     */
    private static String snapshot(final GameManager game) {
        // StringBuilder collects several values into one text.
        // It avoids repeatedly creating intermediate Strings.
        final StringBuilder value = new StringBuilder();

        // Read the board in a fixed order:
        // row zero first, then row one, and so on.
        for (int row = 0; row < GameManager.ROWS; row++) {
            for (int col = 0; col < GameManager.COLUMNS; col++) {

                // Append this position's X, O, or empty space.
                value.append(game.getPieceAt(row, col));
            }
        }

        // Each append returns the same StringBuilder,
        // allowing the calls to be chained.
        //
        // Append the player, whether the game is finished,
        // and a readable representation of the nested coordinates.
        //
        // deepToString reads the contents of nested arrays.
        // For example: [[0, 0], [0, 1], [0, 2], [0, 3]].
        //
        // Finally, convert the accumulated content into a String.
        return value.append(game.getTurn())
                .append(game.isGameFinished())
                .append(Arrays.deepToString(game.getWinningCells()))
                .toString();
    }
}
