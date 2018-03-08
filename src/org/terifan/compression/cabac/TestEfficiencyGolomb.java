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
		try
		{
			final int BITS = 8;
			final int SYMBOLS = 1 << BITS;

			int entropy = 0;

			int [] values = new int[10000];
			Random rnd = new Random();
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
				CabacContext context = new CabacContext(0);
				for (int i = 0; i < values.length; i++)
				{
					writer.encodeExpGolomb(values[i], 0, context);
				}
				writer.encodeFinal(1);
				writer.stopEncoding();
				buffer = baos.toByteArray();
			}

			{
				CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
				CabacContext context = new CabacContext(0);
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					long b = reader.decodeExpGolomb(0, context);
					assert b == values[i] : b+" == "+values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("CABAC264 - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				ArithmeticContext[] context = {new ArithmeticContext(),new ArithmeticContext()};
				for (int i = 0; i < values.length; i++)
				{
					encoder.encodeExpGolomb(values[i], context);
				}
				encoder.stopEncoding();
				buffer = baos.toByteArray();
			}

			{
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
				ArithmeticContext[] context = {new ArithmeticContext(),new ArithmeticContext()};
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = decoder.decodeExpGolomb(context);
					assert b == values[i] : b+" == "+values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("BitAri - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (BitOutputStream bis = new BitOutputStream(baos))
				{
					BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(bis);
					BasicArithmeticContext context = new BasicArithmeticContext(SYMBOLS, true);
					for (int i = 0; i < values.length; i++)
					{
						encoder.encode(values[i], context);
					}
					encoder.stopEncoding();
				}
				buffer = baos.toByteArray();
			}

			{
				BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(buffer)));
				BasicArithmeticContext context = new BasicArithmeticContext(SYMBOLS, true);
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = decoder.decode(context);
					assert b == values[i] : b+" == "+values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("BasicArith - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				CabacModel[] models = {
					new CabacModel()
				};

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CabacEncoder265 encoder = new CabacEncoder265(baos);
//				encoder.write_startcode();

				for (int i = 0; i < values.length; i++)
				{
					encoder.write_CABAC_EGk(values[i], 0);
				}

				encoder.encodeFinal(1);
				encoder.stopEncoding();

//				buffer = Arrays.copyOfRange(encoder.data(), 0, encoder.size());
				buffer = baos.toByteArray();
			}

			{
				CabacModel[] models = {
					new CabacModel()
				};

				CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(buffer));

				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = decoder.decode_CABAC_EGk_bypass(0);
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
}
