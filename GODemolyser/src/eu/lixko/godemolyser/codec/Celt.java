package eu.lixko.godemolyser.codec;

import java.nio.ByteBuffer;
import java.util.Optional;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import eu.lixko.godemolyser.util.logger.StringFormat;

public class Celt {
	
	static {

		
		
		
	}
	
	// TODO: Destruct and free memory when done.
	
	protected int samplingRate;
	// frameSize samples will be encoded into packetSize bytes
	protected int frameSize;
	protected int packetSize;
	protected int channels;
	protected Pointer celtMode;
	protected Optional<Pointer> decoder = Optional.empty();
	protected Optional<Pointer> encoder = Optional.empty();
	
	/**
	 * Source Engine has 4 CELT "qualities" defined in engine/voice_codecs/celt/voiceencoder_celt.cpp:
	 * sample rate, frame size, packet size (number of bytes returned from celt_de/encode)
	 * 44100, 256, 120
	 * 22050, 120, 60
	 * 22050, 256, 60
	 * 22050, 512, 64
	 */
	public Celt(int samplingRate, int frameSize, int packetSize, int channels) throws CeltException {
		this.samplingRate = samplingRate;
		this.frameSize = frameSize;
		this.packetSize = packetSize;
		this.channels = channels;
		
		IntByReference errorPtr = new IntByReference();
		this.celtMode = CeltNative.INSTANCE.celt_mode_create(samplingRate, frameSize, errorPtr);
		
		if ((errorPtr.getValue()) != 0) {
			throw new CeltException("celt_mode_create exception", errorPtr.getValue());
		}
	}
	
	private void maybeCreateDecoder() throws CeltException {
		if (this.decoder.isPresent()) {
			return;
		}
		
		IntByReference errorPtr = new IntByReference();
		this.decoder = Optional.of(CeltNative.INSTANCE.celt_decoder_create_custom(this.celtMode, this.channels, errorPtr));
		
		int errorCode = errorPtr.getValue();
		if (errorCode != 0) {
			throw new CeltException("celt_decoder_create_custom exception", errorCode);
		}
	}
	
	public void decode(byte[] rawData, Pointer pcm) throws CeltException {
		this.maybeCreateDecoder();
		
		int errorCode = CeltNative.INSTANCE.celt_decode(this.decoder.get(), rawData, rawData.length, pcm, this.frameSize);
		if (errorCode < 0) {
			throw new CeltException("celt_decode exception", errorCode);
		}
	}
	
	// TODO: Maybe support streams.
	public byte[] decode(byte[] encoded) throws CeltException {
		var inBuf = ByteBuffer.wrap(encoded);
		
		// FRAME_SIZE * channels * sizeof(celt_int16)
		final int outFrameSize = this.frameSize * this.channels * 2;
		long allocSize = (encoded.length / this.packetSize) * outFrameSize;
		
		try (var buf = new Memory(allocSize)) {
			
			int frames = 0;
			byte[] inChunk = new byte[this.packetSize];
			for (; inBuf.hasRemaining(); frames++) {
				inBuf.get(inChunk);
				
				this.decode(inChunk, buf.share(frames * outFrameSize, outFrameSize));
			}
			
			byte[] ret = buf.getByteArray(0l, (int) buf.size());
			
			return ret;
		}
	}
	
	private void maybeCreateEncoder() throws CeltException {
		if (this.encoder.isPresent()) {
			return;
		}
		
		IntByReference errorPtr = new IntByReference();
		this.encoder = Optional.of(CeltNative.INSTANCE.celt_encoder_create_custom(this.celtMode, this.channels, errorPtr));
		
		int errorCode = errorPtr.getValue();
		if (errorCode != 0) {
			throw new CeltException("celt_encoder_create_custom exception", errorCode);
		}
	}
	
	public void encode(byte[] pcmData, Pointer compressed) throws CeltException {
		this.maybeCreateEncoder();
		
		int errorCode = CeltNative.INSTANCE.celt_encode(this.encoder.get(), pcmData, this.frameSize, compressed, this.packetSize);
		if (errorCode < 0) {
			throw new CeltException("celt_encode exception", errorCode);
		}
	}
	
	// TODO: Maybe support streams.
	public byte[] encode(byte[] decoded) throws CeltException {
		var inBuf = ByteBuffer.wrap(decoded);
		
		// FRAME_SIZE * channels * sizeof(celt_int16)
		final int outFrameSize = this.frameSize * this.channels * 2;
		long allocSize = (decoded.length / outFrameSize) * this.packetSize;
		
		try (var buf = new Memory(allocSize)) {
			int frames = 0;
			byte[] inChunk = new byte[outFrameSize];
			for (; inBuf.hasRemaining(); frames++) {
				inBuf.get(inChunk);
				
				this.encode(inChunk, buf.share(frames * this.packetSize, this.packetSize));
			}
			
			byte[] ret = buf.getByteArray(0l, (int) buf.size());
			
			return ret;
		}
	}
	
	
	
	public class CeltException extends Exception {
		int errorCode;
		
		public CeltException(String errorMessage, int errorCode) {
			super(errorMessage + ", code " + errorCode);
			this.errorCode = errorCode;
		}
		
		public int getCeltError() {
			return this.errorCode;
		}
	}
	
	public interface CeltNative extends Library {
		
		// LD_LIBRARY_PATH must be set to our csgo's /bin/linux64 folder, e. g.:
		// /media/games/SteamLibrary/steamapps/common/Counter-Strike Global Offensive/bin/linux64
		public static CeltNative INSTANCE = (CeltNative) Native.load("vaudio_celt_client.so", CeltNative.class);
		
		public Pointer celt_mode_create(long samplingRate, int frameSize, IntByReference errorPtr);
		
		public Pointer celt_decoder_create_custom(Pointer mode, int channels, IntByReference errorPtr);
		
		public Pointer celt_encoder_create_custom(Pointer mode, int channels, IntByReference errorPtr);
		
		public int celt_decode(Pointer decoder, byte[] rawData, int rawLen, Pointer pcm, int frameSize);
		
		public int celt_encode(Pointer encoder, byte[] pcmData, int frameSize, Pointer compressed, int maxCompressed);
	}
	
	/*
	private static libcelt instance;
	
	public interface libcelt {
		long celt_mode_create(long samplingRate, int frameSize, long errorPtr);
	}
	
	public static libcelt inst() {
		if (instance == null) {
			instance = LibraryLoader
					.create(libcelt.class)
					.load("/media/games/SteamLibrary/steamapps/common/Counter-Strike Global Offensive/bin/linux64/vaudio_celt_client.so");			
		}
		
		return instance;
	}*/
	
}
