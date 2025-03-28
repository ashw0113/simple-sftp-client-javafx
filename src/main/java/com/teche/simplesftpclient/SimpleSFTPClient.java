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
import java.util.HashMap;
import java.util.Map;

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
        Scene scene = new Scene(fxmlLoader.load(), 350, 230);
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
            session.setConfig("compression.c2s", "zlib,none");
            this.session.connect(10000); // 5 seconds timeout
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            Map<String, String> credetailMap = new HashMap<>();
            credetailMap.put("host", host);
            credetailMap.put("port", String.valueOf(port));
            credetailMap.put("username", username);
            credetailMap.put("password", password);
            Stage stage = (Stage) hostText.getScene().getWindow();
            redirectToHome(sftpChannel, stage, credetailMap); // TO-Do : Ugly!! find a better way. Credentials needed for multiple file upload/download as it thread needs its own channel.
        } catch (Exception e) {
            message.setText("Failed to connect: " + e.getMessage());
        }
    }

    private void redirectToHome(ChannelSftp sftpChannel, Stage stage, Map<String, String> credetailMap) {
        FXMLLoader fxmlLoader = null;
        Parent parent = null;
        Scene scene = null;
        try {
            fxmlLoader = new FXMLLoader(new URL("file:src/main/resources/com/teche/simplesftpclient/Home.fxml"));
            parent = fxmlLoader.load();
            Home homeController = fxmlLoader.getController();
            homeController.initialize(sftpChannel, credetailMap);
            scene = new Scene(parent, 700, 520);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage.setTitle("Home");
        stage.setScene(scene);
        try {
            Thread.currentThread().sleep(5000); // Allow home screen to load
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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