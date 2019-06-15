package eu.lixko.godemolyser.util.struct;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import eu.lixko.godemolyser.util.struct.BufferStruct.StringLength;

public class ByteBufferStructReader implements IStructReader {
	
	private ByteBuffer buf;
	
	public ByteBufferStructReader(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public void setBuffer(ByteBuffer buf) {
		this.buf = buf;
	}
	
	public ByteBuffer buffer() {
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
					buf.get(strbuf);
					try {
						Array.set(arr, i, new String(strbuf, charset));
					} catch (UnsupportedEncodingException e) {
						throw new IllegalArgumentException("UnsupportedEncodingException (" + charset + ")! " + f.getName() + " of " + value.getClass().getName());
					}
				} else if (fieldlen > 0) {
					if (fieldlen == 1)
						Array.set(arr, i, buf.get() & 0xFF);
					else if (fieldlen == 2)
						Array.set(arr, i, buf.getShort() & 0xFFFF);
					else if (fieldlen == 4)
						Array.set(arr, i, ((long) buf.getInt()) & 0xFFFFFFFF);
				} else if (value instanceof Byte)
					Array.set(arr, i, buf.get());
				else if (value instanceof Character)
					Array.set(arr, i, buf.getChar());
				else if (value instanceof Boolean)
					Array.set(arr, i, buf.get() > 0);
				else if (value instanceof Short)
					Array.set(arr, i, buf.getShort());
				else if (value instanceof Integer)
					Array.set(arr, i, buf.getInt());
				else if (value instanceof Long)
					Array.set(arr, i, buf.getLong());
				else if (value instanceof Float)
					Array.set(arr, i, buf.getFloat());
				else if (value instanceof Double)
					Array.set(arr, i, buf.getDouble());
				else if (value instanceof BufferStruct)
					((BufferStruct) value).readUsing(this);
				else
					throw new IllegalArgumentException("Non-primitive types are not implemented yet! " + value.getClass().getName());
			}
		}
	}
	
	public void readField(BufferStruct struct, Field f) throws IllegalArgumentException, IllegalAccessException {
		int fieldlen = BufferStruct.getFieldLen(f);
		if (fieldlen > 0) {
			if (fieldlen == 1)
				f.set(struct, buf.get() & 0xFF);
			else if (fieldlen == 2)
				f.set(struct, buf.getShort() & 0xFFFF);
			else if (fieldlen == 4)
				f.set(struct, ((long) buf.getInt()) & 0xFFFFFFFF);
			return;
		}
		Object obj = f.get(struct);
		if (obj instanceof Byte)
			f.set(struct, buf.get());
		else if (obj instanceof Character)
			f.set(struct, buf.getChar());
		else if (obj instanceof Boolean)
			f.set(struct, buf.get() > 0);
		else if (obj instanceof Short)
			f.set(struct, buf.getShort());
		else if (obj instanceof Integer)
			f.set(struct, buf.getInt());
		else if (obj instanceof Long)
			f.set(struct, buf.getLong());
		else if (obj instanceof Float)
			f.set(struct, buf.getFloat());
		else if (obj instanceof Double)
			f.set(struct, buf.getDouble());
		else if (obj instanceof String || f.getType().isAssignableFrom(String.class)) {
			if (!f.isAnnotationPresent(StringLength.class))
				return;
			int len = f.getAnnotation(StringLength.class).size();
			String charset = f.getAnnotation(StringLength.class).charset();

			byte[] strbuf = new byte[len];
			buf.get(strbuf);
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
	}

}
