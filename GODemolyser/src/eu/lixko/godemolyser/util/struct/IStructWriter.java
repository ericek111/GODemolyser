package eu.lixko.godemolyser.util.struct;

import java.lang.reflect.Field;

public interface IStructWriter {
	
	public void writeField(BufferStruct struct, Field f) throws IllegalArgumentException, IllegalAccessException;
	
	public void writeArray(BufferStruct struct, Object arr, Field f) throws InstantiationException, IllegalAccessException;
	
}
