package org.terifan.compression.cabac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;
import org.terifan.compression.cabac_c.CabacDecoder;
import org.terifan.compression.cabac_c.CabacEncoder;
import org.terifan.compression.cabac_c.CabacContext;
import org.terifan.compression.dirac.DiracDecoder;
import org.terifan.compression.dirac.DiracEncoder;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;
import org.terifan.compression.vp8arithmetic.VP8Decoder;
import org.terifan.compression.vp8arithmetic.VP8Encoder;


public class TestPerformanceAllAri
{
	private static String FORMAT = "%-8s - errors: %d, size: %7d bytes, encode: %4dms, decode: %4sms\n";

	
	public static void main(String... args)
	{
		try
		{
			for (int i = 0; i < 5; i++)
			{
				run(255 * i / 4);
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

		System.out.printf("-- %3d [%9d] ------------------------------------------------------------------------------------------------\n", prob, seed);
		
		int [] bits = new int[64*1024*1024];

		Random rnd = new Random(seed);
		for (int i = 0; i < bits.length; i++)
		{
			bits[i] = rnd.nextInt(255) > prob ? 1 : 0;
		}

		byte [] buffer;
		int err = 0;
		long t1;
		long t2;

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DelphiEncoder encoder = new DelphiEncoder(baos);
			DelphiContext context = new DelphiContext();
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				encoder.encode(bits[i], context);
			}
			encoder.stopEncoding();
			t1 = System.nanoTime()-t1;
			buffer = baos.toByteArray();
		}

		{
			DelphiDecoder decoder = new DelphiDecoder(new ByteArrayInputStream(buffer));
			DelphiContext context = new DelphiContext();
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = decoder.decode(context);
				if (b != bits[i])
				{
					err++;
				}
			}
			t2 = System.nanoTime()-t2;
		}

		System.out.printf(FORMAT, "CABAC", err, buffer.length, t1/1000000, t2/1000000);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			CabacEncoder writer = new CabacEncoder(baos);
			CabacContext context = new CabacContext();
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				writer.encodeBit(bits[i], context);
			}
			writer.stopEncoding();
			t1 = System.nanoTime()-t1;
			buffer = baos.toByteArray();
		}

		err = 0;

		{
			CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
			CabacContext context = new CabacContext();
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = reader.decodeBit(context);
				if (b != bits[i])
				{
					err++;
				}
			}
			t2 = System.nanoTime()-t2;
		}

		System.out.printf(FORMAT, "CABAC-C", err, buffer.length, t1/1000000, t2/1000000);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			VP8Encoder encoder = new VP8Encoder(baos);
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				encoder.encodeBit(bits[i], prob);
			}
			t1 = System.nanoTime()-t1;
			encoder.close();
			buffer = baos.toByteArray();
		}

		err = 0;

		{
			VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(buffer));
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = reader.decodeBit(prob);
				if (b != bits[i])
				{
					err++;
				}
			}
			t2 = System.nanoTime()-t2;
		}

		System.out.printf(FORMAT, "VP8", err, buffer.length, t1/1000000, t2/1000000);

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
			t1 = System.nanoTime()-t1;
			buffer = baos.toByteArray();
		}

		err = 0;

		{
			ArithmeticContext context = new ArithmeticContext();
			ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = decoder.decode(context);
				if (b != bits[i])
				{
					err++;
				}
			}
			t2 = System.nanoTime()-t2;
		}

		System.out.printf(FORMAT, "Arith", err, buffer.length, t1/1000000, t2/1000000);

//		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//		{
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
//			t1 = System.nanoTime();
//			for (int i = 0; i < bits.length; i++)
//			{
//				encoder.encode(bits[i], prob);
//			}
//			encoder.stopEncoding();
//			t1 = System.nanoTime()-t1;
//			buffer = baos.toByteArray();
//		}
//
////			System.out.println("");
////			Debug.hexDump(40, buffer);
//		err = 0;
//
//		{
//			ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
//			t2 = System.nanoTime();
//			for (int i = 0; i < bits.length; i++)
//			{
//				int b = decoder.decode(prob);
//				if (b != bits[i])
//				{
//					err++;
//				}
////					System.out.print(b != bits[i] ? "#" : b);
//			}
//			t2 = System.nanoTime()-t2;
////				System.out.println("");
//		}
//
//		System.out.printf(FORMAT, "ArithUni", err, buffer.length, t1/1000000, t2/1000000);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BitOutputStream bos = new BitOutputStream(baos);
			DiracEncoder encoder = new DiracEncoder(bos, 1);
			t1 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				encoder.encodeBit(bits[i]==1, 0);
			}
			encoder.finish();
			bos.close();
			t1 = System.nanoTime()-t1;
			buffer = baos.toByteArray();
		}

		err = 0;

		{
			DiracDecoder decoder = new DiracDecoder(new BitInputStream(new ByteArrayInputStream(buffer)), 1);
			t2 = System.nanoTime();
			for (int i = 0; i < bits.length; i++)
			{
				int b = decoder.decodeBit(0) ? 1 : 0;
				if (b != bits[i])
				{
					err++;
				}
				if (b != bits[i]) System.out.printf("%8d  %12d  %12d\n", i, b, bits[i]);
			}
			t2 = System.nanoTime()-t2;
		}

		System.out.printf(FORMAT, "Dirac",err, buffer.length, t1/1000000, t2/1000000);
	}
}
