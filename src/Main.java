import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main.java
 * Entry point of the Contact Management System.
 * 1. Sets up the database (creates file + table if needed).
 * 2. Opens the Swing window on the Event Dispatch Thread.
 */
public class Main {

    public static void main(String[] args) {
        // Use the operating system's look and feel so the app looks native
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // If this fails, Swing simply uses its default look and feel
        }

        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseConnection.initializeDatabase();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to the database.\n"
                        + "Make sure the SQLite JDBC driver (sqlite-jdbc.jar) is added to the project.\n\n"
                        + "Details: " + e.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                return; // Stop here - the app cannot work without the database
            }
            new ContactManagementGUI().setVisible(true);
        });
    }
}
