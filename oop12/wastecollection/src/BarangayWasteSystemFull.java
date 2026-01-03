import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.Random;
import java.util.List;

/**
 * Barangay Waste Management & MRF Portal
 */
public class BarangayWasteSystemFull extends JFrame {

    // --- Colors and Fonts ---
    private final Color PRIMARY_GREEN = new Color(20, 150, 50);
    private final Color DARK_GREEN = new Color(0, 80, 0);
    private final Color LIGHT_GREEN = new Color(120, 200, 120);
    private final Color BACKGROUND_FADE_GREEN = new Color(220, 255, 220);
    private final Color CARD_BACKGROUND = new Color(255, 255, 255, 230);
    private final Color INPUT_FILL = new Color(245, 245, 245);
    private final Color TEXT_COLOR_DARK = new Color(50, 50, 50);
    private final Color TEXT_COLOR_LIGHT = Color.WHITE;
    private final Color ERROR_RED = new Color(244, 67, 54);
    private final Color INFO_BLUE = new Color(33, 150, 243);
    private final Color SEPARATOR_GRAY = new Color(180, 180, 180);
    private final Color REPORT_ORANGE = new Color(255, 165, 0);

    private final Font FONT_HEADER = new Font("SansSerif", Font.BOLD, 32);
    private final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    private final Font FONT_BOLD_16 = new Font("SansSerif", Font.BOLD, 16);
    private final Font FONT_PLAIN_16 = new Font("SansSerif", Font.PLAIN, 16);
    private final DecimalFormat df = new DecimalFormat("0.00");


    

    //  Auth Store and State 
    // User credentials are now saved in the database
    private UserInfo loggedInUser = null;

    // System State Variables 
    private JPanel mainCardPanel;
    private CardLayout cardLayout = new CardLayout();
    private JLabel dashboardGreetingLabel;
    private JLabel nameLabel, roleLabel, idLabel; // Sidebar references
    private JLabel totalWeightLabel, totalBioLabel, totalRecyLabel, totalResiLabel; // Analytics references
    private String currentFilterDate = null; // For date filtering in analytics
    private JPanel cardHolder; // Dashboard card container for role-based visibility
    private JPanel wasteGiverCard, collectionLogCard, analyticsCard, mrfCard; // Dashboard card references for visibility control
    // Admin control button (login activity persisted in DatabaseManager)
    private JButton viewLoginsBtn;
    private JButton viewDatabaseBtn;
    
    // --- Waste Data Model ---
    private DefaultTableModel wasteGiverTableModel;
    private DefaultTableModel collectionLogTableModel;
    private final String[] WASTE_GIVER_COLUMNS = {"Date", "Purok", "Giver Name", "Waste Type", "Weight (kg)", "Entered By"};
    private final String[] COLLECTION_LOG_COLUMNS = {"Date", "Truck ID", "Purok/Route","Driver", "Biodegradable (kg)", "Recyclable (kg)", "Residual (kg)", "Entered By"};
    private final String[] PUROK_OPTIONS = {"Purok 1", "Purok 2", "Purok 3", "Purok 4", "Purok 5", "Purok 6"};
    private final String[] WASTE_TYPE_OPTIONS = {"Biodegradable", "Recyclable", "Residual"};

    // --- Pattern for Employee ID ---
    private static final Pattern EMPLOYEE_ID_PATTERN = Pattern.compile("^[A-Z0-9]{4}-[A-Z0-9]{4}$");

    // --- Pattern for Password Strength ---
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    public BarangayWasteSystemFull() {
        super("Barangay Waste Management & MRF Portal");

        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) { /* Fall back to default */ }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        // Initialize the database
        DatabaseManager.initializeDatabase();
        initializeDefaultUsers();

        initializeDataModel();

        mainCardPanel = new JPanel(cardLayout);

        // Initialize and add all panels
        mainCardPanel.add(createWelcomeScreen(), "Welcome");
        mainCardPanel.add(createNavigationChoiceScreen(), "NavChoice");
        mainCardPanel.add(createAuthPanel(true), "Login");
        mainCardPanel.add(createAuthPanel(false), "SignUp");
        mainCardPanel.add(createSystemDashboard(), "Dashboard");
        mainCardPanel.add(createMRFPlaceholderScreen(), "MRFPlaceholder");
        mainCardPanel.add(createWasteAnalyticsScreen(), "Analytics");

        setContentPane(mainCardPanel);
        pack();
        setMinimumSize(new Dimension(1000, 750));
        setLocationRelativeTo(null);
        setVisible(true);

