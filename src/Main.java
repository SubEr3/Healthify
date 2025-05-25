package src;

import src.database.DBManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        DBManager.initializeDatabase();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/src/views/MainView.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setTitle("Healthify - Health Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
