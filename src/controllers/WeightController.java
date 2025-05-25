package src.controllers;

import src.database.DBManager;
import src.models.Meal;
import src.models.WeightEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import java.sql.*;
import java.time.LocalDate;

public class WeightController {
    @FXML
    private DatePicker weightDate;
    @FXML
    private TextField weightField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveBtn;

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

    private Integer editingId = null;

    @FXML
    public void initialize() {
        wDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        wValueCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
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
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (WeightEntry we : weightTable.getItems()) {
            series.getData().add(new XYChart.Data<>(we.getDate(), we.getWeight()));
        }
        weightChart.getData().add(series);
    }

    @FXML
    private void handleSaveWeight() {
        String date = weightDate.getValue() != null ? weightDate.getValue().toString() : "";
        String val = weightField.getText();
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
    private void handleEditSelected() {
        WeightEntry sel = weightTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Select an entry.");
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
            statusLabel.setText("Select to delete.");
            return;
        }
        DBManager.deleteWeight(sel.getId());
        loadWeights();
        plotWeights();
        statusLabel.setText("Deleted.");
    }

    @FXML
    private void handleFilterWeights() {
        LocalDate start = weightStartPicker.getValue();
        LocalDate end = weightEndPicker.getValue();
        if (start == null || end == null) {
            statusLabel.setText("Please select both start and end dates.");
            return;
        }
        if (end.isBefore(start)) {
            statusLabel.setText("End date cannot be before start date.");
            return;
        }

        ObservableList<WeightEntry> filtered = FXCollections.observableArrayList();
        String sql = "SELECT * FROM weight_entries WHERE date BETWEEN ? AND ? ORDER BY date";
        try (Connection c = DBManager.connect();
                PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, start.toString());
            p.setString(2, end.toString());
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    filtered.add(new WeightEntry(
                            rs.getInt("id"),
                            rs.getString("date"),
                            rs.getDouble("weight")));
                }
            }
            weightTable.setItems(filtered);
            plotWeights(); // re-plot the chart with filtered data
            statusLabel.setText("Showing " + filtered.size() + " entries from " + start + " to " + end + ".");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error filtering weights.");
        }
    }

    @FXML
    private void handleClearFilterWeights() {
        weightStartPicker.setValue(null);
        weightEndPicker.setValue(null);
        loadWeights();
        plotWeights();
        statusLabel.setText("");
    }

}