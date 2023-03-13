package eu.lixko.godemolyser.codec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.protobuf.ByteString;
import com.valvesoftware.protos.csgo.Netmessages;
import com.valvesoftware.protos.csgo.Netmessages.SVC_Messages;
import com.valvesoftware.protos.csgo.Netmessages.VoiceDataFormat_t;
import com.valvesoftware.protos.csgo.Netmessages.CSVCMsg_VoiceData.Builder;

import eu.lixko.godemolyser.codec.Celt.CeltException;
import eu.lixko.godemolyser.parser.InvalidDataException;
import eu.lixko.godemolyser.parser.dem.DemoFile;
import eu.lixko.godemolyser.sdk.DemoFormat;
import eu.lixko.godemolyser.sdk.DemoFormat.dem_msg;
import eu.lixko.godemolyser.util.logger.StringFormat;
import eu.lixko.godemolyser.util.stream.ByteArrayDataStream;
import eu.lixko.godemolyser.util.stream.ByteBufferUtils;
import eu.lixko.godemolyser.util.struct.ByteBufferStructWriter;

/*
 * Sound files can be converted with e. g. sox:
 * sox memesound.opus -r 22050 -c 1 -b 16 -t raw ./memesound.pcm
 */
public class VoiceWriter {
	
	final int SAMPLE_RATE = 22050;
	final int FRAME_SIZE = 512;
	final int PACKET_SIZE = 64;
	
	protected DemoFile demo;
	protected FileOutputStream outStream;
	protected TreeMap<Integer, List<VoiceLine>> filesAtTicks = new TreeMap<>();
	protected ArrayList<VoiceLine> queuedPlaybacks = new ArrayList<>();
	protected ArrayList<VoiceLine> endedPlaybacks = new ArrayList<>();
	protected Celt celtClient;
	protected ByteBuffer structBuf = ByteBuffer.allocate(1024 * 256);
	protected ByteBufferStructWriter structWriter = new ByteBufferStructWriter(structBuf);
	
	private int lastCheckedTick = -1;
	private int lastInsertedTick = -1;
	private byte[] frameBuf = new byte[FRAME_SIZE * 2];
	private byte[] packetBuf = new byte[PACKET_SIZE];
	private long lastInsertedPos = 0;
	private boolean demoHasEnded = false;
	private boolean hasPacketsToCommit = false;
	private long packetDataEnd = 0;
	private boolean wasZero = false; // sometimes demos have weird things in the beginning
	
	private DemoFormat.democmdheader_t cmdh = new DemoFormat.democmdheader_t();
	private DemoFormat.democmdinfo_t cmdinfo = new DemoFormat.democmdinfo_t();
	
	public VoiceWriter(DemoFile demo, FileOutputStream outStream) throws CeltException {
		this.demo = demo;
		this.celtClient = new Celt(SAMPLE_RATE, FRAME_SIZE, PACKET_SIZE, 1);
		this.outStream = outStream;
		
		cmdh.cmd = (byte) dem_msg.dem_packet.ordinal();
	}
	
