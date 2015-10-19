package org.terifan.compression.cabac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.cabac_c.CabacDecoder;
import org.terifan.compression.cabac_c.CabacEncoder;
import org.terifan.compression.vp8arithmetic.VP8Decoder;
import org.terifan.compression.vp8arithmetic.VP8Encoder;


public class TestPerformanceAllAriEqProb
{
	public static void main(String... args)
	{
		try
		{
			int prob = 200;

			int [] bits = new int[64*1024*1024];
			Random rnd = new Random(1);
			for (int i = 0; i < bits.length; i++)
			{
				bits[i] = rnd.nextInt(256) >= prob ? 1 : 0;
			}

			byte [] buffer;

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DelphiEncoder writer = new DelphiEncoder(baos);
				for (int i = 0; i < bits.length; i++)
				{
//					System.out.print(bits[i]);
					writer.encodeEqProb(bits[i]);
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
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = reader.decodeEqProb();
					if (b != bits[i])
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
				for (int i = 0; i < bits.length; i++)
				{
//					System.out.print(bits[i]);
					writer.encodeBitEqProb(bits[i]);
				}
				writer.encodeFinal(1);
				writer.stopEncoding();
				buffer = baos.toByteArray();
			}

//			System.out.println("");
//			Debug.hexDump(40, buffer);
			err = 0;

			{
				CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = reader.decodeBitEqProb();
					if (b != bits[i])
					{
						err++;
					}
//					System.out.print(b != bits[i] ? "#" : b);
				}
				t = System.nanoTime()-t;
//				System.out.println("");
			}

			System.out.println("CABAC-C - Errors: "+err+", Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				VP8Encoder encoder = new VP8Encoder(baos);
				for (int i = 0; i < bits.length; i++)
				{
					encoder.encodeBitEqProb(bits[i]);
				}
				encoder.close();
				buffer = baos.toByteArray();
			}

//			System.out.println("");
//			Debug.hexDump(40, buffer);
			err = 0;

			{
				VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(buffer));
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = reader.decodeBitEqProb();
					if (b != bits[i])
					{
						err++;
					}
//					System.out.print(b != bits[i] ? "#" : b);
				}
				t = System.nanoTime()-t;
//				System.out.println("");
			}

			System.out.println("VP8     - Errors: "+err+", Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				for (int i = 0; i < bits.length; i++)
				{
					encoder.encodeEqProb(bits[i]);
				}
				encoder.stopEncoding();
				buffer = baos.toByteArray();
			}

//			System.out.println("");
//			Debug.hexDump(40, buffer);
			err = 0;

			{
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = decoder.decodeEqProb();
					if (b != bits[i])
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
