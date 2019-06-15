package eu.lixko.godemolyser.util.struct;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import eu.lixko.godemolyser.util.stream.DataStream;
import eu.lixko.godemolyser.util.struct.BufferStruct.StringLength;

public class DataStreamStructReader implements IStructReader {
	
	private DataStream buf;
	
	public DataStreamStructReader(DataStream buf) {
		this.buf = buf;
	}
	
	public void setBuffer(DataStream buf) {
		this.buf = buf;
	}
	
	public DataStream buffer() {
		return this.buf;
	}
	
	public void readArray(BufferStruct struct, Object arr, Field f) throws InstantiationException, IllegalAccessException {
		byte[] strbuf = new byte[0];
		String charset = "";
		if (f.isAnnotationPresent(StringLength.class)) {
			int len = f.getAnnotation(StringLength.class).size();
			charset = f.getAnnotation(StringLength.class).charset();
			strbuf = new byte[len];
		}
		int fieldlen = BufferStruct.getFieldLen(f);
		for (int i = 0; i < Array.getLength(arr); i++) {
			Object value = Array.get(arr, i);
			if (value == null) {
				// TODO: Multidimensional arrays - .newInstance() ?
				value = arr.getClass().getComponentType();
				Array.set(arr, i, value);
			}
			if (value.getClass().isArray()) {
				this.readArray(struct, value, f);
			} else {
				if (strbuf.length > 0) {
					buf.readBytes(strbuf);
					try {
						Array.set(arr, i, new String(strbuf, charset));
					} catch (UnsupportedEncodingException e) {
						throw new IllegalArgumentException("UnsupportedEncodingException (" + charset + ")! " + f.getName() + " of " + value.getClass().getName());
					}
				} else if (fieldlen > 0) {
					if (fieldlen == 1)
						Array.set(arr, i, buf.readUByte());
					else if (fieldlen == 2)
						Array.set(arr, i, buf.readUShort());
					else if (fieldlen == 4)
						Array.set(arr, i, buf.readUInt());
				} else if (value instanceof Byte)
					Array.set(arr, i, buf.readByte());
				else if (value instanceof Character)
					Array.set(arr, i, buf.readChar());
				else if (value instanceof Boolean)
					Array.set(arr, i, buf.readBoolean());
				else if (value instanceof Short)
					Array.set(arr, i, buf.readShort());
				else if (value instanceof Integer)
					Array.set(arr, i, buf.readInt());
				else if (value instanceof Long)
					Array.set(arr, i, buf.readLong());
				else if (value instanceof Float)
					Array.set(arr, i, buf.readFloat());
				else if (value instanceof Double)
					Array.set(arr, i, buf.readDouble());
				else if (value instanceof BufferStruct)
					((BufferStruct) value).readUsing(this);
				else
					throw new IllegalArgumentException("Non-primitive types are not implemented yet! " + value.getClass().getName());
			}
		}
		//System.out.println("Read: " + arr.getClass().getComponentType().getName() + "[" + Array.getLength(arr) + "] " + f.getName() + " -> " + buf.byteIndex() + " . " + (buf.bitIndex() & 7));
	}
	
	public void readField(BufferStruct struct, Field f) throws IllegalArgumentException, IllegalAccessException {
		int fieldlen = BufferStruct.getFieldLen(f);
		if (fieldlen > 0) {
			if (fieldlen == 1)
				f.set(struct, buf.readUByte());
			else if (fieldlen == 2)
				f.set(struct, buf.readUShort());
			else if (fieldlen == 4)
				f.set(struct, buf.readUInt());
			return;
		}
		Object obj = f.get(struct);
		if (obj instanceof Byte)
			f.set(struct, buf.readByte());
		else if (obj instanceof Character)
			f.set(struct, buf.readChar());
		else if (obj instanceof Boolean)
			f.set(struct, buf.readBoolean());
		else if (obj instanceof Short)
			f.set(struct, buf.readShort());
		else if (obj instanceof Integer)
			f.set(struct, buf.readInt());
		else if (obj instanceof Long)
			f.set(struct, buf.readLong());
		else if (obj instanceof Float)
			f.set(struct, buf.readFloat());
		else if (obj instanceof Double)
			f.set(struct, buf.readDouble());
		else if (obj instanceof String || f.getType().isAssignableFrom(String.class)) {
			if (!f.isAnnotationPresent(StringLength.class))
				return;
			int len = f.getAnnotation(StringLength.class).size();
			String charset = f.getAnnotation(StringLength.class).charset();

			byte[] strbuf = new byte[len];
			buf.readBytes(strbuf);
			try {
				String tempstr = new String(strbuf, charset);
				f.set(struct, tempstr);
			} catch (UnsupportedEncodingException e) {
				throw new IllegalArgumentException("UnsupportedEncodingException (" + charset + ")! " + f.getName() + " of " + obj.getClass().getName());
			}
		} else if (BufferStruct.class.isAssignableFrom(f.getType()))
			((BufferStruct) obj).readUsing(this);
		else
			throw new IllegalArgumentException("Non-primitive types are not implemented yet! " + f.getName() + " of " + (obj != null ? obj.getClass() != null ? obj.getClass().getName() : "[NULL class]" : "[NULL obj]"));
		//System.out.println("Read: " + f.getType() + " " + f.getName() + " -> " + buf.byteIndex() + " . " + (buf.bitIndex() & 7));
	}

}
