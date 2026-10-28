// This interface has the operations that the controller needs to be able to get
// from the view

package four;

/**
 * Defines the visual operations required by the controller.
 *
 * <p>The interface allows controller tests to use a Mockito mock instead
 * of constructing a Swing window.</p>
 */
public interface GameView {

    /** The action command emitted by the reset button. */
    String RESET_COMMAND = "RESET";
    // Just setting a reset command under that name, easier with a reference
    /**
     * Displays a piece at the position accepted by the model.
     *
     * @param column the visual column, from A to G
     * @param rowIndex the zero-based row, with zero at the bottom
     * @param piece the piece to display
     */
    void updateButtonText(char column, int rowIndex, char piece);
    // This things will be defined (implemented) in BoardPanel.java
    /**
     * Highlights the winning positions.
     *
     * <p>The varargs parameter is represented as an int[][] array.
     * Each contained int[] stores one row-column pair.</p>
     *
     * @param cells pairs containing a row index and a column index
     */
    void highlightWinningCells(int[]... cells);

    /** Restores the empty board and its original colors. */
    void resetBoard();
}
