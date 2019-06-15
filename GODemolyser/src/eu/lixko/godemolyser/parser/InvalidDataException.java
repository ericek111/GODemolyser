package eu.lixko.godemolyser.parser;

public class InvalidDataException extends Exception {

	public InvalidDataException(String msg) {
		super(msg);
	}
	
	public InvalidDataException(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = -7794282515354947121L;

}
