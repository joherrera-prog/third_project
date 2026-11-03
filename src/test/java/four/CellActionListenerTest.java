// Find out if the controller communicates the actions to the model and view
// We do use mocks

package four;

import java.awt.event.ActionEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the controller using a real model and a simulated view.
 *
 * <p>The real GameManager applies gravity, manages turns, and detects
 * game results. The mocked GameView records the visual operations
 * requested by the controller without constructing a board panel.</p>
 *
 * <p>JUnit checks values and exceptions. Mockito checks interactions
 * between the controller and its simulated view.</p>
 *
 * <p>Each test creates its own objects and does not depend on another
 * test having executed first.</p>
 */
final class CellActionListenerTest {

    /**
     * Verifies that accepted moves request the correct visual updates.
     *
     * <p>The displayed symbol must identify the piece just placed,
     * even when the model has already changed the current turn.</p>
     */
    @Test
    void displaysAcceptedPiecesAtTheirGravityPositions() {
        // Create a real model with an empty board and X playing first.
        final GameManager game = new GameManager();

        // Ask Mockito to create an object implementing GameView.
        // This object records calls instead of displaying a real board.
        final GameView view = mock(GameView.class);

        // Create the real controller and inject both dependencies.
        // The controller uses the real model and the simulated view.
        final CellActionListener controller =
                new CellActionListener(game, view);

        // Send two cell actions for column A to the controller.
        // The helper constructs the events and calls actionPerformed.
        click(controller, 'A');
        click(controller, 'A');

        // The first move must request X at the bottom of column A.
        // verify checks a previous call; it does not redraw the board.
        // By default, this exact call must have occurred once.
        verify(view).updateButtonText('A', 0, 'X');

        // The second move must request O directly above the first piece.
        verify(view).updateButtonText('A', 1, 'O');

        // These verifications check arguments and call counts.
        // They do not explicitly check the order of the two calls.

        // No additional visual operations should have occurred.
        // An unexpected reset, highlight, or update would fail this check.
        verifyNoMoreInteractions(view);
    }

    /**
     * Verifies that a rejected full-column move does not update the view.
     *
     * <p>The same player must retain the turn after the rejection.</p>
     */
    @Test
    void ignoresAFullColumn() {
        final GameManager game = new GameManager();

        // Fill column A directly through the real model.
        // Six alternating pieces fill the column without a victory.
        for (int index = 0; index < GameManager.ROWS; index++) {
            game.dropPiece('A');
        }

        // After six accepted moves, it is X's turn again.
        // Create a mock that has not received any visual calls.
        final GameView view = mock(GameView.class);

        // Construct the controller and immediately pass it to the helper.
        // This is equivalent to storing it in a local variable first.
        // The requested seventh piece cannot fit in column A.
        click(new CellActionListener(game, view), 'A');

        // No method on the view should have been called at all.
        // In particular, the controller must not display another piece.
        verifyNoInteractions(view);

        // The rejected action must preserve X's turn.
        assertEquals('X', game.getTurn());
    }

    /**
     * Verifies highlighting after a winning move.
     *
     * <p>A subsequent action must not request another visual update
     * because the real model rejects moves after victory.</p>
     */
    @Test
    void highlightsAWinnerAndIgnoresLaterMoves() {
        final GameManager game = new GameManager();

        // Prepare a position one move away from a horizontal X victory.

        // X plays A, B, and C.
        // O plays G, G, and F.
        // A final X in D will complete A1, B1, C1, and D1.
        for (char column : "AGBGCF".toCharArray()) {
            game.dropPiece(column);
        }

        // Preparing the model directly does not call this mock view.
        final GameView view = mock(GameView.class);

        // Inject the prepared model and the fresh mock.
        final CellActionListener controller =
                new CellActionListener(game, view);

        // This accepted move completes X's horizontal winning line.
        click(controller, 'D');

        // The game is now finished.
        // This second action must be rejected without visual changes.
        click(controller, 'A');

        // The winning piece must be displayed at D1.
        verify(view).updateButtonText('D', 0, 'X');

        // Verify that the controller requested winning-cell highlighting.

        // any(int[][].class) is a Mockito argument matcher.
        // It accepts any non-null two-dimensional int array here.
        // It is not itself an array of winning coordinates.

        // This check verifies that the highlighting request occurred.
        // GameManagerTest checks the actual coordinate contents.
        verify(view).highlightWinningCells(any(int[][].class));

        // Only the update and highlighting verified above may occur.
        // If the later click caused another visual call, this would fail.
        verifyNoMoreInteractions(view);
    }

