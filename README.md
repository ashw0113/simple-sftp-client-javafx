# Simple SFTP Client

## Overview
Simple SFTP Client is a JavaFX application that allows users to connect to an SFTP server, browse directories, upload and download files, and execute commands on the server.

## Features
- Connect to an SFTP server using JSch.
- Browse remote directories and files.
- Upload files and directories to the server.
- Download files and directories from the server.
- Execute commands on the server.
- View and edit text files on the server.

## Prerequisites
- Java 21 or higher
- Maven

## Getting Started

### Clone the Repository
git clone https://github.com/yourusername/SimpleSFTPClient.git
cd SimpleSFTPClient

## Build Project
mvn clean install

## Run Project
mvn javafx:run

## Project Structure
src/main/java/com/teche/simplesftpclient: Contains the main application and controllers.
src/main/resources/com/teche/simplesftpclient: Contains FXML files and other resources.
src/main/resources/icons: Contains icons used in the application.

## Dependencies
JavaFX
JSch

## Usage
Launch the application.
Enter the SFTP server details (host, port, username, password) and click "Login".
Browse the remote directory structure using the TreeView.
Right-click on directories or files to upload, download, or move items.
Double-click on text files to view and edit their content.
Use the "Execute Command" option in the menu to run commands on the server.

## License
This project is licensed under the MIT License. See the LICENSE file for details.

## Acknowledgements
JavaFX
JSch
