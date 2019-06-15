package eu.lixko.godemolyser.model;

import com.valvesoftware.protos.csgo.Netmessages.CSVCMsg_SendTable;

public class ServerClass {
	
	public int classId;
	public String name;
	public CSVCMsg_SendTable sendTable;

	public ServerClass(int classId, String name, CSVCMsg_SendTable sendTable) {
		this.classId = classId;
		this.name = name;
		this.sendTable = sendTable;
	}
	
	public int getClassID() {
		return this.classId;
	}
	
	public String getName() {
		return this.name;
	}
	
	public CSVCMsg_SendTable getSendTable() {
		return this.sendTable;
	}
	
}
