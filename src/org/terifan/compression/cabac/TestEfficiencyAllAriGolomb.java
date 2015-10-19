package org.terifan.compression.cabac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;
import org.terifan.compression.cabac_c.CabacDecoder;
import org.terifan.compression.cabac_c.CabacEncoder;
import org.terifan.compression.cabac_c.CabacContext;


public class TestEfficiencyAllAriGolomb
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

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DelphiEncoder writer = new DelphiEncoder(baos);
				DelphiContext context = new DelphiContext();
				for (int i = 0; i < values.length; i++)
				{
					writer.encodeUnaryExpGolomb(values[i], context);
				}
//				writer.encode(0, context);
//				writer.encode(1, context);
				writer.stopEncoding();
				buffer = baos.toByteArray();
			}

//			System.out.println("");
//			Debug.hexDump(40, buffer);
			int err = 0;
			long t;

			{
				DelphiDecoder reader = new DelphiDecoder(new ByteArrayInputStream(buffer));
				DelphiContext context = new DelphiContext();
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = reader.decodeUnaryExpGolomb(context);
					if (b != values[i])
					{
						err++;
					}
//					System.out.print(b != bits[i] ? "#" : b);
				}
				t = System.nanoTime()-t;
//				System.out.println("");
			}

			System.out.println("CABAC   - Errors: "+err+", Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CabacEncoder writer = new CabacEncoder(baos);
				CabacContext context = new CabacContext();
				for (int i = 0; i < values.length; i++)
				{
//					System.out.print(bits[i]);
					writer.encodeExpGolomb(values[i], 0, context);
				}
//				writer.encodeFinal();
				writer.stopEncoding();
				buffer = baos.toByteArray();
			}

//			System.out.println("");
//			Debug.hexDump(40, buffer);
			err = 0;

			{
				CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
				CabacContext context = new CabacContext();
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					long b = reader.decodeExpGolomb(0, context);
					if (b != values[i])
					{
						err++;
					}
//					System.out.print(b != bits[i] ? "#" : b);
				}
				t = System.nanoTime()-t;
//				System.out.println("");
			}

			System.out.println("CABAC-C - Errors: "+err+", Size: "+buffer.length+", Time: "+t/1000000);

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticContext context = new ArithmeticContext();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				for (int i = 0; i < values.length; i++)
				{
					encoder.encodeUnaryExpGolomb(values[i], context);
				}
				encoder.stopEncoding();
				buffer = baos.toByteArray();
			}

//			System.out.println("");
//			Debug.hexDump(40, buffer);
			err = 0;

			{
				ArithmeticContext context = new ArithmeticContext();
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
				t = System.nanoTime();
				for (int i = 0; i < values.length; i++)
				{
					int b = decoder.decodeUnaryExpGolomb(context);
					if (b != values[i])
					{
						err++;
					}
//					System.out.print(b != bits[i] ? "#" : b);
				}
				t = System.nanoTime()-t;
//				System.out.println("");
			}

			System.out.println("Arith   - Errors: "+err+", Size: "+buffer.length+", Time: "+t/1000000);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
