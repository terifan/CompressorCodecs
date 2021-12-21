package org.terifan.compression.vp8arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos))
			{
//				for (int i = 0; i < 100; i++)
//				{
//					writer.encodeBit(0, 250);
//				}

				writer.writeExpGolomb(1563, 1);
			}

			System.out.println("*** " + baos.size());

			VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos.toByteArray()));
//			for (int i = 0; i < 100; i++)
//			{
//				reader.decodeBit(250);
//			}

			System.out.println(reader.readExpGolomb(1));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void xxmain(String... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos))
			{
				writer.encodeBit(0, 250);
				writer.encodeBit(0, 250);
				writer.encodeBit(0, 250);
				writer.encodeBit(0, 250);
				writer.encodeBit(0, 250);
				writer.encodeBit(0, 250);
				writer.encodeBit(0, 250);
				writer.encodeBit(0, 250);
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

	public static void xxxmain(String ... args)
	{
		try
		{
			int seed = new Random().nextInt(Integer.MAX_VALUE);
			System.out.println(seed);

			Random rnd = new Random(seed);

			for (int testMethod = 0; testMethod <= 7; testMethod++)
			{
				int bitsToTest = 1000;
				int [] probas = new int[bitsToTest];

				for (int i = 0; i < bitsToTest; i++)
				{
					boolean parity = (i & 1) != 0;

					probas[i] =
						(testMethod == 0) ? 0 :
						(testMethod == 1) ? 255 :
						(testMethod == 2) ? 128 :
						(testMethod == 3) ? rnd.nextInt(256):
						(testMethod == 4) ? (parity ? 0 : 255) :
						// alternate between low and high proba:
						(testMethod == 5) ? (parity ? rnd.nextInt(128) : 255 - rnd.nextInt(128)) :
						(testMethod == 6) ?
						(parity ? rnd.nextInt(64) : 255 - rnd.nextInt(64)) :
						(parity ? rnd.nextInt(32) : 255 - rnd.nextInt(32));
				}

				for (int bitMethod = 0; bitMethod <= 3; bitMethod++)
				{
					byte[] buffer;

					{
						rnd = new Random(seed);

						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						try (VP8Encoder writer = new VP8Encoder(baos))
						{
							int bit = (bitMethod == 0) ? 0 : (bitMethod == 1) ? 1 : 0;
							for (int i = 0; i < bitsToTest; i++)
							{
								if (bitMethod == 2)
								{
									bit = (i & 1);
								}
								else if (bitMethod == 3)
								{
									bit = rnd.nextInt(2);
								}
								writer.encodeBit(bit, probas[i]);
							}
						}
						buffer = baos.toByteArray();
					}

					{
						rnd = new Random(seed);

						VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(buffer));
						for (int i = 0; i < bitsToTest; i++)
						{
							int bit = (bitMethod == 0) ? 0 : (bitMethod == 1) ? 1 : 0;
							if (bitMethod == 2)
							{
								bit = (i & 1);
							}
							else if (bitMethod == 3)
							{
								bit = rnd.nextInt(2);
							}

							if (reader.decodeBit(probas[i]) != bit)
							{
								System.out.println("Error at  " + i);
								return;
							}
						}
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
