package com.teche.simplesftpclient.controllers;

import com.jcraft.jsch.*;
import com.teche.simplesftpclient.model.FileObj;
import com.teche.simplesftpclient.utils.Utils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Home {
    @FXML
    private VBox homeVBox;
    @FXML
    private HBox menuHBox;
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

    ContextMenu contextMenu;

    private ChannelSftp sftpChannel;
    String lastSearched = null;
    int lastSearchIndex = -1;
    TreeItem<FileObj> selectedItem;
    private Map<String, String> credetailMap;

    public void initialize(ChannelSftp sftpChannel, Map<String, String> credetailMap) {
        this.sftpChannel = sftpChannel;
        this.credetailMap = credetailMap;
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
        Task<Void> task = new Task<>() {
            List<TreeItem<FileObj>> items = new Vector<>();
            @Override
            protected Void call() throws Exception {
                ChannelSftp channel = null;
                try {
                    if (sftpChannel == null) {
                        channel = Utils.getRemoteSession(credetailMap);
                    } else {
                        channel = sftpChannel;
                    }
                    final Vector<FileObj> v = new Vector();
                    ChannelSftp.LsEntrySelector selector = new ChannelSftp.LsEntrySelector() {
                        public int select(ChannelSftp.LsEntry entry) {
                            v.addElement(new FileObj(entry.getFilename(), parentItem.getValue().getPath() + "/" + entry.getFilename(), entry.getAttrs().isDir()));
                            return 0;
                        }
                    };
                    channel.ls(path, selector);
                    for (FileObj entry : v) {
                        if (!entry.getName().equals(".") && !entry.getName().equals("..")) {
                            if (entry.isDirectory()) {
                                TreeItem<FileObj> item = new TreeItem<>(entry, new ImageView("file:src/main/resources/icons/folder.png"));
                                items.add(item);
                            } else {
                                TreeItem<FileObj> item = new TreeItem<>(entry, new ImageView("file:src/main/resources/icons/file.png"));
                                items.add(item);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                Platform.runLater(() -> {
                    parentItem.getChildren().clear();
                    parentItem.getChildren().addAll(items);
                    parentItem.setExpanded(true);
                    items.clear();
                });
            }
        };
        Thread.ofVirtual().start(task);
    }

    public void showSelectedItem(MouseEvent event) {
        selectedItem = remoteTree.getSelectionModel().getSelectedItem();
        if (selectedItem != null && event.getButton() == MouseButton.SECONDARY && selectedItem.getValue().isDirectory()) {
            if (contextMenu == null) {
                contextMenu = new ContextMenu();
            } else {
                contextMenu.getItems().clear();
            }
            MenuItem uploadFiles = new MenuItem("Upload File");
            MenuItem uploadDirectory = new MenuItem("Upload Directory");
            MenuItem moveItem = new MenuItem("Move");
            MenuItem download = new MenuItem("Download");
            uploadFiles.setOnAction(this::uploadFiles);
            uploadDirectory.setOnAction(this::uploadDirectory);
            moveItem.setOnAction(this::move);
            download.setOnAction(this::download);
            contextMenu.getItems().addAll(uploadFiles, uploadDirectory, moveItem, download);
            contextMenu.show(remoteTree, event.getScreenX(), event.getScreenY());
        } else if (selectedItem != null && event.getButton() == MouseButton.SECONDARY && !selectedItem.getValue().isDirectory()) {
            if (contextMenu == null) {
                contextMenu = new ContextMenu();
            } else {
                contextMenu.getItems().clear();
            }
            MenuItem moveItem = new MenuItem("Move");
            MenuItem download = new MenuItem("Download");
            moveItem.setOnAction(this::move);
            download.setOnAction(this::download);
            contextMenu.getItems().addAll(moveItem, download);
            contextMenu.show(remoteTree, event.getScreenX(), event.getScreenY());
        } else if (event.getClickCount() == 2) {
            selectedItem = remoteTree.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                if (selectedItem.getValue().isDirectory()) {
                    populateDirectoryTree(null, selectedItem, selectedItem.getValue().getPath());
                } else {
                    fileContent.setWrapText(true);
                    fileContent.clear();
                    fileContent.setEditable(true);
                    if (selectedItem.getValue().getPath().endsWith(".txt") || selectedItem.getValue().getPath().endsWith(".sh") || selectedItem.getValue().getPath().endsWith(".log") || selectedItem.getValue().getPath().endsWith(".xml") || selectedItem.getValue().getPath().endsWith(".properties") || selectedItem.getValue().getPath().endsWith(".json")) {
                        try {
                            ChannelSftp channel = Utils.getRemoteSession(credetailMap);
                            InputStream inputStream = channel.get(selectedItem.getValue().getPath());
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

    private void download(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        Label downloadLabel = new Label();
        ProgressBar downloadProgressBar = new ProgressBar();
        directoryChooser.setTitle("Select Destination Directory");
        File file = directoryChooser.showDialog(remoteTree.getScene().getWindow());
        if (file != null) {
            String localPath = file.getAbsolutePath() + File.separator + selectedItem.getValue().getName();
            TreeItem<FileObj> downloadItem = remoteTree.getSelectionModel().getSelectedItem();
            downloadLabel.setText("Downloading..." + downloadItem.getValue().getName());
            downloadLabel.setVisible(true);
            downloadProgressBar.setVisible(true);
            menuHBox.getChildren().add(downloadProgressBar);
            menuHBox.getChildren().add(downloadLabel);
            Task<Void> downloadTask = new Task<>() {
                @Override
                protected Void call() {
                    String remotePath = downloadItem.getValue().getPath();
                    downloadDirectory(remotePath, localPath);
                    return null;
                }

                private void downloadDirectory(String remotePath, String localPath) {
                    ChannelSftp downloadChannelSftp = Utils.getRemoteSession(credetailMap);
                    Path path = Path.of(localPath);
                    if (!Files.exists(path)) {
                        try {
                            Files.createDirectories(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    Vector<ChannelSftp.LsEntry> ls;
                    try {
                        ls = downloadChannelSftp.ls(remotePath);
                    } catch (SftpException e) {
                        throw new RuntimeException(e);
                    }
                    Map<String,String> downloadFileMap = new HashMap<>();
                    for (ChannelSftp.LsEntry entry : ls) {
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                            String remoteFilePath = remotePath + "/" + entry.getFilename();
                            String localFilePath = localPath + "/" + entry.getFilename();
                            if (entry.getAttrs().isDir()) {
                                Thread.ofVirtual().start(() -> {
                                    downloadDirectory(remoteFilePath, localFilePath);
                                });
                            } else {
                                downloadFileMap.put(remoteFilePath, localFilePath);
                            }
                        }
                    }
                    downloadFile(downloadFileMap);
                }

                private void downloadFile(Map<String, String> downloadFileMap) {
                    ChannelSftp downloadChannelSftp = (ChannelSftp) Utils.getRemoteSession(credetailMap);
                    for (Map.Entry<String, String> entry : downloadFileMap.entrySet()) {
                        String remotePath = entry.getKey();
                        String localPath = entry.getValue();
                        try (OutputStream outputStream = new FileOutputStream(localPath)) {
                            ChannelSftp finalDownloadChannelSftp = downloadChannelSftp;
                            finalDownloadChannelSftp.get(remotePath, outputStream, new SftpProgressMonitor() {
                                private long max = finalDownloadChannelSftp.lstat(remotePath).getSize();
                                private long count = 0;

                                @Override
                                public void init(int op, String src, String dest, long max) {
                                    this.max = max;
                                }

                                @Override
                                public boolean count(long count) {
                                    this.count += count;
                                    updateProgress(this.count, this.max);
                                    return true;
                                }

                                @Override
                                public void end() {
                                    updateProgress(this.max, this.max);
                                }
                            });
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (SftpException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
            downloadProgressBar.progressProperty().bind(downloadTask.progressProperty());
            downloadTask.setOnSucceeded(e -> {
                menuHBox.getChildren().remove(downloadProgressBar);
                menuHBox.getChildren().remove(downloadLabel);
            });
            downloadTask.setOnFailed(e -> {
                menuHBox.getChildren().remove(downloadProgressBar);
                menuHBox.getChildren().remove(downloadLabel);
                downloadTask.getException().printStackTrace();
            });
            Thread.ofVirtual().start(downloadTask);
        }
    }

    private void uploadFiles(ActionEvent actionEvent) {
        TreeItem<FileObj> tempSelectedItem = remoteTree.getSelectionModel().getSelectedItem();
        ProgressBar uploadProgressBar = new ProgressBar();
        Label uploadLabel = new Label();
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(remoteTree.getScene().getWindow());
        if (file != null) {
            TreeItem<FileObj> uploadPath = remoteTree.getSelectionModel().getSelectedItem();
            menuHBox.getChildren().add(uploadLabel);
            menuHBox.getChildren().add(uploadProgressBar);
            uploadLabel.setText("Uploading..." + file.getName());
            uploadLabel.setVisible(true);
            uploadProgressBar.setVisible(true);
            ChannelSftp uploadChannel = Utils.getRemoteSession(credetailMap);
            Task<Void> uploadTask = new Task<>() {
                @Override
                protected Void call() throws IOException, SftpException {
                    try (InputStream inputStream = new FileInputStream(file)) {
                        String remotePath = uploadPath.getValue().getPath() + "/" + file.getName();
                        uploadChannel.put(inputStream, remotePath, new SftpProgressMonitor() {
                            private long max = file.length();
                            private long count = 0;

                            @Override
                            public void init(int op, String src, String dest, long max) {
                                this.max = max;
                            }

                            @Override
                            public boolean count(long count) {
                                this.count += count;
                                updateProgress(this.count, this.max);
                                return true;
                            }

                            @Override
                            public void end() {
                                updateProgress(this.max, this.max);
                            }
                        });
                    }
                    return null;
                }
            };
            uploadProgressBar.progressProperty().bind(uploadTask.progressProperty());
            uploadTask.setOnSucceeded(e -> {
                menuHBox.getChildren().remove(uploadProgressBar);
                menuHBox.getChildren().remove(uploadLabel);
                tempSelectedItem.getChildren().clear();
                populateDirectoryTree(uploadChannel, tempSelectedItem, tempSelectedItem.getValue().getPath());
            });
            uploadTask.setOnFailed(e -> {
                menuHBox.getChildren().remove(uploadProgressBar);
                uploadTask.getException().printStackTrace();
                menuHBox.getChildren().remove(uploadLabel);
            });
            Thread.ofVirtual().start(uploadTask);
        }
    }

    private void uploadDirectory(ActionEvent actionEvent) {
        TreeItem<FileObj> tempSelectedItem = remoteTree.getSelectionModel().getSelectedItem();
        ProgressBar uploadProgressBar = new ProgressBar();
        Label uploadLabel = new Label();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File directory = directoryChooser.showDialog(remoteTree.getScene().getWindow());
        if (directory != null && directory.isDirectory()) {
            menuHBox.getChildren().add(uploadLabel);
            menuHBox.getChildren().add(uploadProgressBar);
            uploadLabel.setText("Uploading..." + directory.getName());
            uploadLabel.setVisible(true);
            uploadProgressBar.setVisible(true);
            Task<Void> uploadTask = new Task<>() {
                @Override
                protected Void call() throws IOException, SftpException {
                    uploadDirectoryInternal(directory, tempSelectedItem.getValue().getPath());
                    return null;
                }

                private void uploadDirectoryInternal(File localDir, String remotePath) {
                    ChannelSftp uploadChannelSftp = Utils.getRemoteSession(credetailMap);
                    try {
                        SftpATTRS stat = uploadChannelSftp.stat(remotePath + "/" + localDir.getName()); // This step is juct to check if the remote file exists or not. Did not find any other way in JSCH.
                    } catch (SftpException e) {
                        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            try {
                                uploadChannelSftp.mkdir(remotePath + "/" + localDir.getName());
                            } catch (SftpException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                    long totalSize = calculateDirectorySize(localDir);
                    long[] uploadedSize = {0};
                    Map<File, String> uploadFileMap = new HashMap<>();
                    for (File file : localDir.listFiles()) {
                        if (file.isDirectory()) {
                            remotePath = remotePath + "/" + file.getName();
                            String finalRemotePath = remotePath;
                            Thread.ofVirtual().start(() -> {
                                uploadDirectoryInternal(file, finalRemotePath);
                            });
                        } else {
                            String dest = remotePath + "/" + localDir.getName() + "/" + file.getName();
                            uploadFileMap.put(file, dest);
                        }
                    }
                    for (Map.Entry<File, String> entry : uploadFileMap.entrySet()) {
                        File file = entry.getKey();
                        String dest = entry.getValue();
                        try (InputStream inputStream = new FileInputStream(file)) {
                            uploadChannelSftp.put(inputStream, dest, new SftpProgressMonitor() {
                                private long max = file.length();
                                private long count = 0;

                                @Override
                                public void init(int op, String src, String dest, long max) {
                                    this.max = max;
                                }

                                @Override
                                public boolean count(long count) {
                                    this.count += count;
                                    uploadedSize[0] += count;
                                    updateProgress(uploadedSize[0], totalSize);
                                    return true;
                                }

                                @Override
                                public void end() {
                                    updateProgress(totalSize, totalSize);
                                }
                            });
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (SftpException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    Platform.runLater(() -> {
                        menuHBox.getChildren().remove(uploadProgressBar);
                        menuHBox.getChildren().remove(uploadLabel);
                        tempSelectedItem.getChildren().clear();
                    });
                }

                @Override
                protected void failed() {
                    super.failed();
                }
            };
            uploadProgressBar.progressProperty().bind(uploadTask.progressProperty());
            populateDirectoryTree(null, tempSelectedItem, tempSelectedItem.getValue().getPath());
            Thread.ofVirtual().start(uploadTask);
        }
    }

    private long calculateDirectorySize(File directory) {
        long size = 0;
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                size += calculateDirectorySize(file);
            } else {
                size += file.length();
            }
        }
        return size;
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

    public void executeCommand(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("Execute Command");
        //dialog.setTitle("Execute Command");
        dialog.setContentText("Please enter the command:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(command -> {
            fileContent.setDisable(true);
            fileContent.setText("Executing command...");
            new Thread(() -> {
                try {
                    ChannelExec exec = (ChannelExec) sftpChannel.getSession().openChannel("exec");
                    exec.setCommand(command);
                    InputStream in = exec.getInputStream();
                    exec.connect();
                    StringBuilder output = new StringBuilder();
                    try (Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        int c = 0;
                        while ((c = reader.read()) != -1) {
                            output.append((char) c);
                        }
                    }
                    exec.disconnect();
                    Platform.runLater(() -> {
                        fileContent.setDisable(true);
                        closeFileButton.setVisible(true);
                        fileContent.setText(output.toString());
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        fileContent.setWrapText(true);
                        fileContent.setDisable(false);
                        closeFileButton.setVisible(true);
                        fileContent.setText("Failed to execute command: " + e.getMessage());
                    });
                }
            }).start();
        });
    }

    private void downloadFile(String remotePath, String localPath) throws SftpException, IOException {
        try (OutputStream outputStream = new FileOutputStream(localPath)) {
            sftpChannel.get(remotePath, outputStream);
        }
    }

    private void downloadDirectory(String remotePath, String localPath) throws SftpException, IOException {
        File localDir = new File(localPath);
        if (!localDir.exists()) {
            localDir.mkdirs();
        }
        Vector<ChannelSftp.LsEntry> ls = sftpChannel.ls(remotePath);
        for (ChannelSftp.LsEntry entry : ls) {
            if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                String remoteFilePath = remotePath + "/" + entry.getFilename();
                String localFilePath = localPath + "/" + entry.getFilename();
                if (entry.getAttrs().isDir()) {
                    downloadDirectory(remoteFilePath, localFilePath);
                } else {
                    downloadFile(remoteFilePath, localFilePath);
                }
            }
        }
    }

    private void move(ActionEvent actionEvent) {

    }

    public void about(ActionEvent actionEvent) {

    }
}
