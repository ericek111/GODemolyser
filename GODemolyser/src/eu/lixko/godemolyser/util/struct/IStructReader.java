package eu.lixko.godemolyser.util.struct;

import java.lang.reflect.Field;

public interface IStructReader {
	
	public void readField(BufferStruct struct, Field f) throws IllegalArgumentException, IllegalAccessException;
	
	public void readArray(BufferStruct struct, Object arr, Field f) throws InstantiationException, IllegalAccessException;
	
}
