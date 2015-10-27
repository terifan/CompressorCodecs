package org.terifan.compression.cabac2;

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
				int[] bits = new int[len];

				Random rnd = new Random(len);
				for (int i = 0; i < bits.length; i++)
				{
					bits[i] = rnd.nextInt(100) >= 80 ? 1 : 0;
				}

				byte[] buffer;

				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					BinaryEncoder encoder = new BinaryEncoder(baos);
					Context context = new Context(0);
					for (int i = 0; i < bits.length; i++)
					{
						System.out.print(bits[i]);
						encoder.encodeBit(bits[i], context);
					}
					encoder.encodeFinal(1);
					encoder.stopEncoding();
					buffer = baos.toByteArray();
					System.out.println("");
				}

				int err = 0;
				long t;

				{
					BinaryDecoder decoder = new BinaryDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer), 2));
					Context context = new Context(0);
					t = System.nanoTime();
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decodeBit(context);
						if (b != bits[i])
						{
							err++;
						}
						System.out.print(b != bits[i] ? "#" : b);
					}
					t = System.nanoTime() - t;
					System.out.println("");
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
