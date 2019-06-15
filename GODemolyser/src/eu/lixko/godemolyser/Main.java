package eu.lixko.godemolyser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

import com.google.protobuf.InvalidProtocolBufferException;
import com.valvesoftware.protos.csgo.Netmessages;
import com.valvesoftware.protos.csgo.Netmessages.CLC_Messages;
import com.valvesoftware.protos.csgo.Netmessages.SVC_Messages;

import eu.lixko.godemolyser.model.StringTable;
import eu.lixko.godemolyser.model.UserInfoStringTable;
import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.parser.dem.DemoFile;
import eu.lixko.godemolyser.sdk.DemoFormat.PlayerInfo;
import eu.lixko.godemolyser.sdk.DemoFormat.demoheader_t;
import eu.lixko.godemolyser.util.logger.StringFormat;

public class Main {

	public static void main(String[] args) throws IOException {
		// DemoFile demo = new DemoFile(new File("/home/erik/Documents/foss/demoanalyser/powergod.dem"));
		// DemoFile demo = new DemoFile(new File("/home/erik/.steam/steam/steamapps/common/Counter-Strike Global Offensive/csgo/mfkznova.dem"));
		DemoFile demo = new DemoFile(new File("/run/media/erik/DATA/SteamLibrary/steamapps/common/Counter-Strike Global Offensive/csgo/auto-20190608-222527-de_mirage.dem"));
		

		demo.on("demoheader", ev -> {
			demoheader_t cmd = (demoheader_t) ev.getData(0);
			System.out.println(StringFormat.dumpObj(cmd));
		});
		demo.on("consolecmd", ev -> {
			String cmd = (String) ev.getData(0);
			if (cmd.charAt(0) == '+' || cmd.charAt(0) == '-' || !cmd.equals("stop"))
				return;
			System.out.println(cmd);
		});
		demo.getStringTables().on("userupdate", ev -> {
			PlayerInfo info = (PlayerInfo) ev.getData(0);
			String sID3 = "STEAM_1:" + (info.xuid & 1) + ":" + ((info.xuid >> 1) & 0x7FFFFFF);
			System.out.println("user: " + info.name + " > " + info.xuid + " > " + sID3);
		});
		demo.getPacketParser().on("packet", ev -> {
			int cmd = (int) ev.getData(0);
			if (cmd != SVC_Messages.svc_Print_VALUE)
				return;

			byte[] data = (byte[]) ev.getData(1);
			try {
				Netmessages.CSVCMsg_Print print = Netmessages.CSVCMsg_Print.parseFrom(data);
				// System.out.print(print.getText());
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		});
		
		demo.getPacketParser().on("packet", ev -> {
			int cmd = (int) ev.getData(0);
			byte[] data = (byte[]) ev.getData(1);
			
			try {
				if (cmd == SVC_Messages.svc_UpdateStringTable_VALUE) {
					Netmessages.CSVCMsg_UpdateStringTable update = Netmessages.CSVCMsg_UpdateStringTable.parseFrom(data);
					demo.getStringTables().parse(update);
				} else if (cmd == SVC_Messages.svc_CreateStringTable_VALUE) {
					Netmessages.CSVCMsg_CreateStringTable create = Netmessages.CSVCMsg_CreateStringTable.parseFrom(data);
					demo.getStringTables().parse(create);
				} else {
					return;
				}
			} catch (InvalidProtocolBufferException | InvalidDataException e) {
				e.printStackTrace();
			}
		});
		demo.getStringTables().on("new", ev -> {
			StringTable st = (StringTable) ev.getData(0);
			if (st.getName().equals("userinfo")) {
				st.on("userdata", ev2 -> {
					UserInfoStringTable ust = (UserInfoStringTable) st; 
					int idx = (int) ev2.getData(0);
					PlayerInfo info = ust.getPlayerInfo(idx);
					//System.out.println(demo.getCurrentTick() + " : " + "# " + idx + " > " + info.xuid + " > " + info.name);
				});
			}
			
		});

		FileOutputStream voiceout = new FileOutputStream("/home/erik/Documents/RE/csgo_voice/voicedata.dat");
		demo.getPacketParser().on("packet", ev -> {
			int cmd = (int) ev.getData(0);
			byte[] data = (byte[]) ev.getData(1);

			try {
				if (cmd == SVC_Messages.svc_VoiceInit_VALUE) {
					Netmessages.CSVCMsg_VoiceInit voiceData = Netmessages.CSVCMsg_VoiceInit.parseFrom(data);
					// System.out.println(StringFormat.dumpObj(voiceData, Modifier.PRIVATE));
					// System.out.println("Quality: " + voiceData.getQuality() + ", " + voiceData.getCodec());
				} else if (cmd == SVC_Messages.svc_VoiceData_VALUE) {
					Netmessages.CSVCMsg_VoiceData voiceData = Netmessages.CSVCMsg_VoiceData.parseFrom(data);
					if (voiceData.getXuid() != 76561198278091177l)
						return;
					byte[] rawData = voiceData.getVoiceData().toByteArray();
					voiceout.write(rawData);
					//System.out.println("by " + voiceData.getXuid() + " / " + voiceData.getUncompressedSampleOffset() + " / " + voiceData.getSequenceBytes());
					//System.out.println(StringFormat.dumpObj(rawData, Modifier.PRIVATE, false));
				} else {
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		long t1 = System.nanoTime();
		demo.parse();
		voiceout.close();
		
		System.out.printf("Demo parsed in %.3f ms.\n", (System.nanoTime() - t1) / 1000000.0);
	}

}
