package src.controllers;

import src.database.DBManager;
import src.models.Workout;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.*;
import java.time.LocalDate;

public class WorkoutController {

    @FXML private TextField exerciseField;
    @FXML private TextField setsField;
    @FXML private TextField repsField;
    @FXML private DatePicker datePicker;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    @FXML private TableView<Workout> workoutTable;
    @FXML private TableColumn<Workout, String> dateColumn;
    @FXML private TableColumn<Workout, String> exerciseColumn;
    @FXML private TableColumn<Workout, Integer> setsColumn;
    @FXML private TableColumn<Workout, Integer> repsColumn;

    private Integer editingId = null;

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        exerciseColumn.setCellValueFactory(new PropertyValueFactory<>("exercise"));
        setsColumn.setCellValueFactory(new PropertyValueFactory<>("sets"));
        repsColumn.setCellValueFactory(new PropertyValueFactory<>("reps"));
        loadWorkouts();
    }

    @FXML
    public void loadWorkouts() {
        ObservableList<Workout> workouts = FXCollections.observableArrayList();
        String sql = "SELECT id, date, exercise, sets, reps FROM workouts ORDER BY id DESC";
        try (Connection conn = DBManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                workouts.add(new Workout(
                    rs.getInt("id"),
                    rs.getString("date"),
                    rs.getString("exercise"),
                    rs.getInt("sets"),
                    rs.getInt("reps")
                ));
            }
            workoutTable.setItems(workouts);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveWorkout() {
        String exercise = exerciseField.getText().trim();
        String setsText = setsField.getText().trim();
        String repsText = repsField.getText().trim();
        String date     = datePicker.getValue() != null
                        ? datePicker.getValue().toString()
                        : "";

        if (exercise.isEmpty() || setsText.isEmpty() || repsText.isEmpty() || date.isEmpty()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        try {
            int sets = Integer.parseInt(setsText);
            int reps = Integer.parseInt(repsText);

            if (editingId != null) {
                DBManager.updateWorkout(editingId, date, exercise, sets, reps);
                statusLabel.setText("Workout updated!");
                saveButton.setText("Save Workout");
                editingId = null;
            } else {
                DBManager.insertWorkout(date, exercise, sets, reps);
                statusLabel.setText("Workout saved!");
            }

            workoutTable.getItems().clear();
            loadWorkouts();

            exerciseField.clear();
            setsField.clear();
            repsField.clear();
            datePicker.setValue(null);

        } catch (NumberFormatException e) {
            statusLabel.setText("Sets and reps must be numbers.");
        }
    }

    @FXML
    private void handleEditSelected() {
        Workout sel = workoutTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Please select a workout to edit.");
            return;
        }
        editingId = sel.getId();
        exerciseField.setText(sel.getExercise());
        setsField.setText(String.valueOf(sel.getSets()));
        repsField.setText(String.valueOf(sel.getReps()));
        datePicker.setValue(LocalDate.parse(sel.getDate()));
        saveButton.setText("Update Workout");
    }

    @FXML
    private void handleDeleteSelected() {
        Workout sel = workoutTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            statusLabel.setText("Please select a workout to delete.");
            return;
        }
        DBManager.deleteWorkout(sel.getId());
        loadWorkouts();
        statusLabel.setText("Workout deleted.");
    }
}
