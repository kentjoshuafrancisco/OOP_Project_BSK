public class UserInfo {

    private String fullName;
    private String employeeId;
    private String role;
    private String username;
    private String password;
    private boolean suspended;

    public UserInfo(String fullName, String employeeId, String role,
                    String username, String password) {
        this(fullName, employeeId, role, username, password, false);
    }

    public UserInfo(String fullName, String employeeId, String role,
                    String username, String password, boolean suspended) {
        this.fullName = fullName;
        this.employeeId = employeeId;
        this.role = role;
        this.username = username;
        this.password = password;
        this.suspended = suspended;
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



    public boolean isActive() {
        return !suspended;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
}
