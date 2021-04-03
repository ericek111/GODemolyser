package eu.lixko.godemolyser.util.stream;

public class ByteArrayDataStream extends DataStream {
	
	// TODO: long > int cast error handling
	
	private byte[] arr;
	
	private ByteArrayDataStream(byte[] data) {
		this.arr = data;
		this.length = (long) data.length * 8;
	}
	
	private ByteArrayDataStream(byte[] data, long byteStart, long byteLength) {
		this.arr = data;
		this.start = byteStart * 8;
		this.length = byteLength * 8;
	}
	
	public byte[] array() {
		return arr;
	}
			
	@Override
	public byte getRawBit(long bitOffset) {
		return (byte) (arr[(int) (bitOffset >> 3)] >> (bitOffset & 7) & 1);
	}
	
	@Override
	public byte getRawByte(long byteOffset) {
		return arr[(int) byteOffset];
	}
	
	public static ByteArrayDataStream wrap(byte[] arr) {
		return new ByteArrayDataStream(arr);
	}
	
	// TODO: Decide -- should this accept lengths in bits or bytes?
	public static ByteArrayDataStream wrap(byte[] data, long byteStart, long byteLength) {
		return new ByteArrayDataStream(data, byteStart * 8, byteLength * 8);
	}
	
	@Override
	public ByteArrayDataStream subset(long bitStart, long bitLength) {
		return new ByteArrayDataStream(this.arr, bitStart, bitLength);
	}
	
}
