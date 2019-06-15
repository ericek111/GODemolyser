package eu.lixko.godemolyser.util.stream;

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
	
}
