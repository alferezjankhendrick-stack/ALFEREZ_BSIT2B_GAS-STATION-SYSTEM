package com.scc.dao;

import com.scc.model.LoginResult;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final String USER_COLUMNS = "SELECT id, username, password, roles, status FROM users ";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();


    public LoginResult authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user == null) {
            return new LoginResult(LoginResult.Outcome.INVALID_CREDENTIALS, null);
        }
        if (!user.password().equals(password)) {
            activityLogDao.logAction(user.id(), "LOGIN_FAILED",
                    "Failed login attempt (wrong password) for user " + user.username() + ".");
            return new LoginResult(LoginResult.Outcome.INVALID_CREDENTIALS, null);
        }
        if (!"APPROVED".equalsIgnoreCase(user.status())) {
            activityLogDao.logAction(user.id(), "LOGIN_BLOCKED",
                    "Login blocked for user " + user.username() + " - account status is " + user.status() + ".");
            return new LoginResult(LoginResult.Outcome.NOT_APPROVED, user);
        }
        activityLogDao.logAction(user.id(), "LOGIN",
                "User " + user.username() + " (ID: " + user.id() + ") logged in.");
        return new LoginResult(LoginResult.Outcome.SUCCESS, user);
    }

    public int registerUser(String username, String password, String roles) {
        String sql = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, roles.toUpperCase());
            pstmt.executeUpdate();
            int id = DatabaseConnection.lastInsertId(conn);
            activityLogDao.logAction(conn, id, "USER_REGISTERED",
                    "User " + username + " registered as " + roles.toUpperCase() + " (status PENDING).");
            return id;
        } catch (SQLException e) {
            System.err.println("Error occurred while registering user: " + e.getMessage());
        }
        return -1;
    }


    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("status"));
    }

    private List<User> query(String sql) {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving users: " + e.getMessage());
        }
        return users;
    }

    public List<User> getAllUsers() {
        return query(USER_COLUMNS + "ORDER BY id");
    }

    public List<User> getPendingUsers() {
        return query(USER_COLUMNS + "WHERE status = 'PENDING' ORDER BY id");
    }

    public User findById(int userId) {
        return findOne(USER_COLUMNS + "WHERE id = ?", userId, null);
    }

    public User findByUsername(String username) {
        return findOne(USER_COLUMNS + "WHERE username = ?", 0, username);
    }

    private User findOne(String sql, int id, String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (username != null) {
                ps.setString(1, username);
            } else {
                ps.setInt(1, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return null;
    }

    public boolean usernameExists(String username) {
        return findByUsername(username) != null;
    }


    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error approving user: " + e.getMessage());
            return false;
        }
    }

    public boolean approveUser(int adminId, int userId) {
        boolean success = approveUser(userId);
        if (success) {
            activityLogDao.logAction(adminId, "USER_APPROVED",
                    "Admin [" + adminId + "] approved account for User ID " + userId);
        }
        return success;
    }

    public boolean addUser(int adminId, String username, String password, String roles, String status) {
        String sql = "INSERT INTO users (username, password, roles, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, roles.toUpperCase());
            ps.setString(4, status.toUpperCase());
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(conn, adminId, "USER_CREATED",
                        "Admin [" + adminId + "] created user " + username + " as " + roles.toUpperCase()
                                + " (" + status.toUpperCase() + ").");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
        }
        return false;
    }

    public boolean updateUserRole(int userId, String roles) {
        String sql = "UPDATE users SET roles = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roles.toUpperCase());
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user role: " + e.getMessage());
        }
        return false;
    }

    public boolean updateUserRole(int adminId, int userId, String roles) {
        boolean success = updateUserRole(userId, roles);
        if (success) {
            activityLogDao.logAction(adminId, "USER_UPDATED",
                    "Admin [" + adminId + "] changed role of User " + userId + " to " + roles.toUpperCase());
        }
        return success;
    }

    public boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.toUpperCase());
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user status: " + e.getMessage());
        }
        return false;
    }

    public boolean updateUserStatus(int adminId, int userId, String status) {
        boolean success = updateUserStatus(userId, status);
        if (success) {
            activityLogDao.logAction(adminId, "USER_UPDATED",
                    "Admin [" + adminId + "] changed status of User " + userId + " to " + status.toUpperCase());
        }
        return success;
    }

    public boolean deleteUser(int adminId, int userId, String username) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(conn, adminId, "USER_DELETED",
                        "Admin [" + adminId + "] deleted user " + username + " (ID " + userId + ").");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return false;
    }


    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            if (ps.executeUpdate() > 0) {
                activityLogDao.logAction(conn, userId, "PASSWORD_CHANGED",
                        "User ID " + userId + " changed their password.");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error changing password: " + e.getMessage());
        }
        return false;
    }
}