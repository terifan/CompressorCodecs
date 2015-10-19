package org.terifan.compression.adaptivehuffman;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class Test
{
	public static void main(String[] args)
	{
		try
		{
			test("ByteArrayOutputStream encoded = new ByteArrayOutputStream();".getBytes(), 7);
			test("aacabaacabacababacabbaa".getBytes(), 7);
			test("0320649934964".getBytes(), 6);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test(byte[] aText, int aBitsPerSymbol) throws IOException
	{
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (BitOutputStream bos = new BitOutputStream(encoded))
		{
			AdaptiveHuffmanEncoder encoder = new AdaptiveHuffmanEncoder(bos, aBitsPerSymbol);
			encoder.encode(new ByteArrayInputStream(aText));
		}

		byte[] buf = encoded.toByteArray();

		System.out.println(buf.length+" / " + aText.length);

		ByteArrayOutputStream decoded = new ByteArrayOutputStream();

		AdaptiveHuffmanDecoder decoder = new AdaptiveHuffmanDecoder(aBitsPerSymbol);
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(buf));
		decoder.decode(bis, decoded, aText.length);

		if (!Arrays.equals(decoded.toByteArray(), aText))
		{
			System.out.println("err: "+new String(decoded.toByteArray()));
		}
	}
}
