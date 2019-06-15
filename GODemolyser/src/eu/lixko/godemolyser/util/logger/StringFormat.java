package eu.lixko.godemolyser.util.logger;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StringFormat {
	public static String stringprefix = "";
	public static MsgPriority priority = MsgPriority.ALL;
	
	private static ThreadLocal<ArrayList<Object>> alreadyDumped = ThreadLocal.withInitial(() -> new ArrayList<Object>());
	
	public static final int MODIFIER_SHOW = 1 << 31; 

	public StringFormat() {
	}

	public static boolean canSend(MsgPriority msgprior) {
		return msgprior.value <= priority.value;
	}

	public static String formatMessage(String msg) {
		return stringprefix + msg;
	}

	public static void sendComponent(String str) {
		System.out.println(str);
	}

	public static void sendmsg(String msg) {
		if (canSend(MsgPriority.EXPLICIT))
			sendComponent(stringprefix + msg);
	}

	public static void dirmsg(String msg) {
		if (canSend(MsgPriority.EXPLICIT))
			sendComponent(msg);
	}

	public static void msg(String msg) {
		if (canSend(MsgPriority.GENERAL))
			sendmsg(msg);
	}

	public static void critical(String msg) {
		if (canSend(MsgPriority.MUTE))
			msg(msg);
	}

	public static void info(String msg) {
		if (canSend(MsgPriority.INFO))
			sendmsg(msg);
	}

	public static void warn(String msg) {
		if (canSend(MsgPriority.WARNINGS))
			msg(msg);
	}

	public static void syntaxerror(String msg) {
		if (canSend(MsgPriority.SYNTAXERRORS))
			msg(msg);
	}

	public static void error(String msg) {
		if (canSend(MsgPriority.ERRORS))
			msg(msg);
	}

	public static void confchange(String msg) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.GREEN + msg);
	}

	public static void confnochange(String msg) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.GREEN + msg);
	}

	/*
	 * public static void status(String msg) {
	 * Minecraft.getMinecraft().thePlayer.sendStatusMessage(new
	 * TextComponentString(msg), false); }
	 * 
	 * public static void status(String msg, boolean actionbar) {
	 * Minecraft.getMinecraft().thePlayer.sendStatusMessage(new
	 * TextComponentString(msg), actionbar); }
	 */

	public static void custom(String msg, MsgPriority priority) {
		if (canSend(MsgPriority.GENERAL))
			msg(msg);
	}

	public static void out(String msg) {
		System.out.println(msg);
	}

	public static void unknownCommand(String command, String hint) {
		if (command.trim() != "")
			syntaxerror(ChatColor.RED + "Unknown command " + ChatColor.GRAY + ChatColor.ITALIC + command + ChatColor.RED + "!" + hint.trim() == "" ? "" : ("Hint: " + ChatColor.GRAY + ChatColor.ITALIC + hint));
		else
			syntaxerror(ChatColor.RED + "Empty command!");
	}

	public static void unknownModuleCommand(String modulename, String command, String hint) {
		if (command.trim() != "")
			syntaxerror(ChatColor.RED + "Unknown command " + ChatColor.GRAY + ChatColor.ITALIC + command + ChatColor.RED + "!" + hint.trim() == "" ? "" : ("Hint: " + ChatColor.GRAY + ChatColor.ITALIC + hint));
		else
			syntaxerror(ChatColor.RED + "Empty command!");
	}

	public static void notEnoughArguments() {
		syntaxerror("" + ChatColor.RED + ChatColor.RED + "Not enough arguments!");
	}

	public static void notEnoughArguments(int provided, int required) {
		syntaxerror("" + ChatColor.RED + ChatColor.RED + "Not enough arguments! " + ChatColor.GRAY + "[" + ChatColor.RED + provided + ChatColor.GRAY + " / " + ChatColor.YELLOW + required + ChatColor.GRAY + "]");
	}

	public static void notEnoughArguments(String format) {
		syntaxerror("" + ChatColor.RED + ChatColor.RED + "Not enough arguments! Hint:" + ChatColor.GRAY + format);
	}

	public static void notEnoughArguments(String cmd, String format) {
		syntaxerror("" + ChatColor.RED + ChatColor.RED + "Not enough arguments: " + ChatColor.GRAY + ChatColor.ITALIC + cmd + ChatColor.RED + " " + ChatColor.GOLD + format);
	}

	public static void readconf(String what, String value) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.YELLOW + what + ": " + ChatColor.GOLD + ChatColor.ITALIC + value);
	}

	public static void modreadconf(String modulename, String what, String value) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.GRAY + "[" + modulename + "] " + ChatColor.YELLOW + what + ": " + ChatColor.GOLD + ChatColor.ITALIC + value);
	}

	public static void confchange(String what, String from, String to) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.YELLOW + what + ": " + ChatColor.GOLD + ChatColor.ITALIC + to + ChatColor.GREEN + "!");
	}

	public static void confchange(String what, String to) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.YELLOW + what + ChatColor.GREEN + " was changed to " + ChatColor.GOLD + ChatColor.ITALIC + to + ChatColor.GREEN + "!");
	}

	public static void modmsg(String modulename, String msg) {
		if (canSend(MsgPriority.MODULESGENERAL))
			msg(ChatColor.GRAY + "[" + modulename + "] " + msg);
	}

	public static void moderror(String modulename, String msg) {
		if (canSend(MsgPriority.ERRORS))
			msg(ChatColor.GRAY + "[" + modulename + "] " + msg);
	}

	public static void modwarn(String modulename, String msg) {
		if (canSend(MsgPriority.MODULESWARNINGS))
			msg(ChatColor.GRAY + "[" + modulename + "] " + msg);
	}

	public static void modsyntaxerror(String modulename, String msg) {
		if (canSend(MsgPriority.MODULESWARNINGS))
			msg(ChatColor.GRAY + "[" + modulename + "] " + msg);
	}

	public static void moduleAction(String modulename, String msg) {
		if (canSend(MsgPriority.MODULESEXPLICIT))
			msg(ChatColor.GRAY + "[" + modulename + "] " + msg);
	}

	public static void moduleAction(String modulename, String msg, boolean brackets) {
		if (canSend(MsgPriority.MODULESEXPLICIT))
			if (brackets)
				msg(ChatColor.GRAY + "[" + modulename + "]" + ChatColor.RESET + " " + msg);
			else
				msg("" + ChatColor.YELLOW + ChatColor.ITALIC + modulename + ChatColor.RESET + " " + msg);
	}

	public static void modconfchange(String modulename, String what, String from, String to) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.GRAY + "[" + modulename + "] " + ChatColor.YELLOW + what + ChatColor.GRAY + "(" + from + ")" + ChatColor.GREEN + " was changed to " + ChatColor.GOLD + ChatColor.ITALIC + to + ChatColor.GREEN + "!");
	}

	public static void modconfchange(String modulename, String what, String to) {
		if (canSend(MsgPriority.CONFCHANGE))
			msg(ChatColor.GRAY + "[" + modulename + "] " + ChatColor.YELLOW + what + ChatColor.GREEN + " was changed to " + ChatColor.GOLD + ChatColor.ITALIC + to + ChatColor.GREEN + "!");
	}

	public static void modconfnochange(String modulename, String what, String value) {
		if (canSend(MsgPriority.CONFNOCHANGE))
			msg(ChatColor.GRAY + "[" + modulename + "] " + ChatColor.YELLOW + what + ChatColor.GREEN + " is already " + ChatColor.GOLD + ChatColor.ITALIC + value + ChatColor.GREEN + "!");
	}

	public static void modtoggle(String modulename, int state) {
		if (!canSend(MsgPriority.TOGGLESTATUS))
			return;
		if (state == 1)
			msg(ChatColor.YELLOW + modulename + " " + ChatColor.GREEN + "has been enabled!");
		else if (state == 2)
			msg(ChatColor.YELLOW + modulename + " " + ChatColor.RED + "has been disabled!");
		else if (state == 3)
			msg(ChatColor.YELLOW + modulename + " " + ChatColor.GREEN + "is already enabled!");
		else if (state == 4)
			msg(ChatColor.YELLOW + modulename + " " + ChatColor.RED + "is already disabled!");
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		}

		return true;
	}

	public static boolean isFloat(String s) {
		try {
			Float.parseFloat(s);
		} catch (NumberFormatException e) {
			return false;
		}

		return true;
	}

	public static boolean containsColor(String s) {
		return (s.contains("" + ChatColor.AQUA) || s.contains("" + ChatColor.AQUA));
	}

	// convert from UTF-8 -> internal Java String format
	public static String convertFromUTF8(String s) {
		String out = null;

		try {
			out = new String(s.getBytes("ISO-8859-1"), "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return null;
		}

		return out;
	}

	// convert from internal Java String format -> UTF-8
	public static String convertToUTF8(String s) {
		String out = null;

		try {
			out = new String(s.getBytes("UTF-8"), "ISO-8859-1");
		} catch (java.io.UnsupportedEncodingException e) {
			return null;
		}

		return out;
	}

	public static int find(byte[] arr, char c, int off) {
		for (; off < arr.length; off++) {
			if (arr[off] == c)
				return off;
		}
		return -1;
	}

	public static int find(byte[] arr, char c) {
		for (int off = 0; off < arr.length; off++) {
			if (arr[off] == c)
				return off;
		}
		return -1;
	}

	public static String hex(int n) {
		return String.format("0x%8s", Integer.toHexString(n)).replace(' ', '0');
	}

	public static String hex(long n) {
		return String.format("0x%8s", Long.toHexString(n)).replace(' ', '0');
	}
	
	public static String bin(int n) {
		return String.format("%8s", Integer.toBinaryString(n)).replace(' ', '0');
	}

	public static String bin(long n) {
		return String.format("0x%8s", Long.toBinaryString(n)).replace(' ', '0');
	}

	//public static String hex(byte... n) { return javax.xml.bind.DatatypeConverter.printHexBinary(n); }
	
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String hex(byte... bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}


	public static String dump(float[] arr) {
		String str = "";
		for (int i = 0; i < arr.length; i++) {
			str += String.format("[%d] %.3f, ", i, arr[i]);
		}
		return str;
	}

	public static String dump(int[] arr) {
		String str = "";
		for (int i = 0; i < arr.length; i++) {
			str += "[" + i + "] " + arr[i] + ", ";
		}
		return str;
	}
	
	public static String dump(byte[] arr) {
		Formatter formatter = new Formatter();
		for (int i = 0; i < arr.length; i++) {
			formatter.format("%02x ", arr[i]);
		}
		String ret = formatter.toString();
		formatter.close();
		return ret;
	}

	private static String dumpObj(Object obj, String callerstr, int level, Field curfield, int dumpModifiers, boolean numindices) {
		if (level == 0)
			alreadyDumped.get().clear();
		StringBuilder result = new StringBuilder();
		String newLine = System.getProperty("line.separator");
		if (obj == null)
			return "";

		if (obj instanceof Byte) {
			result.append(String.format("%2s", Integer.toHexString((byte) obj & 0xFF)).replace(' ', '0'));
			return result.toString();
		} else if (obj instanceof Character || obj instanceof String || obj instanceof Float || obj instanceof Integer || obj instanceof Long || obj.getClass().isPrimitive()) {
			result.append(obj);
			return result.toString();
		} else if (obj.getClass().isArray()) {
			try {
				if (obj.getClass().getComponentType().isPrimitive()) {
					result.append(dumpArr(obj, level, curfield, dumpModifiers, numindices));
				} else {
					for (int i = 0; i < Array.getLength(obj); i++) {
						result.append(dumpObj(Array.get(obj, i), "", level + 1, curfield, dumpModifiers, numindices));
					}
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			return result.toString();
		}

		//result.append(newLine);
		result.append(obj.getClass().getName());
		result.append(callerstr);
		result.append(" {");
		result.append(newLine);

		//print field names paired with their values in this class only (no fields of superclass)
		for (Field field : obj.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			int mods = field.getModifiers();
			if ((mods & dumpModifiers) == 0) {
				continue;
			}
			for (int i = 0; i < level; ++i)
				result.append("  ");
			try {
				if ((dumpModifiers & MODIFIER_SHOW) != 0) {
					result.append(Modifier.toString(mods));
					result.append(' ');
				}
				Object fobj = field.get(obj);
				if (fobj == null) {
					result.append("null");
				} else if (field.getType().isArray()) {
					result.append(fobj.getClass().getComponentType().getName());
					result.append("[]");
				} else {
					String fieldType = field.getType().getName();
					if (!fieldType.startsWith("java.lang.")) {
						fieldType = field.getGenericType().toString();
					}
					result.append(fieldType.replaceAll("java.lang.", ""));
				}
				result.append(' ');
				result.append(field.getName());
				if (field.getType().isArray()) {
					result.append(dumpArr(fobj, level, field, dumpModifiers, numindices));
				} else {
					result.append(": ");
					if (field.getType().isPrimitive()) {
						result.append(fobj);
					} else {
						if (fobj == null) {
							result.append("null");
						} else if (alreadyDumped.get().contains(fobj)) {
							result.append("[dumped: ");
							result.append(Integer.toHexString(System.identityHashCode(fobj)));
							result.append("]");
						} else {
							alreadyDumped.get().add(fobj);
							if (field.getClass().getMethod("toString").getDeclaringClass() != Object.class) {
								result.append(fobj.toString());
							} else {
								result.append(dumpObj(fobj, "", level + 1, field, dumpModifiers, numindices));
							}
						}
					}
				}
			} catch (IllegalAccessException | NoSuchMethodException | SecurityException ex) {
				System.out.println(ex);
			}
			result.append(newLine);
		}
		for (int i = 1; i < level; ++i)
			result.append("  ");
		result.append("}");
		
		return result.toString();
	}

	public static String dumpObj(Object obj) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		String callerstr = " @ " + caller.getClassName() + "." + caller.getMethodName() + "(" + caller.getFileName() + ":" + caller.getLineNumber() + ")";
		return dumpObj(obj, callerstr, 1, null, ~0 ^ MODIFIER_SHOW, true);
	}

	public static String dumpObj(Object obj, int dumpModifiers) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		String callerstr = " @ " + caller.getClassName() + "." + caller.getMethodName() + "(" + caller.getFileName() + ":" + caller.getLineNumber() + ")";
		return dumpObj(obj, callerstr, 1, null, dumpModifiers, true);
	}
	
	public static String dumpObj(Object obj, int dumpModifiers, boolean numindices) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		String callerstr = " @ " + caller.getClassName() + "." + caller.getMethodName() + "(" + caller.getFileName() + ":" + caller.getLineNumber() + ")";
		return dumpObj(obj, callerstr, 1, null, dumpModifiers, numindices);
	}

	public static String dumpArr(Object fobj, int level, Field curfield, int dumpModifiers, boolean numindices) throws IllegalAccessException {
		String result = "";
		int size = Array.getLength(fobj);
		result += "[" + size + "]: ";
		for (int x = 0; x < size; x++) {
			if (numindices)
				result += "#" + x + ": ";
			Object aobj = Array.get(fobj, x);
			if (aobj == null)
				result += "null";
			else {
				result += dumpObj(aobj, "", level + 2, curfield, dumpModifiers, numindices);
				//result += aobj.getClass().isPrimitive();
			}
			if (!numindices)
				result += " ";
			else if (x < size - 1)
				result += ", ";
		}
		return result;
	}

	public static <E> E[] fill(E[] arr, Supplier<? extends E> supp) {
		for (int i = 0; i < arr.length; i++) {
			arr[i] = supp.get();
		}
		return arr;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueReverse(Map<K, V> map, boolean reverse) {
		return map.entrySet().stream().sorted(reverse ? Map.Entry.comparingByValue(Collections.reverseOrder()) : Map.Entry.comparingByValue(/* Collections.reverseOrder() */)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static byte[] longToBytes(long l) {
		byte[] result = new byte[8];
		for (int i = 7; i >= 0; i--) {
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	public static long bytesToLong(byte[] b) {
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result <<= 8;
			result |= (b[i] & 0xFF);
		}
		return result;
	}
	
	public static void dumpString(byte[] arr) {
		for(byte b : arr) {
			if(b > 31)
				System.out.print((char)(b & 0xFF));
			else
				System.out.print(' ');
		}
		System.out.println();
	}
	
	public static void dumpBuf(ByteBuffer stream, int len) {
		byte[] data = new byte[len];
		stream.get(data);
		stream.position(stream.position() - len);
		for(byte b : data) {
			System.out.print(StringFormat.bin(b & 0xFF) + " ");
		}
		System.out.println();
		for(byte b : data) {
			System.out.print((b & 0xFF) + " ");
		}
		System.out.println();
		for(byte b : data) {
			if(b > 31)
				System.out.print((char)(b & 0xFF));
			else
				System.out.print(' ');
		}
		System.out.println();
		System.out.println(StringFormat.dumpObj(data, ~0, false));
	}
	
}