    /**
     * Verifies that reset updates both the model and the view.
     *
     * <p>The reset command is handled before checking whether the event
     * source is a board cell.</p>
     */
    @Test
    void resetsTheModelAndView() {
        final GameManager game = new GameManager();

        // Place X at A1 so there is something to reset.
        // The accepted move also changes the next turn to O.
        game.dropPiece('A');

        // Create a simulated view and the real controller.
        final GameView view = mock(GameView.class);
        final CellActionListener controller =
                new CellActionListener(game, view);

        // Construct an event carrying the reset action command.

        // new Object() supplies an event source.
        // It does not need to be a CellButton because reset is
        // recognized through its action command before the cell check.

        // ACTION_PERFORMED identifies the action-event type.
        // RESET_COMMAND supplies the same command used by Reset.
        controller.actionPerformed(new ActionEvent(new Object(),
                ActionEvent.ACTION_PERFORMED, GameView.RESET_COMMAND));

        // The real model must remove the previously placed X.
        assertEquals(' ', game.getPieceAt(0, 0));

        // Reset must restore X as the first player.
        assertEquals('X', game.getTurn());

        // The reset model must allow a new game to continue.
        assertFalse(game.isGameFinished());

        // The controller must also request the visual reset.
        // Resetting the model alone would not clear real buttons.
        verify(view).resetBoard();

        // Reset must not also request a piece update or highlighting.
        verifyNoMoreInteractions(view);
    }

    /**
     * Verifies that unrelated event sources do not trigger gameplay.
     */
    @Test
    void ignoresUnrelatedComponents() {
        final GameManager game = new GameManager();
        final GameView view = mock(GameView.class);

        // Create the controller with its normal dependencies.
        final CellActionListener controller =
                new CellActionListener(game, view);

        // The source is a plain Object, not a CellButton.
        // The command is also unrelated to reset.

        // The controller should ignore this event after checking
        // that its source is not a board-cell button.
        controller.actionPerformed(new ActionEvent(new Object(),
                ActionEvent.ACTION_PERFORMED, "OTHER"));

        // Ignoring the event must not request visual changes.
        verifyNoInteractions(view);

        // No piece should have consumed X's initial turn.
        assertEquals('X', game.getTurn());
    }

    /**
     * Verifies that the controller rejects missing dependencies.
     *
     * <p>Both the model and the view are required. The constructor
     * must fail immediately if either reference is null.</p>
     */
    @Test
    void rejectsNullDependencies() {
        // Use a valid simulated view for the first constructor attempt.
        final GameView view = mock(GameView.class);

        // assertThrows receives:
        // 1. The expected exception type.
        // 2. A task that JUnit will execute.

        // The lambda delays construction until assertThrows runs it.
        // The missing model must cause a NullPointerException.
        assertThrows(NullPointerException.class,
                () -> new CellActionListener(null, view));

        // Here the model is valid, but the view is missing.
        // The constructor must reject this situation as well.
        assertThrows(NullPointerException.class,
                () -> new CellActionListener(new GameManager(), null));

        // These checks verify the exception type.
        // They do not check the exception's message.
    }

    /**
     * Delivers a simulated board-cell action to the controller.
     *
     * <p>This is a helper method rather than an independent test.
     * It creates a real cell button and an event, then calls the
     * controller directly without opening a window.</p>
     *
     * @param controller the controller receiving the action
     * @param column the visual column requested by the test
     */
    private static void click(final CellActionListener controller,
                              final char column) {

        // Create a button in the requested column's top visual row.
        // ROWS equals six, so column A creates a button named ButtonA6.

        // The clicked row does not determine the landing row.
        // The real model calculates that position using gravity.
        final CellButton button =
                new CellButton(column, GameManager.ROWS);

        // Create an event with:
        // - the button as its source;
        // - the standard action-event identifier;
        // - CELL as its action command.

        // Deliver it directly to the controller's event handler.
        // This simulates receiving an action; it does not physically
        // click a displayed button or test listener registration.
        controller.actionPerformed(new ActionEvent(button,
                ActionEvent.ACTION_PERFORMED, "CELL"));
    }
}
