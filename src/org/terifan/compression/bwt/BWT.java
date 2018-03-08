package org.terifan.compression.bwt;

import java.util.Arrays;
import java.util.Comparator;


public class BWT
{
	public static void main(String ... args)
	{
		try
		{
			byte[] in = "A number of optimizations can make these algorithms run more efficiently without changing the output. There is no need to represent the table in either the encoder or decoder. In the encoder, each row of the table can be represented by a single pointer into the strings, and the sort performed using the indices. Some care must be taken to ensure that the sort does not exhibit bad worst-case behavior: Standard library sort functions are unlikely to be appropriate. In the decoder, there is also no need to store the table, and in fact no sort is needed at all. In time proportional to the alphabet size and string length, the decoded string may be generated one character at a time from right to left. A \"character\" in the algorithm can be a byte, or a bit, or any other convenient size. There is no need to have an actual 'EOF' character. Instead, a pointer can be used that remembers where in a string the 'EOF' would be if it existed. In this approach, the output of the BWT must include both the transformed string, and the final value of the pointer. That means the BWT does expand its input slightly. The inverse transform then shrinks it back down to the original size: it is given a string and a pointer, and returns just a string. A complete description of the algorithms can be found in Burrows and Wheeler's paper[4][citation needed], or in a number of online sources. When a bijective variant of the Burrows–Wheeler transform is performed on \"^BANANA\", you get ANNBAA^ without the need for a special character for the end of the string. A special character forces one to increase character space by one, or to have a separate field with a numerical value for an offset. Either of these features makes data compression more difficult. When dealing with short files, the savings are great percentage-wise. The bijective transform is done by sorting all rotations of the Lyndon words. In comparing two strings of unequal length, one can compare the infinite periodic repetitions of each of these in lexicographic order and take the last column of the base-rotated Lyndon word. For example, the text \"^BANANA|\" is transformed into \"ANNBAA^|\" through these steps (the red | character indicates the EOF pointer) in the original string. The EOF character is unneeded in the bijective transform, so it is dropped during the transform and re-added to its proper place in the file.\u0000".getBytes();

			byte[] encoded = encode(in);

			System.out.println(new String(encoded));

			byte[] decoded = decode(encoded);

			System.out.println(new String(decoded));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private final static Comparator<byte[]> COMPARATOR = (a,b) ->
	{
		for (int i = 0; i < a.length; i++)
		{
			if (a[i] < b[i]) return -1;
			if (a[i] > b[i]) return 1;
		}
		return 0;
	};


	private static byte[] encode(byte[] aIn)
	{
		int len = aIn.length;

		byte[][] data = new byte[len][len];

		for (int i = 0; i < len; i++)
		{
			for (int j = 0; j < len; j++)
			{
				data[i][j] = aIn[(j + i) % len];
			}
		}

		Arrays.sort(data, COMPARATOR);

		byte[] out = new byte[len];

		for (int i = 0; i < len; i++)
		{
			out[i] = data[i][len - 1];
		}

		return out;
	}


	private static byte[] decode(byte[] aIn)
	{
		int len = aIn.length;

		byte[][] data = new byte[len][len];

		for (int j = 0; j < len; j++)
		{
			for (int i = 0; i < len; i++)
			{
				System.arraycopy(data[i], 0, data[i], 1, len - 1);

				data[i][0] = aIn[i];
			}

			Arrays.sort(data, COMPARATOR);
		}

		for (int i = 0; i < len; i++)
		{
			if (data[i][len - 1] == '\0')
			{
				return data[i];
			}
		}

		throw new IllegalArgumentException();
	}
}
