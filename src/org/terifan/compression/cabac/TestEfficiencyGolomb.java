package org.terifan.compression.cabac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Random;
import org.terifan.compression.basic_arithmetic.BasicArithmeticContext;
import org.terifan.compression.basic_arithmetic.BasicArithmeticDecoder;
import org.terifan.compression.basic_arithmetic.BasicArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;
import org.terifan.compression.cabac265.CabacDecoder265;
import org.terifan.compression.cabac265.CabacEncoder265;
import org.terifan.compression.cabac265.CabacModel;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class TestEfficiencyGolomb
{
	public static void main(String... args)
	{
		for (int i = 0; i<  100; i++)test();
	}


	private static void test()
	{
		try
		{
			final int BITS = 14;
			final int SYMBOLS = 1 << BITS;

			int entropy = 0;

			int [] values = new int[100000];
			Random rnd = new Random(1);
			for (int i = 0; i < values.length; i++)
			{
				int len = rnd.nextInt(BITS);
				entropy += 1 + len;
				values[i] = rnd.nextInt(1 << len);
			}

			System.out.println("Entropy " + entropy / 8);
			System.out.println("==============================================");

			byte [] buffer;
			long t;

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CabacEncoder writer = new CabacEncoder(baos);
				CabacContext[] context = fill(new CabacContext[BITS]);
				for (int i = 0; i < values.length; i++)
				{
					writer.encodeExpGolomb(values[i], 1, context);
				}
				writer.encodeFinal(1);
				writer.stopEncoding();
				buffer = baos.toByteArray();
			}

			{
				CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
				CabacContext[] context = fill(new CabacContext[BITS]);
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					long b = reader.decodeExpGolomb(1, context);
					assert b == values[i] : b+" == "+values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("CABAC264 - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				ArithmeticContext[] context = fill(new ArithmeticContext[BITS]);
				for (int i = 0; i < values.length; i++)
				{
					encoder.encodeExpGolomb(values[i], 1, context);
				}
				encoder.stopEncoding();
				buffer = baos.toByteArray();
			}

			{
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
				ArithmeticContext[] context = fill(new ArithmeticContext[BITS]);
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					long b = decoder.decodeExpGolomb(1, context);
					assert b == values[i] : b+" == "+values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("BitAri - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//			{
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				try (BitOutputStream bis = new BitOutputStream(baos))
//				{
//					BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(bis);
//					BasicArithmeticContext context = new BasicArithmeticContext(SYMBOLS, true);
//					for (int i = 0; i < values.length; i++)
//					{
//						encoder.encode(values[i], context);
//					}
//					encoder.close();
//				}
//				buffer = baos.toByteArray();
//			}
//
//			{
//				BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(buffer)));
//				BasicArithmeticContext context = new BasicArithmeticContext(SYMBOLS, true);
//				t = System.nanoTime();
//				for (int i = 0; i < values.length; i++)
//				{
//					int b = decoder.decode(context);
//					assert b == values[i] : b+" == "+values[i];
//				}
//				t = System.nanoTime()-t;
//			}
//
//			System.out.println("BasicArith - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				CabacModel[] models = fill(new CabacModel[BITS]);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CabacEncoder265 encoder = new CabacEncoder265(baos);

				for (int i = 0; i < values.length; i++)
				{
					encoder.writeCABAC_EGk(values[i], 0, models);
				}

				encoder.encodeFinal(1);
				encoder.stopEncoding();

				buffer = baos.toByteArray();
			}

			{
				CabacModel[] models = fill(new CabacModel[BITS]);

				CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(buffer));

				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = decoder.decodeCABAC_EGk(0, models);
					assert b == values[i] : b+" == "+values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("CABAC265 - Size: "+buffer.length+", Time: "+t/1000000);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static CabacContext[] fill(CabacContext[] aCabacContext)
	{
		for (int i = 0; i < aCabacContext.length; i++)
		{
			aCabacContext[i] = new CabacContext(0);
		}
		return aCabacContext;
	}


	private static ArithmeticContext[] fill(ArithmeticContext[] aCabacContext)
	{
		for (int i = 0; i < aCabacContext.length; i++)
		{
			aCabacContext[i] = new ArithmeticContext();
		}
		return aCabacContext;
	}


	private static CabacModel[] fill(CabacModel[] aCabacContext)
	{
		for (int i = 0; i < aCabacContext.length; i++)
		{
			aCabacContext[i] = new CabacModel();
		}
		return aCabacContext;
	}
}
