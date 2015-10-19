package org.terifan.io.compression.adaptivehuffman;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import org.terifan.io.BitInputStream;
import org.terifan.io.BitOutputStream;
import org.terifan.util.log.Log;


public class Test
{
	public static void main(String[] args)
	{
		try
		{
			test("ByteArrayOutputStream encoded = new ByteArrayOutputStream();".getBytes(), 7);

			test("aacabaacabacababacabbaa".getBytes(), 7);

			test("0320649934964".getBytes(), 6);

			Random rnd = new Random(1);
			for (int i = 0; i < 8; i++)
			{
				byte[] buf = new byte[1 + rnd.nextInt(2 << rnd.nextInt(16))];
				rnd.nextBytes(buf);
				test(buf, 8);
			}
			
			test2();
		}
		catch (Exception e)
		{
			e.printStackTrace(Log.out);
		}
	}


	private static void test(byte[] aText, int aBitsPerSymbol) throws IOException
	{
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (final BitOutputStream bos = new BitOutputStream(encoded))
		{
			AdaptiveHuffmanEncoder encoder = new AdaptiveHuffmanEncoder(bos, aBitsPerSymbol);
			encoder.encode(new ByteArrayInputStream(aText));
		}
		byte[] buf = encoded.toByteArray();

//		Debug.hexDump(buf);
		Log.out.println(buf.length+" / " + aText.length);

		ByteArrayOutputStream decoded = new ByteArrayOutputStream();
		AdaptiveHuffmanDecoder decoder = new AdaptiveHuffmanDecoder(aBitsPerSymbol);
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(buf));
		decoder.decode(bis, decoded, aText.length);

		if (!Arrays.equals(decoded.toByteArray(), aText))
		{
			Log.out.println("err: "+new String(decoded.toByteArray()));
		}
	}


	private static void test2() throws IOException
	{
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		try (final BitOutputStream bos = new BitOutputStream(encoded))
		{
			AdaptiveHuffmanEncoder encoder = new AdaptiveHuffmanEncoder(bos, 24);
			encoder.encode(5498991);
			encoder.encode(464);
			encoder.encode(654194);
			encoder.encode(1256);
			encoder.encode(0);
			encoder.encode(49849);
			encoder.encode(564161);
			encoder.encode(7916964);
		}

		byte[] buf = encoded.toByteArray();

//		Debug.hexDump(buf);
		Log.out.println(buf.length);

		AdaptiveHuffmanDecoder decoder = new AdaptiveHuffmanDecoder(24);
		BitInputStream bis = new BitInputStream(new ByteArrayInputStream(buf));

		Log.out.println(decoder.decode(bis)+" "+decoder.decode(bis)+" "+decoder.decode(bis)+" "+decoder.decode(bis)+" "+decoder.decode(bis)+" "+decoder.decode(bis)+" "+decoder.decode(bis)+" "+decoder.decode(bis));
	}
}
