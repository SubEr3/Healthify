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

    // **Goal UI**
    @FXML
    private TextField weightGoalField;
    @FXML
    private Label weightGoalLabel;

    private Integer editingId = null;

    @FXML
    public void initialize() {
        // table bindings
        wDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        wValueCol.setCellValueFactory(new PropertyValueFactory<>("weight"));

        // load goal from settings
        String gw = DBManager.getSetting("weight_goal");
        if (gw != null) {
            weightGoalLabel.setText(gw + " kg");
        }

        loadWeights();
        plotWeights();
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

        // **goal line**
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

        // --- Zoomed-in, clean Y-axis ---
        if (!series.getData().isEmpty()) {
            // Get the Y axis
            NumberAxis yAxis = (NumberAxis) weightChart.getYAxis();
            yAxis.setAutoRanging(false);

            double range = maxWeight - minWeight;
            double padding = (range > 1.0) ? range * 0.05 : 0.5;

            // Clean lower/upper bound (ends with .0)
            double niceLower = Math.floor(minWeight - padding);
            double niceUpper = Math.ceil(maxWeight + padding);

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
        } else {
            // fallback
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
        try {
            Double.parseDouble(txt); // validate
            DBManager.setSetting("weight_goal", txt);
            weightGoalLabel.setText(txt + " kg");
            statusLabel.setText("Target weight set to " + txt + " kg");
            plotWeights();
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid number.");
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
        /* … existing … */ }

    @FXML
    private void handleClearFilterWeights() {
        /* … existing … */ }
}
