// This class builds a visual board and changes its buttons when the controller
// asks for that

package four;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Displays the board without depending on a top-level Swing window.
 *
 * <p>A JPanel can be constructed, clicked, and painted in headless tests.
 * The panel never decides whether a move is legal or winning.</p>
 */
public final class BoardPanel extends JPanel implements GameView {
// This class contains another panel called grid where the 42 boxes are, and the
// reset button, but the window that contains everything is in ConnectFour.java
    /** Explicit serialization version for this panel class. */
    private static final long serialVersionUID = 1L;

    /** The background shared by all non-winning empty cells. */
    private static final Color BASE_COLOR = new Color(53, 22, 97);

    /** The background shared by the four winning cells. */
    private static final Color WIN_COLOR = new Color(186, 145, 75);

    /** The background visible between adjacent cells. */
    private static final Color GAP_COLOR = new Color(19, 0, 43, 255);

    /** Muted red background for X pieces. */
    private static final Color X_COLOR = new Color(145, 65, 75);

    /** Muted blue background for O pieces. */
    private static final Color O_COLOR = new Color(60, 100, 145);

    /** The piece represented by the muted red background. */
    private static final char X_PIECE = 'X';

    /** The gap between adjacent cells in pixels. */
    private static final int GAP_SIZE = 4;

    /** The piece font size in points. */
    private static final int FONT_SIZE = 36;

    /** The font shared by every cell. */
    private static final Font PIECE_FONT =
            new Font("SansSerif", Font.BOLD, FONT_SIZE);

    /** References to the existing buttons, indexed from the bottom row. */
    private final CellButton[][] buttonGrid =
            new CellButton[GameManager.ROWS][GameManager.COLUMNS];

    /** The reset button remains enabled after a finished game. */
    private final JButton resetButton = new JButton("Reset");

    /** Builds every visual component without creating a model. */
    public BoardPanel() {
        super(new BorderLayout());

        // Interior panel, also create another panel called Grid
        // Grid panel is just the grid panel, doesnt include the reset button,
        // Board Panel does
        final JPanel grid = new JPanel(new GridLayout(GameManager.ROWS,
                GameManager.COLUMNS, GAP_SIZE, GAP_SIZE));

        grid.setBackground(GAP_COLOR);


        // GridLayout displays the first added row at the top.
        // Add visual row six first, while storing it at matrix index five.
        for (int row = GameManager.ROWS; row >= 1; row--) {
            for (int colIndex = 0; colIndex < GameManager.COLUMNS; colIndex++) {
                final char column = (char) ('A' + colIndex);
                final CellButton button = new CellButton(column, row);

                button.setUI(new BasicButtonUI()); // Respect custom colors.
                button.setBackground(BASE_COLOR);
                button.setForeground(Color.WHITE); // Text color
                button.setFont(PIECE_FONT);

                buttonGrid[row - 1][colIndex] = button;
                grid.add(button);
            }
        }

        // Reset Button config
        resetButton.setName("ButtonReset"); // Hyperskill component name.
        resetButton.setActionCommand(RESET_COMMAND); // Controller command.
        resetButton.setEnabled(true);

        // Its just like "this.add", refers to the object BoardPanel that we are
        // building
        add(grid, BorderLayout.CENTER);
        add(resetButton, BorderLayout.SOUTH);
    }

    /**
     * Registers the same controller on all board cells and the reset button.
     *
     * @param listener the controller that processes UI actions
     */
    // This is the controller
    public void addActionListener(final ActionListener listener) {
        Objects.requireNonNull(listener, "The controller must not be null");
        // Register the listener in every button
        for (CellButton[] row : buttonGrid) {
            for (CellButton button : row) {
                button.addActionListener(listener);
            }
        }

        resetButton.addActionListener(listener);
    }

    /**
     * Displays the piece at the position calculated by the model.
     *
     * @param column the visual column letter
     * @param rowIndex the zero-based row index
     * @param piece the accepted piece
     */
    @Override
    public void updateButtonText(final char column, final int rowIndex,
                                 final char piece) {
        // Convert the column letter into an array index.
        int columnIndex = column - 'A';

        // Retrieve the button where the piece actually landed.
        CellButton button = buttonGrid[rowIndex][columnIndex];

        // Display the symbol of the player who made this move.
        button.setText(String.valueOf(piece));

        // Choose the background according to the placed piece.
        if (piece == X_PIECE) {
            // Use a soft, dark red for X.
            button.setBackground(X_COLOR);
        } else {
            // Use a soft, dark blue for O.
            button.setBackground(O_COLOR);
        }

        // Keep the symbol readable on the colored background.
        button.setForeground(Color.WHITE);
    }

    /**
     * Applies the winning background and a contrasting text color.
     *
     * @param cells pairs of row and column indices supplied by the model
     */
    @Override
    public void highlightWinningCells(final int[]... cells) {
        // cells will have the winning pairs, the loop takes a pair in each
        // iteration
        for (int[] cell : cells) {
            final CellButton button = buttonGrid[cell[0]][cell[1]];
            button.setBackground(WIN_COLOR);
            button.setForeground(Color.BLACK);
        }
    }

    /** Clears every displayed piece and restores the original colors. */
    @Override
    public void resetBoard() {
        for (CellButton[] row : buttonGrid) {
            for (CellButton button : row) {
                button.setText(" ");
                button.setBackground(BASE_COLOR);
                button.setForeground(Color.WHITE);
            }
        }
    }
}
