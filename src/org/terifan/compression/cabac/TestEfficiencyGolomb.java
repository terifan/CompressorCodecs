package org.terifan.compression.cabac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Random;
import org.terifan.compression.basic_arithmetic.BasicArithmeticContext;
import org.terifan.compression.basic_arithmetic.BasicArithmeticDecoder;
import org.terifan.compression.basic_arithmetic.BasicArithmeticEncoder;
import org.terifan.compression.basic_arithmetic.BasicArithmeticModel;
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

			int [] values = new int[1000];
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
				ArithmeticContext context = new ArithmeticContext();
				for (int i = 0; i < values.length; i++)
				{
					encoder.encodeUnaryExpGolomb(values[i], context);
				}
				encoder.stopEncoding();
				buffer = baos.toByteArray();
			}

			{
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
				ArithmeticContext context = new ArithmeticContext();
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = decoder.decodeUnaryExpGolomb(context);
					assert b == values[i] : b+" == "+values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("Arith - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				BitOutputStream bis = new BitOutputStream(baos);
				BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(new BasicArithmeticModel(), bis);
				BasicArithmeticContext context = new BasicArithmeticContext(SYMBOLS, true);
				for (int i = 0; i < values.length; i++)
				{
					encoder.encode(context, values[i]);
				}
				encoder.encodeEnd();
				buffer = baos.toByteArray();
			}

			{
				BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BasicArithmeticModel(), new BitInputStream(new ByteArrayInputStream(buffer)));
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

				CabacEncoder265 encoder = new CabacEncoder265();

				for (int i = 0; i < values.length; i++)
				{
					encoder.write_CABAC_EGk(values[i], 8);
				}
				encoder.write_CABAC_term_bit(1);
				encoder.flush_CABAC();

				buffer = Arrays.copyOfRange(encoder.data(), 0, encoder.size());
			}

			{
				CabacModel[] models = {
					new CabacModel()
				};

				CabacDecoder265 decoder = new CabacDecoder265(buffer, buffer.length);

				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = decoder.decode_CABAC_EGk_bypass(8);
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
