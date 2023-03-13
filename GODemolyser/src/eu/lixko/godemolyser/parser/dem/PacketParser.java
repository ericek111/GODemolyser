package eu.lixko.godemolyser.parser.dem;

import eu.lixko.godemolyser.events.Eventable;
import eu.lixko.godemolyser.events.ServerInfoAvailable;
import eu.lixko.godemolyser.parser.IChunkParser;
import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.util.stream.DataStream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.valvesoftware.protos.csgo.Netmessages;
import com.valvesoftware.protos.csgo.Netmessages.NET_Messages;
import com.valvesoftware.protos.csgo.Netmessages.SVC_Messages;

public class PacketParser extends Eventable implements IChunkParser {

	@Override
	public void parse(DataStream chunk) throws InvalidDataException {
		this.fire("packet_start");
		while(chunk.remaining() > 0) {
			int cmd = chunk.readVarInt32();
			var pos2 = chunk.byteIndex();
			byte[] data = chunk.readVBytes();
			var pos3 = chunk.byteIndex();
			
			if (cmd == SVC_Messages.svc_VoiceData_VALUE)
			// System.out.println(cmd + " / " + (pos3 - pos2) + " | " + data.length );
			
			
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
			
			if (cmd == SVC_Messages.svc_ServerInfo_VALUE) {
				try {
					Netmessages.CSVCMsg_ServerInfo serverInfo = Netmessages.CSVCMsg_ServerInfo.parseFrom(data);
					this.fire(new ServerInfoAvailable(serverInfo));
				} catch (InvalidProtocolBufferException e) {
					e.printStackTrace();
				}
			}
			//System.out.println("cmd: " + msgtype.name() + " / " + data.length);		
		}
		this.fire("packet_end");
	}

}
