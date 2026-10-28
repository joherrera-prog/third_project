// This class is used to be a button and conserve its own identity

package four;

import javax.swing.JButton;

/**
 * Represents a board button that knows its visual coordinates.
 */
public final class CellButton extends JButton {

    /** Explicit serialization version for this button class. */
    private static final long serialVersionUID = 1L;

    /** The visual column, from A to G. */
    private final char column;

    /** The visual row, from one to six. */
    private final int row;

    /**
     * Creates an empty cell with its Hyperskill component name.
     *
     * @param newColumn the visual column
     * @param newRow the visual row
     */
    public CellButton(final char newColumn, final int newRow) {
        super(" "); // Keep the initial label empty.

        column = newColumn; // Store the primitive column value.
        row = newRow; // Store the primitive row value.

        // The internal name is independent of the displayed piece.
        setName("Button" + column + row);

        setFocusPainted(false); // Hide the focus indicator.
        setOpaque(true); // Allow background painting.
        setContentAreaFilled(true); // Fill the content area.
        setBorderPainted(false); // Panel gaps separate the cells.
    }

    /**
     * Returns the visual column.
     *
     * @return the column letter
     */
    public char getColumn() {
        return column;
    }

    /**
     * Returns the visual row, rather than a matrix index.
     *
     * @return the row number
     */
    public int getRow() {
        return row;
    }
}
