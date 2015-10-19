package org.terifan.compression.bitari;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;


public class Test
{
	public static void main(String... args)
	{
		try
		{
//			for (int i = 0; i < 8; i++)
//			{
//				for (int j = 0; j < 8; j++)
//				{
//					System.out.printf("%8d ", i+j==0?0:30000*j/(i+j));
//				}
//				
//				System.out.print("    ");
//
//				for (int j = 0; j < 8; j++)
//				{
//					int s = i+j==0?0:(1<<16)/(i+j);
//					System.out.printf("%8d ", (30000*j*s)>>16);
//				}
//				System.out.println("");
//			}
			
			test1();
//			test2();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
	
	
	public static void test2() throws IOException
	{
		for (int test = 0; test < 10000; test++)
		{
			Random rnd = new Random(test);
			int [] bits = new int[1+rnd.nextInt(10000)];
			for (int i = 0; i < bits.length; i++)
			{
				bits[i] = rnd.nextInt(100) > 80 ? 1 : 0;
			}
			for (int i = bits.length/4; i < 2*bits.length/4; i++)
			{
				bits[i] = rnd.nextBoolean() ? 1 : 0;
			}

			byte [] buffer1;
			byte [] buffer2;
			int err = 0;

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticContext ctx = new ArithmeticContext();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				for (int i = 0; i < bits.length/4; i++)
				{
					encoder.encode(bits[i], ctx);
				}
				for (int i = bits.length/4; i < 2*bits.length/4; i++)
				{
					encoder.encode(bits[i], ctx);
				}
				for (int i = 2*bits.length/4; i < bits.length; i++)
				{
					encoder.encode(bits[i], ctx);
				}
				encoder.stopEncoding();
				buffer1 = baos.toByteArray();
			}

			{
				ArithmeticContext ctx = new ArithmeticContext();
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer1));
				for (int i = 0; i < bits.length/4; i++)
				{
					int b = decoder.decode(ctx);
					if (b != bits[i])
					{
						err++;
					}
				}
				for (int i = bits.length/4; i < 2*bits.length/4; i++)
				{
					int b = decoder.decode(ctx);
					if (b != bits[i])
					{
						err++;
					}
				}
				for (int i = 2*bits.length/4; i < bits.length; i++)
				{
					int b = decoder.decode(ctx);
					if (b != bits[i])
					{
						err++;
					}
				}
			}

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticContext ctx = new ArithmeticContext();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				for (int i = 0; i < bits.length/4; i++)
				{
					encoder.encode(bits[i], ctx);
				}
				for (int i = bits.length/4; i < 2*bits.length/4; i++)
				{
					encoder.encodeEqProb(bits[i]);
				}
				for (int i = 2*bits.length/4; i < bits.length; i++)
				{
					encoder.encode(bits[i], ctx);
				}
				encoder.stopEncoding();
				buffer2 = baos.toByteArray();
			}

			{
				ArithmeticContext ctx = new ArithmeticContext();
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer2));
				for (int i = 0; i < bits.length/4; i++)
				{
					int b = decoder.decode(ctx);
					if (b != bits[i])
					{
						err++;
					}
				}
				for (int i = bits.length/4; i < 2*bits.length/4; i++)
				{
					int b = decoder.decodeEqProb();
					if (b != bits[i])
					{
						err++;
					}
				}
				for (int i = 2*bits.length/4; i < bits.length; i++)
				{
					int b = decoder.decode(ctx);
					if (b != bits[i])
					{
						err++;
					}
				}
			}
			
			System.out.printf("%8d %8d %8d\n", bits.length, 8*buffer1.length, 8*buffer2.length);

			if (err > 0)
			{
				System.out.println("decode failed "+test+", errors: "+err);
				return;
			}
		}
	}
	
	
	public static void test1()
	{
		try
		{
			long t = System.nanoTime();
			for (int test = 0; test < 10000; test++)
//			for (int test = 0; test < 1; test++)
			{
				Random rnd = new Random(test);
				int [] bits = new int[1+rnd.nextInt(10000)];
//				int [] bits = new int[64];
				for (int i = 0; i < bits.length; i++)
				{
					bits[i] = rnd.nextInt(100) > 80 ? 1 : 0;
//					bits[i] = rnd.nextBoolean() ? 1 : 0;
				}

				byte [] buffer;
				int err = 0;

				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ArithmeticContext ctx = new ArithmeticContext();
					ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
					encoder.encodeUnaryExpGolomb(test, ctx);
					encoder.encodeExpGolombEqProb(test, 0);
					for (int i = 0; i < bits.length; i++)
					{
						encoder.encode(bits[i], ctx);
//						System.out.print(bits[i]);
					}
					encoder.stopEncoding();
					buffer = baos.toByteArray();
//					System.out.println("");
				}
				
//				System.out.println("Size: "+buffer.length);
//				System.out.println("Encode: "+t/1000000);

//				Debug.hexDump(40, buffer);

				{
					ArithmeticContext ctx = new ArithmeticContext();
					ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
					if (decoder.decodeUnaryExpGolomb(ctx) != test)
					{
						err++;
					}
					if (decoder.decodeExpGolombEqProb(0) != test)
					{
						err++;
					}
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decode(ctx);
						if (b != bits[i])
						{
							err++;
						}
//						System.out.print(b != bits[i] ? "#" : b);
					}
//					System.out.println("");
				}

//				System.out.println("Decode: "+t/1000000);
//				System.out.println("Errors: "+err);
				
				if (err > 0)
				{
					System.out.println("decode failed "+test+", errors: "+err);
				}
			}
			System.out.println("Time: "+(System.nanoTime()-t)/1000000);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
