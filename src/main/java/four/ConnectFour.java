// Window that contains tha game

package four;

import javax.swing.JFrame;

/**
 * Provides the top-level window expected by Hyperskill Swing tests.
 *
 * <p>Constructing this class prepares the complete game, even when a test
 * creates the window directly instead of invoking Main.main().</p>
 */
public final class ConnectFour extends JFrame {

    /** Explicit serialization version for this window class. */
    private static final long serialVersionUID = 1L;

    /** Initial window width in pixels. */
    private static final int WINDOW_WIDTH = 700;

    /** Initial window height in pixels. */
    private static final int WINDOW_HEIGHT = 600;

    /** Creates the window and requests application wiring. */
    public ConnectFour() {
        // Setting title from superclass
        super("Connect Four");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        // Pass this existing window to the composition root.
        // initializeGame does not construct another ConnectFour.
        Main.initializeGame(this);

        setLocationRelativeTo(null);

        // The named buttons already exist when the window becomes visible.
        setVisible(true);
    }
}
