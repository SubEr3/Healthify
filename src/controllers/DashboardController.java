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
    public void initialize() {
        LocalDate now = LocalDate.now();
        LocalDate ago7 = now.minusDays(7);
        LocalDate ago30 = now.minusDays(30);

        // workouts summary
        String countSql = "SELECT COUNT(*) AS cnt FROM workouts WHERE date BETWEEN ? AND ?";
        wk7Label.setText("Last 7 days:  " + queryCount(countSql, ago7, now) + " workouts");
        wk30Label.setText("Last 30 days: " + queryCount(countSql, ago30, now) + " workouts");

        // weight change
        double startW = fetchWeightOnOrBefore(ago30);
        double endW = fetchWeightOnOrBefore(now);
        wtChangeLabel.setText(
                String.format("%.1f → %.1f   (Δ %.1f kg)", startW, endW, endW - startW));

        // mini-chart
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
                if (rs.next())
                    return rs.getDouble("weight");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

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
            miniWeightChart.setData(FXCollections.observableArrayList(series));
        }
    }
}
