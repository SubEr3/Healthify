package src.controllers;

import src.database.DBManager;
import src.models.WeightEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.sql.*;
import java.time.LocalDate;

public class WeightController {
    @FXML
    private DatePicker weightDate;
    @FXML
    private TextField weightField;
    @FXML
    private Button saveBtn;
    @FXML
    private Label statusLabel;

    @FXML
    private TableView<WeightEntry> weightTable;
    @FXML
    private TableColumn<WeightEntry, String> wDateCol;
    @FXML
    private TableColumn<WeightEntry, Double> wValueCol;

    @FXML
    private DatePicker weightStartPicker;
    @FXML
    private DatePicker weightEndPicker;

    @FXML
    private LineChart<String, Number> weightChart;

    // Goal UI
    @FXML
    private TextField weightGoalField;
    @FXML
    private Label weightGoalLabel;
    @FXML
    private DatePicker weightGoalDatePicker;
    @FXML
    private Label weightGoalDateLabel;

    private Integer editingId = null;

    @FXML
    public void initialize() {
        // table bindings
        wDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        wValueCol.setCellValueFactory(new PropertyValueFactory<>("weight"));

        loadWeights();
        plotWeights();

        // Load goal weight from settings
        String goalWeight = DBManager.getSetting("weight_goal");
        if (goalWeight != null) {
            weightGoalLabel.setText(goalWeight + " kg");
            weightGoalField.setText(goalWeight);
        } else {
            weightGoalLabel.setText("not set");
        }

        // Load goal date from settings
        String goalDate = DBManager.getSetting("weight_goal_date");
        if (goalDate != null) {
            weightGoalDateLabel.setText("by " + goalDate);
            try {
                weightGoalDatePicker.setValue(LocalDate.parse(goalDate));
            } catch (Exception e) {
                weightGoalDatePicker.setValue(null);
            }
        } else {
            weightGoalDateLabel.setText("");
            weightGoalDatePicker.setValue(null);
        }
    }

    @FXML
    public void loadWeights() {
        ObservableList<WeightEntry> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM weight_entries ORDER BY date";
        try (Connection c = DBManager.connect();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new WeightEntry(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getDouble("weight")));
            }
            weightTable.setItems(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void plotWeights() {
        weightChart.getData().clear();

        // data series
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        double minWeight = Double.MAX_VALUE;
        double maxWeight = Double.MIN_VALUE;

        for (WeightEntry we : weightTable.getItems()) {
            double w = we.getWeight();
            series.getData().add(new XYChart.Data<>(we.getDate(), w));
            if (w < minWeight)
                minWeight = w;
            if (w > maxWeight)
                maxWeight = w;
        }
        weightChart.getData().add(series);

        // goal line
        String gw = DBManager.getSetting("weight_goal");
        if (gw != null) {
            try {
                double goal = Double.parseDouble(gw);
                XYChart.Series<String, Number> goalLine = new XYChart.Series<>();
                goalLine.setName("Goal");
                for (WeightEntry we : weightTable.getItems()) {
                    goalLine.getData().add(new XYChart.Data<>(we.getDate(), goal));
                }
                weightChart.getData().add(goalLine);
            } catch (NumberFormatException ignored) {
            }
        }

        // Zoomed-in, clean Y-axis
        if (!series.getData().isEmpty()) {
            NumberAxis yAxis = (NumberAxis) weightChart.getYAxis();
            yAxis.setAutoRanging(false);

            double range = maxWeight - minWeight;
            double padding = (range > 1.0) ? range * 0.05 : 0.5;

            double niceLower = Math.floor(minWeight - padding);
            double niceUpper = Math.ceil(maxWeight + padding);

            yAxis.setLowerBound(niceLower);
            yAxis.setUpperBound(niceUpper);

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
        } else {
            ((NumberAxis) weightChart.getYAxis()).setAutoRanging(true);
        }
    }

    @FXML
    private void handleSaveWeight() {
        String date = weightDate.getValue() != null
                ? weightDate.getValue().toString()
                : "";
        String val = weightField.getText().trim();
        if (date.isEmpty() || val.isEmpty()) {
            statusLabel.setText("Fill date and weight.");
            return;
        }
        try {
            double w = Double.parseDouble(val);
            if (editingId != null) {
                DBManager.updateWeight(editingId, date, w);
                saveBtn.setText("Save Weight");
                editingId = null;
            } else {
                DBManager.insertWeight(date, w);
            }
            loadWeights();
            plotWeights();
            weightDate.setValue(null);
            weightField.clear();
            statusLabel.setText("Saved.");
        } catch (NumberFormatException ex) {
            statusLabel.setText("Weight must be a number.");
        }
    }

    @FXML
    private void handleSetWeightGoal() {
        String txt = weightGoalField.getText().trim();
        LocalDate date = weightGoalDatePicker.getValue();

        boolean goalSet = false;
        boolean dateSet = false;

        if (txt != null && !txt.isEmpty()) {
            try {
                Double.parseDouble(txt); // validate
                DBManager.setSetting("weight_goal", txt);
                weightGoalLabel.setText(txt + " kg");
                statusLabel.setText("Target weight set to " + txt + " kg");
                plotWeights();
                goalSet = true;
            } catch (NumberFormatException e) {
                statusLabel.setText("Please enter a valid number.");
            }
        }

        if (date != null) {
            DBManager.setSetting("weight_goal_date", date.toString());
            weightGoalDateLabel.setText("by " + date.toString());
            if (goalSet) {
                statusLabel.setText("Target weight and date set!");
            } else {
                statusLabel.setText("Target date set!");
            }
            dateSet = true;
        } else {
            // Optionally clear the date label if user removes the date
            weightGoalDateLabel.setText("");
        }

        if (!goalSet && !dateSet) {
            statusLabel.setText("Please enter a valid goal and/or pick a date.");
        }
    }

    @FXML
    private void handleEditSelected() {
        WeightEntry sel = weightTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Please select an entry to edit.");
            return;
        }
        editingId = sel.getId();
        weightDate.setValue(LocalDate.parse(sel.getDate()));
        weightField.setText(String.valueOf(sel.getWeight()));
        saveBtn.setText("Update Weight");
        statusLabel.setText("");
    }

    @FXML
    private void handleDeleteSelected() {
        WeightEntry sel = weightTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Please select an entry to delete.");
            return;
        }
        DBManager.deleteWeight(sel.getId());
        loadWeights();
        plotWeights();
        statusLabel.setText("Entry deleted.");
    }

    @FXML
    private void handleFilterWeights() {
        LocalDate start = weightStartPicker.getValue();
        LocalDate end = weightEndPicker.getValue();

        ObservableList<WeightEntry> filtered = FXCollections.observableArrayList();

        for (WeightEntry entry : weightTable.getItems()) {
            LocalDate entryDate = LocalDate.parse(entry.getDate());
            boolean afterStart = (start == null) || !entryDate.isBefore(start);
            boolean beforeEnd = (end == null) || !entryDate.isAfter(end);
            if (afterStart && beforeEnd) {
                filtered.add(entry);
            }
        }
        weightTable.setItems(filtered);
        plotWeights();
        statusLabel.setText("Filtered by date.");
    }

    @FXML
    private void handleClearFilterWeights() {
        weightStartPicker.setValue(null);
        weightEndPicker.setValue(null);
        loadWeights();
        plotWeights();
        statusLabel.setText("Filter cleared.");
    }
}
