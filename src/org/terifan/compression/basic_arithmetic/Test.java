package org.terifan.compression.basic_arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.terifan.compression.bwt.BWT;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class Test
{
	public static void main(String ... args)
	{
		try
		{
			byte[] in = "A number of optimizations can make these algorithms run more efficiently without changing the output. There is no need to represent the table in either the encoder or decoder. In the encoder, each row of the table can be represented by a single pointer into the strings, and the sort performed using the indices. Some care must be taken to ensure that the sort does not exhibit bad worst-case behavior: Standard library sort functions are unlikely to be appropriate. In the decoder, there is also no need to store the table, and in fact no sort is needed at all. In time proportional to the alphabet size and string length, the decoded string may be generated one character at a time from right to left. A \"character\" in the algorithm can be a byte, or a bit, or any other convenient size. There is no need to have an actual 'EOF' character. Instead, a pointer can be used that remembers where in a string the 'EOF' would be if it existed. In this approach, the output of the BWT must include both the transformed string, and the final value of the pointer. That means the BWT does expand its input slightly. The inverse transform then shrinks it back down to the original size: it is given a string and a pointer, and returns just a string. A complete description of the algorithms can be found in Burrows and Wheeler's paper[4][citation needed], or in a number of online sources. When a bijective variant of the Burrows-Wheeler transform is performed on \"^BANANA\", you get ANNBAA^ without the need for a special character for the end of the string. A special character forces one to increase character space by one, or to have a separate field with a numerical value for an offset. Either of these features makes data compression more difficult. When dealing with short files, the savings are great percentage-wise. The bijective transform is done by sorting all rotations of the Lyndon words. In comparing two strings of unequal length, one can compare the infinite periodic repetitions of each of these in lexicographic order and take the last column of the base-rotated Lyndon word. For example, the text \"^BANANA|\" is transformed into \"ANNBAA^|\" through these steps (the red | character indicates the EOF pointer) in the original string. The EOF character is unneeded in the bijective transform, so it is dropped during the transform and re-added to its proper place in the file.".getBytes();

			byte[] bwt = in.clone();
			int index = BWT.encode(bwt);

			test(in, false, false, "");
			test(in, false, true, "    predict");
			test(bwt, false, false, "bwt");
			test(bwt, false, true, "bwt predict");
			
			test(in, true, false, "            mtf");
			test(in, true, true, "    predict mtf");
			test(bwt, true, false, "bwt         mtf");
			test(bwt, true, true, "bwt predict mtf");
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test(byte[] aPlainText, boolean aMTF, boolean aPredict, String aDescription) throws IOException
	{
		byte [] compressedData;
		byte [] decompressedData;

		{
			int[] order = new int[256];
			for (int i = 0; i < 256; i++)
			{
				order[i] = i;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try (BitOutputStream bos = new BitOutputStream(baos))
			{
				BasicArithmeticContext[] context = new BasicArithmeticContext[aPredict ? 256 : 1];
				for (int i = 0; i < context.length; i++)
				{
					context[i] = new BasicArithmeticContext(256, true);
				}

				BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(bos);
				for (int i = 0, p = 0; i < aPlainText.length; i++)
				{
					int c = aPlainText[i] & 255;
					int sym = c;

					if (aMTF)
					{
						for (int k = 0; k < 256; k++)
						{
							if (order[k] == c)
							{
								sym = k;
								break;
							}
						}
					}
					
					encoder.encode(sym, context[p]);

					if (aMTF)
					{
						for (int j = sym; j > 0; j--)
						{
							order[j] = order[j - 1];
						}
						order[0] = c;
					}

					if (aPredict)
					{
						p = sym;
					}
				}
				encoder.stopEncoding();
			}

			compressedData = baos.toByteArray();
		}

		{
			int[] order = new int[256];
			for (int i = 0; i < 256; i++)
			{
				order[i] = i;
			}
			
			BasicArithmeticContext[] context = new BasicArithmeticContext[aPredict ? 256 : 1];
			for (int i = 0; i < context.length; i++)
			{
				context[i] = new BasicArithmeticContext(256, true);
			}
			
			BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(compressedData)));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			for (int i = 0, p = 0; i < aPlainText.length; i++)
			{
				int c = decoder.decode(context[p]);
				int sym = c;
				
				if (aMTF)
				{
					sym = order[c];
				}

				baos.write(sym);

				if (aMTF)
				{
					for (int j = c; j > 0; j--)
					{
						order[j] = order[j - 1];
					}
					order[0] = sym;
				}
				if (aPredict)
				{
					p = c;
				}
			}

			decompressedData = baos.toByteArray();
		}

		System.out.printf("[%-15s] success=%b, in=%d, out=%d, ratio=%.1f\n", aDescription, Arrays.equals(aPlainText, decompressedData), aPlainText.length, compressedData.length, 100 - compressedData.length*100.0 / aPlainText.length);
	}
}