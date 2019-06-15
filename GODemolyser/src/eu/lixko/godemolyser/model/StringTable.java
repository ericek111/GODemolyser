package eu.lixko.godemolyser.model;

import java.util.HashMap;
import java.util.Map;

import eu.lixko.godemolyser.events.Eventable;

public class StringTable extends Eventable {
	private HashMap<Integer, String> entries = new HashMap<>();
	private HashMap<Integer, String> clientEntries = new HashMap<>();
	private HashMap<Integer, byte[]> userData = new HashMap<>();
	private String name;
	private int tableIdx;
	private int numEntries;
	private int maxEntries;
	private boolean userDataFixedSize;
	private int userDataSize;
	private int userDataSizeBits;
	private int flags;

	public StringTable(int idx, String name) {
		this.tableIdx = idx;
		this.name = name;
	}

	public StringTable(int idx, String name, int numEntries, int maxEntries, boolean userDataFixedSize, int userDataSize, int userDataSizeBits, int flags) {
		this.name = name;
		this.numEntries = numEntries;
		this.maxEntries = maxEntries;
		this.userDataFixedSize = userDataFixedSize;
		this.userDataSize = userDataSize;
		this.userDataSizeBits = userDataSizeBits;
		this.flags = flags;
	}

	public int getIndex() {
		return this.tableIdx;
	}

	public String getName() {
		return this.name;
	}

	public int getNumEntries() {
		return this.numEntries;
	}

	public int getMaxEntries() {
		return this.maxEntries;
	}

	public boolean hasUserDataFixedSize() {
		return userDataFixedSize;
	}

	public int getUserDataSize() {
		return userDataSize;
	}

	public int getUserDataSizeBits() {
		return userDataSizeBits;
	}
	
	public int getFlags() {
        return flags;
    }

	public Map<Integer, String> getEntries() {
		return this.entries;
	}

	public String getEntry(int idx) {
		return entries.get(idx);
	}

	public void setEntry(int idx, String value) {
		entries.put(idx, value);
		//numEntries = idx + 1;
		this.fire("entry", idx, value);
	}

	public Map<Integer, String> getClientEntries() {
		return this.clientEntries;
	}

	public String getClientEntry(int idx) {
		return clientEntries.get(idx);
	}

	public void setClientEntry(int idx, String value) {
		clientEntries.put(idx, value);
		this.fire("cliententry", idx, value);
	}

	public Map<Integer, byte[]> getUserDataEntries() {
		return this.userData;
	}

	public byte[] getUserData(int idx) {
		return this.userData.get(idx);
	}

	public boolean hasUserData(int idx) {
		return this.userData.get(idx) != null;
	}

	public void setUserData(int idx, byte[] data) {
		this.userData.put(idx, data);
		this.fire("userdata", idx, data);
	}
}
