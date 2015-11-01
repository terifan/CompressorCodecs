package org.terifan.compression.vp8arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import org.terifan.compression.util.Log;


public class Test
{
	public static void xmain(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos))
			{
				writer.encodeValue(Integer.MAX_VALUE, 32);
				writer.encodeValue(Integer.MIN_VALUE, 32);
				writer.encodeBit(0, 0);
				writer.encodeBit(0, 127);
				writer.encodeBit(0, 255);
				writer.encodeBit(1, 0);
				writer.encodeBit(1, 127);
				writer.encodeBit(1, 255);
			}

			System.out.println("*** " + baos.size());

			VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos.toByteArray()));
			System.out.println(reader.decodeValue(32) == Integer.MAX_VALUE);
			System.out.println(reader.decodeValue(32) == Integer.MIN_VALUE);
			System.out.println(reader.decodeBit(0));
			System.out.println(reader.decodeBit(127));
			System.out.println(reader.decodeBit(255));
			System.out.println(reader.decodeBit(0));
			System.out.println(reader.decodeBit(127));
			System.out.println(reader.decodeBit(255));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
	
	public static void main(String ... args)
	{
		try
		{
			int seed = new Random().nextInt(Integer.MAX_VALUE);
			System.out.println(seed);

			Random rnd = new Random(seed);
			boolean [] bits = new boolean[100_000_000];
			int [] prob = new int[bits.length];
			for (int i = 0; i < bits.length; i++)
			{
				bits[i] = rnd.nextInt(100) == 0;
				prob[i] = 255 * 99 / 100;
			}

			byte [] buffer;

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (VP8Encoder writer = new VP8Encoder(baos))
				{
					for (int i = 0; i < bits.length; i++)
					{
						writer.encodeBit(bits[i]?1:0, prob[i]);
					}
				}
				buffer = baos.toByteArray();
			}
			
			Log.out.printf("in=%,d / out=%,d, ratio=%.1f%%\n", bits.length/8, buffer.length, 100-buffer.length*100.0/(bits.length/8));

			{
				VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(buffer));
				for (int i = 0; i < bits.length; i++)
				{
					if (reader.decodeBit(prob[i]) != (bits[i]?1:0))
					{
						System.out.println("Error at  " + i);
						return;
					}
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
