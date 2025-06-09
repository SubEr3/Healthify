package src.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

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
            Node newNode = FXMLLoader.load(getClass().getResource("/src/views/" + fxml));
            Node oldNode = contentPane.getChildren().isEmpty() ? null : contentPane.getChildren().get(0);

            if (oldNode != null) {
                // Fade out the old node
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), oldNode);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    // After fade out, replace with the new node
                    contentPane.getChildren().setAll(newNode);

                    // Fade in the new node
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newNode);
                    newNode.setOpacity(0);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                });
                fadeOut.play();
            } else {
                // If no node, just show new with fade in
                newNode.setOpacity(0);
                contentPane.getChildren().setAll(newNode);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newNode);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
