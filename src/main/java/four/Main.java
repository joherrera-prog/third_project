
package four;

import javax.swing.SwingUtilities;

/**
 * Acts as the application entry point and composition root.
 */
public final class Main {

    /** Prevents instantiation of this utility class. */
    private Main() {
    }

    /**
     * Requests window creation on the Swing event-dispatch thread.
     *
     * @param args command-line arguments, which are not used
     */
    public static void main(final String[] args) {
        // A constructor reference is the task submitted to Swing.
        SwingUtilities.invokeLater(ConnectFour::new);
    }

    /**
     * Wires an existing window without recursively creating another one.
     *
     * @param window the window currently being initialized
     */
    static void initializeGame(final ConnectFour window) {
        final GameManager model = new GameManager();
        final BoardPanel view = new BoardPanel();

        // The controller depends on GameView, not on JFrame.
        // Dependency injection
        final CellActionListener controller =
                new CellActionListener(model, view);

        // Register the controller to receive actions from the panel's buttons.
        view.addActionListener(controller);
        window.setContentPane(view);
    }
}
