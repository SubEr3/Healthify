package src.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class MainController {
    @FXML
    private StackPane contentPane;
    @FXML
    private Button dashboardBtn, workoutBtn, mealBtn, weightBtn, calendarBtn;

    @FXML
    public void initialize() {
        // Default view
        switchPage("DashboardView.fxml");

        dashboardBtn.setOnAction(e -> switchPage("DashboardView.fxml"));
        workoutBtn.setOnAction(e -> switchPage("WorkoutView.fxml"));
        mealBtn.setOnAction(e -> switchPage("MealView.fxml"));
        weightBtn.setOnAction(e -> switchPage("WeightView.fxml"));
        calendarBtn.setOnAction(e -> switchPage("CalendarView.fxml"));
    }

    private void switchPage(String fxml) {
        try {
            Node node = FXMLLoader.load(getClass().getResource("/src/views/" + fxml));
            contentPane.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
