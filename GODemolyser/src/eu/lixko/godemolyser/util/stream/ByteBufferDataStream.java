package eu.lixko.godemolyser.util.stream;

import java.nio.ByteBuffer;

public class ByteBufferDataStream extends WritableDataStream {
	
	protected ByteBuffer buf;
	
	private ByteBufferDataStream(ByteBuffer buf) {
		this.buf = buf;
		this.length = buf.limit() * 8;
	}
	
	private ByteBufferDataStream(ByteBuffer buf, long byteStart, long byteLength) {
		this.buf = buf;
		this.start = byteStart * 8;
		this.length = byteLength * 8;
	}
	
	public ByteBuffer buffer() {
		return buf;
	}
			
	@Override
	public byte getRawBit(long bitOffset) {
		return (byte) (buf.get((int) (bitOffset >> 3)) >> (bitOffset & 7) & 1);
	}
	
	@Override
	public byte getRawByte(long byteOffset) {
		return buf.get((int) byteOffset);
	}
	

	@Override
	public void setByte(long bitOffset, byte value) {
		// TODO: Add support for unaligned writes
		this.buf.put((int) (bitOffset >> 3), value);		
	}
	
	@Override
	public void setBytes(long bitOffset, byte[] values) {
		// TODO: Add support for unaligned writes
		this.buf.put((int) (bitOffset >> 3), values);		
	}
	
	@Override
	protected void makeSpace(long bitOffset, long bitsLen) {
		// TODO Auto-generated method stub
		
	}
	
	public static ByteBufferDataStream wrap(ByteBuffer buf) {
		return new ByteBufferDataStream(buf);
	}
	
	// TODO: Decide -- should this accept lengths in bits or bytes?
	public static ByteBufferDataStream wrap(ByteBuffer buf, long byteStart, long byteLength) {
		return new ByteBufferDataStream(buf, byteStart * 8, byteLength * 8);
	}
	
	@Override
	public ByteBufferDataStream subset(long bitStart, long bitLength) {
		return new ByteBufferDataStream(this.buf, bitStart, bitLength);
	}
	
}
