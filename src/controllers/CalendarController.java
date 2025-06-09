package src.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.util.*;
import src.database.DBManager;
import java.sql.*;

public class CalendarController {
    @FXML
    private Label monthLabel;
    @FXML
    private Button prevMonthBtn, nextMonthBtn;
    @FXML
    private GridPane calendarGrid;
    @FXML
    private ListView<String> entryListView;

    private YearMonth currentMonth;

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        drawCalendar();

        prevMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            drawCalendar();
        });
        nextMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            drawCalendar();
        });
    }

    private void drawCalendar() {
        calendarGrid.getChildren().clear();
        entryListView.getItems().clear();

        monthLabel.setText(currentMonth.getMonth().toString() + " " + currentMonth.getYear());

        // Add day-of-week labels
        String[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(days[i]);
            lbl.setStyle("-fx-font-weight: bold;");
            calendarGrid.add(lbl, i, 0);
        }

        // Get all entries for this month (to color the cells)
        Map<LocalDate, List<String>> entriesByDay = loadEntriesForMonth();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeekIndex = firstOfMonth.getDayOfWeek().getValue() - 1; // Monday=0, Sunday=6
        int row = 1;
        int col = dayOfWeekIndex;

        int daysInMonth = currentMonth.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            Button dayBtn = new Button(String.valueOf(day));
            dayBtn.setPrefSize(36, 36);

            if (entriesByDay.containsKey(date)) {
                dayBtn.setStyle("-fx-background-color: #9be7a4;");
            } else {
                dayBtn.setStyle("-fx-background-color: #eeeeee;");
            }

            // Optional: Highlight today
            if (date.equals(LocalDate.now())) {
                dayBtn.setStyle(dayBtn.getStyle() + "; -fx-border-color: #1976d2; -fx-border-width: 2;");
            }

            dayBtn.setOnAction(e -> {
                showEntriesForDate(date, entriesByDay.getOrDefault(date, new ArrayList<>()));
            });

            calendarGrid.add(dayBtn, col, row);
            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private void showEntriesForDate(LocalDate date, List<String> entries) {
        entryListView.getItems().clear();
        if (entries.isEmpty()) {
            entryListView.getItems().add("No entries.");
        } else {
            entryListView.getItems().addAll(entries);
        }
    }

    // Load all workout/meal/weight entries for current month, grouped by day
    private Map<LocalDate, List<String>> loadEntriesForMonth() {
        Map<LocalDate, List<String>> map = new HashMap<>();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        try (Connection c = DBManager.connect()) {
            // Workouts
            String ws = "SELECT date, exercise, sets, reps FROM workouts WHERE date BETWEEN ? AND ?";
            try (PreparedStatement p = c.prepareStatement(ws)) {
                p.setString(1, start.toString());
                p.setString(2, end.toString());
                try (ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        LocalDate d = LocalDate.parse(rs.getString("date"));
                        String text = "[Workout] " + rs.getString("exercise")
                                + " (" + rs.getInt("sets") + "x" + rs.getInt("reps") + ")";
                        map.computeIfAbsent(d, k -> new ArrayList<>()).add(text);
                    }
                }
            }
            // Meals
            String ms = "SELECT date, type, food, calories FROM meals WHERE date BETWEEN ? AND ?";
            try (PreparedStatement p = c.prepareStatement(ms)) {
                p.setString(1, start.toString());
                p.setString(2, end.toString());
                try (ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        LocalDate d = LocalDate.parse(rs.getString("date"));
                        String text = "[Meal] " + rs.getString("type") + ": " + rs.getString("food")
                                + " (" + rs.getInt("calories") + " kcal)";
                        map.computeIfAbsent(d, k -> new ArrayList<>()).add(text);
                    }
                }
            }
            // Weights
            String ws2 = "SELECT date, weight FROM weight_entries WHERE date BETWEEN ? AND ?";
            try (PreparedStatement p = c.prepareStatement(ws2)) {
                p.setString(1, start.toString());
                p.setString(2, end.toString());
                try (ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        LocalDate d = LocalDate.parse(rs.getString("date"));
                        String text = "[Weight] " + rs.getDouble("weight") + " kg";
                        map.computeIfAbsent(d, k -> new ArrayList<>()).add(text);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}
