package at.mcnetwork.lausi;

/**
 * Methods for spacing chat text for Minecraft
 *
 * @author jascotty2, based on work by tkelly
 */
public class ChatStr {

	// these values might have changed in the years.. this was made back in 2011
	public final static int chatwidth = 318;
	public static String charWidthIndexIndex
			= " !\"#$%&'()*+,-./"
			+ "0123456789:;<=>?"
			+ "@ABCDEFGHIJKLMNO"
			+ "PQRSTUVWXYZ[\\]^_"
			+ "'abcdefghijklmno"
			+ "pqrstuvwxyz{|}~⌂"
			+ "ÇüéâäàåçêëèïîìÄÅ"
			+ "ÉæÆôöòûùÿÖÜø£Ø×ƒ"
			+ "áíóúñÑªº¿®¬½¼¡«»";
	public static int[] charWidths = {
		4, 2, 5, 6, 6, 6, 6, 3, 5, 5, 5, 6, 2, 6, 2, 6,
		6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2, 2, 5, 6, 5, 6,
		7, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6,
		6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 6, 6,
		3, 6, 6, 6, 6, 6, 5, 6, 6, 2, 6, 5, 3, 6, 6, 6,
		6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 5, 2, 5, 7, 6,
		6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 3, 6, 6,
		6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6,
		6, 3, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 2, 6, 6};
	// chat limmitation: repetitions of characters is limited to 119 per line
	//      so: repeating !'s will not fill a line

	/**
	 * Get the pixel width of a string
	 *
	 * @param s
	 * @return
	 */
	public static int getStringWidth(String s) {
		int i = 0;
		if (s != null) {
			s = s.replaceAll("\\u00A7.", "");
			for (int j = 0; j < s.length(); j++) {
				if (s.charAt(j) >= 0) {
					i += getCharWidth(s.charAt(j));
				}
			}
		}
		return i;
	}

	/**
	 * Get the pixel width of a character
	 *
	 * @param c
	 * @return
	 */
	public static int getCharWidth(char c) {
		//return getCharWidth(c, 0);
		int k = charWidthIndexIndex.indexOf(c);
		if (c != '\247' && k >= 0) {
			return charWidths[k];
		}
		return 0;
	}

	/**
	 * Get the pixel width of a character
	 *
	 * @param c
	 * @param defaultReturn what to return if this character is unknown
	 * @return
	 */
	public static int getCharWidth(char c, int defaultReturn) {
		int k = charWidthIndexIndex.indexOf(c);
		if (c != '\247' && k >= 0) {
			return charWidths[k];
		}
		return defaultReturn;
	}

	/**
	 * pads str on the right with pad (left-align)
	 *
	 * @param str string to format
	 * @param len spaces to pad
	 * @param pad character to use when padding
	 * @return str with padding appended
	 */
	public static String padRight(String str, int len, char pad) {
		// for purposes of this function, assuming a normal char to be 6
		len *= 6;
		// don't over-pad a line
		if (len > chatwidth) {
			len = chatwidth;
		}
		len -= getStringWidth(str);
		return str + repeat(pad, len / getCharWidth(pad, 6));
	}

	protected static String repeat(char c, int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; ++i) {
			sb.append(c);
		}
		return sb.toString();
	}

}
