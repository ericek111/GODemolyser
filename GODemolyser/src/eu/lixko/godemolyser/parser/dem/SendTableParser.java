package eu.lixko.godemolyser.parser.dem;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.valvesoftware.protos.csgo.Netmessages;
import com.valvesoftware.protos.csgo.Netmessages.CSVCMsg_ClassInfo;
import com.valvesoftware.protos.csgo.Netmessages.CSVCMsg_ClassInfo.class_t.Builder;
import com.valvesoftware.protos.csgo.Netmessages.CSVCMsg_SendTable;
import com.valvesoftware.protos.csgo.Netmessages.SVC_Messages;

import eu.lixko.godemolyser.events.Eventable;
import eu.lixko.godemolyser.parser.IChunkParser;
import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.sdk.DataTable;
import eu.lixko.godemolyser.util.stream.DataStream;

public class SendTableParser extends Eventable implements IChunkParser {

	private ArrayList<CSVCMsg_SendTable> sendTables = new ArrayList<>();
	private ArrayList<CSVCMsg_ClassInfo.class_t> serverClasses = new ArrayList<>();
	private HashMap<Integer, byte[]> instanceBaselines;

	public SendTableParser(HashMap<Integer, byte[]> instanceBaselines) {
		this.instanceBaselines = instanceBaselines;
	}

	@Override
	public void parse(DataStream chunk) throws InvalidDataException {
		while (chunk.remaining() > 0) {
			int type = chunk.readVarInt32();
			SVC_Messages msgtype = SVC_Messages.forNumber(type);
			
			if (msgtype == null) {
				throw new InvalidDataException("Unknown SVC_Messages type " + type);
			} else if (!msgtype.equals(SVC_Messages.svc_SendTable)) {
				throw new InvalidDataException("Expected SendTable, got " + msgtype.name());
			}
			
			CSVCMsg_SendTable sendTable;
			try {
				sendTable = CSVCMsg_SendTable.parseFrom(chunk.readVBytes());
			} catch (InvalidProtocolBufferException e) {
				throw new InvalidDataException(e);
			}

			if (sendTable.getIsEnd())
				break;
			sendTables.add(sendTable);

			sendTable.getPropsList().forEach(prop -> {
				if ((prop.getFlags() & DataTable.SPROP_EXCLUDE) != 0) {
					// System.out.println("exclude: " + sendTable.getNetTableName() + " / " + prop.getVarName());
				}
				// if (prop.type.ordinal() == 6) System.out.println("> " + prop.DTName + " / " + prop.varName);
			});
		}

		int serverClassCount = chunk.readShort();
		serverClasses.ensureCapacity(serverClassCount);

		for (int i = 0; i < serverClassCount; i++) {
			int classId = chunk.readShort();
			if (classId != i)
				throw new InvalidDataException("Invalid server class entry (" + classId + ") for " + i);

			String name = chunk.readString();
			String dtName = chunk.readString();
			CSVCMsg_SendTable dataTable = this.findTableByName(dtName);
			if (dataTable == null)
				throw new InvalidDataException("Invalid DataTable for " + i + ": " + dtName + " in " + name);
			
			Builder clb = CSVCMsg_ClassInfo.class_t.newBuilder();
			clb.setClassName(name);
			clb.setDataTableName(dtName);
			clb.setClassId(classId);
			CSVCMsg_ClassInfo.class_t cl = clb.build();
			serverClasses.add(i, cl);
		}

		this.fire("parsed");
	}

	public CSVCMsg_SendTable findTableByName(String name) {
		for (CSVCMsg_SendTable st : sendTables) {
			if (st.getNetTableName().equals(name))
				return st;
		}
		return null;
	}

	public ArrayList<CSVCMsg_SendTable> getSendTables() {
		return this.sendTables;
	}

	public ArrayList<CSVCMsg_ClassInfo.class_t> getServerClasses() {
		return this.serverClasses;
	}

}
