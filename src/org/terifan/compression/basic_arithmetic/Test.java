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
			test("aabaabaacaaabbaababaaaacaabababaaaaaacabbbbabaa".getBytes(), 3, 'a'); // 2bits/s=11.75 bytes, huffman=8.25 bytes, ari=8 bytes
			test("big biggles biggeled olbig bigling boggled bigs".getBytes(), 256, 0);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test(byte[] aPlainText, int aSymbolCount, int aAdjust) throws IOException
	{
		byte [] compressedData;
		byte [] decompressedData;

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (BitOutputStream bitOutputStream = new BitOutputStream(baos))
			{
				BasicArithmeticContext context = new BasicArithmeticContext(aSymbolCount, true);
				BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(bitOutputStream);
				for (int i = 0; i < aPlainText.length; i++)
				{
					int c = (aPlainText[i] & 255) - aAdjust;
					encoder.encode(c, context);
				}
				encoder.stopEncoding();
			}
			compressedData = baos.toByteArray();
		}

		{
			BasicArithmeticContext context = new BasicArithmeticContext(aSymbolCount, true);
			BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(compressedData)));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (int i = 0; i < aPlainText.length; i++)
			{
				baos.write(decoder.decode(context) + aAdjust);
			}
			decompressedData = baos.toByteArray();
		}

		System.out.printf("success=%b, in=%d, out=%d, ratio=%.1f\n", Arrays.equals(aPlainText, decompressedData), aPlainText.length, compressedData.length, 100 - compressedData.length*100.0 / aPlainText.length);
	}
}