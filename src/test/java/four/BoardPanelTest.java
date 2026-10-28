// Make sure the view with real Swing components

package four;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests real Swing components without constructing a JFrame.
 *
 * <p>All component operations run on Swing's event-dispatch thread,
 * also called the EDT. invokeAndWait makes the JUnit thread wait
 * until each submitted task finishes.</p>
 *
 * <p>These tests do not use Mockito. Some inspect the panel directly,
 * while others connect the real model, controller, and panel to check
 * their collaboration through actual button actions.</p>
 */
final class BoardPanelTest {

    /**
     * Verifies the buttons and their initial configuration.
     *
     * <p>The panel must contain 42 named cells and one reset button.
     * All board cells must initially be empty and share a background.</p>
     *
     * @throws Exception if the EDT task cannot complete normally
     */
    @Test
    void createsAllNamedButtonsWithTheSameInitialColor() throws Exception {
        // Run component creation and inspection on Swing's EDT.
        // The JUnit thread waits until this entire lambda finishes.
        SwingUtilities.invokeAndWait(() -> {

            // Construct the real panel and its buttons.
            final BoardPanel panel = new BoardPanel();

            // Our helper searches the panel's nested containers.
            // The returned list includes cell buttons and Reset.
            final List<JButton> buttons = buttons(panel);

            // The board has 6 * 7 cells plus one reset button.
            assertEquals(43, buttons.size());

            // Use A1's background as the initial reference color.
            // The test checks consistency instead of a specific RGB.
            final Color baseline =
                    button(panel, "ButtonA1").getBackground();

            // These are visual row numbers: 1 through 6.
            // They differ from the model's row indices: 0 through 5.
            for (int row = 1; row <= GameManager.ROWS; row++) {

                // These are visual column letters: A through G.
                for (char col = 'A'; col <= 'G'; col++) {

                    // For col C and row 2, the name becomes ButtonC2.
                    // The helper returns the existing named button.
                    final JButton current =
                            button(panel, "Button" + col + row);

                    // Every board position must use our CellButton type.
                    assertTrue(current instanceof CellButton);

                    // The cast permits access to CellButton's getters.
                    // The stored coordinates must match the name.
                    assertEquals(col,
                            ((CellButton) current).getColumn());
                    assertEquals(row,
                            ((CellButton) current).getRow());

                    // An empty cell displays one space.
                    assertEquals(" ", current.getText());

                    // All cells must initially share the same background.
                    assertEquals(baseline, current.getBackground());

                    // The initial text color must be white.
                    assertEquals(Color.WHITE, current.getForeground());

                    // Players must be able to activate the cells.
                    assertTrue(current.isEnabled());
                }
            }

            // Find Reset using its component name.
            final JButton reset = button(panel, "ButtonReset");

            // Reset must be available when the panel is created.
            assertTrue(reset.isEnabled());

            // The emitted command must match the controller's contract.
            // The component name and action command are different values.
            assertEquals(GameView.RESET_COMMAND,
                    reset.getActionCommand());
        });
    }

    /**
     * Verifies highlighting and restoration of prepared winning cells.
     *
     * <p>Four cells receive X pieces and are highlighted. Reset must
     * then restore every cell's empty text and initial colors.</p>
     *
     * @throws Exception if the EDT task cannot complete normally
     */
    @Test
    void highlightsExactlyFourCellsAndRestoresTheirColors()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            final BoardPanel panel = new BoardPanel();

            // Remember the background used before any pieces are shown.
            final Color baseline =
                    button(panel, "ButtonA1").getBackground();

            // These are model coordinates, expressed as {row, column}.
            // They identify A1, B1, C1, and D1.
            final int[][] winning = {
                    {0, 0},
                    {0, 1},
                    {0, 2},
                    {0, 3}
            };

            // Display X in the four prepared positions.
            // This tests the view directly; no model is involved here.
            for (char col = 'A'; col <= 'D'; col++) {
                panel.updateButtonText(col, 0, 'X');
            }

            // Pass the coordinate array to the highlighting method.
            // The view trusts these coordinates; it does not detect wins.
            final Color pieceColor = button(panel, "ButtonA1").getBackground();

            panel.highlightWinningCells(winning);

            // Winning backgrounds must differ from both empty and X cells.
            final Color winningColor = button(panel, "ButtonA1").getBackground();
            assertNotEquals(baseline, winningColor);
            assertNotEquals(pieceColor, winningColor);

            // Count board cells whose backgrounds differ from baseline.
            int changed = 0;

            // The helper also returns Reset, so filter out that button.
            for (JButton current : buttons(panel)) {
                if (current instanceof CellButton
                        && !baseline.equals(current.getBackground())) {

                    // Count this cell with a non-baseline background.
                    changed++;
                    assertEquals(winningColor, current.getBackground());

                    // The prepared pieces must remain visible.
                    assertEquals("X", current.getText());

                    // Highlighting must apply the winning text color.
                    assertEquals(Color.BLACK, current.getForeground());
                }
            }

