package org.terifan.compression.cabac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			int [] bits = new int[128];
			Random rnd = new Random(1);
			for (int i = 0; i < bits.length; i++)
			{
				bits[i] = rnd.nextInt(100) >= 80 ? 1 : 0;
			}

			byte [] buffer;

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DelphiEncoder encoder = new DelphiEncoder(baos);
				DelphiContext context = new DelphiContext();
				for (int i = 0; i < bits.length; i++)
				{
					System.out.print(bits[i]);
					encoder.encode(bits[i], context);
				}
//				writer.encode(0, context);
//				writer.encode(1, context);
				encoder.stopEncoding();
				buffer = baos.toByteArray();
				System.out.println("");
			}

//			Debug.hexDump(40, buffer);
			int err = 0;
			long t;

			{
				DelphiDecoder decoder = new DelphiDecoder(new ByteArrayInputStream(buffer));
				DelphiContext context = new DelphiContext();
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = decoder.decode(context);
					if (b != bits[i])
					{
						err++;
					}
					System.out.print(b != bits[i] ? "#" : b);
				}
				t = System.nanoTime()-t;
				System.out.println("");
			}

			System.out.println("Errors: "+err+", Size: "+buffer.length+", Time: "+t/1000000);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
