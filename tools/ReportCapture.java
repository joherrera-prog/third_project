import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/** Renders an existing HTML report into a PNG without opening a window. */
public final class ReportCapture {

    private ReportCapture() {
    }

    /**
     * Uses Swing's HTML renderer, rather than pretending to be a browser.
     * Browser-specific CSS and JavaScript are not supported by this preview.
     *
     * @param args the source HTML path and destination PNG path
     * @throws Exception if reading, rendering, or writing fails
     */
    public static void main(final String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected HTML and PNG paths");
        }
        final Path source = Path.of(args[0]).toAbsolutePath();
        final Path destination = Path.of(args[1]).toAbsolutePath();
        Files.createDirectories(destination.getParent());

        SwingUtilities.invokeAndWait(() -> {
            try {
                final JEditorPane report = new JEditorPane();
                final HTMLEditorKit kit = new HTMLEditorKit();
                final HTMLDocument document =
                        (HTMLDocument) kit.createDefaultDocument();

                // Load the document synchronously before measuring its size.
                document.setAsynchronousLoadPriority(-1);
                document.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
                document.setBase(source.toUri().toURL());
                report.setEditorKit(kit);
                report.setDocument(document);
                report.setEditable(false);
                report.setBackground(Color.WHITE);
                report.setMargin(new java.awt.Insets(24, 24, 24, 24));
                try (var reader = Files.newBufferedReader(source)) {
                    kit.read(reader, document, 0);
                }

                // The original report content is rendered without editing it.
                final int width = 1280;
                report.setSize(new Dimension(width, 10000));
                final int height = Math.max(320,
                        report.getPreferredSize().height + 24);
                report.setSize(width, height);

                final BufferedImage image = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_RGB);
                final Graphics2D graphics = image.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, width, height);
                    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    report.printAll(graphics);
                } finally {
                    graphics.dispose();
                }
                ImageIO.write(image, "png", destination.toFile());
                System.out.println(destination.getFileName() + ": "
                        + width + " x " + height);
            } catch (Exception exception) {
                throw new IllegalStateException("Report rendering failed", exception);
            }
        });
    }
}
