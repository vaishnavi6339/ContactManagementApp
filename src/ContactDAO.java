import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * ContactDAO.java  (DAO = Data Access Object)
 * Contains all database operations for contacts: add, view, search, update, delete.
 * Every query uses PreparedStatement to prevent SQL injection.
 * Results are returned as an ArrayList of Contact objects.
 */
public class ContactDAO {

    /** Inserts a new contact into the database. */
    public void addContact(Contact contact) throws SQLException {
        String sql = "INSERT INTO contacts (name, email, mobile) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contact.getName());
            ps.setString(2, contact.getEmail());
            ps.setString(3, contact.getMobile());
            ps.executeUpdate();
        }
    }

    /** Returns all contacts, ordered by ID. */
    public ArrayList<Contact> getAllContacts() throws SQLException {
        String sql = "SELECT id, name, email, mobile FROM contacts ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return readContacts(ps);
        }
    }

    /**
     * Returns contacts whose name, email or mobile contains the keyword.
     * The search is case-insensitive for names and emails.
     */
    public ArrayList<Contact> searchContacts(String keyword) throws SQLException {
        String sql = "SELECT id, name, email, mobile FROM contacts "
                   + "WHERE LOWER(name) LIKE ? OR LOWER(email) LIKE ? OR mobile LIKE ? "
                   + "ORDER BY id";
        String pattern = "%" + keyword.toLowerCase() + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            return readContacts(ps);
        }
    }

    /** Updates an existing contact (matched by ID). */
    public void updateContact(Contact contact) throws SQLException {
        String sql = "UPDATE contacts SET name = ?, email = ?, mobile = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, contact.getName());
            ps.setString(2, contact.getEmail());
            ps.setString(3, contact.getMobile());
            ps.setInt(4, contact.getId());
            ps.executeUpdate();
        }
    }

    /** Deletes the contact with the given ID. */
    public void deleteContact(int id) throws SQLException {
        String sql = "DELETE FROM contacts WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Checks whether an email is already used by ANOTHER contact.
     * Pass excludeId = -1 when adding, or the contact's own ID when updating
     * (so a contact is not reported as a duplicate of itself).
     */
    public boolean emailExists(String email, int excludeId) throws SQLException {
        return valueExists("SELECT COUNT(*) FROM contacts WHERE LOWER(email) = LOWER(?) AND id <> ?",
                email, excludeId);
    }

    /** Checks whether a mobile number is already used by ANOTHER contact. */
    public boolean mobileExists(String mobile, int excludeId) throws SQLException {
        return valueExists("SELECT COUNT(*) FROM contacts WHERE mobile = ? AND id <> ?",
                mobile, excludeId);
    }

    // ---------- Private helper methods (avoid duplicate code) ----------

    /** Runs a COUNT query and returns true if at least one row matches. */
    private boolean valueExists(String sql, String value, int excludeId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Executes a SELECT query and converts every row into a Contact object. */
    private ArrayList<Contact> readContacts(PreparedStatement ps) throws SQLException {
        ArrayList<Contact> contacts = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                contacts.add(new Contact(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("mobile")));
            }
        }
        return contacts;
    }
}
