public class UserInfo {

    private String fullName;
    private String employeeId;
    private String role;
    private String username;
    private String password;

    public UserInfo(String fullName, String employeeId, String role,
                    String username, String password) {
        this.fullName = fullName;
        this.employeeId = employeeId;
        this.role = role;
        this.username = username;
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String input) {
        return password.equals(input);
    }

    // Expose password for persistence handling (kept internal otherwise)
    public String getPassword() {
        return password;
    }
}
