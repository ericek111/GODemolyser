package eu.lixko.godemolyser.util.struct;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import eu.lixko.godemolyser.util.struct.BufferStruct.StringLength;

public class ByteBufferStructWriter implements IStructWriter {
	
	private ByteBuffer buf;
	
	public ByteBufferStructWriter(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public void setBuffer(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public ByteBuffer buffer() {
		return this.buf;
	}

	public void writeField(BufferStruct struct, Field f) throws IllegalArgumentException, IllegalAccessException {
		Object obj = f.get(struct);
		
		// for unsigned fields
		// TODO: Will this even work with the utility methods instead of setting bytes directly?
		int fieldlen = BufferStruct.getFieldLen(f);
		if (fieldlen > 0) {
			if (fieldlen == 1)
				buf.put((byte) (f.getByte(struct) & 0xFF));
			else if (fieldlen == 2)
				buf.putShort((short) (f.getShort(struct) & 0xFFFF));
			else if (fieldlen == 4)
				buf.putInt((int) (f.getInt(struct) & 0xFFFFFFFF));
			return;
		}
		
		// TODO: These all should be just basic casts probably, instead of using .getShort and that crap. 
		if (obj instanceof Byte)
			buf.put((byte) obj);
		else if (obj instanceof Character)
			buf.putChar((char) obj);
		else if (obj instanceof Boolean)
			buf.put((byte) ((boolean) obj ? 1 : 0));
		else if (obj instanceof Short)
			buf.putShort((short) obj);
		else if (obj instanceof Integer)
			buf.putInt((int) obj);
		else if (obj instanceof Long)
			buf.putLong((long) obj);
		else if (obj instanceof Float)
			buf.putFloat((float) obj);
		else if (obj instanceof Double)
			buf.putDouble((double) obj);
		else if (obj instanceof String || f.getType().isAssignableFrom(String.class)) {
			if (!f.isAnnotationPresent(StringLength.class))
				return;
			int len = f.getAnnotation(StringLength.class).size();
			String charset = f.getAnnotation(StringLength.class).charset();

			try {
				String structVal = (String) f.get(obj);
				byte[] stringBytes = structVal.getBytes(charset);
				// Copy at most `len` bytes, as specified in StringLength. If empty, write only the null terminator.
				buf.put(stringBytes, 0, Math.max(0, len));
				buf.put((byte) '\0');
			} catch (UnsupportedEncodingException e) {
				throw new IllegalArgumentException("UnsupportedEncodingException (" + charset + ")! " + f.getName() + " of " + obj.getClass().getName());
			}
		} else if (BufferStruct.class.isAssignableFrom(f.getType())) {
			((BufferStruct) obj).writeUsing(this);
		} else {
			throw new IllegalArgumentException("Non-primitive types are not implemented yet! " + f.getName() + " of " + (obj != null ? obj.getClass() != null ? obj.getClass().getName() : "[NULL class]" : "[NULL obj]"));
		}
	}

	public void writeArray(BufferStruct struct, Object arr, Field f) throws InstantiationException, IllegalAccessException {
		int stringLen = 0;
		String charset = "";
		if (f.isAnnotationPresent(StringLength.class)) {
			stringLen = f.getAnnotation(StringLength.class).size();
			charset = f.getAnnotation(StringLength.class).charset();
		}
		
		int fieldlen = BufferStruct.getFieldLen(f);
		for (int i = 0; i < Array.getLength(arr); i++) {
			Object value = Array.get(arr, i);
			if (value == null) {
				// TODO: Multidimensional arrays - .newInstance() ?
				value = arr.getClass().getComponentType();
				Array.set(arr, i, value);
				System.out.println("value null for " + f.getName());
			}
			
			if (value.getClass().isArray()) {
				this.writeArray(struct, value, f);
			} else {
				// TODO: Check if it's actually a string and not some crap with the StringLength annot., do the same in reader.
				if (stringLen > 0) {
					try {
						String structVal = (String) Array.get(arr, i);
						if (structVal != null) {
							byte[] stringBytes = structVal.getBytes(charset);
							buf.put(stringBytes, 0, Math.max(0, stringLen));
						}
						buf.put((byte) '\0');
					} catch (UnsupportedEncodingException e) {
						throw new IllegalArgumentException("UnsupportedEncodingException (" + charset + ")! " + f.getName() + " of " + value.getClass().getName());
					}
				} else if (fieldlen > 0) {
					if (fieldlen == 1)
						buf.put((byte) (Array.getByte(arr, i) & 0xFF));
					else if (fieldlen == 2)
						buf.putShort((short) (Array.getShort(arr, i) & 0xFFFF));
					else if (fieldlen == 4)
						buf.putInt((int) (Array.getInt(arr, i) & 0xFFFFFFFF));
				} else if (value instanceof Byte)
					buf.put(Array.getByte(arr, i));
				else if (value instanceof Character)
					buf.putChar(Array.getChar(arr, i));
				else if (value instanceof Boolean)
					buf.put((byte) (Array.getBoolean(arr, i) ? 1 : 0));
				else if (value instanceof Short)
					buf.putShort(Array.getShort(arr, i));
				else if (value instanceof Integer)
					buf.putInt(Array.getInt(arr, i));
				else if (value instanceof Long)
					buf.putLong(Array.getLong(arr, i));
				else if (value instanceof Float)
					buf.putFloat(Array.getFloat(arr, i));
				else if (value instanceof Double)
					buf.putShort(Array.getShort(arr, i));
				else if (value instanceof BufferStruct)
					((BufferStruct) value).writeUsing(this);
				else
					throw new IllegalArgumentException("Non-primitive types are not implemented yet! " + value.getClass().getName());
			}
		}
	}
}
