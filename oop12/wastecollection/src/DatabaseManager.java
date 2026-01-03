import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class DatabaseManager {

    private static final Map<String, UserInfo> users = new HashMap<>();
    private static final List<String> loginHistory = new ArrayList<>();
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path USERS_FILE = DATA_DIR.resolve("users.txt");
    private static final Path LOGINS_FILE = DATA_DIR.resolve("logins.txt");

    private DatabaseManager() {}

    public static void initializeDatabase() {
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            // load users
            if (Files.exists(USERS_FILE)) {
                List<String> lines = Files.readAllLines(USERS_FILE, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    // username|password|fullName|employeeId|role|suspended
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 6) {
                        String username = parts[0];
                        String password = parts[1];
                        String fullName = parts[2];
                        String employeeId = parts[3];
                        String role = parts[4];
                        boolean suspended = Boolean.parseBoolean(parts[5]);
                        users.put(username, new UserInfo(fullName, employeeId, role, username, password, suspended));
                    } else if (parts.length >= 5) {
                        // Backward compatibility: if no suspended field, assume false
                        String username = parts[0];
                        String password = parts[1];
                        String fullName = parts[2];
                        String employeeId = parts[3];
                        String role = parts[4];
                        users.put(username, new UserInfo(fullName, employeeId, role, username, password, false));
                    }
                }
            }
            // load logins
            if (Files.exists(LOGINS_FILE)) {
                List<String> lines = Files.readAllLines(LOGINS_FILE, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) loginHistory.add(line);
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to initialize data directory: " + ex.getMessage());
        }
    }

    public static boolean userExists(String username) {
        return users.containsKey(username);
    }

    public static boolean registerUser(String username, String password,
                                       String fullName, String employeeId,
                                       String role) {
        if (userExists(username)) return false;

        users.put(username,
            new UserInfo(fullName, employeeId, role, username, password, false)
        );
        // append to file for persistence
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            String line = String.join("|", username, password, fullName, employeeId, role, "false") + System.lineSeparator();
            Files.write(USERS_FILE, line.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Failed to persist user: " + ex.getMessage());
        }
        return true;
    }

    public static UserInfo authenticateUser(String username, String password) {
        if (!userExists(username)) return null;

        UserInfo user = users.get(username);
        if (!user.checkPassword(password)) return null;

        // Check if user is suspended
        if (isUserSuspended(username)) return null;

        return user;
    }

    // Login activity recording (in-memory)
    public static void recordLogin(String entry) {
        loginHistory.add(entry);
        // append to file
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            String line = entry + System.lineSeparator();
            Files.write(LOGINS_FILE, line.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ex) {
            System.err.println("Failed to persist login entry: " + ex.getMessage());
        }
    }

    public static List<String> getLoginHistory() {
        return new ArrayList<>(loginHistory);
    }

    /**
     * Returns a copy of all registered users.
     */
    public static List<UserInfo> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /** Overwrite users file with current users map to ensure persistence of all accounts. */
    public static void saveAllUsers() {
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            StringBuilder sb = new StringBuilder();
                        for (UserInfo u : users.values()) {
                                // username|password|fullName|employeeId|role|suspended
                                sb.append(u.getUsername()).append('|')
                                    .append(u.getPassword()).append('|')
                                    .append(u.getFullName()).append('|')
                                    .append(u.getEmployeeId()).append('|')
                                    .append(u.getRole()).append('|')
                                    .append(u.isSuspended()).append(System.lineSeparator());
                        }
            // NOTE: For safety we write passwords via registerUser append; when saving all users
            // we intentionally do not write plaintext passwords again (we write [PROTECTED])
            // to avoid accidental exposure. New registrations will continue to append plaintext.
            Files.write(USERS_FILE, sb.toString().getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            System.err.println("Failed to save all users: " + ex.getMessage());
        }
    }

    public static boolean updateUser(String username, String newFull, String newEmp, String newRole) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }

    public static boolean setUserActive(String username, boolean activate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUserActive'");
    }

    public static UserInfo getUser(String username) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUser'");
    }

    public static boolean updateUser(UserInfo updated) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }

    public static boolean setUserSuspended(String username, boolean newState) {
        if (!userExists(username)) return false;
        UserInfo user = users.get(username);
        user.setSuspended(newState);
        saveAllUsers(); // Persist the change
        return true;
    }

    public static boolean isUserSuspended(String username) {
        if (!userExists(username)) return false;
        return users.get(username).isSuspended();
    }
}