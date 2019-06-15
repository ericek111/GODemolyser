package eu.lixko.godemolyser.parser.dem;

import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.valvesoftware.protos.csgo.Netmessages;

import eu.lixko.godemolyser.events.Eventable;
import eu.lixko.godemolyser.model.InstanceBaselineStringTable;
import eu.lixko.godemolyser.model.UserInfoStringTable;
import eu.lixko.godemolyser.model.StringTable;
import eu.lixko.godemolyser.parser.IChunkParser;
import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.sdk.DemoFormat;
import eu.lixko.godemolyser.util.MathUtil;
import eu.lixko.godemolyser.util.logger.StringFormat;
import eu.lixko.godemolyser.util.stream.ByteArrayDataStream;
import eu.lixko.godemolyser.util.stream.DataStream;

public class StringTableParser extends Eventable implements IChunkParser {

	private HashMap<String, StringTable> tablesByName = new HashMap<>();
	private ArrayList<StringTable> tablesByIdx = new ArrayList<>();

	@Override
	public void parse(DataStream chunkbuf) throws InvalidDataException {
		int numTables = chunkbuf.readUByte();
		tablesByIdx.ensureCapacity(numTables);
		
		for (int i = 0; i < numTables; i++) {
			String tableName = chunkbuf.readString();			
			int numEntries = chunkbuf.readUShort();
			
			StringTable st = insertTable(i, tableName, numEntries, numEntries);
			
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + tableName + " with " + numEntries);

			for (int entryIndex = 0; entryIndex < numEntries; entryIndex++) {
				String entry = chunkbuf.readString();
				st.setEntry(entryIndex, entry);

				if (chunkbuf.readBitBoolean()) { // hasUserData
					int userDataSize = chunkbuf.readUShort();
					byte[] userData = new byte[userDataSize];
					chunkbuf.readBytes(userData);
					st.setUserData(entryIndex, userData);
				}
			}

			// handle client-side entries
			if (chunkbuf.readBitBoolean()) {
				int numStrings = chunkbuf.readUShort();

				for (int entryIndex = 0; entryIndex < numStrings; entryIndex++) {
					String entry = chunkbuf.readString();
					st.setClientEntry(entryIndex, entry);

					if (chunkbuf.readBitBoolean()) { // is this even possible?
						int userDataSize = chunkbuf.readUShort();
						byte[] userData = new byte[userDataSize];
						chunkbuf.readBytes(userData);
						st.setUserData(entryIndex, userData);
					}
				}
			}
			
			this.fire("st_done", st);
		}
	}
	
	private void parseSTUpdate(StringTable st, DataStream chunk, int numEntries) throws InvalidDataException {
		int entryBits = MathUtil.log2(st.getMaxEntries());
		if (chunk.readBitBoolean())
			throw new InvalidDataException("Dictionary encoding is not supported (in '" + st.getName() + "')!");
		
		ArrayList<String> history = new ArrayList<>();
		history.ensureCapacity((int) Math.pow(2, DemoFormat.MAX_USERDATA_BITS));
		
		int lastEntry = -1;
		for (int i = 0; i < numEntries; i++) {
			int entryIndex = lastEntry + 1;
			
			if (!chunk.readBitBoolean()) {
				// underflow silently
				entryIndex = chunk.readBits(Math.min(entryBits, chunk.remaining()), false);
			}
			
			if (entryIndex < 0 || entryIndex >= st.getMaxEntries())
				throw new InvalidDataException("Bogus string index " + entryIndex + " / " + st.getMaxEntries() + " (in '" + st.getName() + "')!");
			lastEntry = entryIndex;			
			
			String entry = null;
			
			// has the entry changed?
			if (chunk.readBitBoolean()) {
				// substring check
				if (chunk.readBitBoolean()) {
					int index = chunk.readBits(5, false);
					int bytesToCopy = chunk.readBits(DemoFormat.SUBSTRING_BITS, false);
					String last = history.get(index);
					if (last == null) {
						throw new InvalidDataException("String table entry is delta from non-existent entry " + index + " (in '" + st.getName() + "')!");
					}
					
					entry = last.substring(0, Math.min(last.length(), bytesToCopy)) + chunk.readString();
				} else {
					entry = chunk.readString();
				}
				// System.out.println("CHANGE #" + entryIndex + ": " + st.getName() + " > " + entry);
				st.setEntry(entryIndex, entry);
			} else {
				entry = st.getEntry(entryIndex);
			}
			
			byte[] userData;
			// read in the user data
			if (chunk.readBitBoolean()) {
				// don't read the length, it's fixed length and the length was networked down already
				if (st.hasUserDataFixedSize()) {
					if (st.getUserDataSizeBits() <= 8) {
						userData = new byte[] { (byte) chunk.readBits(st.getUserDataSizeBits(), false) };
					} else {
						int bufsize = st.getUserDataSizeBits() >> 3;
						if ((st.getUserDataSizeBits() & 7) > 0)
							bufsize++;
						userData = new byte[bufsize];
						ByteBuffer buf = ByteBuffer.wrap(userData);
						buf.order(ByteOrder.LITTLE_ENDIAN);
						for (int x = 0; x <= (st.getUserDataSizeBits() >> 3); x++) {
							buf.put(chunk.readByte());
						}
						if ((st.getUserDataSizeBits() & 7) > 0) {
							buf.put((byte) chunk.readBits(st.getUserDataSizeBits() & 7, false));
						}
					}
					// System.out.println("bufsize: " + userData.length + " / " + st.getUserDataSizeBits() + " > " + st.getUserDataSize() );
				} else {
					int bytes = chunk.readBits(DemoFormat.MAX_USERDATA_BITS, false);
					userData = new byte[bytes];
					chunk.readBytes(userData);
				}
				st.setUserData(entryIndex, userData);
				// System.out.println("CHANGE U: " + st.getName() + " > " + entry);

			}
			this.fire("update", st, entryIndex);
			
			history.add(entry);
			if (history.size() >= (int) Math.pow(2, DemoFormat.MAX_USERDATA_BITS)) {
				history.remove(0);
			}
			
		}
	}
	private StringTable insertTable(int idx, String tableName, int numEntries, int maxEntries) {
		return this.insertTable(idx, tableName, numEntries, maxEntries, false, 0, 0, 0);
	}
	
	private StringTable insertTable(int idx, String tableName, int numEntries, int maxEntries, boolean userDataFixedSize, int userDataSize, int userDataSizeBits, int flags) {
		StringTable st;
		if (tableName.equals("userinfo")) {
			st = new UserInfoStringTable(idx, tableName, numEntries, maxEntries, userDataFixedSize, userDataSize, userDataSizeBits, flags);
		} else if (tableName.equals("instancebaseline")) {
			st = new InstanceBaselineStringTable(idx, tableName, numEntries, maxEntries, userDataFixedSize, userDataSize, userDataSizeBits, flags);
		} else {
			st = new StringTable(idx, tableName, numEntries, maxEntries, userDataFixedSize, userDataSize, userDataSizeBits, flags);
		}
		tablesByName.put(tableName, st);
		tablesByIdx.ensureCapacity(idx + 1);
		tablesByIdx.add(idx, st);
		this.fire("new", st);
		return st;
	}
	
	public void parse(Netmessages.CSVCMsg_CreateStringTable packet) throws InvalidDataException {
		if (tablesByName.get(packet.getName()) != null)
			throw new InvalidDataException("CSVCMsg_CreateStringTable: table '" + packet.getName() + "' already exists!");
		//System.out.println(StringFormat.dumpObj(packet, Modifier.PRIVATE));
		StringTable st = insertTable(tablesByIdx.size(), packet.getName(), packet.getNumEntries(), packet.getMaxEntries(), packet.getUserDataFixedSize(), packet.getUserDataSize(), packet.getUserDataSizeBits(), packet.getFlags());
		parseSTUpdate(st, ByteArrayDataStream.wrap(packet.getStringData().toByteArray()), packet.getNumEntries());
	}
	
	public void parse(Netmessages.CSVCMsg_UpdateStringTable packet) throws InvalidDataException {
		if (packet.getTableId() >= tablesByIdx.size())
			throw new InvalidDataException("CSVCMsg_UpdateStringTable: table with index " + packet.getTableId() + " doesn't exists!");
		
		StringTable st = this.tablesByIdx.get(packet.getTableId());
		//System.out.println("UPDATE: " + st.getName());
		
		parseSTUpdate(st, ByteArrayDataStream.wrap(packet.getStringData().toByteArray()), packet.getNumChangedEntries());
	}
	
	public UserInfoStringTable getUserInfoST() {
		return (UserInfoStringTable) this.tablesByName.get("userinfo");
	}
	
	public UserInfoStringTable getInstanceBaselineST() {
		return (UserInfoStringTable) this.tablesByName.get("instancebaseline");
	}

	public StringTable getTable(String name) {
		return this.tablesByName.get(name);
	}
	
	public StringTable getTable(int idx) {
		return this.tablesByIdx.get(idx);
	}

	public List<StringTable> getTables() {
		return this.tablesByIdx;
	}

}
