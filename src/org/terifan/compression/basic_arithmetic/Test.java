package org.terifan.compression.basic_arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class Test
{
	public static void main(String ... args)
	{
		try
		{
			test("aabaabaacaaabbaababaaaacaabababaaaaaacabbbbabaaaaba".getBytes(), 3, 'a', false);
			
			String s = "big biggles biggeled olbig bigling boggled bigs";

			test((s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s).getBytes(), 256, 0, false);
			test((s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s+s).getBytes(), 256, 0, true);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test(byte[] aPlainText, int aSymbolCount, int aAdjust, boolean aPredict) throws IOException
	{
		byte [] compressedData;
		byte [] decompressedData;

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try (BitOutputStream bos = new BitOutputStream(baos))
			{
				BasicArithmeticContext[] context = new BasicArithmeticContext[aSymbolCount];
				for (int i = 0; i < (aPredict ? aSymbolCount : 1); i++)
				{
					context[i] = new BasicArithmeticContext(aSymbolCount, true);
				}

				BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(bos);
				for (int i = 0, p = 0; i < aPlainText.length; i++)
				{
					int c = (aPlainText[i] & 255) - aAdjust;

					encoder.encode(c, context[p]);

					if (aPredict)
					{
						p = c;
					}
				}
				encoder.stopEncoding();
			}

			compressedData = baos.toByteArray();
		}

		{
			BasicArithmeticContext[] context = new BasicArithmeticContext[aSymbolCount];
			for (int i = 0; i < (aPredict ? aSymbolCount : 1); i++)
			{
				context[i] = new BasicArithmeticContext(aSymbolCount, true);
			}
			
			BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(compressedData)));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			for (int i = 0, p = 0; i < aPlainText.length; i++)
			{
				int c = decoder.decode(context[p]);

				baos.write(c + aAdjust);

				if (aPredict)
				{
					p = c;
				}
			}

			decompressedData = baos.toByteArray();
		}

		System.out.printf("success=%b, in=%d, out=%d, ratio=%.1f\n", Arrays.equals(aPlainText, decompressedData), aPlainText.length, compressedData.length, 100 - compressedData.length*100.0 / aPlainText.length);
	}
}