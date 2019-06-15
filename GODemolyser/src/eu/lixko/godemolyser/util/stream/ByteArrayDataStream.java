package eu.lixko.godemolyser.util.stream;

public class ByteArrayDataStream extends DataStream {
	
	private byte[] arr;
	
	private ByteArrayDataStream(byte[] data) {
		this.arr = data;
		this.length = data.length * 8;
	}
	
	private ByteArrayDataStream(byte[] data, int bitStart, int bitLtart) {
		this.arr = data;
		this.start = bitStart * 8;
		this.length = bitLtart * 8;
	}
	
	public byte[] array() {
		return arr;
	}
			
	@Override
	public byte getRawBit(int bitOffset) {
		return (byte) (arr[bitOffset >> 3] >> (bitOffset & 7) & 1);
	}
	
	@Override
	public byte getRawByte(int byteOffset) {
		return arr[byteOffset];
	}
	
	public static ByteArrayDataStream wrap(byte[] arr) {
		return new ByteArrayDataStream(arr);
	}
	
	public static ByteArrayDataStream wrap(byte[] data, int start, int length) {
		return new ByteArrayDataStream(data, start * 8, length * 8);
	}

	@Override
	public ByteArrayDataStream subset(int bitStart, int bitLength) {
		return new ByteArrayDataStream(this.arr, bitStart, bitLength);
	}
	
}
