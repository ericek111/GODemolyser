package eu.lixko.godemolyser.util.stream;

import java.nio.ByteOrder;

import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.util.logger.StringFormat;

public abstract class DataStream {

	protected int bitIndex = 0;
	protected int start = 0;
	protected int length = 0;
	protected ByteOrder endianess;

	// byte[] arr;

	public abstract byte getRawBit(int bitOffset);

	public abstract byte getRawByte(int byteOffset);

	public abstract DataStream subset(int bitStart, int bitLength);

	public int byteIndex() {
		return bitIndex >> 3;
	}

	public int bitIndex() {
		return bitIndex;
	}

	public int byteIndex(int bytes) {
		return bitIndex = bytes * 8;
	}

	public int bitIndex(int bits) {
		return bitIndex = bits;
	}

	public int start() {
		return start;
	}

	public int bitLength() {
		return length;
	}

	public int byteLength() {
		return length >> 3;
	}

	public int remaining() {
		return length - bitIndex;
	}

	public void forward(int bits) {
		// TODO: Implement overflow
		bitIndex += bits;
	}

	public void back(int bits) {
		// TODO: Implement underflow
		bitIndex -= bits;
	}

	public void rewind() {
		bitIndex = 0;
	}

	// TODO: Implement endianess.
	public void order(ByteOrder order) {
		this.endianess = order;
	}

	public boolean hasRemaining() {
		return this.bitIndex < this.length;
	}

	public int readBits(int offset, int bits, boolean signed, boolean advance) {
		int available = (this.length - offset);
		offset += this.start;

		// TODO: Handle 1 byte reads more efficiently (condition and skip the loop?)

		if (bits > available) {
			throw new Error("BufferUnderflow: Cannot get " + bits + " bit" + (bits > 1 ? "s" : "") + " from offset " + offset + ", " + available + " available");
		}

		if (bits > 32) {
			throw new Error("Tried to read more than 32 bits: " + bits + " bits from offset " + offset + ".");
		}

		if (advance)
			forward(bits);

		if (bits == 8 && (offset & 7) == 0) {
			if (signed)
				return getRawByte(offset >> 3);
			else
				return (getRawByte(offset >> 3) >>> 0) & 0xFF;
		}

		int value = 0;
		for (int i = 0; i < bits;) {
			int read;

			// Read an entire byte if we can.
			if ((bits - i) >= 8 && ((offset & 7) == 0)) {
				value |= (int) (((getRawByte(offset >> 3) & 0xFF) << i));
				read = 8;
			} else {
				value |= (getRawBit(offset) << i);
				read = 1;
			}

			offset += read;
			i += read;
		}

		offset -= bits;
		// System.out.println("Read " + bits + " b @ " + (offset >> 3) + " . " + (offset & 7) + ": " + StringFormat.hex(value));

		if (signed) {
			// If we're not working with a full 32 bits, check the
			// imaginary MSB for this bit count and convert to a
			// valid 32-bit signed value if set.
			if (bits != 32 && (value & (1 << (bits - 1))) > 0) {
				value |= -1 ^ ((1 << bits) - 1);
			}

			return value;
		}

		return value >>> 0;
	}

	/******************************** GET (without position increment) ********************************/

	public int getBits(int offset, int bits, boolean signed) {
		return this.readBits(offset, bits, signed, false);
	}

	public byte getBit(int offset) {
		return (byte) getRawBit(offset);
	}

	public boolean getBoolean(int offset) {
		return getBits(offset, 1 * 8, true) != 0;
	}

	public boolean getBitBoolean(int offset) {
		return getBits(offset, 1, false) != 0;
	}

	public byte getByte(int offset) {
		return (byte) getBits(offset, Byte.BYTES * 8, true);
	}

	public byte getUByte(int offset) {
		return (byte) getBits(offset, Byte.BYTES * 8, false);
	}

	public short getShort(int offset) {
		return (short) getBits(offset, Short.BYTES * 8, true);
	}

	public int getUShort(int offset) {
		return (int) getBits(offset, Short.BYTES * 8, false);
	}

	public char getChar(int offset) {
		return (char) getBits(bitIndex, Character.BYTES * 8, true);
	}

