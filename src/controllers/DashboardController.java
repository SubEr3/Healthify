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
    private Label dailyStreakLabel;
    @FXML
    private Label weeklyStreakLabel;

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
        dailyStreakLabel.setText("Daily Streak: " + computeDailyStreak() + " days");
        weeklyStreakLabel.setText("Weekly Streak: " + computeWeeklyStreak() + " weeks");

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

        // Mini chart (last 30 days)
        loadMiniWeightChart(ago30);
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

            // --- Y-Axis: always clean .0 bounds! ---
            NumberAxis yAxis = (NumberAxis) miniWeightChart.getYAxis();
            yAxis.setAutoRanging(false);

            double range = maxWeight - minWeight;
            double padding = (range > 1.0) ? range * 0.05 : 0.5;

            // Calculate lower and upper, rounded to .0
            double rawLower = minWeight - padding;
            double rawUpper = maxWeight + padding;

            double niceLower = Math.floor(rawLower);
            double niceUpper = Math.ceil(rawUpper);

            yAxis.setLowerBound(niceLower);
            yAxis.setUpperBound(niceUpper);

            // Choose a “nice” tick unit (0.5, 1.0, 2.0, etc.)
            double totalRange = niceUpper - niceLower;
            double[] candidates = { 0.5, 1.0, 2.0, 5.0, 10.0 };
            double bestTick = 1.0;
            for (double candidate : candidates) {
                if (totalRange / candidate <= 10) {
                    bestTick = candidate;
                    break;
                }
            }
            yAxis.setTickUnit(bestTick);
        }
    }

    private int computeDailyStreak() {
        // Returns count of consecutive days up to today with at least one workout each
        // day
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(
                        "SELECT DISTINCT date FROM workouts WHERE date <= ? ORDER BY date DESC")) {
            p.setString(1, LocalDate.now().toString());
            try (ResultSet rs = p.executeQuery()) {
                int streak = 0;
                LocalDate expected = LocalDate.now();
                while (rs.next()) {
                    LocalDate workoutDate = LocalDate.parse(rs.getString("date"));
                    if (workoutDate.equals(expected)) {
                        streak++;
                        expected = expected.minusDays(1);
                    } else if (workoutDate.isBefore(expected)) {
                        // Streak broken
                        break;
                    }
                    // (If workoutDate is after expected, keep checking, but this shouldn't happen)
                }
                return streak;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int computeWeeklyStreak() {
        // Returns count of consecutive weeks (ending this week) with at least one
        // workout per week
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(
                        "SELECT DISTINCT strftime('%Y-%W', date) as year_week FROM workouts WHERE date <= ? ORDER BY year_week DESC")) {
            p.setString(1, LocalDate.now().toString());
            try (ResultSet rs = p.executeQuery()) {
                int streak = 0;
                LocalDate now = LocalDate.now();
                int currentYear = now.getYear();
                int currentWeek = now.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                while (rs.next()) {
                    String[] yw = rs.getString("year_week").split("-");
                    int y = Integer.parseInt(yw[0]);
                    int w = Integer.parseInt(yw[1]);
                    if (y == currentYear && w == currentWeek) {
                        streak++;
                    } else if (y == currentYear && w == currentWeek - 1) {
                        streak++;
                        currentWeek--;
                    } else if (w == 52 && y == currentYear - 1 && currentWeek == 1) {
                        // Year rollover
                        streak++;
                        currentYear--;
                        currentWeek = 52;
                    } else {
                        break;
                    }
                    currentWeek--;
                    if (currentWeek < 1) {
                        currentWeek = 52;
                        currentYear--;
                    }
                }
                return streak;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

}