        cardLayout.show(mainCardPanel, "Welcome");
    }

    
    private void initializeDefaultUsers() {
        if (!DatabaseManager.userExists("admin")) {
            DatabaseManager.registerUser("admin", "password123", "System Administrator", "ADM1-0001", "Administrator");
        }
        if (!DatabaseManager.userExists("juan")) {
            DatabaseManager.registerUser("juan", "password123", "Juan Dela Cruz", "TRK0-0005", "Garbage Collector");
        }
        if (!DatabaseManager.userExists("oya123")) {
            DatabaseManager.registerUser("oya123", "password123", "Oya Santos", "BRGY-0001", "Barangay Official");
        }
        if (!DatabaseManager.userExists("jayjay")) {
            DatabaseManager.registerUser("jayjay", "password123", "Jayjay Reyes", "TRK0-0006", "Garbage Collector");
        }
        // Ensure all current users (including any previously requested accounts) are persisted
        DatabaseManager.saveAllUsers();
    }

   
    private void initializeDataModel() {
        wasteGiverTableModel = new DefaultTableModel(WASTE_GIVER_COLUMNS, 0);
        collectionLogTableModel = new DefaultTableModel(COLLECTION_LOG_COLUMNS, 0);

        String date1 = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MMM d"));
        String date2 = LocalDate.now().minusDays(2).format(DateTimeFormatter.ofPattern("MMM d"));

        // Records for oya123 (Barangay Official)
        wasteGiverTableModel.addRow(new Object[]{date1, "Purok 4", "Ana Lopez", "Biodegradable", 5.2, "oya123"});
        wasteGiverTableModel.addRow(new Object[]{date2, "Purok 5", "Carlos Mendoza", "Recyclable", 2.8, "oya123"});

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d"));
        collectionLogTableModel.addRow(new Object[]{today, "T-001", "Route A (Puroks 1, 2)", "Ramon Cruz", 125.5, 45.2, 80.0, "jayjay"});
        collectionLogTableModel.addRow(new Object[]{today, "T-002", "Route B (Puroks 3, 4)", "Liza Morales", 98.0, 31.7, 65.5, "jayjay"});
        collectionLogTableModel.addRow(new Object[]{date1, "T-003", "Route C (Puroks 5, 6)", "Jose Alvarez", 150.0, 50.0, 70.0, "jayjay"});

        // Records for jayjay (Garbage Collector)
        collectionLogTableModel.addRow(new Object[]{date1, "T-004", "Route D (Puroks 1, 3)", "Pedro Garcia", 110.0, 40.0, 75.0, "jayjay"});
        collectionLogTableModel.addRow(new Object[]{date2, "T-005", "Route E (Puroks 2, 4)", "Maria Santos", 95.0, 35.0, 60.0, "jayjay"});
    }


    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(FONT_BOLD_16);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private void confirmExit() {
        // Only show exit confirmation if the user is authenticated (on Dashboard)
        if (loggedInUser != null) {
            int result = JOptionPane.showConfirmDialog(
                BarangayWasteSystemFull.this,
                "Are you sure you want to close the application?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (result == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        } else {
             // If not logged in, just exit immediately from the frame listener 
             System.exit(0);
        }
    }

    private JPanel createNavCard(String title, String description, Color baseColor, String cardName) {
        JPanel card = new NewCardGradientPanel(baseColor);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(baseColor.darker(), 3, true), 
            new EmptyBorder(25, 25, 25, 25)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setLayout(new BorderLayout(0, 15));
        
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_COLOR_LIGHT);
        titleLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        JLabel descLabel = new JLabel("<html><center>" + description + "</center></html>", JLabel.CENTER);
        descLabel.setFont(FONT_PLAIN_16);
        descLabel.setForeground(TEXT_COLOR_LIGHT.brighter());

        card.add(titleLabel, BorderLayout.CENTER);
        card.add(descLabel, BorderLayout.SOUTH);
        
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (cardName.equals("Analytics")) {
                    calculateAndDisplayAnalytics();
                }
                cardLayout.show(mainCardPanel, cardName);
            }
        });
        
        return card;
    }

    private void calculateAndDisplayAnalytics() {
        calculateAndDisplayAnalytics(null);
    }

    private void calculateAndDisplayAnalytics(String filterDate) {
        // Simple calculation using simulated data from the Collection Log
        double totalWeight = 0;
        double totalBio = 0;
        double totalRecy = 0;
        double totalResi = 0;

        for (int i = 0; i < collectionLogTableModel.getRowCount(); i++) {
            String rowDate = (String) collectionLogTableModel.getValueAt(i, 0);
            if (filterDate != null && !filterDate.trim().isEmpty() && !rowDate.equalsIgnoreCase(filterDate.trim())) {
                continue; // Skip rows that don't match the filter date
            }

            // Ensure data types are handled correctly (assuming doubles based on init data)
            try {
                // Get values and handle potential errors in casting
                Object bioValue = collectionLogTableModel.getValueAt(i, 4);
                Object recyValue = collectionLogTableModel.getValueAt(i, 5);
                Object resiValue = collectionLogTableModel.getValueAt(i, 6);

                double bio = (bioValue instanceof Number) ? ((Number) bioValue).doubleValue() : 0.0;
                double recy = (recyValue instanceof Number) ? ((Number) recyValue).doubleValue() : 0.0;
                double resi = (resiValue instanceof Number) ? ((Number) resiValue).doubleValue() : 0.0;

                totalBio += bio;
                totalRecy += recy;
                totalResi += resi;
                totalWeight += bio + recy + resi;
            } catch (Exception ex) {
                System.err.println("Error calculating analytics: Data conversion failed at row " + i + ". " + ex.getMessage());

            }
        }

        // Handle division by zero if totalWeight is 0
        String bioPercent = (totalWeight > 0) ? df.format((totalBio / totalWeight) * 100) : "0.00";
        String recyPercent = (totalWeight > 0) ? df.format((totalRecy / totalWeight) * 100) : "0.00";
        String resiPercent = (totalWeight > 0) ? df.format((totalResi / totalWeight) * 100) : "0.00";

        String dateSuffix = (filterDate != null && !filterDate.trim().isEmpty()) ? " on " + filterDate : "";
        totalWeightLabel.setText("Total Collected: " + df.format(totalWeight) + " kg" + dateSuffix);
        totalBioLabel.setText("Biodegradable: " + df.format(totalBio) + " kg (" + bioPercent + "%)");
        totalRecyLabel.setText("Recyclable: " + df.format(totalRecy) + " kg (" + recyPercent + "%)");
        totalResiLabel.setText("Residual: " + df.format(totalResi) + " kg (" + resiPercent + "%)");
    }

    
    private JPanel createLogScreen(String title, DefaultTableModel model, boolean isGiverLog) {
        JPanel panel = new JPanel(new BorderLayout());

        // Header Panel (Title + Back Button)
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // "Back to Dashboard" button
        JButton backButton = createStyledButton("⬅️ Back to Dashboard", INFO_BLUE, Color.WHITE);
        backButton.addActionListener(e -> cardLayout.show(mainCardPanel, "Dashboard"));
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backWrap.setBorder(new EmptyBorder(20, 20, 0, 0));
        backWrap.add(backButton);
        headerPanel.add(backWrap, BorderLayout.WEST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // Search Panel 
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        searchPanel.setBackground(BACKGROUND_FADE_GREEN);
        JLabel searchLabel = new JLabel("Search Records: ");
        searchLabel.setFont(FONT_PLAIN_16);
        JTextField searchField = new JTextField(15);
        searchField.setFont(FONT_PLAIN_16);
        JButton searchBtn = createStyledButton("SEARCH", PRIMARY_GREEN, Color.WHITE);
        JButton clearSearchBtn = createStyledButton("CLEAR SEARCH", INFO_BLUE, Color.WHITE);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearSearchBtn);

        JTable table = createStyledTable(model);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Define role-based filter
        final RowFilter<DefaultTableModel, Integer> roleFilter;
        if (loggedInUser != null && !"Administrator".equalsIgnoreCase(loggedInUser.getRole())) {
            int enteredByColumn = model.getColumnCount() - 1; // "Entered By" is the last column
            roleFilter = RowFilter.regexFilter("^" + loggedInUser.getUsername() + "$", enteredByColumn);
        } else {
            roleFilter = null;
        }

        // Apply initial role-based filtering
        sorter.setRowFilter(roleFilter);

        JScrollPane scrollPane = new JScrollPane(table);

        // Search action
        searchBtn.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            if (!searchText.isEmpty()) {
                RowFilter<DefaultTableModel, Integer> searchFilter = RowFilter.regexFilter(searchText);
                if (roleFilter != null) {
                    sorter.setRowFilter(RowFilter.andFilter(java.util.Arrays.asList(roleFilter, searchFilter)));
                } else {
                    sorter.setRowFilter(searchFilter);
                }
            } else {
                sorter.setRowFilter(roleFilter);
            }
        });

        // Clear search action
        clearSearchBtn.addActionListener(e -> {
            searchField.setText("");
            sorter.setRowFilter(roleFilter);
        });

        // --- Center Panel (Search + Table) ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // --- Footer Panel for Add/Delete/Edit ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));

        boolean canModify = loggedInUser != null && !"Administrator".equalsIgnoreCase(loggedInUser.getRole());

        if (canModify) {
            JButton deleteButton = createStyledButton("➖ DELETE SELECTED RECORD", ERROR_RED, Color.WHITE);
            deleteButton.addActionListener(e -> deleteSelectedRow(table, model));

            JButton editButton = createStyledButton("✏️ EDIT SELECTED RECORD", INFO_BLUE, Color.WHITE);
            editButton.addActionListener(e -> launchEditRecordDialog(isGiverLog, table, model));

            JButton addButton = createStyledButton("+ ADD NEW RECORD", isGiverLog ? PRIMARY_GREEN : DARK_GREEN, Color.WHITE);
            addButton.addActionListener(e -> launchAddRecordDialog(isGiverLog, model));

            footer.add(deleteButton);
            footer.add(editButton);
            footer.add(addButton);
        }

        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    private void launchAddRecordDialog(boolean isGiverLog, DefaultTableModel model) {
        if (isGiverLog) {
            addWasteGiverRecord(model);
        } else {
            addCollectionLogRecord(model);
        }
    }

    private void launchEditRecordDialog(boolean isGiverLog, JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a row to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);

        if (isGiverLog) {
            editWasteGiverRecord(model, modelRow);
        } else {
            editCollectionLogRecord(model, modelRow);
        }
    }
    
    private void editCollectionLogRecord(DefaultTableModel model, int modelRow) {
        // Retrieve existing values
        String date = (String) model.getValueAt(modelRow, 0);
        String truckId = (String) model.getValueAt(modelRow, 1);
        String route = (String) model.getValueAt(modelRow, 2);
        String driver = (String) model.getValueAt(modelRow, 3);
        double bio = ((Number) model.getValueAt(modelRow, 4)).doubleValue();
        double recy = ((Number) model.getValueAt(modelRow, 5)).doubleValue();
        double resi = ((Number) model.getValueAt(modelRow, 6)).doubleValue();

        // Create fields pre-filled with existing data
        JTextField dateField = new JTextField(date);
        JTextField truckIdField = new JTextField(truckId);
        JTextField routeField = new JTextField(route);
        JTextField driverField = new JTextField(driver);
        JFormattedTextField bioWeightField = createNumericInputField();
        bioWeightField.setValue(bio);
        JFormattedTextField recyWeightField = createNumericInputField();
        recyWeightField.setValue(recy);
        JFormattedTextField resiWeightField = createNumericInputField();
        resiWeightField.setValue(resi);

        JPanel panel = createFormPanel(
            new String[]{"Date:", "Truck ID:", "Purok/Route:", "Driver:", "Biodegradable (kg):", "Recyclable (kg):", "Residual (kg):"},
            new JComponent[]{dateField, truckIdField, routeField, driverField, bioWeightField, recyWeightField, resiWeightField}
        );

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Truck Collection Log",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                // Parse values, ensuring they are valid numbers
                double newBio = parseNumericField(bioWeightField);
                double newRecy = parseNumericField(recyWeightField);
                double newResi = parseNumericField(resiWeightField);

                // Update the model row
                model.setValueAt(dateField.getText(), modelRow, 0);
                model.setValueAt(truckIdField.getText().trim(), modelRow, 1);
                model.setValueAt(routeField.getText().trim(), modelRow, 2);
                model.setValueAt(driverField.getText().trim(), modelRow, 3);
                model.setValueAt(newBio, modelRow, 4);
                model.setValueAt(newRecy, modelRow, 5);
                model.setValueAt(newResi, modelRow, 6);
                // Entered By remains unchanged

                JOptionPane.showMessageDialog(this, "Collection Log entry updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid numeric data entered: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please fill all fields correctly.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void editWasteGiverRecord(DefaultTableModel model, int modelRow) {
        // Retrieve existing values
        String date = (String) model.getValueAt(modelRow, 0);
        String purok = (String) model.getValueAt(modelRow, 1);
        String giverName = (String) model.getValueAt(modelRow, 2);
        String wasteType = (String) model.getValueAt(modelRow, 3);
        double weight = ((Number) model.getValueAt(modelRow, 4)).doubleValue();

        // Create fields pre-filled with existing data
        JTextField dateField = new JTextField(date);
        JComboBox<String> purokDropdown = new JComboBox<>(PUROK_OPTIONS);
        purokDropdown.setSelectedItem(purok);
        JTextField giverNameField = new JTextField(giverName);
        JComboBox<String> wasteTypeDropdown = new JComboBox<>(WASTE_TYPE_OPTIONS);
        wasteTypeDropdown.setSelectedItem(wasteType);
        JFormattedTextField weightField = createNumericInputField();
        weightField.setValue(weight);

        JPanel panel = createFormPanel(
            new String[]{"Date:", "Purok:", "Giver Name:", "Waste Type:", "Weight (kg):"},
            new JComponent[]{dateField, purokDropdown, giverNameField, wasteTypeDropdown, weightField}
        );

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Household Waste Contribution",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                // Parse values, ensuring they are valid numbers
                double newWeight = parseNumericField(weightField);

                // Update the model row
                model.setValueAt(dateField.getText(), modelRow, 0);
                model.setValueAt(purokDropdown.getSelectedItem(), modelRow, 1);
                model.setValueAt(giverNameField.getText().trim(), modelRow, 2);
                model.setValueAt(wasteTypeDropdown.getSelectedItem(), modelRow, 3);
                model.setValueAt(newWeight, modelRow, 4);
                // Entered By remains unchanged

                JOptionPane.showMessageDialog(this, "Waste Giver record updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid numeric data entered: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please fill all fields correctly.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // --- Giver Log Record ---
    private void addWasteGiverRecord(DefaultTableModel model) {
        JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d")));
        JComboBox<String> purokDropdown = new JComboBox<>(PUROK_OPTIONS);
        JTextField giverNameField = new JTextField();
        JComboBox<String> wasteTypeDropdown = new JComboBox<>(WASTE_TYPE_OPTIONS);
        JFormattedTextField weightField = createNumericInputField();

        JPanel panel = createFormPanel(
            new String[]{"Date:", "Purok:", "Giver Name:", "Waste Type:", "Weight (kg):"},
            new JComponent[]{dateField, purokDropdown, giverNameField, wasteTypeDropdown, weightField}
        );

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Household Waste Contribution", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                double weight = parseNumericField(weightField);

                model.addRow(new Object[]{
                    dateField.getText(),
                    purokDropdown.getSelectedItem(),
                    giverNameField.getText().trim(),
                    wasteTypeDropdown.getSelectedItem(),
                    weight,
                    loggedInUser.getUsername()
                });
                JOptionPane.showMessageDialog(this, "Waste Giver record added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number entered for Weight.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                 JOptionPane.showMessageDialog(this, "Please fill all fields correctly.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // --- Collection Log Record ---
    private void addCollectionLogRecord(DefaultTableModel model) {
        JTextField dateField = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d")));
        JTextField truckIdField = new JTextField("T-00X");
        JTextField routeField = new JTextField();
        JTextField driverField = new JTextField("Driver Name");
        JFormattedTextField bioWeightField = createNumericInputField();
        JFormattedTextField recyWeightField = createNumericInputField();
        JFormattedTextField resiWeightField = createNumericInputField();

        JPanel panel = createFormPanel(
            new String[]{"Date:", "Truck ID:", "Purok/Route:", "Driver:", "Biodegradable (kg):", "Recyclable (kg):", "Residual (kg):"},
            new JComponent[]{dateField, truckIdField, routeField, driverField, bioWeightField, recyWeightField, resiWeightField}
        );

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Truck Collection Log",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                // Parse values, ensuring they are valid numbers
                double bio = parseNumericField(bioWeightField);
                double recy = parseNumericField(recyWeightField);
                double resi = parseNumericField(resiWeightField);

                model.addRow(new Object[]{
                    dateField.getText(),
                    truckIdField.getText().trim(),
                    routeField.getText().trim(),
                    driverField.getText().trim(),
                    bio, recy, resi,
                    loggedInUser.getUsername()
                });
                JOptionPane.showMessageDialog(this, "Collection Log entry added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid numeric data entered: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                 JOptionPane.showMessageDialog(this, "Please fill all fields correctly.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    
    private JPanel createFormPanel(String[] labels, JComponent[] fields) {
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(FONT_PLAIN_16);
            panel.add(label);
            panel.add(fields[i]);
        }
        return panel;
    }
    
    
    private JFormattedTextField createNumericInputField() {
        DecimalFormat format = new DecimalFormat("#0.0");
        JFormattedTextField field = new JFormattedTextField(format);
        field.setValue(0.0);
        field.setColumns(10);
        return field;
    }
    
  
    private double parseNumericField(JFormattedTextField field) throws NumberFormatException {
         Object value = field.getValue();
         if (value instanceof Number) {
             double num = ((Number) value).doubleValue();
             if (num < 0) throw new NumberFormatException("Value cannot be negative.");
             return num;
         }
         throw new NumberFormatException("Field value is not a valid number.");
    }

    private void deleteSelectedRow(JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = table.convertRowIndexToModel(selectedRow);
            int confirm = JOptionPane.showConfirmDialog(
                this, 
                "Are you sure you want to delete the selected record?", 
                "Confirm Deletion", 
                JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                model.removeRow(modelRow);
                JOptionPane.showMessageDialog(this, "Record deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, 
                "Please select a row to delete.", 
                "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(FONT_PLAIN_16);
        table.getTableHeader().setFont(FONT_BOLD_16);
        table.getTableHeader().setBackground(PRIMARY_GREEN.darker());
        table.getTableHeader().setForeground(TEXT_COLOR_LIGHT);
        table.setSelectionBackground(LIGHT_GREEN);
        table.setSelectionForeground(TEXT_COLOR_DARK);
        return table;
    }

    private void openMatiMRFRoute(String startAddress, String endAddress) {
          
          String url = "https://www.google.com/maps/dir/?api=1&origin=" + 
                       startAddress.replaceAll(" ", "+") + 
                       "&destination=" + 
                       endAddress.replaceAll(" ", "+") +
                       "&travelmode=driving";
          
          try {
              if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                  Desktop.getDesktop().browse(new URI(url));
                  JOptionPane.showMessageDialog(this, "Opening route map in your default browser...", "Map View", JOptionPane.INFORMATION_MESSAGE);
              } else {
                  JOptionPane.showMessageDialog(this, 
                      "<html>Desktop browsing is not supported.<br>Simulated route URL:<br>" + url + "</html>", 
                      "Map View Simulation", JOptionPane.INFORMATION_MESSAGE);
              }
          } catch (Exception ex) {
              JOptionPane.showMessageDialog(this, "Failed to open browser: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          }
    }
    
    

    
    private JPanel createWelcomeScreen() {
        JPanel panel = new NewScreenGradientPanel();
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 15, 20, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1; 

        JLabel titleLabel = new JLabel("WELCOME TO", JLabel.CENTER);
        titleLabel.setFont(FONT_HEADER.deriveFont(Font.BOLD, 30));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        JLabel systemNameLabel = new JLabel("BARANGAY WASTE COLLECTION & MRF INITIATIVE SYSTEM", JLabel.CENTER);
        systemNameLabel.setFont(FONT_HEADER.deriveFont(Font.BOLD, 40));
        systemNameLabel.setForeground(LIGHT_GREEN.brighter());
        gbc.gridy = 1;
        panel.add(systemNameLabel, gbc);

        JLabel subLabel = new JLabel("Pioneering Sustainable Waste Management", JLabel.CENTER);
        subLabel.setFont(new Font("SansSerif", Font.ITALIC, 24));
        subLabel.setForeground(TEXT_COLOR_LIGHT.darker());
        gbc.gridy = 2; gbc.insets = new Insets(10, 15, 80, 15);
        panel.add(subLabel, gbc);
        
        // --- Button Row ---
        gbc.fill = GridBagConstraints.NONE;
        
        JButton proceedButton = createStyledButton("PROCEED ➡️", PRIMARY_GREEN, Color.WHITE);
        proceedButton.setFont(FONT_BOLD_16);
        proceedButton.setPreferredSize(new Dimension(250, 45));
        
        gbc.gridy = 3; gbc.insets = new Insets(30, 15, 20, 15);
        panel.add(proceedButton, gbc);

        proceedButton.addActionListener(e -> cardLayout.show(mainCardPanel, "NavChoice"));
        return panel;
    }

   
    
    /** Creates the screen with Login/Register options (Access Portal). */
    private JPanel createNavigationChoiceScreen() {
        JPanel panel = new NewScreenGradientPanel(); 

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 15, 20, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        JLabel titleLabel = new JLabel("♻️ ACCESS PORTAL", JLabel.CENTER);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_COLOR_LIGHT);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(titleLabel, gbc);

        JLabel subLabel = new JLabel("Please log in or request access to continue.", JLabel.CENTER);
        subLabel.setFont(new Font("SansSerif", Font.ITALIC, 20));
        subLabel.setForeground(TEXT_COLOR_LIGHT);
        gbc.gridy = 1; gbc.insets = new Insets(0, 15, 60, 15);
        panel.add(subLabel, gbc);

        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        
        Dimension buttonDim = new Dimension(350, 65); 

        JButton loginButton = createStyledButton("LOG IN TO SYSTEM", PRIMARY_GREEN, Color.WHITE);
        loginButton.setPreferredSize(buttonDim); 
        loginButton.setFont(FONT_TITLE);
        gbc.gridx = 0; gbc.gridy = 2; gbc.insets = new Insets(20, 40, 20, 40);
        panel.add(loginButton, gbc);

        JButton registerButton = createStyledButton("REQUEST ACCESS", LIGHT_GREEN.darker(), Color.WHITE);
        registerButton.setPreferredSize(buttonDim); 
        registerButton.setFont(FONT_TITLE);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(registerButton, gbc);
        
        loginButton.addActionListener(e -> cardLayout.show(mainCardPanel, "Login"));
        registerButton.addActionListener(e -> cardLayout.show(mainCardPanel, "SignUp"));

        return panel;
    }

   

    /** Creates the Authentication Panel for Login or Sign Up.*/
    private JPanel createAuthPanel(boolean isLogin) {
        JPanel container = new NewScreenGradientPanel(); 

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(CARD_BACKGROUND);
        
        Dimension fixedSize = new Dimension(450, isLogin ? 500 : 700); 
        panel.setPreferredSize(fixedSize);
        panel.setMinimumSize(fixedSize);
        panel.setMaximumSize(fixedSize);
        
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SEPARATOR_GRAY, 1, true),
            BorderFactory.createEmptyBorder(40, 50, 40, 50)
        ));
        
        container.add(panel);

        Border inputBorder = BorderFactory.createCompoundBorder(
            new LineBorder(SEPARATOR_GRAY.brighter(), 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;

        // Title Setup
        JLabel titleLabel = new JLabel(isLogin ? "PORTAL LOGIN" : "REQUEST ACCESS", JLabel.CENTER);
        titleLabel.setFont(FONT_TITLE.deriveFont(Font.BOLD, 28));
        titleLabel.setForeground(TEXT_COLOR_DARK);
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 30, 0);
        panel.add(titleLabel, gbc);

        gbc.insets = new Insets(5, 0, 5, 0);
        int currentY = 1;

        // Form Fields
        final JTextField fullNameField = createCustomTextField(25, inputBorder, "Full Name", TEXT_COLOR_DARK);
        final JComboBox<String> roleDropdown = createCustomComboBox(
            new String[]{"Select User Role", "Barangay Official", "Garbage Collector", "Administrator"},
            inputBorder
        );
        final JTextField employeeIdField = createCustomTextField(25, inputBorder, "Employee ID (XXXX-XXXX)", TEXT_COLOR_DARK);
        final JTextField userField = createCustomTextField(25, inputBorder, "Username", TEXT_COLOR_DARK);
        final JPasswordField passField = createCustomPasswordField(25, inputBorder, "Password", TEXT_COLOR_DARK);
        final JPasswordField confirmPassField = createCustomPasswordField(25, inputBorder, "Confirm Password", TEXT_COLOR_DARK);

        // Prepare password fields with visibility toggle (eye)
        JPanel passWrap = new JPanel(new BorderLayout());
        passWrap.add(passField, BorderLayout.CENTER);
        JToggleButton passToggle = new JToggleButton("Show");
        passToggle.setFocusable(false);
        char passEcho = passField.getEchoChar();
        passToggle.addActionListener(ae -> passField.setEchoChar(passToggle.isSelected() ? '\0' : passEcho));
        passWrap.add(passToggle, BorderLayout.EAST);

        JPanel confirmWrap = new JPanel(new BorderLayout());
        confirmWrap.add(confirmPassField, BorderLayout.CENTER);
        JToggleButton confirmToggle = new JToggleButton("Show");
        confirmToggle.setFocusable(false);
        char confirmEcho = confirmPassField.getEchoChar();
        confirmToggle.addActionListener(ae -> confirmPassField.setEchoChar(confirmToggle.isSelected() ? '\0' : confirmEcho));
        confirmWrap.add(confirmToggle, BorderLayout.EAST);

        if (!isLogin) {
            // Sign Up fields (Role first, then Employee ID auto-filled based on role)
            currentY = addFormRow(panel, gbc, currentY, "Full Name:", fullNameField, TEXT_COLOR_DARK);
            currentY = addFormRow(panel, gbc, currentY, "Role:", roleDropdown, TEXT_COLOR_DARK);
            currentY = addFormRow(panel, gbc, currentY, "Employee ID:", employeeIdField, TEXT_COLOR_DARK);

            // Auto-generate ID when role is selected
            Random rnd = new Random();
            roleDropdown.addActionListener(e -> {
                String role = (String) roleDropdown.getSelectedItem();
                if (role == null || role.equals("Select User Role")) return;
                String prefix = "USER";
                if (role.equalsIgnoreCase("Administrator")) prefix = "ADM1";
                else if (role.equalsIgnoreCase("Garbage Collector")) prefix = "TRK0";
                else if (role.equalsIgnoreCase("Barangay Official")) prefix = "BRGY";
                String id = prefix + "-" + String.format("%04d", rnd.nextInt(10000));
                employeeIdField.setText(id);
            });
        }
        
        // Fields common to both
        currentY = addFormRow(panel, gbc, currentY, "Username:", userField, TEXT_COLOR_DARK);
        currentY = addFormRow(panel, gbc, currentY, "Password:", passWrap, TEXT_COLOR_DARK);

        if (!isLogin) {
            currentY = addFormRow(panel, gbc, currentY, "Confirm Password:", confirmWrap, TEXT_COLOR_DARK);
        }

        // Feedback Label
        final JLabel authFeedbackLabel = new JLabel("", JLabel.CENTER);
        authFeedbackLabel.setFont(FONT_BOLD_16);
        gbc.gridy = currentY++; gbc.insets = new Insets(15, 0, 15, 0);
        panel.add(authFeedbackLabel, gbc);
        

        // Submit Button
        JButton submitButton = createStyledButton(isLogin ? "LOG IN" : "SIGN UP", PRIMARY_GREEN, Color.WHITE);
        submitButton.setPreferredSize(new Dimension(300, 50));
        gbc.gridy = currentY++; gbc.insets = new Insets(10, 0, 10, 0);
        panel.add(submitButton, gbc);
        
        // Switch Link
        final JLabel switchLabel = new JLabel(
            isLogin ? "<html><span style='color:rgb(0, 80, 0);'>New user? </span><a href='#'>Request Access</a></html>" : 
                      "<html><span style='color:rgb(0, 80, 0);'>Already have an account? </span><a href='#'>Log In</a></html>", 
            JLabel.CENTER
        );
        switchLabel.setFont(FONT_PLAIN_16);
        switchLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = currentY++; gbc.insets = new Insets(10, 0, 10, 0);
        panel.add(switchLabel, gbc);
        
        // Action Listeners
        submitButton.addActionListener(e -> {
            passField.requestFocusInWindow(); 

            String fullName = fullNameField.getText().trim();
            String employeeId = employeeIdField.getText().trim();
            String role = (String)roleDropdown.getSelectedItem();
            String username = userField.getText().trim();
            String password = new String(passField.getPassword()).trim(); 
            String confirmPassword = new String(confirmPassField.getPassword()).trim();

            handleAuthAction(isLogin, fullName, employeeId, role, username, password, confirmPassword, authFeedbackLabel);
        });
        
        switchLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Clear all fields before switching
                fullNameField.setText("");
                employeeIdField.setText("");
                userField.setText("");
                passField.setText("");
                confirmPassField.setText("");
                authFeedbackLabel.setText("");
                
                cardLayout.show(mainCardPanel, isLogin ? "SignUp" : "Login");
            }
        });

        return container;
    }

    private JTextField createCustomTextField(int columns, Border border, String tooltip, Color fgColor) {
        JTextField field = new JTextField(columns);
        field.setBorder(border);
        field.setToolTipText(tooltip);
        field.setPreferredSize(new Dimension(300, 45));
        field.setFont(FONT_PLAIN_16);
        field.setForeground(fgColor);
        field.setBackground(INPUT_FILL);
        return field;
    }

    private JPasswordField createCustomPasswordField(int columns, Border border, String tooltip, Color fgColor) {
        JPasswordField field = new JPasswordField(columns);
        field.setBorder(border);
        field.setToolTipText(tooltip);
        field.setPreferredSize(new Dimension(300, 45));
        field.setFont(FONT_PLAIN_16);
        field.setForeground(fgColor);
        field.setBackground(INPUT_FILL);
        return field;
    }

    private JComboBox<String> createCustomComboBox(String[] items, Border border) {
        JComboBox<String> dropdown = new JComboBox<>(items);
        dropdown.setBorder(border);
        dropdown.setBackground(INPUT_FILL);
        dropdown.setPreferredSize(new Dimension(300, 45));
        dropdown.setFont(FONT_PLAIN_16);
        return dropdown;
    }

    private int addFormRow(JPanel panel, GridBagConstraints gbc, int y, String labelText, JComponent field, Color labelColor) {
        gbc.gridx = 0; gbc.gridy = y; gbc.insets = new Insets(5, 0, 0, 0);
        JLabel label = new JLabel(labelText);
        label.setFont(FONT_BOLD_16);
        label.setForeground(labelColor);
        panel.add(label, gbc);

        gbc.gridy = y + 1; gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(field, gbc);
        return y + 2;
    }

    private void handleAuthAction(boolean isLogin,
                                  String fullName, String employeeId, String role, String username, String password,
                                  String confirmPassword, JLabel feedback) {
        if (isLogin) {
            if (username.isEmpty() || password.isEmpty()) { feedback.setText("Username and Password are required."); feedback.setForeground(ERROR_RED); return; }
            
            // Authenticate from database
            UserInfo user = DatabaseManager.authenticateUser(username, password);
            if (user != null) {
                feedback.setText("Authentication successful! Loading portal...");
                feedback.setForeground(LIGHT_GREEN.brighter());
                Timer timer = new Timer(500, e -> {
                    startSystem(user);
                    ((Timer)e.getSource()).stop();
                });
                timer.setRepeats(false);
                timer.start();
            } else {
                // Check if user exists and is suspended
                if (DatabaseManager.userExists(username) && DatabaseManager.isUserSuspended(username)) {
                    feedback.setText("LogIn Failed: Account is Suspended.");
                    feedback.setForeground(ERROR_RED);
                } else {
                    feedback.setText("Login failed: Invalid username or password.");
                    feedback.setForeground(ERROR_RED);
                }
            }
        } else {
            // Sign Up logic
            if (fullName.isEmpty() || employeeId.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                feedback.setText("All fields are required."); feedback.setForeground(ERROR_RED); return;
            }
            if (role.equals("Select User Role")) {
                feedback.setText("Please select a valid User Role."); feedback.setForeground(ERROR_RED); return;
            }
            if (!password.equals(confirmPassword)) {
                feedback.setText("Passwords do not match."); feedback.setForeground(ERROR_RED); return;
            }
            if (!PASSWORD_PATTERN.matcher(password).matches()) {
                feedback.setText("<html>Password requires minimum 8 characters<br>(letters, numbers, symbols).</html>"); feedback.setForeground(ERROR_RED); return;
            }
            if (DatabaseManager.userExists(username)) {
                feedback.setText("Username already taken. Please choose another."); feedback.setForeground(ERROR_RED); return;
            }
            if (!EMPLOYEE_ID_PATTERN.matcher(employeeId).matches()) {
                feedback.setText("Invalid Employee ID format. Use the format XXXX-XXXX."); feedback.setForeground(ERROR_RED); return;
            }

            // Register user in database
            boolean registrationSuccess = DatabaseManager.registerUser(username, password, fullName, employeeId, role);
            if (!registrationSuccess) {
                feedback.setText("Registration failed. Please try again."); feedback.setForeground(ERROR_RED); return;
            }

            // Create UserInfo for auto-login
            UserInfo newUser = new UserInfo(fullName, employeeId, role, username, password);

            // AUTO-LOGIN AND PROCEED TO DASHBOARD
            feedback.setText("<html><center>Registration successful! Logging you in...</center></html>");
            feedback.setForeground(LIGHT_GREEN.brighter());

            Timer timer = new Timer(1500, e -> {
                startSystem(newUser); // Auto-login the new user
                ((Timer)e.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        }
    }

    /** Initializes the system dashboard after successful login. */
    private void startSystem(UserInfo user) {
        loggedInUser = user;
        // Record login activity with timestamp and user details (persisted to DatabaseManager)
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        DatabaseManager.recordLogin(ts + " - " + user.getFullName() + " (" + user.getRole() + ")");
        updateSidebar(user.getFullName(), user.getRole(), user.getEmployeeId());
        dashboardGreetingLabel.setText("Welcome, " + user.getFullName() + "!");

        // Add log screens now that loggedInUser is set
        mainCardPanel.add(createWasteGiverLogScreen(), "GiverLog");
        mainCardPanel.add(createCollectionLogScreen(), "CollectionLog");

        // Set card visibility based on user role
        String role = user.getRole();
        if ("Administrator".equalsIgnoreCase(role)) {
            wasteGiverCard.setVisible(true);
            collectionLogCard.setVisible(true);
            analyticsCard.setVisible(true);
            mrfCard.setVisible(true);
            cardHolder.removeAll();
            cardHolder.setLayout(new GridLayout(2, 2, 25, 25));
            cardHolder.add(wasteGiverCard);
            cardHolder.add(collectionLogCard);
            cardHolder.add(analyticsCard);
            cardHolder.add(mrfCard);
        } else if ("Garbage Collector".equalsIgnoreCase(role)) {
            wasteGiverCard.setVisible(false);
            collectionLogCard.setVisible(true);
            analyticsCard.setVisible(true);
            mrfCard.setVisible(true);
            cardHolder.removeAll();
            cardHolder.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 25));
            cardHolder.add(collectionLogCard);
            cardHolder.add(analyticsCard);
            cardHolder.add(mrfCard);
        } else if ("Barangay Official".equalsIgnoreCase(role)) {
            wasteGiverCard.setVisible(true);
            collectionLogCard.setVisible(false);
            analyticsCard.setVisible(false);
            mrfCard.setVisible(true);
            // Center the visible cards for Barangay Official
            cardHolder.removeAll();
            cardHolder.setLayout(new FlowLayout(FlowLayout.CENTER, 25, 25));
            cardHolder.add(wasteGiverCard);
            cardHolder.add(mrfCard);
        } else {
            // Default to all visible for unrecognized roles
            wasteGiverCard.setVisible(true);
            collectionLogCard.setVisible(true);
            analyticsCard.setVisible(true);
            mrfCard.setVisible(true);
            cardHolder.removeAll();
            cardHolder.setLayout(new GridLayout(2, 2, 25, 25));
            cardHolder.add(wasteGiverCard);
            cardHolder.add(collectionLogCard);
            cardHolder.add(analyticsCard);
            cardHolder.add(mrfCard);
        }

        // Refresh the card holder layout after visibility changes
        cardHolder.revalidate();
        cardHolder.repaint();

        cardLayout.show(mainCardPanel, "Dashboard");
    }

    

    private JPanel createSystemDashboard() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Header Panel with Title (EXIT button removed as requested)
        JPanel northPanel = new JPanel(new BorderLayout());
        dashboardGreetingLabel = new JLabel("Welcome!", JLabel.CENTER);
        dashboardGreetingLabel.setFont(FONT_HEADER);
        dashboardGreetingLabel.setBorder(new EmptyBorder(50, 0, 50, 0));
        northPanel.add(dashboardGreetingLabel, BorderLayout.CENTER);
        
        panel.add(northPanel, BorderLayout.NORTH);
        
        this.cardHolder = new JPanel(new GridLayout(2, 2, 25, 25));
        this.cardHolder.setBorder(new EmptyBorder(50, 50, 50, 50));
        this.cardHolder.setBackground(BACKGROUND_FADE_GREEN);

        // Navigation Cards
        wasteGiverCard = createNavCard("📝 Waste Giver Log", "Record waste contributions from households.", PRIMARY_GREEN, "GiverLog");
        this.cardHolder.add(wasteGiverCard);
        collectionLogCard = createNavCard("🚛 Collection Logistics", "Record truck operations, distance, and time.", DARK_GREEN, "CollectionLog");
        this.cardHolder.add(collectionLogCard);
        analyticsCard = createNavCard("📊 System Analytics", "View summary of total collected waste and metrics.", LIGHT_GREEN.darker(), "Analytics");
        this.cardHolder.add(analyticsCard);
        mrfCard = createNavCard("🚧 MRF & Truck Status", "Live monitoring and management of MRF operations.", SEPARATOR_GRAY.darker(), "MRFPlaceholder");
        this.cardHolder.add(mrfCard);

        panel.add(createSidebar(), BorderLayout.WEST);
        panel.add(this.cardHolder, BorderLayout.CENTER);
        return panel;
    }
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(DARK_GREEN);
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(new LineBorder(PRIMARY_GREEN, 1, false));

        // --- User Info Panel ---
        JPanel userInfoPanel = new JPanel();
        userInfoPanel.setLayout(new BoxLayout(userInfoPanel, BoxLayout.Y_AXIS));
        userInfoPanel.setBackground(DARK_GREEN.darker());
        userInfoPanel.setBorder(new EmptyBorder(30, 15, 30, 15));

        JLabel iconLabel = new JLabel("👤");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 50));
        iconLabel.setForeground(LIGHT_GREEN);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        nameLabel = new JLabel("Name: N/A");
        roleLabel = new JLabel("Role: N/A");
        idLabel = new JLabel("ID: N/A");
        
        JLabel[] infoLabels = {nameLabel, roleLabel, idLabel};
        for (JLabel label : infoLabels) {
            label.setForeground(TEXT_COLOR_LIGHT);
            label.setFont(FONT_PLAIN_16);
            label.setBorder(new EmptyBorder(5, 0, 5, 0));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        userInfoPanel.add(iconLabel);
        userInfoPanel.add(Box.createVerticalStrut(20));
        userInfoPanel.add(nameLabel);
        userInfoPanel.add(roleLabel);
        userInfoPanel.add(idLabel);
        sidebar.add(userInfoPanel, BorderLayout.NORTH);

        // --- Navigation Panel (for future links/settings) ---
        JPanel navPanel = new JPanel(new GridLayout(4, 1, 0, 5));
        navPanel.setBackground(DARK_GREEN);
        navPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

        // Admin: View Database and Login Activity buttons (hidden unless admin)
        viewDatabaseBtn = createStyledButton("VIEW DATABASE", INFO_BLUE, TEXT_COLOR_LIGHT);
        viewDatabaseBtn.setPreferredSize(new Dimension(200, 40));
        viewDatabaseBtn.setVisible(false);
        viewDatabaseBtn.addActionListener(e -> showDatabaseTableDialog());
        navPanel.add(viewDatabaseBtn);

        viewLoginsBtn = createStyledButton("VIEW LOGIN ACTIVITY", INFO_BLUE, TEXT_COLOR_LIGHT);
        viewLoginsBtn.setPreferredSize(new Dimension(200, 40));
        viewLoginsBtn.setVisible(false);
        viewLoginsBtn.addActionListener(e -> showLoginActivityTableDialog());
        navPanel.add(viewLoginsBtn);

        // --- Log Out Button ---
        JButton logoutButton = createStyledButton("LOGOUT", INFO_BLUE, TEXT_COLOR_LIGHT);
        logoutButton.setPreferredSize(new Dimension(200, 40));
        JPanel logoutWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoutWrap.setBackground(DARK_GREEN);
        logoutWrap.setBorder(new EmptyBorder(10, 0, 10, 0));
        logoutWrap.add(logoutButton);
        
        logoutButton.addActionListener(e -> logoutUser());

        sidebar.add(navPanel, BorderLayout.CENTER);
        sidebar.add(logoutWrap, BorderLayout.SOUTH);
        return sidebar;
    }

    private void updateSidebar(String name, String role, String id) {
        nameLabel.setText("Name: " + name);
        roleLabel.setText("Role: " + role);
        idLabel.setText("ID: " + id);
        // Show admin-only controls when appropriate
        boolean isAdmin = "Administrator".equalsIgnoreCase(role);
        if (viewLoginsBtn != null) viewLoginsBtn.setVisible(isAdmin);
        if (viewDatabaseBtn != null) viewDatabaseBtn.setVisible(isAdmin);
    }

    private void logoutUser() {
        loggedInUser = null;
        JOptionPane.showMessageDialog(this, "You have been successfully logged out.", "Logout", JOptionPane.INFORMATION_MESSAGE);
        // Navigate back to the portal selection screen
        cardLayout.show(mainCardPanel, "NavChoice");
    }


    private JPanel createWasteGiverLogScreen() {
        // Reuses the generic log screen creator (isGiverLog = true)
        return createLogScreen("📝 Household Waste Contribution Log", wasteGiverTableModel, true);
    }

    private JPanel createCollectionLogScreen() {
        // Reuses the generic log screen creator (isGiverLog = false)
        return createLogScreen("🚛 Truck Collection Logistics Log", collectionLogTableModel, false);
    }


    private JPanel createWasteAnalyticsScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // --- Header Panel ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("📊 Waste Management Analytics Summary", JLabel.CENTER);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        // Back Button
        JButton backButton = createStyledButton("⬅ Back to Dashboard", INFO_BLUE, Color.WHITE);
        backButton.addActionListener(e -> cardLayout.show(mainCardPanel, "Dashboard"));
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backWrap.setBorder(new EmptyBorder(20, 20, 0, 0));
        backWrap.add(backButton);
        headerPanel.add(backWrap, BorderLayout.WEST);
        
    panel.add(headerPanel, BorderLayout.NORTH);

    // --- Filter Panel ---
    JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    filterPanel.setBorder(new EmptyBorder(10, 50, 10, 50));
    filterPanel.setBackground(BACKGROUND_FADE_GREEN);
    JLabel filterLabel = new JLabel("Filter by Date (e.g., Jan 1): ");
    filterLabel.setFont(FONT_PLAIN_16);
    JTextField dateFilterField = new JTextField(10);
    dateFilterField.setFont(FONT_PLAIN_16);
    JButton applyFilterBtn = createStyledButton("SEARCH", PRIMARY_GREEN, Color.WHITE);
    applyFilterBtn.addActionListener(e -> {
        String filterText = dateFilterField.getText().trim();
        currentFilterDate = filterText.isEmpty() ? null : filterText;
        calculateAndDisplayAnalytics(currentFilterDate);
    });
    filterPanel.add(filterLabel);
    filterPanel.add(dateFilterField);
    filterPanel.add(applyFilterBtn);

    // --- Analytics Display (Center) ---
    JPanel analyticsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
    analyticsPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

    totalWeightLabel = new JLabel("Total Collected: N/A", JLabel.CENTER);
    totalBioLabel = new JLabel("Biodegradable: N/A", JLabel.CENTER);
    totalRecyLabel = new JLabel("Recyclable: N/A", JLabel.CENTER);
    totalResiLabel = new JLabel("Residual: N/A", JLabel.CENTER);

    JLabel[] dataLabels = {totalWeightLabel, totalBioLabel, totalRecyLabel, totalResiLabel};
    for (JLabel label : dataLabels) {
        label.setFont(FONT_TITLE.deriveFont(Font.BOLD, 26));
        label.setForeground(TEXT_COLOR_DARK);
        label.setOpaque(true);
        label.setBackground(CARD_BACKGROUND);
        label.setBorder(BorderFactory.createLineBorder(SEPARATOR_GRAY, 1, true));
    }

    analyticsPanel.add(totalWeightLabel);
    analyticsPanel.add(totalBioLabel);
    analyticsPanel.add(totalRecyLabel);
    analyticsPanel.add(totalResiLabel);

    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(filterPanel, BorderLayout.NORTH);
    centerPanel.add(analyticsPanel, BorderLayout.CENTER);

    panel.add(centerPanel, BorderLayout.CENTER);
        
        // Recalculate button at the bottom
        JButton refreshButton = createStyledButton("🔄 REFRESH ANALYTICS", PRIMARY_GREEN, Color.WHITE);
        refreshButton.setPreferredSize(new Dimension(300, 50));
        refreshButton.addActionListener(e -> calculateAndDisplayAnalytics());
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBorder(BorderFactory.createEmptyBorder(10, 10, 30, 10));
        footer.add(refreshButton);
        panel.add(footer, BorderLayout.SOUTH);
        
        return panel;
    }

    

    /** * Creates the MRF and Truck Status screen, including a button to view the MRF route. 
     */
    private JPanel createMRFPlaceholderScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // --- Header Panel (Title + Back Button) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("MRF & Logistics Monitoring", JLabel.CENTER);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setBorder(new EmptyBorder(20, 0, 20, 0));
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Back Button
        JButton backButton = createStyledButton("⬅ Back to Dashboard", INFO_BLUE, Color.WHITE);
        backButton.addActionListener(e -> cardLayout.show(mainCardPanel, "Dashboard"));
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backWrap.setBorder(new EmptyBorder(20, 20, 0, 0));
        backWrap.add(backButton);
        headerPanel.add(backWrap, BorderLayout.WEST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // --- Center Content (MRF Route Focus) ---
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(BACKGROUND_FADE_GREEN);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(15, 10, 15, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        // Title
        JLabel routeTitle = new JLabel("Mati Sanitary Landfill Route Navigation", JLabel.CENTER);
        routeTitle.setFont(FONT_TITLE.deriveFont(Font.BOLD, 30));
        routeTitle.setForeground(DARK_GREEN);
        contentPanel.add(routeTitle, gbc);

        
        // Route Information (Hardcoded based on the previous request)
        JLabel routeInfo = new JLabel(
            "<html><center>" +
            "Destination: Mati Sanitary Landfill, Sitio Tagbobolo, Barangay Sainz, Mati City" +
            "<br>Primary Route: Maharlika Highway" +
            "<br>Status: Awaiting Truck Dispatch" +
            "</center></html>"
        , JLabel.CENTER);
        routeInfo.setFont(FONT_BOLD_16);
        routeInfo.setForeground(TEXT_COLOR_DARK);
        contentPanel.add(routeInfo, gbc);

        // --- View Map Button ---
        JButton viewRouteButton = createStyledButton("📍 VIEW LIVE ROUTE MAP (Mati SLF)", REPORT_ORANGE, Color.WHITE);
        viewRouteButton.setPreferredSize(new Dimension(400, 60));
        viewRouteButton.addActionListener(e -> {
            String start = "Barangay Mati MRF, City of Mati";
            String destination = "Mati Sanitary Landfill, Sitio Tagbobolo, Barangay Sainz, City of Mati, Davao Oriental";
            openMatiMRFRoute(start, destination);
        });
        contentPanel.add(viewRouteButton, gbc);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ----------------- New admin dialogs and helpers -----------------

    /**
     * Shows all registered users in a table dialog, with Update and Suspend/Activate buttons.
     *
     * Requires DatabaseManager methods:
     * - List<UserInfo> getAllUsers()
     * - boolean isUserSuspended(String username)
     * - boolean setUserSuspended(String username, boolean suspended)
     * - boolean updateUser(UserInfo user)
     */
    private void showDatabaseTableDialog() {
        List<UserInfo> users = DatabaseManager.getAllUsers();
        String[] cols = {"Username", "Full Name", "Role", "Employee ID", "Suspended"};
        DefaultTableModel userModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // editing via Update dialog only
            }
        };

        for (UserInfo u : users) {
            boolean suspended = false;
            try {
                suspended = DatabaseManager.isUserSuspended(u.getUsername());
            } catch (Exception ex) {
                // If DB doesn't support isUserSuspended, assume false
            }
            userModel.addRow(new Object[]{u.getUsername(), u.getFullName(), u.getRole(), u.getEmployeeId(), suspended});
        }

        JTable userTable = createStyledTable(userModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setPreferredSize(new Dimension(700, 350));

        JButton updateBtn = createStyledButton("UPDATE SELECTED", INFO_BLUE, Color.WHITE);
        JButton suspendBtn = createStyledButton("SUSPEND/ACTIVATE", ERROR_RED, Color.WHITE);
        JButton closeBtn = createStyledButton("CLOSE", INFO_BLUE, Color.WHITE);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(updateBtn);
        btnPanel.add(suspendBtn);
        btnPanel.add(closeBtn);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBorder(new EmptyBorder(10, 10, 10, 10));
        container.add(scroll, BorderLayout.CENTER);
        container.add(btnPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(this, "Registered Users", true);
        dialog.getContentPane().add(container);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        // Update selected user
        updateBtn.addActionListener(e -> {
            int sel = userTable.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(dialog, "Please select a user to update.", "No selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = userTable.convertRowIndexToModel(sel);
            String username = (String) userModel.getValueAt(modelRow, 0);
            // find matching UserInfo
            UserInfo selected = null;
            for (UserInfo uu : users) if (uu.getUsername().equals(username)) { selected = uu; break; }
            if (selected == null) {
                JOptionPane.showMessageDialog(dialog, "Selected user not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            openUpdateUserDialog(selected, userModel, userTable);
        });

        // Suspend / Activate toggle
        suspendBtn.addActionListener(e -> {
            int sel = userTable.getSelectedRow();
            if (sel < 0) {
                JOptionPane.showMessageDialog(dialog, "Please select a user to suspend/activate.", "No selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = userTable.convertRowIndexToModel(sel);
            String username = (String) userModel.getValueAt(modelRow, 0);
            boolean cur = Boolean.FALSE.equals(userModel.getValueAt(modelRow, 4)) ? false : true;
            boolean newState = !cur;
            // Confirm
            int conf = JOptionPane.showConfirmDialog(dialog,
                    (newState ? "Suspend " : "Activate ") + "user '" + username + "'?",
                    (newState ? "Confirm Suspend" : "Confirm Activate"),
                    JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) return;
            boolean ok = false;
            try {
                ok = DatabaseManager.setUserSuspended(username, newState);
            } catch (NoSuchMethodError nsme) {
                // If DB manager doesn't implement suspension, show message and flip locally
                JOptionPane.showMessageDialog(dialog, "DatabaseManager does not implement setUserSuspended(). Please add persistence support.", "Not implemented", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Failed to change user state: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (ok) {
                userModel.setValueAt(newState, modelRow, 4);
                JOptionPane.showMessageDialog(dialog, "User '" + username + "' is now " + (newState ? "suspended." : "active."), "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to change user state. See logs.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    /**
     * Opens a dialog to update user's editable fields and persist via DatabaseManager.updateUser(...)
     */
    private void openUpdateUserDialog(UserInfo user, DefaultTableModel model, JTable table) {
        JTextField fullNameField = new JTextField(user.getFullName());
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Barangay Official", "Garbage Collector", "Administrator"});
        roleBox.setSelectedItem(user.getRole());
        JTextField empIdField = new JTextField(user.getEmployeeId());

        JPanel form = createFormPanel(
                new String[]{"Username:", "Full Name:", "Role:", "Employee ID:"},
                new JComponent[]{
                        new JLabel(user.getUsername()),
                        fullNameField,
                        roleBox,
                        empIdField
                });

        int res = JOptionPane.showConfirmDialog(this, form, "Update User: " + user.getUsername(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String newFull = fullNameField.getText().trim();
            String newRole = (String) roleBox.getSelectedItem();
            String newEmpId = empIdField.getText().trim();

            if (newFull.isEmpty() || newRole == null || newRole.isEmpty() || newEmpId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!EMPLOYEE_ID_PATTERN.matcher(newEmpId).matches()) {
                JOptionPane.showMessageDialog(this, "Employee ID must match format XXXX-XXXX.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            UserInfo updated = new UserInfo(newFull, newEmpId, newRole, user.getUsername(), user.getPassword());
            boolean ok = false;
            try {
                ok = DatabaseManager.updateUser(updated);
            } catch (NoSuchMethodError nsme) {
                JOptionPane.showMessageDialog(this, "DatabaseManager.updateUser(UserInfo) not implemented. Please add persistence support.", "Not implemented", JOptionPane.ERROR_MESSAGE);
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to update user: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (ok) {
                // update table row display if present
                // find row by username
                for (int r = 0; r < model.getRowCount(); r++) {
                    if (model.getValueAt(r, 0).equals(user.getUsername())) {
                        model.setValueAt(updated.getFullName(), r, 1);
                        model.setValueAt(updated.getRole(), r, 2);
                        model.setValueAt(updated.getEmployeeId(), r, 3);
                        break;
                    }
                }
                JOptionPane.showMessageDialog(this, "User successfully updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update user in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Shows login history in a table (Timestamp | Activity)
     * Uses DatabaseManager.getLoginHistory()
     */
    private void showLoginActivityTableDialog() {
        java.util.List<String> entries = DatabaseManager.getLoginHistory();
        if (entries == null || entries.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No login activity recorded yet.", "Login Activity", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] cols = {"Timestamp", "Activity"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // Show most recent first
        for (int i = entries.size() - 1; i >= 0; i--) {
            String e = entries.get(i);
            String ts = "";
            String act = e;
            // try parse "yyyy-MM-dd HH:mm:ss - rest"
            int idx = e.indexOf(" - ");
            if (idx > 0) {
                ts = e.substring(0, idx);
                act = e.substring(idx + 3);
            }
            model.addRow(new Object[]{ts, act});
        }

        JTable table = createStyledTable(model);
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(600, 350));

        JButton close = createStyledButton("CLOSE", INFO_BLUE, Color.WHITE);
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btn.add(close);

        JPanel container = new JPanel(new BorderLayout(8, 8));
        container.setBorder(new EmptyBorder(10, 10, 10, 10));
        container.add(sp, BorderLayout.CENTER);
        container.add(btn, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(this, "Login Activity", true);
        dialog.getContentPane().add(container);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        close.addActionListener(ae -> dialog.dispose());
        dialog.setVisible(true); 
    }

    // ----------------- End admin helpers -----------------

    public static void main(String[] args) {
        // Run the Swing application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new BarangayWasteSystemFull());
    }
}