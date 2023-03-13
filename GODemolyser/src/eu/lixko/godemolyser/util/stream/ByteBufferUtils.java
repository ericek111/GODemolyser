package eu.lixko.godemolyser.util.stream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferUtils {

	public static String readCString(ByteBuffer stream) {
		StringBuilder sb = new StringBuilder();
		while(stream.remaining() > 0) {
			char c = (char) (stream.get() & 0xFF);
			if(c == '\0') break;
			sb.append(c);
		}
		return sb.toString();
	}
	
	public static ByteBuffer readFile(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        int bytesRead;
        try (FileInputStream fileReader = new FileInputStream(file)) {
            bytesRead = fileReader.read(buffer);
        }
        assert bytesRead == file.length() : file;
        return ByteBuffer.wrap(buffer);
    }
	
	public static final byte[] intToByteArray(int value) {
	    return new byte[] {
            (byte)(value >>> 24),
            (byte)(value >>> 16),
            (byte)(value >>> 8),
            (byte)value
        };
	}
	
	public static final byte[] intToByteArrayBE(int value) {
	    return new byte[] {
    		(byte)value,
    		(byte)(value >>> 8),
            (byte)(value >>> 16),
            (byte)(value >>> 24)
        };
	}
}
