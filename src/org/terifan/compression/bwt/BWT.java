package org.terifan.compression.bwt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;


public class BWT
{
	public static void main(String... args)
	{
		try
		{
			byte[] in = "A number of optimizations can make these algorithms run more efficiently without changing the output. There is no need to represent the table in either the encoder or decoder. In the encoder, each row of the table can be represented by a single pointer into the strings, and the sort performed using the indices. Some care must be taken to ensure that the sort does not exhibit bad worst-case behavior: Standard library sort functions are unlikely to be appropriate. In the decoder, there is also no need to store the table, and in fact no sort is needed at all. In time proportional to the alphabet size and string length, the decoded string may be generated one character at a time from right to left. A \"character\" in the algorithm can be a byte, or a bit, or any other convenient size. There is no need to have an actual 'EOF' character. Instead, a pointer can be used that remembers where in a string the 'EOF' would be if it existed. In this approach, the output of the BWT must include both the transformed string, and the final value of the pointer. That means the BWT does expand its input slightly. The inverse transform then shrinks it back down to the original size: it is given a string and a pointer, and returns just a string. A complete description of the algorithms can be found in Burrows and Wheeler's paper[4][citation needed], or in a number of online sources. When a bijective variant of the Burrows-Wheeler transform is performed on \"^BANANA\", you get ANNBAA^ without the need for a special character for the end of the string. A special character forces one to increase character space by one, or to have a separate field with a numerical value for an offset. Either of these features makes data compression more difficult. When dealing with short files, the savings are great percentage-wise. The bijective transform is done by sorting all rotations of the Lyndon words. In comparing two strings of unequal length, one can compare the infinite periodic repetitions of each of these in lexicographic order and take the last column of the base-rotated Lyndon word. For example, the text \"^BANANA|\" is transformed into \"ANNBAA^|\" through these steps (the red | character indicates the EOF pointer) in the original string. The EOF character is unneeded in the bijective transform, so it is dropped during the transform and re-added to its proper place in the file.".getBytes();

			AtomicInteger oIndex = new AtomicInteger();			
			byte[] en = encode(in, oIndex);
			byte[] de = decode(en, oIndex.get());

			System.out.println(new String(en));
			System.out.println(new String(de));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static final int R = 256;


	public static byte[] encode(byte[] s, AtomicInteger oIndex)
	{
		int n = s.length;

		byte[] tmp = Arrays.copyOfRange(s, 0, 2 * n);
		System.arraycopy(s, 0, tmp, n, n);
		byte[][] rotatedStrings = new byte[n][];
		for (int i = 0; i < n; i++)
		{
			rotatedStrings[i] = Arrays.copyOfRange(tmp, i, i + n);
		}

		Arrays.sort(rotatedStrings, COMPARATOR);

		int index = 0;
		for (int i = 0; i < rotatedStrings.length; i++)
		{
			if (Arrays.equals(rotatedStrings[i], s))
			{
				index = i;
				break;
			}
		}

		oIndex.set(index);

		byte[] out = new byte[n];
		for (int i = 0; i < n; i++)
		{
			out[i] = rotatedStrings[i][n - 1];
		}
		return out;
	}


	public static byte[] decode(byte[] aInput, int aIndex)
	{
		int n = aInput.length;

		int[] next = new int[n];
		byte[] symbols = new byte[n];
		int[] count = new int[R + 1];

		for (int i = 0; i < n; i++)
		{
			count[aInput[i] + 1]++;
		}
		for (int r = 0; r < R; r++)
		{
			count[r + 1] += count[r];
		}
		for (int i = 0; i < n; i++)
		{
			next[count[aInput[i]]] = i;
			symbols[count[aInput[i]]++] = aInput[i];
		}

		byte[] out = new byte[n];

		for (int i = 0; i < n; i++)
		{
			out[i] = symbols[aIndex];
			aIndex = next[aIndex];
		}
		
		return out;
	}


	private final static Comparator<byte[]> COMPARATOR = (a, b) ->
	{
		for (int i = 0; i < a.length; i++)
		{
			if (a[i] < b[i])
			{
				return -1;
			}
			if (a[i] > b[i])
			{
				return 1;
			}
		}
		return 0;
	};
}
