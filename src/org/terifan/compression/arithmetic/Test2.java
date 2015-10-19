package org.terifan.compression.arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class Test2
{
	public static void main(String... args)
	{
		try
		{
			for (int N = 2; N <= 256; N++)
			for (int test = 0; test < 1000; test++)
			{
				Random rnd = new Random(test);
				int [] bits = new int[1+rnd.nextInt(100000)];
				for (int i = 0; i < bits.length; i++)
				{
//					bits[i] = rnd.nextInt(100) > 80 ? 1 : 0;
					bits[i] = rnd.nextInt(N);
				}

				byte [] buffer;
				long t;
				int err = 0;

				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();

					try (BitOutputStream bos = new BitOutputStream(baos))
					{
						FrequencyTable table = new FrequencyTable(N, true);
						ArithmeticModel model = new ArithmeticModel();
						ArithmeticEncoder encoder = new ArithmeticEncoder(model, bos);
						
						t = System.nanoTime();
						for (int i = 0; i < bits.length; i++)
						{
							encoder.encode(table, bits[i]);
						}
						encoder.encodeEnd();
						t = System.nanoTime()-t;
					}
					buffer = baos.toByteArray();
				}

				System.out.println("Size: "+buffer.length);
				System.out.println("Encode: "+t/1000000);

//				System.out.println("");
//				Debug.hexDump(40, buffer);

				{
					ArithmeticModel model = new ArithmeticModel();
					FrequencyTable table = new FrequencyTable(N, true);
					ArithmeticDecoder decoder = new ArithmeticDecoder(model, new BitInputStream(new ByteArrayInputStream(buffer)));
					t = System.nanoTime();
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decode(table);
						if (b != bits[i])
						{
							err++;
						}
//						System.out.print(b != bits[i] ? "#" : b);
					}
					t = System.nanoTime()-t;
//					System.out.println("");
				}

				System.out.println("Decode: "+t/1000000);
				System.out.println("Errors: "+err);
				
				if (err > 0)
				{
					System.out.println(N+" "+test);
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
