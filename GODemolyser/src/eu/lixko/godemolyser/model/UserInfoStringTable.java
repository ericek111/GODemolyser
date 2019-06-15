package eu.lixko.godemolyser.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import eu.lixko.godemolyser.sdk.Const;
import eu.lixko.godemolyser.sdk.DemoFormat.PlayerInfo;
import eu.lixko.godemolyser.util.logger.StringFormat;
import eu.lixko.godemolyser.util.struct.ByteBufferStructReader;

public class UserInfoStringTable extends StringTable {
	
	private PlayerInfo[] userinfoEntries = StringFormat.fill(new PlayerInfo[Const.MAXPLAYERS + 1], () -> new PlayerInfo());

	public UserInfoStringTable(int idx, String name) {
		super(idx, name);
	}
	
	public UserInfoStringTable(int idx, String tableName, int numEntries, int maxEntries, boolean userDataFixedSize, int userDataSize, int userDataSizeBits, int flags) {
		super(idx, tableName, numEntries, maxEntries, userDataFixedSize, userDataSize, userDataSizeBits, flags);
	}
	
	@Override
	public void setUserData(int idx, byte[] data) {
		int entityIdx = Integer.parseInt(this.getEntry(idx));
		ByteBuffer userDataBuf = ByteBuffer.wrap(data);
		ByteBufferStructReader structreader = new ByteBufferStructReader(userDataBuf);
		userDataBuf.order(ByteOrder.BIG_ENDIAN);
		userinfoEntries[entityIdx].readUsing(structreader);
		userDataBuf.order(ByteOrder.LITTLE_ENDIAN);
		super.setUserData(idx, data);
	}
	
	public PlayerInfo getPlayerInfo(int idx) {
		return this.userinfoEntries[idx];
	}
	
	public PlayerInfo getPlayerInfoByEntity(int idx) {
		int entityIdx = Integer.parseInt(this.getEntry(idx));
		return this.userinfoEntries[entityIdx];
	}

}
