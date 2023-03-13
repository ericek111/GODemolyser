package eu.lixko.godemolyser.events;

import com.valvesoftware.protos.csgo.Netmessages;

public class ServerInfoAvailable extends DemoEvent<Netmessages.CSVCMsg_ServerInfo> {

	public ServerInfoAvailable(Netmessages.CSVCMsg_ServerInfo data) {
		super("serverinfo", data);
	}
	
	public Netmessages.CSVCMsg_ServerInfo getInfo() {
		return this.getData(0);
	}

}
