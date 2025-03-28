package com.teche.simplesftpclient.utils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.Map;

public class Utils {

    public static ChannelSftp getRemoteSession(Map<String, String> credentials) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;
        String host = credentials.get("host");
        int port = Integer.parseInt(credentials.get("port"));
        String username = credentials.get("username");
        String password = credentials.get("password");
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig("compression.c2s", "zlib,none");
            session.connect(10000);
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }
        return channelSftp;
    }
}
