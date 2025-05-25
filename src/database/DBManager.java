package src.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBManager {
    private static final String DB_URL = "jdbc:sqlite:WorkoutTracker.db";

    public static Connection connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(DB_URL);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void initializeDatabase() {
        String createWorkouts = """
                CREATE TABLE IF NOT EXISTS workouts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT,
                    exercise TEXT,
                    sets INTEGER,
                    reps INTEGER
                );
                """;

        String createMeals = """
                CREATE TABLE IF NOT EXISTS meals (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    date     TEXT,
                    type     TEXT,
                    food     TEXT,
                    calories INTEGER
                );
                """;

        String createWeights = """
                CREATE TABLE IF NOT EXISTS weight_entries (
                    id     INTEGER PRIMARY KEY AUTOINCREMENT,
                    date   TEXT,
                    weight REAL
                );
                """;

        String createSettings = """
                CREATE TABLE IF NOT EXISTS settings (
                  key   TEXT PRIMARY KEY,
                  value TEXT
                );
                """;

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {

            stmt.execute(createWorkouts);
            stmt.execute(createMeals);
            stmt.execute(createWeights);
            stmt.execute(createSettings);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteWorkout(int id) {
        String sql = "DELETE FROM workouts WHERE id = ?";
        try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateWorkout(int id, String date, String exercise, int sets, int reps) {
        String sql = "UPDATE workouts SET date=?, exercise=?, sets=?, reps=?, weight=? WHERE id=?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            pstmt.setString(2, exercise);
            pstmt.setInt(3, sets);
            pstmt.setInt(4, reps);
            pstmt.setInt(6, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertWorkout(String date, String exercise, int sets, int reps) {
        String sql = "INSERT INTO workouts (date, exercise, sets, reps, weight) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            pstmt.setString(2, exercise);
            pstmt.setInt(3, sets);
            pstmt.setInt(4, reps);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertMeal(String date, String type, String food, int calories) {
        String sql = "INSERT INTO meals(date,type,food,calories) VALUES(?,?,?,?)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, date);
            p.setString(2, type);
            p.setString(3, food);
            p.setInt(4, calories);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteMeal(int id) {
        String sql = "DELETE FROM meals WHERE id = ?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateMeal(int id, String date, String type, String food, int calories) {
        String sql = "UPDATE meals SET date=?,type=?,food=?,calories=? WHERE id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, date);
            p.setString(2, type);
            p.setString(3, food);
            p.setInt(4, calories);
            p.setInt(5, id);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertWeight(String date, double weight) {
        String sql = "INSERT INTO weight_entries(date,weight) VALUES(?,?)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, date);
            p.setDouble(2, weight);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateWeight(int id, String date, double weight) {
        String sql = "UPDATE weight_entries SET date=?,weight=? WHERE id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, date);
            p.setDouble(2, weight);
            p.setInt(3, id);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteWeight(int id) {
        String sql = "DELETE FROM weight_entries WHERE id=?";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, id);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setSetting(String key, String value) {
        String sql = """
                  INSERT INTO settings(key,value)
                  VALUES(?,?)
                  ON CONFLICT(key) DO UPDATE SET value=excluded.value
                """;
        try (Connection c = connect();
                PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, key);
            p.setString(2, value);
            p.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getSetting(String key) {
        String sql = "SELECT value FROM settings WHERE key=?";
        try (Connection c = connect();
                PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, key);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next())
                    return rs.getString("value");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
