package org.terifan.io.compression.adaptivehuffman;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import org.terifan.io.BitInputStream;
import org.terifan.io.BitOutputStream;
import static org.testng.Assert.*;
import org.testng.annotations.Test;


public class AdaptiveHuffmanEncoderNGTest
{
	@Test
	public void test1() throws IOException
	{
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();

		Random rnd = new Random();

		int range = 30;

		int[] symbols = new int[10000];
		for (int i = 0; i < symbols.length; i++)
		{
			symbols[i] = rnd.nextInt((int)(1L << (4 + rnd.nextInt(range - 4)) - 1));
		}

		try (BitOutputStream bos = new BitOutputStream(encoded))
		{
			AdaptiveHuffmanEncoder encoder = new AdaptiveHuffmanEncoder(bos, range);
			for (int s : symbols)
			{
				encoder.encode(s);
			}
		}

		byte[] buf = encoded.toByteArray();

		AdaptiveHuffmanDecoder decoder = new AdaptiveHuffmanDecoder(range);
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(buf));

		for (int s : symbols)
		{		
			assertEquals(decoder.decode(bis), s);
		}
	}
}
