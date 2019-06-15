package eu.lixko.godemolyser.parser.dem;

public interface IPacketParser {
	
	public void parsePacket(int cmd, byte[] data);
	
}
