package eu.lixko.godemolyser.util;

public class MathUtil {
	public static int log2(int n) {
		if (n <= 0)
			throw new IllegalArgumentException();
		return 31 - Integer.numberOfLeadingZeros(n);
	}
}
