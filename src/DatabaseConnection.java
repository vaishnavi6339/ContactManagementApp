import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConnection.java
 * Handles the connection to the SQLite database and creates the contacts table.
 */
public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:contacts.db";

    private static final String CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS contacts ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL, "
            + "email TEXT NOT NULL UNIQUE, "
            + "mobile TEXT NOT NULL UNIQUE)";

    /** Opens and returns a connection to the SQLite database. */
    public static Connection getConnection() throws SQLException {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found. Add sqlite-jdbc.jar to the project.", e);
        }
        return DriverManager.getConnection(DB_URL);
    }

    /** Creates the contacts table if it does not exist. */
    public static void initializeDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        }
    }
}