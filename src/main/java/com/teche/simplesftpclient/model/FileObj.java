package com.teche.simplesftpclient.model;

public class FileObj {
    private String name;
    private String path;
    private boolean isDirectory;

    public static FileObj getFileObj() {
        return new FileObj();
    }

    public FileObj() {
    }

    public FileObj(String name, String path, boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public void setDirectory(boolean directory) {
        isDirectory = directory;
    }

    @Override
    public String toString() {
        return name;
    }
}
