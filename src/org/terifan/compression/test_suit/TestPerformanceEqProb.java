package org.terifan.compression.test_suit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.cabac264.CabacDecoder;
import org.terifan.compression.cabac264.CabacEncoder;
import org.terifan.compression.cabac265.CabacDecoder265;
import org.terifan.compression.cabac265.CabacEncoder265;
import org.terifan.compression.dirac.DiracDecoder;
import org.terifan.compression.dirac.DiracEncoder;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;
import org.terifan.compression.vp8arithmetic.VP8Decoder;
import org.terifan.compression.vp8arithmetic.VP8Encoder;


public class TestPerformanceEqProb
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
			long t;

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CabacEncoder encoder = new CabacEncoder(baos);
				for (int i = 0; i < bits.length; i++)
				{
					encoder.encodeBitEqProb(bits[i]);
				}
				encoder.encodeFinal(1);
				encoder.close();
				buffer = baos.toByteArray();
			}

			{
				CabacDecoder reader = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(buffer)));
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = reader.decodeBitEqProb();
					assert b == bits[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("CABAC    - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				CabacEncoder265 encoder = new CabacEncoder265(baos);
				for (int i = 0; i < bits.length; i++)
				{
					encoder.writeCABAC_bypass(bits[i]);
				}
				encoder.encodeFinal(1);
				encoder.close();
				buffer = baos.toByteArray();
			}

			{
				CabacDecoder265 reader = new CabacDecoder265(new PushbackInputStream(new ByteArrayInputStream(buffer)));
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = reader.decodeCABAC_bypass();
					assert b == bits[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("CABAC265 - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (VP8Encoder encoder = new VP8Encoder(baos))
				{
					for (int i = 0; i < bits.length; i++)
					{
						encoder.writeBitEqProb(bits[i]);
					}
				}
				buffer = baos.toByteArray();
			}

			{
				VP8Decoder decoder = new VP8Decoder(new ByteArrayInputStream(buffer));
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = decoder.readBitEqProb();
					assert b == bits[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("VP8      - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
				for (int i = 0; i < bits.length; i++)
				{
					encoder.encodeEqProb(bits[i]);
				}
				encoder.close();
				buffer = baos.toByteArray();
			}

			{
				ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer));
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = decoder.decodeEqProb();
					assert b == bits[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("Arith    - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				BitOutputStream bos = new BitOutputStream(baos);
				DiracEncoder encoder = new DiracEncoder(bos, 1);
				for (int i = 0; i < bits.length; i++)
				{
					encoder.encodeBit(bits[i], 0);
				}
				encoder.close();
				bos.close();
				buffer = baos.toByteArray();
			}

			{
				DiracDecoder decoder = new DiracDecoder(new BitInputStream(new ByteArrayInputStream(buffer)), 1);
				t = System.nanoTime();
				for (int i = 0; i < bits.length; i++)
				{
					int b = decoder.decodeBit(0) ? 1 : 0;
					assert b == bits[i];
				}
				t = System.nanoTime()-t;
			}

			System.out.println("Dirac    - Size: "+buffer.length+", Time: "+t/1000000);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
