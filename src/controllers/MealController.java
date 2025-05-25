package src.controllers;

import src.database.DBManager;
import src.models.DailyCalories;
import src.models.Meal;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;

public class MealController {
    @FXML
    private DatePicker mealDate;
    @FXML
    private ComboBox<String> mealType;
    @FXML
    private TextField foodField;
    @FXML
    private TextField calField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveBtn;

    @FXML
    private TableView<Meal> mealTable;
    @FXML
    private TableColumn<Meal, String> dateCol;
    @FXML
    private TableColumn<Meal, String> typeCol;
    @FXML
    private TableColumn<Meal, String> foodCol;
    @FXML
    private TableColumn<Meal, Integer> calCol;

    @FXML
    private TableView<DailyCalories> summaryTable;
    @FXML
    private TableColumn<DailyCalories, String> summaryDateCol;
    @FXML
    private TableColumn<DailyCalories, Integer> summaryCalCol;

    private Integer editingId = null;

    @FXML
    public void initialize() {
        // bind meal table
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        foodCol.setCellValueFactory(new PropertyValueFactory<>("food"));
        calCol.setCellValueFactory(new PropertyValueFactory<>("calories"));

        // bind summary table
        summaryDateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        summaryCalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        loadMeals();
        loadDailySummary();
    }

    @FXML
    public void loadMeals() {
        ObservableList<Meal> meals = FXCollections.observableArrayList();
        String sql = "SELECT * FROM meals ORDER BY id DESC";
        try (Connection conn = DBManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                meals.add(new Meal(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getString("type"),
                        rs.getString("food"),
                        rs.getInt("calories")));
            }
            mealTable.setItems(meals);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading meals.");
        }
    }

    @FXML
    public void loadDailySummary() {
        ObservableList<DailyCalories> summary = FXCollections.observableArrayList();
        String sql = "SELECT date, SUM(calories) AS total FROM meals GROUP BY date ORDER BY date DESC";
        try (Connection conn = DBManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                summary.add(new DailyCalories(
                        rs.getString("date"),
                        rs.getInt("total")));
            }
            summaryTable.setItems(summary);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveMeal() {
        String date = mealDate.getValue() != null ? mealDate.getValue().toString() : "";
        String type = mealType.getValue();
        String food = foodField.getText();
        String calText = calField.getText();

        if (date.isEmpty() || type == null || food.isEmpty() || calText.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            int calories = Integer.parseInt(calText);
            if (editingId != null) {
                DBManager.updateMeal(editingId, date, type, food, calories);
                statusLabel.setText("Meal updated!");
                saveBtn.setText("Save Meal");
                editingId = null;
            } else {
                DBManager.insertMeal(date, type, food, calories);
                statusLabel.setText("Meal saved!");
            }
            loadMeals();
            loadDailySummary();

            mealDate.setValue(null);
            mealType.setValue(null);
            foodField.clear();
            calField.clear();
        } catch (NumberFormatException e) {
            statusLabel.setText("Calories must be a number.");
        }
    }

    @FXML
    private void handleEditSelected() {
        Meal sel = mealTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Please select a meal to edit.");
            return;
        }
        editingId = sel.getId();
        mealDate.setValue(LocalDate.parse(sel.getDate()));
        mealType.setValue(sel.getType());
        foodField.setText(sel.getFood());
        calField.setText(String.valueOf(sel.getCalories()));
        saveBtn.setText("Update Meal");
        statusLabel.setText("");
    }

    @FXML
    private void handleDeleteSelected() {
        Meal sel = mealTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Please select a meal to delete.");
            return;
        }
        DBManager.deleteMeal(sel.getId());
        loadMeals();
        loadDailySummary();
        statusLabel.setText("Meal deleted.");
    }
}
