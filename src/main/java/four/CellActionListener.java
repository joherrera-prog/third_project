// The controller, its job will be to receive an action and ask the model to
// process it and tell the view what to show

package four;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

/**
 * Coordinates UI actions with the model through an injected view contract.
 */
public final class CellActionListener implements ActionListener {

    /** The model used by this controller. */
    // Dependency
    private final GameManager gameManager;

    /** The visual contract, implemented by Swing or mocked in tests. */
    // Dependency
    private final GameView boardView;

    /**
     * Receives the dependencies instead of constructing them internally.
     *
     * @param model the game model
     * @param view the visual implementation
     */
    // Dependency injectiom
    public CellActionListener(final GameManager model, final GameView view) {
        gameManager = Objects.requireNonNull(model, "model");
        boardView = Objects.requireNonNull(view, "view");
    }

    /**
     * Processes reset actions and board-cell clicks.
     *
     * @param event the action emitted by a registered component
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        if (GameView.RESET_COMMAND.equals(event.getActionCommand())) {
            gameManager.reset(); // Reset the actual game data.
            boardView.resetBoard(); // Reset the displayed board.
            return; // Reset is not a board-cell move.
        }

        if (!(event.getSource() instanceof CellButton)) {
            return; // Ignore unrelated components safely.
        }

        // This cast refers to the existing button; it creates no button.
        final CellButton clickedButton = (CellButton) event.getSource();
        final char column = clickedButton.getColumn();

        final int dropRowIndex = gameManager.dropPiece(column);

        if (dropRowIndex == -1) {
            return; // Rejected moves must not change the view.
        }

        final int colIndex = column - 'A';

        // Read the stored piece because the model may have changed turns.
        final char piece = gameManager.getPieceAt(dropRowIndex, colIndex);

        // Update the visible button
        boardView.updateButtonText(column, dropRowIndex, piece);

        // We highlight the winning tokens
        if (gameManager.hasWinner()) {
            boardView.highlightWinningCells(gameManager.getWinningCells());
        }
    }
}