            // Only the four prepared cells should differ from baseline.
            // Piece colors may already differ before highlighting.
            assertEquals(4, changed);

            // Restore the panel after the prepared winning situation.
            panel.resetBoard();

            // Inspect every board cell, excluding the reset button.
            for (JButton current : buttons(panel)) {
                if (current instanceof CellButton) {

                    // Reset must remove every displayed piece.
                    assertEquals(" ", current.getText());

                    // Restore the original background and text color.
                    assertEquals(baseline, current.getBackground());
                    assertEquals(Color.WHITE, current.getForeground());
                }
            }
        });
    }

    /**
     * Verifies that both players receive distinct painted backgrounds.
     *
     * @throws Exception if the EDT task cannot complete normally
     */
    @Test
    void displaysDistinctColorsForBothPlayers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final BoardPanel panel = new BoardPanel();
            final JButton xCell = button(panel, "ButtonA1");
            final JButton oCell = button(panel, "ButtonB1");
            final Color baseline = xCell.getBackground();

            // Exercise each player's color selection independently.
            panel.updateButtonText('A', 0, 'X');
            panel.updateButtonText('B', 0, 'O');

            assertEquals("X", xCell.getText());
            assertEquals("O", oCell.getText());
            assertNotEquals(baseline, xCell.getBackground());
            assertNotEquals(baseline, oCell.getBackground());
            assertNotEquals(xCell.getBackground(), oCell.getBackground());
            assertEquals(Color.WHITE, xCell.getForeground());
            assertEquals(Color.WHITE, oCell.getForeground());

            // Verify that Swing paints the selected colors as well.
            xCell.setSize(100, 80);
            oCell.setSize(100, 80);
            assertPaintedColor(xCell, xCell.getBackground());
            assertPaintedColor(oCell, oCell.getBackground());
        });
    }

    /**
     * Verifies gameplay through registered button listeners.
     *
     * <p>This test connects the real model, controller, and panel.
     * Programmatic button clicks exercise gravity, victory detection,
     * blocked moves, and reset behavior together.</p>
     *
     * @throws Exception if the EDT task cannot complete normally
     */
    @Test
    void playsAndResetsACompleteGameThroughRealButtons()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            // Create the real game state and real visual components.
            final GameManager model = new GameManager();
            final BoardPanel panel = new BoardPanel();

            // Create the controller with these same objects.
            // Register it on the panel's cells and reset button.
            panel.addActionListener(
                    new CellActionListener(model, panel));

            // Activate top-row buttons in the prepared order.
            // Gravity must place pieces in lower available positions.
            for (char col : "AGBGCFD".toCharArray()) {

                // For column A, this finds ButtonA6.
                //
                // doClick triggers the button's registered listeners.
                // Zero removes the simulated press delay.
                button(panel, "Button" + col + "6").doClick(0);
            }

            // The sequence must produce a horizontal victory for X.
            assertTrue(model.hasWinner());

            // The final piece must appear at D1, not the clicked D6.
            assertEquals("X", button(panel, "ButtonD1").getText());

            // Try another move after the game has finished.
            button(panel, "ButtonA6").doClick(0);

            // A1 is occupied, so a wrongly accepted move would fill A2.
            // A2 must remain empty because the game is finished.
            assertEquals(" ", button(panel, "ButtonA2").getText());

            // Trigger reset through its real registered listener.
            button(panel, "ButtonReset").doClick(0);

            // Reset must restore the model to an active state.
            assertFalse(model.isGameFinished());

            // Verify that a new move is possible after resetting.
            button(panel, "ButtonG6").doClick(0);

            // X must start again, landing at the bottom of column G.
            assertEquals("X", button(panel, "ButtonG1").getText());
        });
    }

    /**
     * Verifies that a full-column action does not consume a turn.
     *
     * @throws Exception if the EDT task cannot complete normally
     */
    @Test
    void preservesTheTurnWhenAFullColumnIsClicked() throws Exception {
        SwingUtilities.invokeAndWait(() -> {

            final GameManager model = new GameManager();
            final BoardPanel panel = new BoardPanel();

            // Connect the real controller to all panel buttons.
            panel.addActionListener(
                    new CellActionListener(model, panel));

            // ROWS is six, and <= produces seven iterations.
            // Six clicks fill column A; the seventh must be rejected.
            for (int index = 0; index <= GameManager.ROWS; index++) {
                button(panel, "ButtonA6").doClick(0);
            }

            // After six accepted moves, it is X's turn.
            // The rejected seventh attempt must preserve that turn.
            button(panel, "ButtonB6").doClick(0);

            // X must appear in the new column.
            // An incorrect turn change on rejection would display O.
            assertEquals("X", button(panel, "ButtonB1").getText());
        });
    }

    /**
     * Verifies that a button paints its configured background.
     *
     * <p>The button is rendered into an image in memory, allowing the
     * test to inspect a background pixel without displaying a window.</p>
     *
     * @throws Exception if the EDT task cannot complete normally
     */
    @Test
    void paintsTheConfiguredBackgroundInHeadlessMode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {

            final BoardPanel panel = new BoardPanel();

            // Find one real board cell.
            final JButton cell = button(panel, "ButtonA1");

            // Remember its initial background color.
            final Color base = cell.getBackground();

            // Give the button dimensions for off-screen painting.
            // No visible window is providing its layout here.
            cell.setSize(100, 80);

            // Paint the button into an image and inspect a pixel.
            assertPaintedColor(cell, base);

            // Highlight only A1 to test the view's painting behavior.
            //
            // The view accepts coordinates; it does not enforce that
            // four positions were supplied or verify a game victory.
            panel.highlightWinningCells(new int[][] {{0, 0}});

            // The winning background must differ from the initial one.
            assertNotEquals(base, cell.getBackground());

            // The newly configured color must also be painted.
            // Checking getBackground alone would not prove that.
            assertPaintedColor(cell, cell.getBackground());
        });
    }

    /**
     * Verifies that the panel rejects a missing action listener.
     *
     * @throws Exception if the EDT task cannot complete normally
     */
    @Test
    void rejectsANullListener() throws Exception {
        SwingUtilities.invokeAndWait(() -> {

            final BoardPanel panel = new BoardPanel();

            // JUnit executes the lambda and expects the stated exception.
            // The panel must not silently accept a null listener.
            assertThrows(NullPointerException.class,
                    () -> panel.addActionListener(null));
        });
    }

    /**
     * Paints a button into an image and checks one background pixel.
     *
     * <p>The inspected pixel is away from the centered text and border.
     * This checks a background sample rather than the entire image.</p>
     *
     * @param cell the real button to paint
     * @param expected the expected background color
     */
    private static void assertPaintedColor(final JButton cell,
                                           final Color expected) {

        // Create a 100-by-80 image stored entirely in memory.
        // TYPE_INT_RGB stores opaque red, green, and blue pixel colors.
        final BufferedImage image = new BufferedImage(100, 80,
                BufferedImage.TYPE_INT_RGB);

        // Obtain a drawing context that writes into this image.
        final Graphics2D graphics = image.createGraphics();

        try {
            // Ask the real Swing button to paint into that context.
            cell.paint(graphics);
        } finally {
            // Always release the drawing context's resources.
            // finally also runs if painting throws an exception.
            graphics.dispose();
        }

        // Convert the expected Color to its integer representation.
        // Compare it with the image pixel at x = 10 and y = 10.
        assertEquals(expected.getRGB(), image.getRGB(10, 10));
    }

    /**
     * Finds a button by its internal component name.
     *
     * <p>The name is the value assigned through setName, such as
     * ButtonA1 or ButtonReset. It is not the displayed button text.</p>
     *
     * @param parent the container from which the search begins
     * @param name the required component name
     * @return the first button with the requested name
     */
    private static JButton button(final Container parent,
                                  final String name) {

        // Obtain all buttons found under this container.
        return buttons(parent).stream()

                // Keep only buttons whose component names match.
                // candidate represents one button from the stream.
                .filter(candidate -> name.equals(candidate.getName()))

                // Select the first matching button.
                .findFirst()

                // Return it, or throw if the required button is missing.
                // A missing required component therefore fails the test.
                .orElseThrow();
    }

    /**
     * Collects buttons from a container and its nested containers.
     *
     * <p>The method uses recursion: when it finds another container,
     * it calls itself to search inside that container.</p>
     *
     * @param parent the container to inspect
     * @return the buttons found in its component hierarchy
     */
    private static List<JButton> buttons(final Container parent) {

        // Create an initially empty collection of discovered buttons.
        // CellButton instances also qualify because they extend JButton.
        final List<JButton> result = new ArrayList<>();

        // getComponents returns this container's direct children.
        // It does not automatically return every nested descendant.
        for (Component component : parent.getComponents()) {

            // If this child is a button, add the existing object.
            if (component instanceof JButton) {
                result.add((JButton) component);

                // Otherwise, check whether the child can contain components.
            } else if (component instanceof Container) {

                // Search that nested container using the same method.
                // addAll adds each discovered button to our result list.
                result.addAll(buttons((Container) component));
            }
        }

        // Return the buttons collected from this part of the hierarchy.
        return result;
    }
}
