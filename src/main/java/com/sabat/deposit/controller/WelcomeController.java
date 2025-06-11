package com.sabat.deposit.controller;

import com.sabat.deposit.navigation.NavigationManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.net.URL;

public class WelcomeController {

    @FXML
    ImageView logoImage;

    @FXML
    public void initialize() {
        logoImage.setImage(new Image(getClass().getResource("/com/sabat/deposit/images/reg.png").toExternalForm()));
    }


    @FXML
    protected void onStartClick(ActionEvent event) throws IOException {
        NavigationManager.switchScene(event, "/com/sabat/deposit/views/auth-view.fxml");


    }
}