	public int getInt(int offset) {
		return getBits(offset, Integer.BYTES * 8, true);
	}

	public int getUInt(int offset) {
		return getBits(offset, Integer.BYTES * 8, false);
	}

	public long getLong(int offset) {
		int x = getBits(offset, 4 * 8, false);
		int y = getBits(offset, 4 * 8, false);
		return (((long) x) << 32) | (y & 0xffffffffL);
	}

	public float getFloat(int offset) {
		return Float.intBitsToFloat(getBits(offset, 4 * 8, false));
	}

	public double getDouble(int offset) {
		return Double.longBitsToDouble(getLong(offset));
	}

	public byte[] getBytes(int offset, byte[] to) {
		for (int i = 0; i < to.length; i++) {
			to[i] = getByte(offset + i * 8);
		}
		return to;
	}

	public byte[] getBytes(int offset, int length) {
		return this.getBytes(offset, new byte[length]);
	}

	public String getFixedString(int offset, int length) {
		byte[] data = new byte[length];
		getBytes(offset, data);
		return new String(data);
	}

	public String getString(int offset) {
		StringBuilder sb = new StringBuilder();
		for (int i = offset; i < length; i += 8) {
			char c = (char) (getByte(i) & 0xFF);
			if (c == '\0')
				break;
			sb.append(c);
		}
		return sb.toString();
	}

	/******************************** READ (with position increment) ********************************/

	public int readBits(int bits, boolean signed) {
		return readBits(bitIndex, bits, signed, true);
	}

	public byte readBit() {
		return (byte) readBits(bitIndex, 1, false, true);
	}

	public boolean readBoolean() {
		return readBits(bitIndex, 1 * 8, true, true) != 0;
	}

	public boolean readBitBoolean() {
		return readBits(bitIndex, 1, false, true) != 0;
	}

	public byte readByte() {
		return (byte) readBits(bitIndex, Byte.BYTES * 8, true, true);
	}

	public byte readUByte() {
		return (byte) readBits(bitIndex, Byte.BYTES * 8, false, true);
	}

	public short readShort() {
		return (short) readBits(bitIndex, Short.BYTES * 8, true, true);
	}

	public int readUShort() {
		return (short) readBits(bitIndex, Short.BYTES * 8, false, true);
	}

	public char readChar() {
		return (char) readBits(bitIndex, Character.BYTES * 8, true, true);
	}

	public int readInt() {
		return readBits(bitIndex, Integer.BYTES * 8, true, true);
	}

	public long readUInt() {
		return readBits(bitIndex, Integer.BYTES * 8, false, true) & 0xffffffff;
	}

	public long readLong() {
		int x = readBits(bitIndex, 4 * 8, false, true);
		int y = readBits(bitIndex, 4 * 8, false, true);
		return (((long) x) << 32) | (y & 0xffffffffL);
	}

	public float readFloat() {
		return Float.intBitsToFloat(readBits(bitIndex, Float.BYTES * 8, false, true));
	}

	public double readDouble() {
		return Double.longBitsToDouble(readLong());
	}

	public byte[] readBytes(byte[] to) {
		// System.out.println("reading " + (to.length * 8) + " from " + this.bitIndex() + " / " + this.remaining() + " > " + (this.bitIndex() + to.length * 8) + " (" +
		// (this.length - this.bitIndex() - to.length * 8) + ")");
		byte[] ret = getBytes(bitIndex, to);
		forward(to.length * 8);
		return ret;
	}

	public byte[] readBytes(int length) {
		byte[] to = new byte[length];
		return this.readBytes(to);
	}

	public String readFixedString(int length) {
		return new String(readBytes(length));
	}

	public String readString() {
		StringBuilder sb = new StringBuilder();
		for (int i = bitIndex; i < length; i += 8) {
			char c = (char) (getByte(i) & 0xFF);
			if (c == '\0')
				break;
			sb.append(c);
		}
		forward((sb.length() + 1) * 8);
		return sb.toString();
	}

