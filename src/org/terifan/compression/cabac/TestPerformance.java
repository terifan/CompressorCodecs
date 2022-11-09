package org.terifan.compression.cabac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;
import org.terifan.compression.cabac265.CabacDecoder265;
import org.terifan.compression.cabac265.CabacEncoder265;
import org.terifan.compression.cabac265.CabacModel;
import org.terifan.compression.dirac.DiracDecoder;
import org.terifan.compression.dirac.DiracEncoder;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;
import org.terifan.compression.vp8arithmetic.VP8Decoder;
import org.terifan.compression.vp8arithmetic.VP8Encoder;


public class TestPerformance
{
	private static String FORMAT = "%-8s - size: %8d bytes, encode: %4dms, decode: %4sms\n";


	public static void main(String... args)
	{
		try
		{
			for (int i = 0; i < 5; i++)
			{
				run(100 * i / 4);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void run(int prob) throws IOException
	{
		int seed = new Random().nextInt(Integer.MAX_VALUE);

		System.out.printf("-- %3d%% zeros [%9d] ------------------------------------------------------------------------------------------------\n", prob, seed);

		int[] bits = new int[64 * 1024 * 1024 + 1];

		Random rnd = new Random(seed);
		for (int i = 0; i < bits.length; i++)
		{
			bits[i] = rnd.nextInt(100) > prob ? 1 : 0;
		}

		byte[] buffer;
		long t1;
		long t2;

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CabacEncoder writer = new CabacEncoder(baos);
			CabacContext context = new CabacContext(0);
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				writer.encodeBit(bits[i], context);
			}
			writer.encodeFinal(1);
			writer.stopEncoding();
			t1 = System.nanoTime() - t1;
			buffer = baos.toByteArray();
		}

		{
			CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer), 2));
			CabacContext context = new CabacContext(0);
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = reader.decodeBit(context);
				assert b == bits[i];
			}
			t2 = System.nanoTime() - t2;
		}

		System.out.printf(FORMAT, "CABAC", buffer.length, t1 / 1000000, t2 / 1000000);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CabacEncoder265 writer = new CabacEncoder265(baos);
			CabacModel model = new CabacModel();
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				writer.writeCABAC_bit(bits[i], model);
			}
			writer.encodeFinal(1);
			writer.stopEncoding();
			t1 = System.nanoTime() - t1;
			buffer = baos.toByteArray();
		}

		{
			CabacDecoder265 reader = new CabacDecoder265(new PushbackInputStream(new ByteArrayInputStream(buffer), 2));
			CabacModel model = new CabacModel();
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = reader.decodeCABAC_bit(model);
				assert b == bits[i];
			}
			t2 = System.nanoTime() - t2;
		}

		System.out.printf(FORMAT, "CABAC265", buffer.length, t1 / 1000000, t2 / 1000000);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			VP8Encoder encoder = new VP8Encoder(baos);
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				encoder.writeBit(bits[i], prob);
			}
			t1 = System.nanoTime() - t1;
			encoder.close();
			buffer = baos.toByteArray();
		}

		{
			VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(buffer));
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = reader.readBit(prob);
				assert b == bits[i];
			}
			t2 = System.nanoTime() - t2;
		}

		System.out.printf(FORMAT, "VP8", buffer.length, t1 / 1000000, t2 / 1000000);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ArithmeticContext context = new ArithmeticContext();
			ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				encoder.encode(bits[i], context);
			}
			encoder.stopEncoding();
			t1 = System.nanoTime() - t1;
			buffer = baos.toByteArray();
		}

		{
			ArithmeticContext context = new ArithmeticContext();
			ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = decoder.decode(context);
				assert b == bits[i];
			}
			t2 = System.nanoTime() - t2;
		}

		System.out.printf(FORMAT, "Arith", buffer.length, t1 / 1000000, t2 / 1000000);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BitOutputStream bos = new BitOutputStream(baos);
			DiracEncoder encoder = new DiracEncoder(bos, 1);
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				encoder.encodeBit(bits[i] == 1, 0);
			}
			encoder.stopEncoding();
			bos.close();
			t1 = System.nanoTime() - t1;
			buffer = baos.toByteArray();
		}

		{
			DiracDecoder decoder = new DiracDecoder(new BitInputStream(new ByteArrayInputStream(buffer)), 1);
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = decoder.decodeBit(0) ? 1 : 0;
				assert b == bits[i];
			}
			t2 = System.nanoTime() - t2;
		}

		System.out.printf(FORMAT, "Dirac", buffer.length, t1 / 1000000, t2 / 1000000);
	}
}
