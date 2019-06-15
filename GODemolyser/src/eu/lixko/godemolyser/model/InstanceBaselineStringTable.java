package eu.lixko.godemolyser.model;

import java.util.HashMap;

public class InstanceBaselineStringTable extends StringTable {

	private HashMap<Integer, byte[]> instanceBaselines = new HashMap<>();

	public InstanceBaselineStringTable(int idx, String name) {
		super(idx, name);
	}
	
	public InstanceBaselineStringTable(int idx, String tableName, int numEntries, int maxEntries, boolean userDataFixedSize, int userDataSize, int userDataSizeBits, int flags) {
		super(idx, tableName, numEntries, maxEntries, userDataFixedSize, userDataSize, userDataSizeBits, flags);
	}

	@Override
	public void setUserData(int idx, byte[] data) {
		int classId = Integer.parseInt(this.getEntry(idx));
		this.instanceBaselines.put(classId, data);
		super.setUserData(idx, data);
	}

}
