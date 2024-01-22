package org.terifan.compression.vp8arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;


public class TestExpGolomb
{
	public static void main(String ... args)
	{
		try
		{
			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos1))
			{
				Random r = new Random();
				for (int i = 0; i < 8; i++)
				{
					int v = r.nextInt(1 << r.nextInt(16));
					System.out.println(v);
					writer.encodeExpGolomb(v, 3);
				}
			}
			System.out.println("-".repeat(100));
			try (VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos1.toByteArray())))
			{
				for (int i = 0; i < 8; i++)
				{
					System.out.println(reader.decodeExpGolomb(3));
				}
			}
			System.out.println("-".repeat(100));
			System.out.println(baos1.size());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
