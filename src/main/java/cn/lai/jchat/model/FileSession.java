package cn.lai.jchat.model;

import okio.BufferedSink;
import okio.BufferedSource;

import java.io.IOException;
import java.net.Socket;

public class FileSession extends Session {
    public static final String NAME = "FILE";
    private final boolean isClient;

    public FileSession(Socket socket, BufferedSource source, BufferedSink sink, boolean isClient) throws IOException {
        super(socket, source, sink);
        this.isClient = isClient;
    }

    public boolean isClient() {
        return isClient;
    }

    @Override
    public String toString() {
        return "FileSession{" +
                "isClient=" + isClient +
                '}';
    }
}
