package eu.lixko.godemolyser.util.stream;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamDataStream extends DataStream {

	private InputStream stream;
	
	private InputStreamDataStream(InputStream stream) throws IOException {
		this.stream = stream;
		this.length = (long) stream.available() * 8;
	}
	
	@Override
	public byte getRawBit(long bitOffset) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte getRawByte(long byteOffset) {
		final byte[] tmp = new byte[1];
		/*try {
			stream.read(tmp, byteOffset, 1);
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		return 0;
	}

	// Beware of unnecessary fragmentation -- splitting the stream and treating is a buffer will cause frequent skips.
	@Override
	public DataStream subset(long bitStart, long bitLength) {
		// TODO Auto-generated method stub
		return null;
	}

}
