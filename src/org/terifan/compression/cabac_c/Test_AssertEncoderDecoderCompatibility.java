package org.terifan.compression.cabac_c;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;


public class Test_AssertEncoderDecoderCompatibility
{
	public static void main(String... args)
	{
		try
		{
			int [] bits = new int[10000000];
			Random seedRnd = new Random();
			Random rnd = new Random();
			for (int runCounter = 0; ; runCounter++)
			{
				long seed = seedRnd.nextLong() & Long.MAX_VALUE;
				System.out.println(runCounter + " - " + seed);
				rnd.setSeed(seed);

				int prob = rnd.nextInt(256);
				int length = 1+rnd.nextInt(10000000);
				for (int i = 0; i < length; i++)
				{
					bits[i] = rnd.nextInt(256) >= prob ? 1 : 0;
				}

				byte [] buffer;

				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					CabacEncoder encoder = new CabacEncoder(baos);
					CabacContext context = new CabacContext();
					for (int i = 0; i < length; i++)
					{
						encoder.encodeBit(bits[i], context);
					}
					encoder.encodeFinal(1);
					encoder.stopEncoding();
					buffer = baos.toByteArray();
				}

//				System.out.println("");
//				Debug.hexDump(40, buffer);
				int err = 0;

				{
					CabacDecoder decoder = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
					CabacContext context = new CabacContext();
					for (int i = 0; i < length; i++)
					{
						int b = decoder.decodeBit(context);
						if (b != bits[i])
						{
							err++;
						}
//						System.out.print(b != bits[i] ? "#" : b);
					}
//					System.out.println("");
				}

				if (err > 0)
				{
					System.out.println("ERROR "+seed+" "+err);
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