	public void inject() {
		this.demo.on("end", ev -> {
			System.out.println("last cmd!");
			// this.demoHasEnded = true;
		});
		
		this.demo.getPacketParser().on("packet_end", ev -> {
			var currentTick = this.demo.getCurrentTick();
			if (currentTick == 0)
				wasZero = true;
			
			if (this.demoHasEnded || !this.wasZero)
				return;
			
			if (currentTick <= this.lastInsertedTick)
				return;
			this.lastInsertedTick = currentTick;
						
			var playbacksIterator = this.queuedPlaybacks.iterator();
			while (playbacksIterator.hasNext()) {
				var line = playbacksIterator.next();
				try {
					this.processOneLine(line);
				} catch (CeltException | IOException | InvalidDataException e) {
					System.err.println("Failed to process line for " + line.path + " starting at " + line.startTick);
					e.printStackTrace();
					playbacksIterator.remove();
				}
			}
			
			for (var line : this.endedPlaybacks) {
				this.queuedPlaybacks.remove(line);
			}
			
			if (hasPacketsToCommit) {
				try {
					this.commitPackets();  // write to the output
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (currentTick <= lastCheckedTick) {
				return;
			}
			
			// get all scheduled voice lines (excluding) between the last processed tick and the current one (including)
			NavigableMap<Integer, List<VoiceLine>> subMap = this.filesAtTicks.subMap(lastCheckedTick, false, currentTick, true);
			this.lastCheckedTick = currentTick;
			if (subMap.isEmpty()) {
				return;
			}
			
			subMap.forEach((tick, list) -> {
				list.forEach(line -> this.queuePlayback(line));
			});
		});
	}
	
	public void finish() {
		try {
			this.maybeCopyFromDemo();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void queuePlayback(VoiceLine voiceLine) {
		this.queuedPlaybacks.add(voiceLine);
		System.out.println("Queuing " + voiceLine.path + " at " + this.demo.getCurrentTick() + " / should've been " + voiceLine.startTick);

	}
	
	protected void processOneLine(VoiceLine voiceLine) throws CeltException, IOException, InvalidDataException {
		int currentTick = this.demo.getCurrentTick();
		int tickRate = this.demo.getTickRate();
		int tickProgress = currentTick - voiceLine.startTick;
		
		/*while (voiceLine.buf.hasRemaining()) {
			// TODO: Fill the rest of the last buffer with 0.
			voiceLine.buf.get(this.frameBuf, 0, Math.min(this.frameBuf.length, voiceLine.buf.remaining()));
			byte[] encoded = this.celtClient.encode(frameBuf);
			this.writeFakePacket(voiceLine, encoded);
		}*/
		
		 // half a second of buffer to prevent stutters in-game
		long shouldBeAtSample = Math.round(SAMPLE_RATE * ((double) tickProgress / tickRate + 0.2));
		long shouldBeAtByte = (shouldBeAtSample * 2);
		
		// At most 511 samples will be cut off here. I'm too lazy to implement some kind of partial retrieval.
		while (voiceLine.buf.position() < shouldBeAtByte && voiceLine.buf.remaining() >= this.frameBuf.length) {
			voiceLine.buf.get(this.frameBuf, 0, Math.min(this.frameBuf.length, voiceLine.buf.remaining()));
			byte[] encoded = this.celtClient.encode(frameBuf);
			this.writeFakePacket(voiceLine, encoded);
			
			// System.out.println("Offered " + shouldBeAtByte + " B at " + this.demo.getCurrentTick() + " / should be at " + shouldBeAtSample + " pos: " + voiceLine.buf.position());
		}
	}
	
	protected void writeFakePacket(VoiceLine voiceLine, byte[] encodedVoice) throws IOException, InvalidDataException {
		/*
		Making our own packets does not work -- the seq in and out numbers need to match for CS:GO to even consider
		processing the packets. If set to 0, it stops processing the demo any further (the game does not freeze, but
		it looks like a dropped connection). If set to some random non-zero value, it doesn't get processed out of order.
		So, we piggyback on some other packets. Cons: reduced temporal resolution, manageable with bigger buffers.
		
		structBuf.rewind();
		
		cmdh.tick = this.demo.getCurrentTick();
		cmdh.writeUsing(this.structWriter);
		
		for (int i = 0; i < DemoFormat.MAX_SPLITSCREEN_CLIENTS; i++) {
			cmdinfo.writeUsing(this.structWriter);
		}
		
		this.structBuf.putInt(this.demo.seqIn + 1); // SeqNrIn
		this.structBuf.putInt(this.demo.seqIn + 100); // SeqNrOut
		
		this.outStream.write(this.structBuf.array(), 0, this.structBuf.position());*/
		
		if (!hasPacketsToCommit) {
			this.structBuf.rewind();
			
			packetDataEnd = this.demo.getMainStream().byteIndex();
			
			// rewind the demo before the size int and data
			this.demo.getMainStream().byteIndex(this.demo._packetDataStart);
			this.maybeCopyFromDemo(); // copy everything until now
			
			// read the already parsed packet data (from the event) into our temp. buf.
			byte[] rawData = this.demo.getMainStream().readIBytes();
			this.structBuf.put(rawData);
			
			this.demo.getMainStream().byteIndex(packetDataEnd);
			this.lastInsertedPos = packetDataEnd; // we're copying the packet, too
			
			hasPacketsToCommit = true;
		}

		Builder msgBuilder = Netmessages.CSVCMsg_VoiceData.newBuilder();
		msgBuilder.setXuid(0);
		msgBuilder.setVoiceData(ByteString.copyFrom(encodedVoice));
		msgBuilder.setFormat(VoiceDataFormat_t.VOICEDATA_FORMAT_ENGINE);
		msgBuilder.setClient(voiceLine.client);
		
		var voiceMsg = msgBuilder.build();
		var msgData = voiceMsg.toByteArray();
		
		var cmdArr = com.google.protobuf.Int32Value.of(SVC_Messages.svc_VoiceData_VALUE).toByteArray();
		this.structBuf.put(cmdArr, 1, cmdArr.length - 1); // get rid of the 08 byte in front of our varint, courtesy of ^
		
		var dataLenArr = com.google.protobuf.Int32Value.of(msgData.length).toByteArray();
		this.structBuf.put(dataLenArr, 1, dataLenArr.length - 1);
		
		this.structBuf.put(msgData);
		
	}
	
	protected void maybeCopyFromDemo() throws IOException {
		var currentPos = this.demo.getMainStream().byteIndex();
		if (currentPos == this.lastInsertedPos) {
			// This can only be true if we have already copied everything. 
			return;
		}
		
		// Allocate a buffer for all the demo data we have received until now.
		// TODO: This can be done MUCH more efficiently, for example by not allocating the buffer every time.
		byte[] demoData = new byte[(int) (currentPos - this.lastInsertedPos)];
		this.demo.getMainStream().getBytes(this.lastInsertedPos * 8, demoData);
		this.outStream.write(demoData);
		
		// System.out.println("Wrote " + demoData.length + " B into " + currentPos + " (before at " + this.lastInsertedPos + ")");
		
		this.lastInsertedPos = currentPos;
	}
	
	protected void commitPackets() throws IOException {
		this.outStream.write(ByteBufferUtils.intToByteArrayBE(this.structBuf.position()));
		this.outStream.write(this.structBuf.array(), 0, this.structBuf.position());
		hasPacketsToCommit = false;
	}
	
	protected void cutIntoDemo() throws IOException {
		var currentPos = this.demo.getMainStream().byteIndex();
		if (currentPos == this.lastInsertedPos) {
			// This can only be true if we have already copied everything. 
			return;
		}
		
		// Allocate a buffer for all the demo data we have received until now.
		// TODO: This can be done MUCH more efficiently, for example by not allocating the buffer every time.
		byte[] demoData = new byte[(int) (currentPos - this.lastInsertedPos)];
		this.demo.getMainStream().getBytes(this.lastInsertedPos * 8, demoData);
		this.outStream.write(demoData);
		
		System.out.println("@ " + this.demo.getCurrentTick() + "   " + "Wrote " + demoData.length + " B into " + currentPos + " (before at " + this.lastInsertedPos + ")");
		
		this.lastInsertedPos = currentPos;
	}
	
	protected void endLine(VoiceLine voiceLine) {
		this.endedPlaybacks.add(voiceLine);
	}
	
	public void addVoice(int tick, int client, String path) throws IOException {
		var line = new VoiceLine(tick, client, path);
		filesAtTicks
			.computeIfAbsent(tick, k -> new ArrayList<>())
			.add(line);
	}
	
	protected class VoiceLine {
		
		public int startTick;
		public int client;
		public String path;
		public ByteBuffer buf;
		
		protected VoiceLine(int startTick, int client, String path) throws IOException {
			this.startTick = startTick;
			this.client = client;
			this.path = path;
			this.buf = ByteBufferUtils.readFile(new File(path));
		}
	}
	
}
