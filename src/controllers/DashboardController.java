package src.controllers;

import src.database.DBManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import javafx.collections.FXCollections;

/**
 * Controller for DashboardView.fxml
 */
public class DashboardController {
    // === existing fields ===
    @FXML
    private Label wk7Label;
    @FXML
    private Label wk30Label;
    @FXML
    private Label wtChangeLabel;

    // === new goal/activity labels ===
    @FXML
    private Label weightGoalDashboardLabel;
    @FXML
    private Label workoutGoalDashboardLabel;
    @FXML
    private Label activityWeekLabel;
    @FXML
    private Label activityMonthLabel;

    // === existing mini‐chart injection ===
    @FXML
    private LineChart<String, Number> miniWeightChart;
    @FXML
    private Label miniWeightPlaceholder;

    @FXML
    public void initialize() {
        LocalDate now = LocalDate.now();
        LocalDate ago7 = now.minusDays(7);
        LocalDate ago30 = now.minusDays(30);

        // --- 1) Workouts summary for last 7 / 30 days ---
        String countSql = "SELECT COUNT(*) AS cnt FROM workouts WHERE date BETWEEN ? AND ?";
        int count7 = queryCount(countSql, ago7, now);
        int count30 = queryCount(countSql, ago30, now);
        wk7Label.setText("Last 7 days:  " + count7 + " workouts");
        wk30Label.setText("Last 30 days: " + count30 + " workouts");

        // --- 2) Weight change (30 days) ---
        double startW = fetchWeightOnOrBefore(ago30);
        double endW = fetchWeightOnOrBefore(now);
        wtChangeLabel.setText(
                String.format("%.1f → %.1f   (Δ %.1f kg)", startW, endW, endW - startW));

        // --- 3) Load “Goals” from settings table ---
        // Weight goal:
        String wg = DBManager.getSetting("weight_goal");
        if (wg != null) {
            weightGoalDashboardLabel.setText("Weight Goal: " + wg + " kg");
        } else {
            weightGoalDashboardLabel.setText("Weight Goal: not set");
        }
        // Workout goal (optional—assumes you may someday store "workout_goal"
        // similarly):
        String wkg = DBManager.getSetting("workout_goal");
        if (wkg != null) {
            workoutGoalDashboardLabel.setText("Workout Goal: " + wkg + " sessions");
        } else {
            workoutGoalDashboardLabel.setText("Workout Goal: not set");
        }

        // --- 4) Activity counts (again for last 7 / 30 days) ---
        activityWeekLabel.setText("This Week: " + count7 + " workouts");
        activityMonthLabel.setText("This Month: " + count30 + " workouts");

        // --- 5) Mini weight chart for last 7 days ---
        loadMiniWeightChart(ago7);
    }

    /**
     * Helper: run a COUNT(*) query
     */
    private int queryCount(String sql, LocalDate from, LocalDate to) {
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, from.toString());
            p.setString(2, to.toString());
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt("cnt") : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Helper: fetch the most recent weight on or before a given date
     */
    private double fetchWeightOnOrBefore(LocalDate date) {
        String wsql = """
                    SELECT weight
                      FROM weight_entries
                     WHERE date <= ?
                     ORDER BY date DESC
                     LIMIT 1
                """;
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(wsql)) {
            p.setString(1, date.toString());
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("weight");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Build the mini weight chart for entries since "since" date.
     * If no data, hide the chart and show the placeholder label.
     */
    private void loadMiniWeightChart(LocalDate since) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String sql = "SELECT date, weight FROM weight_entries WHERE date>=? ORDER BY date";
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, since.toString());
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    series.getData().add(new XYChart.Data<>(
                            rs.getString("date"),
                            rs.getDouble("weight")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (series.getData().isEmpty()) {
            miniWeightChart.setVisible(false);
            miniWeightPlaceholder.setVisible(true);
        } else {
            miniWeightPlaceholder.setVisible(false);
            miniWeightChart.setVisible(true);
            miniWeightChart.setData(FXCollections.observableArrayList(
                    java.util.Collections.singletonList(series)));
        }
    }
}
