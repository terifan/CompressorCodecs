package org.terifan.io.compression.adaptivehuffman;

import org.terifan.compression.adaptivehuffman.AdaptiveHuffmanDecoder;
import org.terifan.compression.adaptivehuffman.AdaptiveHuffmanEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;
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

		int[] symbols = new int[1000];
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

	
	@Test
	public void test2() throws IOException
	{
		Random rnd = new Random();

		int[] symbols = new int[1000];
		int[] lengths = new int[1000];
		for (int i = 0; i < symbols.length; i++)
		{
			lengths[i] = rnd.nextInt(4);
			symbols[i] = rnd.nextInt(1 << (2 << lengths[i]));
		}

		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (BitOutputStream bos = new BitOutputStream(encoded))
		{
			AdaptiveHuffmanEncoder[] encoders = {
				new AdaptiveHuffmanEncoder(bos, 2),
				new AdaptiveHuffmanEncoder(bos, 4),
				new AdaptiveHuffmanEncoder(bos, 8),
				new AdaptiveHuffmanEncoder(bos, 16)
			};
			for (int i = 0; i < symbols.length; i++)
			{
				encoders[lengths[i]].encode(symbols[i]);
			}
		}

		byte[] buf = encoded.toByteArray();

		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(buf));

		AdaptiveHuffmanDecoder[] decoders = {
			new AdaptiveHuffmanDecoder(2),
			new AdaptiveHuffmanDecoder(4),
			new AdaptiveHuffmanDecoder(8),
			new AdaptiveHuffmanDecoder(16)
		};

		for (int i = 0; i < symbols.length; i++)
		{
			assertEquals(decoders[lengths[i]].decode(bis), symbols[i]);
		}
	}
}
