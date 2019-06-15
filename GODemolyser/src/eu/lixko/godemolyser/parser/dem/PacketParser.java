package eu.lixko.godemolyser.parser.dem;

import eu.lixko.godemolyser.events.Eventable;
import eu.lixko.godemolyser.parser.IChunkParser;
import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.util.stream.DataStream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.valvesoftware.protos.csgo.Netmessages.NET_Messages;
import com.valvesoftware.protos.csgo.Netmessages.SVC_Messages;

public class PacketParser extends Eventable implements IChunkParser {

	@Override
	public void parse(DataStream chunk) throws InvalidDataException {
		while(chunk.remaining() > 0) {
			int cmd = chunk.readVarInt32();
			byte[] data = chunk.readVBytes();
			
			this.fire("packet", cmd, data);
			
			SVC_Messages msgtype = SVC_Messages.forNumber(cmd);
			
			if (msgtype == null) {
				NET_Messages netmsg = NET_Messages.forNumber(cmd);
				if (netmsg == null) {
					System.out.println("unknown: " + cmd);
					continue;
				}
				//System.out.println("net: " + netmsg.name());
				continue; // unknown SVC?
			}
			
			//System.out.println("cmd: " + msgtype.name() + " / " + data.length);		
		}
	}

}
