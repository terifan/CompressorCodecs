package org.terifan.compression.cabac2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;


public class TestEfficiencyGolomb
{
	public static void main(String... args)
	{
		try
		{
			int [] values = new int[4*1024*1024];
			Random rnd = new Random(1);
			for (int i = 0; i < values.length; i++)
			{
				values[i] = rnd.nextInt(10000);
			}

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
					assert b == values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("CABAC - Size: "+buffer.length+", Time: "+t/1000000);

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
					assert b == values[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("Arith - Size: "+buffer.length+", Time: "+t/1000000);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
