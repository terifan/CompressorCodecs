package org.terifan.compression.cabac264;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			for (int len = 1; len < 500; len++)
			{
				long[] values = new long[len];

				Random rnd = new Random(len);
				for (int i = 0; i < values.length; i++)
				{
					values[i] = rnd.nextLong() & Long.MAX_VALUE;
				}

				byte[] buffer;

				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					CabacEncoder encoder = new CabacEncoder(baos);
					CabacContext context = new CabacContext(0);
					for (int i = 0; i < values.length; i++)
					{
						encoder.encodeExpGolomb(values[i], 0, context);
					}
					encoder.encodeFinal(1);
					encoder.close();
					buffer = baos.toByteArray();
//					System.out.println("");
				}

				int err = 0;
				long t;

				{
					CabacDecoder decoder = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer), 2));
					CabacContext context = new CabacContext(0);
					t = System.nanoTime();
					for (int i = 0; i < values.length; i++)
					{
						long b = decoder.decodeExpGolomb(0, context);
						if (b != values[i])
						{
							err++;
						}
//						System.out.print(b != bits[i] ? "#" : b);
					}
					t = System.nanoTime() - t;
//					System.out.println("");
				}

				System.out.println("Errors: " + err + ", Size: " + buffer.length + ", Time: " + t / 1000000.0);

				if (err > 0)
				{
					break;
				}
			}
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
			for (int len = 1; len < 500; len++)
			{
				int[] bits = new int[len];

				Random rnd = new Random(len);
				for (int i = 0; i < bits.length; i++)
				{
					bits[i] = rnd.nextInt(100) >= 80 ? 1 : 0;
				}

				byte[] buffer;

				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					CabacEncoder encoder = new CabacEncoder(baos);
					CabacContext context = new CabacContext(0);
					for (int i = 0; i < bits.length; i++)
					{
//						System.out.print(bits[i]);
						encoder.encodeBit(bits[i], context);
					}
					encoder.encodeFinal(1);
					encoder.close();
					buffer = baos.toByteArray();
//					System.out.println("");
				}

				int err = 0;
				long t;

				{
					CabacDecoder decoder = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer), 2));
					CabacContext context = new CabacContext(0);
					t = System.nanoTime();
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decodeBit(context);
						if (b != bits[i])
						{
							err++;
						}
//						System.out.print(b != bits[i] ? "#" : b);
					}
					t = System.nanoTime() - t;
//					System.out.println("");
				}

				System.out.println("Errors: " + err + ", Size: " + buffer.length + ", Time: " + t / 1000000.0);

				if (err > 0)
				{
					break;
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
