package eu.lixko.godemolyser.parser;

import eu.lixko.godemolyser.util.stream.DataStream;

public interface IChunkParser {
	
	public void parse(DataStream chunk) throws InvalidDataException;
	
}
