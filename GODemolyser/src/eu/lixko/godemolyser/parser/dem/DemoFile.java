package eu.lixko.godemolyser.parser.dem;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

import eu.lixko.godemolyser.events.Eventable;
import eu.lixko.godemolyser.sdk.DemoFormat;
import eu.lixko.godemolyser.util.logger.StringFormat;
import eu.lixko.godemolyser.util.stream.ByteArrayDataStream;
import eu.lixko.godemolyser.util.stream.DataStream;
import eu.lixko.godemolyser.util.struct.DataStreamStructReader;


public class DemoFile extends Eventable {
	
	private ByteArrayDataStream stream;
	private DataStreamStructReader streamsr;
	private DemoFormat.demoheader_t header = new DemoFormat.demoheader_t();
	private DemoFormat.democmdheader_t cmdh = new DemoFormat.democmdheader_t();
	private DemoFormat.democmdinfo_t[] cmdinfoarr = StringFormat.fill(new DemoFormat.democmdinfo_t[DemoFormat.MAX_SPLITSCREEN_CLIENTS], () -> new DemoFormat.democmdinfo_t());
	private HashMap<Integer, byte[]> instanceBaselines = new HashMap<>();
	private StringTableParser stringTables = new StringTableParser(instanceBaselines);
	private SendTableParser dataTables = new SendTableParser(instanceBaselines);
	private PacketParser packetParser = new PacketParser();
	private int demotick = 0;

	public DemoFile(File f) throws IOException {
		this.stream = ByteArrayDataStream.wrap(Files.readAllBytes(f.toPath()));
	}

	public void parse() {
		this.fire("begin");
		streamsr = new DataStreamStructReader(stream);
		stream.order(ByteOrder.LITTLE_ENDIAN);
		stream.rewind();
		//if ((stream.remaining() * 8) < header.size())
		//	throw new IllegalArgumentException("Failed to open demo: file too small.");
		header.readUsing(streamsr);
		this.fire("demoheader", header);

		if (!Arrays.equals(header.demofilestamp, DemoFormat.DEMO_HEADER_ID))
			throw new IllegalArgumentException("Invalid demo header: " + StringFormat.dumpObj(header.demofilestamp));

		if (header.demoprotocol != DemoFormat.DEMO_PROTOCOL)
			throw new IllegalArgumentException("Invalid demo protocol (" + header.demoprotocol + "), expected " + DemoFormat.DEMO_PROTOCOL);
		
		this.fire("start");
		try {
			while (demoLoop())
				;
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.fire("end");
	}

	private boolean demoLoop() throws Exception {
		if (!readCmdHeader())
			return false;
		
		//if (DemoFormat.dem_msg.values()[cmdh.cmd] == DemoFormat.dem_msg.dem_signon) {
		// System.out.println(" >>> " + DemoFormat.dem_msg.values()[cmdh.cmd].name() + " @ " + cmdh.tick + " @ " + stream.byteIndex());
		
		this.fire("cmd");
		if ((stream.bitIndex() & 7) != 0) {
			//System.out.println(stream.bitIndex() & 7);
			//System.exit(0);
		}
		
		// COMMAND HANDLERS
		switch (DemoFormat.dem_msg.values()[cmdh.cmd]) {
			case dem_synctick:
				break;

			case dem_stop: {
				return false;
			}

			case dem_consolecmd: {
				this.fire("consolecmd", new String(stream.readIBytes()));
			}
				break;
			
			case dem_datatables: {
				dataTables.parse(ByteArrayDataStream.wrap(stream.readIBytes()));
			}
				break;

			case dem_stringtables: {
				stringTables.parse(ByteArrayDataStream.wrap(stream.readIBytes()));
			}
				break;

			case dem_usercmd: {
				stream.readInt(); // outgoing sequence
				byte[] chunk = stream.readIBytes();
			}
				break;

			case dem_signon:
			case dem_packet: {
				for (DemoFormat.democmdinfo_t cmdinfo : cmdinfoarr)
					cmdinfo.readUsing(streamsr);
				
				stream.readInt(); // SeqNrIn
				stream.readInt(); // SeqNrOut
				
				packetParser.parse(ByteArrayDataStream.wrap(stream.readIBytes()));
			}
				break;
			case dem_lastcmd:
				return true;
			default:
				throw new Exception("Unrecognized command: " + cmdh.cmd);
		}

		return true;
	}

	private boolean readCmdHeader() {
		try {
			cmdh.readUsing(streamsr);
		} catch (BufferUnderflowException ex) {
			ex.printStackTrace(System.err);
			return false;
		}
		if (cmdh.cmd < 1 || cmdh.cmd > DemoFormat.dem_msg.dem_lastcmd.ordinal()) {
			System.out.println("Invalid demo cmd (missing end tag?): " + cmdh.cmd + " @ " + stream.byteIndex());
			cmdh.cmd = (byte) DemoFormat.dem_msg.dem_stop.ordinal();
			return false;
		}
		this.fire("cmdheader", cmdh);
		demotick = cmdh.tick;
		return true;
	}

	public StringTableParser getStringTables() {
		return this.stringTables;
	}
	
	public SendTableParser getDataTables() {
		return this.dataTables;
	}
	
	public PacketParser getPacketParser() {
		return this.packetParser;
	}
	
	public int getCurrentTick() {
		return this.demotick;
	}
	
	public DemoFormat.demoheader_t getDemoHeader() {
		return this.header;
	}
	
	public DemoFormat.democmdheader_t getCommandHeader() {
		return this.cmdh;
	}
	
	public DataStream getMainStream() {
		return this.stream;
	}
	
	// Rarely used
	public DemoFormat.democmdinfo_t[] getCommandInfos() {
		return this.cmdinfoarr;
	}

}
