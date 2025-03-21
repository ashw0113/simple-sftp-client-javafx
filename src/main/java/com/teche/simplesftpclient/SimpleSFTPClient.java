package com.teche.simplesftpclient;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.teche.simplesftpclient.controllers.Home;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SimpleSFTPClient extends Application {

    @FXML
    private TextField hostText;
    @FXML
    private TextField portText;
    @FXML
    private TextField usernameText;
    @FXML
    private TextField passwordText;
    @FXML
    private Label message;

    private Session session = null;
    private ChannelSftp sftpChannel = null;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleSFTPClient.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 450, 600);
        stage.setScene(scene);
        stage.show();
    }

    public void login(ActionEvent actionEvent) {
        String host = hostText.getText();
        int port = Integer.parseInt(portText.getText());
        String username = usernameText.getText();
        String password = passwordText.getText();
        JSch jsch = new JSch();
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            this.session.connect(10000); // 5 seconds timeout
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            Stage stage = (Stage) hostText.getScene().getWindow();
            redirectToHome(sftpChannel, stage);
        } catch (Exception e) {
            message.setText("Failed to connect: " + e.getMessage());
        }
        System.out.println("Login button clicked!");
    }

    private void redirectToHome(ChannelSftp sftpChannel, Stage stage) {
        FXMLLoader fxmlLoader = null;
        Parent parent = null;
        Scene scene = null;
        try {
            fxmlLoader = new FXMLLoader(new URL("file:src/main/resources/com/teche/simplesftpclient/Home.fxml"));
            parent = fxmlLoader.load();
            Home homeController = fxmlLoader.getController();
            homeController.setSftpChannel(sftpChannel);
            scene = new Scene(parent, 640, 480);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage.setTitle("Home");
        stage.setScene(scene);
        stage.show();
    }

    public void stop() throws Exception {
        System.out.println("Closing SFTP connection");
        if (sftpChannel != null) {
            sftpChannel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
        System.out.println("SFTP connection closed");
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }
}