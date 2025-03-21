module com.teche.simplesftpclient {
 requires javafx.controls;
 requires javafx.fxml;
 requires com.jcraft.jsch;
 opens com.teche.simplesftpclient to javafx.fxml;
 exports com.teche.simplesftpclient;
 exports com.teche.simplesftpclient.controllers;
 opens com.teche.simplesftpclient.controllers to javafx.fxml;
}