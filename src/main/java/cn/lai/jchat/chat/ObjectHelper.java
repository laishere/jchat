package cn.lai.jchat.chat;

import okio.Buffer;
import okio.ByteString;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectHelper {
    public static Buffer serialize(Object o) throws IOException {
        Buffer buffer = new Buffer();
        try (ObjectOutputStream objectOut = new ObjectOutputStream(buffer.outputStream())) {
            objectOut.writeObject(o);
        }
        return buffer;
    }

    public static Object deserialize(ByteString byteString) throws IOException, ClassNotFoundException {
        Buffer buffer = new Buffer();
        buffer.write(byteString);
        return deserialize(buffer);
    }

    public static Object deserialize(Buffer buffer) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectIn = new ObjectInputStream(buffer.inputStream())) {
            return objectIn.readObject();
        }
    }
}
