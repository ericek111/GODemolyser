package eu.lixko.godemolyser;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.valvesoftware.protos.csgo.Netmessages;
import com.valvesoftware.protos.csgo.Netmessages.CLC_Messages;
import com.valvesoftware.protos.csgo.Netmessages.CSVCMsg_SendTable;
import com.valvesoftware.protos.csgo.Netmessages.SVC_Messages;

import eu.lixko.godemolyser.codec.Celt;
import eu.lixko.godemolyser.codec.Celt.CeltException;
import eu.lixko.godemolyser.codec.VoiceWriter;
import eu.lixko.godemolyser.parser.dem.DemoFile;
import eu.lixko.godemolyser.sdk.DemoFormat.PlayerInfo;
import eu.lixko.godemolyser.sdk.DemoFormat.demoheader_t;
import eu.lixko.godemolyser.util.logger.StringFormat;
import eu.lixko.godemolyser.util.stream.ByteBufferUtils;

public class Main {
	
	public static DemoFile demo;

	public static void main(String[] args) throws IOException {
		
		/* if (args.length < 2) {
			System.err.println("arguments: (path to demo) (steamID64)");
			System.exit(1);
		}*/
		//demo = new DemoFile(new File(args[0]));
		final long voiceSid = 76561198203377300l;//Long.parseLong(args[1]);
		
		demo = new DemoFile(new File("/home/erik/Documents/experiment/voiceinject/meme_2.dem"));
		// demo = new DemoFile(new File("/home/erik/.steam/steam/Steam/steamapps/common/Counter-Strike Global Offensive/csgo/strom.dem"));
		// demo = new DemoFile(new File(args[0]));
		
		VoiceWriter voiceWriter = null;
		
		try {
			voiceWriter = new VoiceWriter(demo, new FileOutputStream("/home/erik/Documents/experiment/voiceinject/modtest.dem", false));
			//voiceWriter.addVoice(100, 1021, "/home/erik/Documents/experiment/voiceinject/rawtest.pcm");
			voiceWriter.addVoice(40, 0, "/home/erik/Documents/experiment/voiceinject/rawtest.pcm");
			voiceWriter.addVoice(200, 1023, "/home/erik/Documents/experiment/voiceinject/pokemon.pcm");
			voiceWriter.addVoice(1500, 1024, "/home/erik/Documents/experiment/voiceinject/rawtest.pcm");
			voiceWriter.inject();
		} catch (CeltException e1) {
			e1.printStackTrace();
		}
		
		demo.on("demoheader", ev -> {
			demoheader_t cmd = (demoheader_t) ev.getData(0);
			System.out.println(StringFormat.dumpObj(cmd));
		});
		demo.on("consolecmd", ev -> {
			String cmd = (String) ev.getData(0);
			if (cmd.charAt(0) == '+' || cmd.charAt(0) == '-')
				return;
			// System.out.println(cmd);
		});
		
		demo.getStringTables().on("userupdate", ev -> {
			PlayerInfo info = (PlayerInfo) ev.getData(0);
			// System.out.println(StringFormat.dumpObj(info));
			System.out.println("user: " + info.name + " > " + info.xuid);
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
		
		ArrayList<Long> xuids = new ArrayList<>();
		FileOutputStream voiceout = new FileOutputStream("/home/erik/Documents/RE/csgo_voice/voicedata.dat", true);
		demo.getPacketParser().on("packet", ev -> {
			int cmd = (int) ev.getData(0);
			byte[] data = (byte[]) ev.getData(1);

			try {
				if (cmd == SVC_Messages.svc_VoiceInit_VALUE) {
					Netmessages.CSVCMsg_VoiceInit voiceData = Netmessages.CSVCMsg_VoiceInit.parseFrom(data);
					// System.out.println(StringFormat.dumpObj(voiceData, Modifier.PRIVATE));
					System.out.println("Quality: " + voiceData.getQuality() + ", " + voiceData.getCodec());
				} else if (cmd == SVC_Messages.svc_VoiceData_VALUE) {
					Netmessages.CSVCMsg_VoiceData voiceData = Netmessages.CSVCMsg_VoiceData.parseFrom(data);
					if (!xuids.contains(voiceData.getXuid())) {
						xuids.add(voiceData.getXuid());
						System.out.println(">>>> " + voiceData.getXuid());
					}
					//if (voiceData.getXuid() != voiceSid)
//						return;
					
					byte[] rawData = voiceData.getVoiceData().toByteArray();
					// System.out.println("by " + voiceData.getXuid() + " (" + voiceData.getClient() + ") " + voiceData.getUncompressedSampleOffset() + " / " + voiceData.getSequenceBytes() + " / " + voiceData.getSectionNumber());
					voiceout.write(rawData);
					// System.out.println("at " + demo.getCurrentTick() + ": " + StringFormat.dumpObj(rawData, Modifier.PRIVATE, false));
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
		
		if (voiceWriter != null) {
			voiceWriter.finish();			
		}
		
		ByteBuffer c_voiceData = ByteBufferUtils.readFile(new File("/home/erik/Documents/RE/csgo_voice/voicedata.dat"));
		
		/*try {
			Celt celtClient = new Celt(22050, 512, 64, 1);
			byte[] gameEncoded = c_voiceData.array();
			byte[] decoded = celtClient.decode(gameEncoded);
			
			try (FileOutputStream stream = new FileOutputStream("/home/erik/Documents/RE/csgo_voice/rawaudio.dat", false)) {
			    stream.write(decoded);
			}

		} catch (CeltException e1) {
			e1.printStackTrace();
		}*/
		
		/*AtomicInteger totalst = new AtomicInteger(0);
		demo.getStringTables().getTables().forEach((k, v) -> {
			totalst.addAndGet( v.length);
		});
		System.out.println("Total StringTables: " + totalst.get());
		for (String key : demo.getStringTables().getTables().keySet()) {
			System.out.println(key);
		}
		
		System.out.println("======================================= client stringtables:");
		for (String key : demo.getStringTables().getClientTables().keySet()) {
			System.out.println(key);
		}
		
		System.out.println("======================================= client entries:");
		System.out.println(StringFormat.dumpObj(demo.getStringTables().getTable("GameRulesCreation")));

		for (CSVCMsg_SendTable st : demo.getDataTables().getSendTables()) {
			st.getNetTableName();
		}*/
		
		System.out.printf("Demo parsed in %.3f ms.\n", (System.nanoTime() - t1) / 1000000.0);
	}


}
