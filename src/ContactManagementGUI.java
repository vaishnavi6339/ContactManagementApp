import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * ContactManagementGUI.java
 * The Java Swing window. It shows the input form, the buttons, the search box
 * and the JTable. It validates user input and calls ContactDAO for database work.
 */
public class ContactManagementGUI extends JFrame {

    // ---------- Validation rules ----------
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String MOBILE_REGEX = "^\\d{10}$";   // exactly 10 digits

    // ---------- Colours and fonts ----------
    private static final Color HEADER_COLOR = new Color(33, 97, 140);
    private static final Color BACKGROUND_COLOR = new Color(245, 247, 250);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 13);

    // ---------- Components ----------
    private final JTextField nameField = new JTextField(25);
    private final JTextField emailField = new JTextField(25);
    private final JTextField mobileField = new JTextField(25);
    private final JTextField searchField = new JTextField(25);
    private final JTable contactTable;
    private final DefaultTableModel tableModel;

    // ---------- Data access ----------
    private final ContactDAO contactDAO = new ContactDAO();

    // ID of the contact currently selected in the table (-1 means none)
    private int selectedContactId = -1;

    public ContactManagementGUI() {
        setTitle("Contact Management System");
        setSize(820, 600);
        setMinimumSize(new Dimension(700, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // centre on screen

        // Table model: columns are fixed and cells are NOT editable directly
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Mobile"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        contactTable = new JTable(tableModel);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(BACKGROUND_COLOR);

        mainPanel.add(createTitlePanel(), BorderLayout.NORTH);
        mainPanel.add(createFormPanel(), BorderLayout.WEST);
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);

        setContentPane(mainPanel);

        // Load saved contacts as soon as the window opens
        loadContacts();
    }

    // =====================================================================
    //  UI building methods
    // =====================================================================

    /** Blue header bar with the application title. */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(HEADER_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("Contact Management System");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        panel.add(title);
        return panel;
    }

    /** Left side: input fields and the four action buttons. */
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Contact Details", TitledBorder.LEFT, TitledBorder.TOP, BUTTON_FONT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        addLabeledField(panel, gbc, 0, "Full Name:", nameField);
        addLabeledField(panel, gbc, 2, "Email Address:", emailField);
        addLabeledField(panel, gbc, 4, "Mobile Number (10 digits):", mobileField);

        // Buttons in a 2 x 2 grid
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(createButton("Add Contact", new Color(46, 139, 87), e -> addContact()));
        buttonPanel.add(createButton("Update Contact", new Color(33, 97, 140), e -> updateContact()));
        buttonPanel.add(createButton("Delete Contact", new Color(192, 57, 43), e -> deleteContact()));
        buttonPanel.add(createButton("Clear Fields", new Color(127, 140, 141), e -> clearFields()));

        gbc.gridy = 6;
        gbc.insets = new Insets(16, 10, 6, 10);
        panel.add(buttonPanel, gbc);

        // Push everything to the top of the panel
        gbc.gridy = 7;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    /** Right side: search box and the contacts table. */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 8));
        panel.setBackground(BACKGROUND_COLOR);

        // ----- Search bar -----
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setBackground(BACKGROUND_COLOR);
        JLabel searchLabel = new JLabel("Search (name / email / mobile):");
        searchLabel.setFont(LABEL_FONT);
        searchField.setFont(LABEL_FONT);
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        // Live search: the table updates every time the user types
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadContacts(); }
            public void removeUpdate(DocumentEvent e) { loadContacts(); }
            public void changedUpdate(DocumentEvent e) { loadContacts(); }
        });

        // ----- Table -----
        contactTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        contactTable.setRowHeight(24);
        contactTable.getTableHeader().setFont(BUTTON_FONT);
        contactTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactTable.getColumnModel().getColumn(0).setMaxWidth(50);

        // When a row is selected, copy its details into the input fields
        contactTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFieldsFromSelectedRow();
            }
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(contactTable), BorderLayout.CENTER);
        return panel;
    }

    /** Adds a label above a text field (helper to avoid repeating code). */
    private void addLabeledField(JPanel panel, GridBagConstraints gbc, int row, String text, JTextField field) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        field.setFont(LABEL_FONT);
        gbc.gridy = row;
        panel.add(label, gbc);
        gbc.gridy = row + 1;
        panel.add(field, gbc);
    }

    /** Creates a coloured button with an action (helper to avoid repeating code). */
    private JButton createButton(String text, Color color, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(action);
        return button;
    }

    // =====================================================================
    //  Button actions
    // =====================================================================

    /** ADD: validate the input, check duplicates, then save. */
    private void addContact() {
        Contact contact = readValidatedInput(-1);
        if (contact == null) {
            return; // validation failed - message already shown
        }
        try {
            contactDAO.addContact(contact);
            showInfo("Contact added successfully.");
            clearFields();
        } catch (SQLException e) {
            showError("Could not add contact: " + e.getMessage());
        }
    }

    /** UPDATE: a contact must be selected; then validate and save changes. */
    private void updateContact() {
        if (selectedContactId == -1) {
            showError("Please select a contact from the table to update.");
            return;
        }
        Contact contact = readValidatedInput(selectedContactId);
        if (contact == null) {
            return;
        }
        contact.setId(selectedContactId);
        try {
            contactDAO.updateContact(contact);
            showInfo("Contact updated successfully.");
            clearFields();
        } catch (SQLException e) {
            showError("Could not update contact: " + e.getMessage());
        }
    }

    /** DELETE: a contact must be selected; ask for confirmation first. */
    private void deleteContact() {
        if (selectedContactId == -1) {
            showError("Please select a contact from the table to delete.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete \"" + nameField.getText().trim() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            contactDAO.deleteContact(selectedContactId);
            showInfo("Contact deleted successfully.");
            clearFields();
        } catch (SQLException e) {
            showError("Could not delete contact: " + e.getMessage());
        }
    }

    /** CLEAR: empty the inputs, clear the search, and deselect the table row. */
    private void clearFields() {
        nameField.setText("");
        emailField.setText("");
        mobileField.setText("");
        searchField.setText("");   // this also reloads the full table
        contactTable.clearSelection();
        selectedContactId = -1;
        loadContacts();
        nameField.requestFocus();
    }

    // =====================================================================
    //  Helper methods
    // =====================================================================

    /**
     * Reads the three input fields, validates them and checks for duplicates.
     * Returns a Contact if everything is valid, or null (after showing an error).
     * excludeId is -1 for Add, or the selected contact's ID for Update.
     */
    private Contact readValidatedInput(int excludeId) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim().toLowerCase();
        String mobile = mobileField.getText().trim();

        if (name.isEmpty()) {
            showError("Name cannot be empty.");
            nameField.requestFocus();
            return null;
        }
        if (email.isEmpty()) {
            showError("Email address cannot be empty.");
            emailField.requestFocus();
            return null;
        }
        if (!email.matches(EMAIL_REGEX)) {
            showError("Please enter a valid email address (example: name@example.com).");
            emailField.requestFocus();
            return null;
        }
        if (mobile.isEmpty()) {
            showError("Mobile number cannot be empty.");
            mobileField.requestFocus();
            return null;
        }
        if (!mobile.matches(MOBILE_REGEX)) {
            showError("Mobile number must contain exactly 10 digits (numbers only).");
            mobileField.requestFocus();
            return null;
        }

        try {
            if (contactDAO.emailExists(email, excludeId)) {
                showError("This email address is already saved for another contact.");
                emailField.requestFocus();
                return null;
            }
            if (contactDAO.mobileExists(mobile, excludeId)) {
                showError("This mobile number is already saved for another contact.");
                mobileField.requestFocus();
                return null;
            }
        } catch (SQLException e) {
            showError("Database error while checking duplicates: " + e.getMessage());
            return null;
        }

        return new Contact(name, email, mobile);
    }

    /**
     * Reloads the table. If the search box has text, only matching contacts are
     * shown; otherwise all contacts are shown.
     */
    private void loadContacts() {
        String keyword = searchField.getText().trim();
        try {
            ArrayList<Contact> contacts = keyword.isEmpty()
                    ? contactDAO.getAllContacts()
                    : contactDAO.searchContacts(keyword);

            tableModel.setRowCount(0); // remove old rows
            for (Contact c : contacts) {
                tableModel.addRow(new Object[]{c.getId(), c.getName(), c.getEmail(), c.getMobile()});
            }
        } catch (SQLException e) {
            showError("Could not load contacts: " + e.getMessage());
        }
    }

    /** Copies the selected table row into the input fields. */
    private void fillFieldsFromSelectedRow() {
        int row = contactTable.getSelectedRow();
        if (row == -1) {
            selectedContactId = -1;
            return;
        }
        selectedContactId = (int) tableModel.getValueAt(row, 0);
        nameField.setText(tableModel.getValueAt(row, 1).toString());
        emailField.setText(tableModel.getValueAt(row, 2).toString());
        mobileField.setText(tableModel.getValueAt(row, 3).toString());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
