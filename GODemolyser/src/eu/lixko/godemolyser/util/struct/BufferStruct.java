package eu.lixko.godemolyser.util.struct;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BufferStruct {

	private int size = 0;
	private int fieldlen = 0;
	private long lastReadAddr = 0;
	private Exception lastException;
	
	public boolean checkBounds(ByteBuffer buf) {
		return buf.remaining() >= this.countStruct();
	}

	public void readUsing(IStructReader reader) {
		this.beforeRead();
		for (Field f : this.getClass().getDeclaredFields()) {
			if (!Modifier.isPublic(f.getModifiers()) || f.isAnnotationPresent(SkipField.class))
				continue;
			fieldlen = getFieldLen(f);
			try {
				if (f.getType().isArray())
					reader.readArray(this, f.get(this), f);
				else
					reader.readField(this, f);
			} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
				this.lastException = e;
			}
		}
		this.afterRead();
	}


	private int countStruct() {
		for (Field f : this.getClass().getDeclaredFields()) {
			if (!Modifier.isPublic(f.getModifiers()) || f.isAnnotationPresent(SkipField.class))
				continue;
			fieldlen = getFieldLen(f);
			try {
				if (f.getType().isArray())
					countArray(f.get(this));
				else
					countField(f);
			} catch (IllegalArgumentException | IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			}
		}
		return size;
	}

	private void countArray(Object arr) throws InstantiationException, IllegalAccessException {
		for (int i = 0; i < Array.getLength(arr); i++) {
			Object value = Array.get(arr, i);
			if (value == null) {
				value = arr.getClass().getComponentType().newInstance();
				Array.set(arr, i, value);
			}
			if (value.getClass().isArray()) {
				countArray(value);
			} else {
				if (fieldlen > 0)
					size += fieldlen;
				else if (value instanceof Byte)
					size += Byte.BYTES;
				else if (value instanceof Character)
					size += Character.BYTES;
				else if (value instanceof Boolean)
					size += 1;
				else if (value instanceof Short)
					size += Short.BYTES;
				else if (value instanceof Integer)
					size += Integer.BYTES;
				else if (value instanceof Long)
					size += Long.BYTES;
				else if (value instanceof Float)
					size += Float.BYTES;
				else if (value instanceof Double)
					size += Double.BYTES;
				else if (value instanceof BufferStruct)
					size += ((BufferStruct) value).countStruct();
				else
					throw new IllegalArgumentException("Non-primitive types are not implemented yet! " + value.getClass().getName());
			}
			//System.out.println(i + " > " + size);
		}
	}

	private void countField(Field f) throws IllegalArgumentException, IllegalAccessException {
		if (fieldlen > 0) {
			size += fieldlen;
			return;
		}
		Object obj = f.get(this);
		if (obj instanceof Byte)
			size += Byte.BYTES;
		else if (obj instanceof Character)
			size += Character.BYTES;
		else if (obj instanceof Boolean)
			size += 1;
		else if (obj instanceof Short)
			size += Short.BYTES;
		else if (obj instanceof Integer)
			size += Integer.BYTES;
		else if (obj instanceof Long)
			size += Long.BYTES;
		else if (obj instanceof Float)
			size += Float.BYTES;
		else if (obj instanceof Double)
			size += Double.BYTES;
		else if (obj instanceof String || f.getType().isAssignableFrom(String.class)) {
			//TODO: Implement zero-terminated strings with variable length
			if (!f.isAnnotationPresent(StringLength.class))
				return;
			int len = f.getAnnotation(StringLength.class).size();
			size += len;
		} else if (BufferStruct.class.isAssignableFrom(f.getType()))
			size += ((BufferStruct) obj).countStruct();
		else
			throw new IllegalArgumentException("Non-primitive types are not implemented yet! " + (f != null && obj != null ? (f.getName() + " of " + obj.getClass().getName()) : ""));
		// System.err.println(f.getName() + " > " + size);
	}
	
	public int size() {
		if (size == 0)
			countStruct();
		return this.size;
	}
	
	public long lastRead() {
		return this.lastReadAddr;
	}
	
	protected void beforeRead() {
	}
	
	protected void afterRead() {
	}
	
	public static int getFieldLen(Field f) {
		return getFieldLen(f, true);
	}
	
	public static int getFieldLen(Field f, boolean validate) {
		if (f.isAnnotationPresent(UnsignedField.class)) {
			int len = f.getAnnotation(UnsignedField.class).value();
			if (len == 1 || len == 2 || len == 4) {
				return len;
			} else if (validate)
				throw new IllegalArgumentException("Invalid UnsignedField length on " + f.getName() + ": " + len);
		}
		return 0;
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface SkipField {
		public boolean skip() default true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface UnsignedField {
		int value() default 1;
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface StringLength {
		int size() default 1;
		String charset() default "UTF-8";
	}

	// TODO: Implement
	@Retention(RetentionPolicy.RUNTIME)
	public @interface PointerToStruct {
		public boolean isPointer() default true;
	}
	
	// TODO: Implement
	@Retention(RetentionPolicy.RUNTIME)
	public @interface StringPointer {
		public boolean isPointer() default true;
	}

}