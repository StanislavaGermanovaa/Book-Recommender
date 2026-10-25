package fmi.uni.masters.db;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:project_library.db";

    public DatabaseManager() {
        try (Connection conn = DriverManager.getConnection(URL)) {
            if (conn != null) {
                Statement stmt = conn.createStatement();

                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "username TEXT PRIMARY KEY, " +
                        "password TEXT);");

                stmt.execute("CREATE TABLE IF NOT EXISTS user_likes (" +
                        "username TEXT, " +
                        "book_id TEXT, " +
                        "PRIMARY KEY (username, book_id));");
            }
        } catch (SQLException e) {
            System.err.println("DB error " + e.getMessage());
        }
    }

    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("User: '" + username + "' registered successfully.");
            return true;

        } catch (SQLException e) {
            System.err.println("Error registering user '" + username + "': " + e.getMessage());
            return false;
        }
    }

    public boolean validateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            System.err.println("[DB] Error validating user " + e.getMessage());
            return false;
        }
    }

    public boolean insertLike(String username, String bookId) {
        String sql = "INSERT OR IGNORE INTO user_likes(username, book_id) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, bookId);
            pstmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean removeLike(String username, String bookId) {
        String sql = "DELETE FROM user_likes WHERE username = ? AND book_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, bookId);
            int rowsAffected = pstmt.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public void syncLikesToOntology(OntModel baseModel, String ns) {
        String sql = "SELECT username, book_id FROM user_likes";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ObjectProperty hasLiked = baseModel.getObjectProperty(ns + "hasLiked");
            if (hasLiked == null) {
                hasLiked = baseModel.createObjectProperty(ns + "hasLiked");
            }

            OntClass userClass = baseModel.getOntClass(ns + "User");

            int counter = 0;
            while (rs.next()) {
                String username = rs.getString("username");
                String bookId = rs.getString("book_id");

                Individual userInd = baseModel.getIndividual(ns + username);
                if (userInd == null && userClass != null) {
                    userInd = baseModel.createIndividual(ns + username, userClass);
                }

                Individual bookInd = baseModel.getIndividual(ns + bookId);

                if (userInd != null && bookInd != null) {
                    if (!userInd.hasProperty(hasLiked, bookInd)) {
                        userInd.addProperty(hasLiked, bookInd);
                        counter++;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error syncing likes from SQLite: " + e.getMessage());
        }
    }
}