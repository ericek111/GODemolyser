package eu.lixko.godemolyser.parser.dem;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eu.lixko.godemolyser.Main;
import eu.lixko.godemolyser.events.Eventable;
import eu.lixko.godemolyser.parser.IChunkParser;
import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.sdk.Const;
import eu.lixko.godemolyser.sdk.DemoFormat.PlayerInfo;
import eu.lixko.godemolyser.util.logger.StringFormat;
import eu.lixko.godemolyser.util.stream.ByteArrayDataStream;
import eu.lixko.godemolyser.util.stream.DataStream;
import eu.lixko.godemolyser.util.struct.ByteBufferStructReader;
import eu.lixko.godemolyser.util.struct.DataStreamStructReader;

public class StringTableParser extends Eventable implements IChunkParser {
	
	private PlayerInfo[] userinfoEntries = StringFormat.fill(new PlayerInfo[Const.MAXPLAYERS + 1], () -> new PlayerInfo());
	private HashMap<String, String[]> tables = new HashMap<>();
	private HashMap<String, String[]> clientTables = new HashMap<>();
	private HashMap<Integer, byte[]> instanceBaselines;
	
	public StringTableParser(HashMap<Integer, byte[]> instanceBaselines) {
		this.instanceBaselines = instanceBaselines;
	}
	
	@Override
	public void parse(DataStream chunkbuf) throws InvalidDataException {
		int numTables = chunkbuf.readUByte();
		for(int i = 0; i < numTables; i++) {
			
			String tableName = chunkbuf.readString();
			int numEntries = chunkbuf.readUShort();
			String[] entries = new String[numEntries];
			
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + tableName + " with " + numEntries + " @ " + Main.demo.getMainStream().byteIndex() + ", locally " + chunkbuf.byteIndex() + ", len " + chunkbuf.byteLength() + " = " + (chunkbuf.bitLength() - chunkbuf.bitIndex()) + " b rem");
			
			for(int entryIndex = 0; entryIndex < numEntries; entryIndex++) {
				String entry = chunkbuf.readString();
				entries[entryIndex] = entry;
				
				if (tableName.contains("Rules"))
				System.out.println("entry: @ " + chunkbuf.remaining() + ": " + entry);
				
				if(chunkbuf.readBitBoolean()) { // hasUserData
					
					int userDataSize = chunkbuf.readUShort();
					byte[] userData = new byte[userDataSize];
					chunkbuf.readBytes(userData);
					
					System.out.println("Has userdata: " + (userDataSize * 8) + " > " + new String(userData) + ", " + StringFormat.dump(userData));
					
					// System.out.println(tableName + " / " + userDataSize + ": " + StringFormat.dump(userData) + " > "+ new String(userData));

					if(tableName.equals("userinfo")) {
						int idx = Integer.parseInt(entry);
						ByteBuffer userDataBuf = ByteBuffer.wrap(userData);
						ByteBufferStructReader structreader = new ByteBufferStructReader(userDataBuf);
						userDataBuf.order(ByteOrder.BIG_ENDIAN);
						userinfoEntries[idx].readUsing(structreader);
						userDataBuf.order(ByteOrder.LITTLE_ENDIAN);
						this.fire("userupdate", userinfoEntries[idx]);
					} else if (tableName.equals("instancebaseline")) {
						int classId = Integer.parseInt(entry);
						this.instanceBaselines.put(classId, userData);
					}
					
				} else if (tableName.contains("Rules")) {
					System.out.println("BROKEN");
					System.exit(0);
				}
				
				//if (tableName.equals("instancebaseline"))
				//System.out.println(" P " + entry + "   @ " + chunkbuf.bitIndex() + ", #" + entryIndex);
			}
			this.tables.put(tableName, entries);
			
			// handle client-side entries
			if(chunkbuf.readBitBoolean()) {
				int numStrings = chunkbuf.readUShort();
				String[] entries_c = new String[numStrings];
				
				for(int entryIndex = 0; entryIndex < numStrings; entryIndex++) {
					String entry = chunkbuf.readString();
					entries_c[entryIndex] = entry;
					//System.out.println(" >  " + entry + "   @ " + chunkbuf.bitIndex());
					
					if(chunkbuf.readBitBoolean()) { // is this even possible?
						int userDataSize = chunkbuf.readUShort();
						byte[] userData = new byte[userDataSize];
						chunkbuf.readBytes(userData);
					}
				}
				this.clientTables.put(tableName, entries_c);
			}
			
		}
	}
	
	public PlayerInfo getUserInfo(int idx) {
		return this.userinfoEntries[idx];
	}
	
	public PlayerInfo[] getUserInfoEntries() {
		return this.userinfoEntries;
	}
	
	public String getEntry(String table, int idx) {
		return this.tables.get(table)[idx];
	}
	
	public String[] getTable(String name) {
		return this.tables.get(name);
	}
	
	public HashMap<String, String[]> getTables() {
		return this.tables;
	}

	public String getClientEntry(String table, int idx) {
		return this.tables.get(table)[idx];
	}
	
	public String[] getClientTable(String name) {
		return this.clientTables.get(name);
	}
	
	public HashMap<String, String[]> getClientTables() {
		return this.clientTables;
	}

}
