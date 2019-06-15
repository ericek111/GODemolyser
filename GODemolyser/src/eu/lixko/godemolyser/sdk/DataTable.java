package eu.lixko.godemolyser.sdk;

public class DataTable {

	// Max number of properties in a datatable and its children.
	public static final int MAX_DATATABLES = 1024; // must be a power of 2.
	public static final int MAX_DATATABLE_PROPS = 4096;

	public static final int MAX_ARRAY_ELEMENTS = 2048; // a network array should have more that 1024 elements

	public static final float HIGH_DEFAULT = -121121.121121f;

	public static final int BITS_FULLRES = -1; // Use the full resolution of the type being encoded.
	public static final int BITS_WORLDCOORD = -2; // Encode as a world coordinate.

	public static final int DT_MAX_STRING_BITS = 9;
	public static final int DT_MAX_STRING_BUFFERSIZE = (1 << DT_MAX_STRING_BITS); // Maximum length of a string that can be sent.

	// SendProp::m_Flags.
	public static final int SPROP_UNSIGNED = (1 << 0); // Unsigned integer data.

	public static final int SPROP_COORD = (1 << 1); // If this is set, the float/vector is treated like a world coordinate.
													// Note that the bit count is ignored in this case.

	public static final int SPROP_NOSCALE = (1 << 2); // For floating point, don't scale into range, just take value as is.

	public static final int SPROP_ROUNDDOWN = (1 << 3); // For floating point, limit high value to range minus one bit unit

	public static final int SPROP_ROUNDUP = (1 << 4); // For floating point, limit low value to range minus one bit unit

	public static final int SPROP_NORMAL = (1 << 5); // If this is set, the vector is treated like a normal (only valid for vectors)

	public static final int SPROP_EXCLUDE = (1 << 6); // This is an exclude prop (not excludED, but it points at another prop to be
														// excluded).

	public static final int SPROP_XYZE = (1 << 7); // Use XYZ/Exponent encoding for vectors.

	public static final int SPROP_INSIDEARRAY = (1 << 8); 	// This tells us that the property is inside an array, so it shouldn't be put
															// into the flattened property list. Its array will point at it when it needs to.

	public static final int SPROP_PROXY_ALWAYS_YES = (1 << 9);  // Set for datatable props using one of the default datatable proxies like
																// SendProxy_DataTableToDataTable that always send the data to all clients.

	public static final int SPROP_CHANGES_OFTEN = (1 << 10); // this is an often changed field, moved to head of sendtable so it gets a small index

	public static final int SPROP_IS_A_VECTOR_ELEM = (1 << 11); // Set automatically if SPROP_VECTORELEM is used.

	public static final int SPROP_COLLAPSIBLE = (1 << 12);  // Set automatically if it's a datatable with an offset of 0 that doesn't change
															// the pointer (ie: for all automatically-chained base classes).
															// In this case, it can get rid of this SendPropDataTable altogether and spare
															// the trouble of walking the hierarchy more than necessary.

	public static final int SPROP_COORD_MP = (1 << 13); // Like SPROP_COORD;, but special handling for multiplayer games
	public static final int SPROP_COORD_MP_LOWPRECISION = (1 << 14); // Like SPROP_COORD;, but special handling for multiplayer games where the
																	 // fractional component only gets a 3 bits instead of 5
	public static final int SPROP_COORD_MP_INTEGRAL = (1 << 15); // SPROP_COORD_MP;, but coordinates are rounded to integral boundaries

	public static final int SPROP_VARINT = SPROP_NORMAL; // reuse existing flag so we don't break demo. note you want to include
															// SPROP_UNSIGNED if needed, its more efficient

	public static final int SPROP_NUMFLAGBITS_NETWORKED = 16;

	// This is server side only, it's used to mark properties whose SendProxy_* functions
	// encode against gpGlobals->tickcount (the only ones that currently do this are
	// m_flAnimTime and m_flSimulationTime. MODs shouldn't need to mess with this probably
	public static final int SPROP_ENCODED_AGAINST_TICKCOUNT = (1 << 16);

	// See SPROP_NUMFLAGBITS_NETWORKED for the ones which are networked
	public static final int SPROP_NUMFLAGBITS = 17;

	// Used by the SendProp and RecvProp functions to disable debug checks on type sizes.
	public static final int SIZEOF_IGNORE = -1;

	public static enum SendPropType {
		DPT_Int, DPT_Float, DPT_Vector, DPT_VectorXY, // Only encodes the XY of a vector, ignores Z
		DPT_String, DPT_Array, // An array of the base types (can't be of datatables).
		DPT_DataTable, DPT_Int64,

		DPT_NUMSendPropTypes

	};

	public static class DVariant {
		public DVariant() {
			m_Type = SendPropType.DPT_Float;
		}

		public DVariant(float val) {
			m_Type = SendPropType.DPT_Float;
			m_Float = val;
		}

		@Override
		public String toString() {
			String text;

			switch (m_Type) {
			case DPT_Int:
				text = String.format("%i", m_Int);
				break;
			case DPT_Float:
				text = String.format("%.3f", m_Float);
				break;
			case DPT_Vector:
				text = String.format("(%.3f,%.3f,%.3f)", m_Vector[0], m_Vector[1], m_Vector[2]);
				break;
			case DPT_VectorXY:
				text = String.format("(%.3f,%.3f)", m_Vector[0], m_Vector[1]);
				break;
			case DPT_String:
				if (m_pString != null)
					return m_pString;
				else
					return "NULL";
			case DPT_Array:
				text = String.format("Array");
				break;
			case DPT_DataTable:
				text = String.format("DataTable");
				break;
			case DPT_Int64:
				text = String.format("%I64d", m_Int64);
				break;
			default:
				text = String.format("DVariant type %i unknown", m_Type);
				break;
			}

			return text;
		}

		float m_Float;
		int m_Int;
		String m_pString; // const char*
		long m_pData; // void*, For DataTables.
		float[] m_Vector = new float[3];

		long m_Int64;
		SendPropType m_Type;
	};

	// This can be used to set the # of bits used to transmit a number between 0 and nMaxElements-1.
	public static int NumBitsForCount(int nMaxElements) {
		int nBits = 0;
		while (nMaxElements > 0) {
			++nBits;
			nMaxElements >>= 1;
		}
		return nBits;
	}

}