	public int readVarInt32_fast() throws InvalidDataException {
		byte tmp = this.readByte();
		if (tmp >= 0) {
			return tmp;
		}
		int result = tmp & 0x7f;
		if ((tmp = this.readByte()) >= 0) {
			result |= tmp << 7;
		} else {
			result |= (tmp & 0x7f) << 7;
			if ((tmp = this.readByte()) >= 0) {
				result |= tmp << 14;
			} else {
				result |= (tmp & 0x7f) << 14;
				if ((tmp = this.readByte()) >= 0) {
					result |= tmp << 21;
				} else {
					result |= (tmp & 0x7f) << 21;
					result |= (tmp = this.readByte()) << 28;
					if (tmp < 0) {
						// byte[] data = new byte[16];
						// this.readBytes(data);
						this.back((16 + 5) * 8);
						throw new InvalidDataException("Malformed varint detected: "/* + StringFormat.hex(data) */);
					}
				}
			}
		}
		return result;
	}

	public int readVarInt_orig() throws InvalidDataException {
		// See implementation notes for readRawVarint64
		int origi = this.bitIndex();
		int x;
		
		if ((x = this.readByte()) >= 0) {
			return x;
		} else if ((this.remaining() / 8) < 9) {
			this.bitIndex(origi);
			return (int) readVarint64SlowPath();
		} else if ((x ^= (this.readByte() << 7)) < 0) {
			x ^= (~0 << 7);
		} else if ((x ^= (this.readByte() << 14)) >= 0) {
			x ^= (~0 << 7) ^ (~0 << 14);
		} else if ((x ^= (this.readByte() << 21)) < 0) {
			x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
		} else {
			int y = this.readByte();
			x ^= y << 28;
			x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
			if (y < 0 && this.readByte() < 0 && this.readByte() < 0 && this.readByte() < 0 && this.readByte() < 0 && this.readByte() < 0) {
				this.bitIndex(origi);
				throw new InvalidDataException("Malformed varint detected.");
			}
		}
		return x;
	}

	public int readVarInt32() throws InvalidDataException {
		if (true)
			return this.readVarInt_orig();
		byte b = (byte) 0x80;
		int result = 0;
		for (int count = 0; (b & 0x80) != 0; count++) {
			b = this.readByte();

			if ((count < 4) || ((count == 4) && (((b & 0xF8) == 0) || ((b & 0xF8) == 0xF8))))
				result |= (b & ~0x80) << (7 * count);
			else {
				if (count >= 10)
					throw new InvalidDataException("Nope nope nope nope! 10 bytes max!");
				if ((count == 9) ? (b != 1) : ((b & 0x7F) != 0x7F))
					throw new InvalidDataException("more than 32 bits are not supported");
			}
		}
		return result;
	}

	private long readVarint64SlowPath() throws InvalidDataException {
		long result = 0;
		for (int shift = 0; shift < 64; shift += 7) {
			final byte b = readByte();
			result |= (long) (b & 0x7F) << shift;
			if ((b & 0x80) == 0) {
				return result;
			}
		}
		throw new InvalidDataException("Malformed varint detected.");
	}

	public String readVString() throws InvalidDataException {
		int length = this.readVarInt32();
		if (length < 0)
			throw new InvalidDataException("Negative string length: " + length);

		if (length == 0)
			return "";

		return this.readFixedString(length);
	}

	public byte[] readIBytes() throws InvalidDataException {
		int length = this.readInt();
		return this.readBytes(length);
	}

	public String readIString() throws InvalidDataException {
		int length = this.readInt();
		if (length < 0)
			throw new InvalidDataException("Negative string length: " + length);

		return this.readFixedString(length);
	}

	public byte[] readVBytes() throws InvalidDataException {
		int length = this.readVarInt32();
		return this.readBytes(length);
	}

	public static void dumpBuf(DataStream stream, int len) {
		byte[] data = new byte[len];
		stream.readBytes(data);
		stream.back(len * 8);
		for (byte b : data) {
			System.out.print(StringFormat.bin(b & 0xFF) + " ");
		}
		System.out.println();
		for (byte b : data) {
			System.out.print((b & 0xFF) + " ");
		}
		System.out.println();
		for (byte b : data) {
			if (b > 31)
				System.out.print((char) (b & 0xFF));
			else
				System.out.print(' ');
		}
		System.out.println();
		System.out.println(StringFormat.dumpObj(data));
	}

}