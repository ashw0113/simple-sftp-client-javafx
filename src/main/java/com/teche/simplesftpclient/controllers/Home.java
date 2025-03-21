package com.teche.simplesftpclient.controllers;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.teche.simplesftpclient.model.FileObj;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.Vector;

public class Home {
    @FXML
    public VBox homeVBox;
    @FXML
    private TreeView<FileObj> remoteTree;
    @FXML
    private TextField searchText;
    @FXML
    private Button searchButton;
    @FXML
    private TextArea fileContent;
    @FXML
    private Button closeFileButton;
    @FXML
    private Button closeFindButton;

    private ChannelSftp sftpChannel;
    String lastSearched = null;
    int lastSearchIndex = -1;
    TreeItem<FileObj> selectedItem;


    public void setSftpChannel(ChannelSftp sftpChannel) {
        this.sftpChannel = sftpChannel;
        TreeItem<FileObj> rootItem = null;
        try {
            FileObj fileObj = FileObj.getFileObj();
            fileObj.setPath(sftpChannel.getHome().toString());
            fileObj.setName("Home");
            fileObj.setDirectory(true);
            rootItem = new TreeItem<>(fileObj);
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
        populateDirectoryTree(sftpChannel, rootItem, ".");
        remoteTree.setShowRoot(false);
        remoteTree.setEditable(true);
        remoteTree.setRoot(rootItem);
        homeVBox.setVgrow(remoteTree, Priority.ALWAYS); // to streth with the screen size
    }

    private void populateDirectoryTree(ChannelSftp sftpChannel, TreeItem<FileObj> parentItem, String path) {
        try {
            Vector<ChannelSftp.LsEntry> ls = sftpChannel.ls(path);
            for (ChannelSftp.LsEntry entry : ls) {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    if (entry.getAttrs().isDir()) {
                        FileObj file = FileObj.getFileObj();
                        file.setName(entry.getFilename());
                        file.setPath(parentItem.getValue().getPath() + "/" + entry.getFilename());
                        file.setDirectory(true);
                        TreeItem<FileObj> item = new TreeItem<>(file, new ImageView("file:src/main/resources/icons/folder.png"));
                        parentItem.getChildren().add(item);
                        //populateSecondLevelTree(sftpChannel, item, path + "/" + entry.getFilename());
                    } else {
                        FileObj file = FileObj.getFileObj();
                        file.setName(entry.getFilename());
                        file.setPath(parentItem.getValue().getPath() + "/" + entry.getFilename());
                        file.setDirectory(false);
                        TreeItem<FileObj> item = new TreeItem<>(file, new ImageView("file:src/main/resources/icons/file.png"));
                        parentItem.getChildren().add(item);
                    }
                }
            }
            parentItem.setExpanded(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showSelectedItem(MouseEvent event) {
        if (event.getClickCount() == 2) {
            selectedItem = remoteTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.getValue().isDirectory()) {
                    populateDirectoryTree(sftpChannel, selectedItem, selectedItem.getValue().getPath());
                } else {
                    fileContent.setWrapText(true);
                    fileContent.clear();
                    fileContent.setEditable(true);
                    if (selectedItem.getValue().getPath().endsWith(".txt") ||
                            selectedItem.getValue().getPath().endsWith(".sh") ||
                            selectedItem.getValue().getPath().endsWith(".log") ||
                            selectedItem.getValue().getPath().endsWith(".xml") ||
                            selectedItem.getValue().getPath().endsWith(".properties") ||
                            selectedItem.getValue().getPath().endsWith(".json")) {
                        try {
                            InputStream inputStream = sftpChannel.get(selectedItem.getValue().getPath());
                            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                            while (scanner.hasNext()) {
                                String fileContent = scanner.next();
                                this.fileContent.appendText(fileContent);
                            }
                            closeFileButton.setVisible(true);
                        } catch (SftpException e) {
                            e.printStackTrace();
                        }

                    } else {
                        fileContent.clear();
                        fileContent.setText("File type not supported");
                    }
                }
            }
        }
    }

    public void exit(ActionEvent actionEvent) {
        if (sftpChannel != null) {
            sftpChannel.disconnect();
        }
        System.exit(0);
    }

    public void keyPressedFunctions(KeyEvent keyEvent) {
        if (keyEvent.isControlDown() && keyEvent.getCode().getName().equals("S") || keyEvent.getCode().equals("s")) {
            System.out.println("Save file");
            try {
                InputStream inputStream = new ByteArrayInputStream(fileContent.getText().getBytes(StandardCharsets.UTF_8));
                sftpChannel.put(inputStream, selectedItem.getValue().getPath());
                System.out.println("File saved");
            } catch (SftpException e) {
                System.out.println("Failed to save file");
                throw new RuntimeException(e);
            }
        }
        if (keyEvent.isControlDown() && keyEvent.getCode().getName().equals("F") || keyEvent.getCode().equals("f")) {
            searchText.setVisible(true);
            searchButton.setVisible(true);
            closeFindButton.setVisible(true);
        }
    }

    public void find(ActionEvent actionEvent) {
        String text = searchText.getText();
        int index = -1;
        String fileContent = this.fileContent.getText();

        if (text != null && !text.isBlank() && text.equals(lastSearched)) {
            index = fileContent.indexOf(text, lastSearchIndex + 1);
            lastSearchIndex = index;
        } else if ((fileContent != null && !fileContent.isBlank()) && (text != null && !text.isBlank())) {
            index = fileContent.indexOf(text);
            lastSearchIndex = index;
            lastSearched = text;
        }
        if (index != -1) {
            this.fileContent.selectRange(index, index + text.length());
        }
    }

    public void closeFile(ActionEvent actionEvent) {
        fileContent.clear();
        fileContent.setEditable(false);
        closeFileButton.setVisible(false);
    }

    public void searchTextKeyPressedEvent(KeyEvent keyEvent) {
        if (keyEvent.getText().isBlank()) {
            searchButton.setDisable(true);
        } else {
            searchButton.setDisable(false);
        }
    }

    public void closeFind(ActionEvent actionEvent) {
        searchText.clear();
        searchText.setVisible(false);
        searchButton.setVisible(false);
        closeFindButton.setVisible(false);
    }

    public void remoteTreeDragEntered(MouseDragEvent mouseDragEvent) {


    }

    public void copy(ActionEvent actionEvent) {

    }

    public void paste(ActionEvent actionEvent) {

    }

    public void about(ActionEvent actionEvent) {

    }
}
