package com.sabat.deposit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/sabat/deposit/views/welcome-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);

        scene.getStylesheets().add(getClass().getResource("/com/sabat/deposit/style.css").toExternalForm());

        stage.setTitle("Вітаємо у Вкладах!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
