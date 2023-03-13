package eu.lixko.godemolyser.util.stream;

public abstract class WritableDataStream extends DataStream {
	
	public abstract void setByte(long bitOffset, byte value);
		
	protected abstract void makeSpace(long bitOffset, long bitsLen);
	
	public void setBytes(long bitOffset, byte[] values) {
		for (byte b : values) {
			this.setByte(bitOffset, b);
			bitOffset += Byte.SIZE;
		}
	}
	
	public void insertBytes(long bitOffset, byte[] values) {
		this.makeSpace(bitOffset, values.length * Byte.SIZE);
		this.setBytes(bitOffset, values);
	}
	
	
	/******************************** WRITE (with position increment) ********************************/
	
	

}
