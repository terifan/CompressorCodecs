package org.terifan.compression.test_suit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;
import org.terifan.compression.cabac264.CabacContext;
import org.terifan.compression.cabac264.CabacDecoder;
import org.terifan.compression.cabac264.CabacEncoder;
import org.terifan.compression.cabac265.CabacDecoder265;
import org.terifan.compression.cabac265.CabacEncoder265;
import org.terifan.compression.cabac265.CabacModel;
import org.terifan.compression.dirac.DiracDecoder;
import org.terifan.compression.dirac.DiracEncoder;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;
import org.terifan.compression.vp8arithmetic.VP8Decoder;
import org.terifan.compression.vp8arithmetic.VP8Encoder;
import static org.terifan.compression.test_suit._LoadTestData.loadTestDataInt;


public class TestEfficiencyGolomb
{
//	public static void main(String... args)
//	{
//		try
//		{
//			int[] values = loadTestDataInt(LoadTestData.Source.LENNA_GRAY);
//
//			System.out.println(values.length);
//
//			for (int i = 0; i < 100; i++)
//			{
//				System.out.println("======== " + "#" + i + " " + " ======================================");
//
//				test(8, values);
//			}
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}


	public static void main(String... args)
	{
		try
		{
			int BITS = 14;

			for (int i = 0; i < 100; i++)
			{
				int entropy = 0;

				int[] values = new int[100_000];
				Random rnd = new Random(i);
				for (int j = 0; j < values.length; j++)
				{
					int len = rnd.nextInt(BITS);
					entropy += 1 + len;
					values[j] = rnd.nextInt(1 << len);
				}

				System.out.println("======== " + "#" + i + " " + "Entropy " + entropy / 8 + " ======================================");

				test(BITS, values);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test(int aBits, int[] aValues)
	{
		try
		{
			byte[] buffer;
			long t;

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CabacEncoder writer = new CabacEncoder(baos);
				CabacContext[] context = fill(new CabacContext[aBits]);
				for (int i = 0; i < aValues.length; i++)
				{
					writer.encodeExpGolomb(aValues[i], 1, context);
				}
				writer.encodeFinal(1);
				writer.close();
				buffer = baos.toByteArray();
			}

			{
				CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
				CabacContext[] context = fill(new CabacContext[aBits]);
				t = System.nanoTime();
				for (int i = 0; i < aValues.length; i++)
				{
					long b = reader.decodeExpGolomb(1, context);
					assert b == aValues[i] : b + " == " + aValues[i];
				}
				t = System.nanoTime() - t;
			}

			System.out.println("CABAC264 - Size: " + buffer.length + ", Time: " + t / 1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			{
				CabacModel[] models = fill(new CabacModel[aBits]);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (CabacEncoder265 encoder = new CabacEncoder265(baos))
				{
					for (int i = 0; i < aValues.length; i++)
					{
						encoder.writeCABAC_EGk(aValues[i], 0, models);
					}

					encoder.encodeFinal(1);
				}

				buffer = baos.toByteArray();
			}

			{
				CabacModel[] models = fill(new CabacModel[aBits]);

				try (CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(buffer)))
				{
					t = System.nanoTime();
					for (int i = 0; i < aValues.length; i++)
					{
						int b = decoder.decodeCABAC_EGk(0, models);
						assert b == aValues[i] : b + " == " + aValues[i];
					}
					t = System.nanoTime() - t;
				}
			}

			System.out.println("CABAC265 - Size: " + buffer.length + ", Time: " + t / 1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				ArithmeticContext[] context = fill(new ArithmeticContext[aBits]);
				for (int i = 0; i < aValues.length; i++)
				{
					encoder.encodeExpGolomb(aValues[i], 1, context);
				}
				encoder.close();
				buffer = baos.toByteArray();
			}

			{
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
				ArithmeticContext[] context = fill(new ArithmeticContext[aBits]);
				t = System.nanoTime();
				for (int i = 0; i < aValues.length; i++)
				{
					long b = decoder.decodeExpGolomb(1, context);
					assert b == aValues[i] : b + " == " + aValues[i];
				}
				t = System.nanoTime() - t;
			}

			System.out.println("BitAri   - Size: " + buffer.length + ", Time: " + t / 1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				BitOutputStream bos = new BitOutputStream(baos);
				DiracEncoder encoder = new DiracEncoder(bos, aBits + 2);
				for (int i = 0; i < aValues.length; i++)
				{
					encoder.encodeUInt(aValues[i], 0, aBits);
				}
				encoder.close();
				bos.close();
				buffer = baos.toByteArray();
			}

			{
				DiracDecoder decoder = new DiracDecoder(new BitInputStream(new ByteArrayInputStream(buffer)), aBits + 2);
				t = System.nanoTime();
				for (int i = 0; i < aValues.length; i++)
				{
					long b = decoder.decodeUInt(0, aBits);
					assert b == aValues[i] : b + " == " + aValues[i];
				}
				t = System.nanoTime() - t;
			}

			System.out.println("Dirac    - Size: " + buffer.length + ", Time: " + t / 1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				VP8Encoder encoder = new VP8Encoder(baos);
				for (int i = 0; i < aValues.length; i++)
				{
					encoder.writeExpGolomb(aValues[i], 0);
				}
				encoder.close();
				buffer = baos.toByteArray();
			}

			{
				VP8Decoder decoder = new VP8Decoder(new ByteArrayInputStream(buffer));
				t = System.nanoTime();
				for (int i = 0; i < aValues.length; i++)
				{
					long b = decoder.readExpGolomb(0);
					assert b == aValues[i] : b + " == " + aValues[i];
				}
				t = System.nanoTime() - t;
			}

			System.out.println("VP8      - Size: " + buffer.length + ", Time: " + t / 1000000);
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
