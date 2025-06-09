package src.controllers;

import src.database.DBManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import javafx.collections.FXCollections;

public class DashboardController {
    @FXML
    private Label wk7Label;
    @FXML
    private Label wk30Label;
    @FXML
    private Label wtChangeLabel;
    @FXML
    private LineChart<String, Number> miniWeightChart;
    @FXML
    private Label miniWeightPlaceholder;
    @FXML
    private Label weightGoalDashboardLabel;
    @FXML
    private Label activityWeekLabel;
    @FXML
    private Label activityMonthLabel;

    @FXML
    public void initialize() {
        LocalDate now = LocalDate.now();
        LocalDate ago7 = now.minusDays(6);
        LocalDate ago30 = now.minusDays(29);

        // Workouts summary
        String countSql = "SELECT COUNT(*) AS cnt FROM workouts WHERE date BETWEEN ? AND ?";
        wk7Label.setText("Last 7 days:  " + queryCount(countSql, ago7, now) + " workouts");
        wk30Label.setText("Last 30 days: " + queryCount(countSql, ago30, now) + " workouts");

        // Weight change (use first-ever weight)
        double startW = fetchEarliestWeight();
        double endW = fetchWeightOnOrBefore(now);
        wtChangeLabel.setText(
                String.format("%.1f → %.1f   (Δ %.1f kg)", startW, endW, endW - startW));

        // GOALS SECTION (weight only)
        String weightGoal = DBManager.getSetting("weight_goal");
        if (weightGoal != null && !weightGoal.isEmpty()) {
            weightGoalDashboardLabel.setText("Weight Goal: " + weightGoal + " kg");
        } else {
            weightGoalDashboardLabel.setText("Weight Goal: -");
        }

        // ACTIVITY SECTION
        int workoutsThisWeek = queryCount(
                "SELECT COUNT(*) AS cnt FROM workouts WHERE date BETWEEN ? AND ?",
                ago7, now);

        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        int workoutsThisMonth = queryCount(
                "SELECT COUNT(*) AS cnt FROM workouts WHERE date BETWEEN ? AND ?",
                firstDayOfMonth, now);

        activityWeekLabel.setText("This Week: " + workoutsThisWeek + " workouts");
        activityMonthLabel.setText("This Month: " + workoutsThisMonth + " workouts");

        // Mini chart (last 7 days)
        loadMiniWeightChart(ago7);
    }

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

    private double fetchEarliestWeight() {
        String wsql = "SELECT weight FROM weight_entries ORDER BY date ASC LIMIT 1";
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(wsql);
                ResultSet rs = p.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("weight");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

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

    private void loadMiniWeightChart(LocalDate since) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        double minWeight = Double.MAX_VALUE;
        double maxWeight = Double.MIN_VALUE;

        String sql = "SELECT date, weight FROM weight_entries WHERE date>=? ORDER BY date";
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, since.toString());
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    String dateString = rs.getString("date");
                    double w = rs.getDouble("weight");
                    series.getData().add(new XYChart.Data<>(dateString, w));

                    if (w < minWeight)
                        minWeight = w;
                    if (w > maxWeight)
                        maxWeight = w;
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

            // Add series to chart
            miniWeightChart.setData(FXCollections.singletonObservableList(series));

            // “Zoom in” the Y-axis around [minWeight, maxWeight]
            NumberAxis yAxis = (NumberAxis) miniWeightChart.getYAxis();
            yAxis.setAutoRanging(false);

            double range = maxWeight - minWeight;
            double padding = (range > 1.0) ? range * 0.05 : 0.5;

            yAxis.setLowerBound(minWeight - padding);
            yAxis.setUpperBound(maxWeight + padding);

            // Choose a reasonable tick unit (here: split padded range into 5)
            double totalRange = (maxWeight + padding) - (minWeight - padding);
            yAxis.setTickUnit(totalRange / 5.0);
        }
    }
}
