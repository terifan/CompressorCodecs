package org.terifan.compression.test_suit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.cabac264.CabacDecoder264;
import org.terifan.compression.cabac264.CabacEncoder264;
import org.terifan.compression.cabac265.CabacDecoder265;
import org.terifan.compression.cabac265.CabacEncoder265;
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
				try (CabacEncoder264 encoder = new CabacEncoder264(baos))
				{
					for (int i = 0; i < bits.length; i++)
					{
						encoder.encodeBitEqProb(bits[i]);
					}
					encoder.encodeFinal(1);
				}
				buffer = baos.toByteArray();
			}

			{
				t = System.nanoTime();
				try (CabacDecoder264 decoder = new CabacDecoder264(new PushbackInputStream(new ByteArrayInputStream(buffer))))
				{
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decodeBitEqProb();
						assert b == bits[i];
					}
				}
				t = System.nanoTime()-t;
			}

			System.out.println("CABAC    - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (CabacEncoder265 encoder = new CabacEncoder265(baos))
				{
					for (int i = 0; i < bits.length; i++)
					{
						encoder.encodeCABAC_bypass(bits[i]);
					}
					encoder.encodeFinal(1);
				}
				buffer = baos.toByteArray();
			}

			{
				t = System.nanoTime();
				try (CabacDecoder265 decoder = new CabacDecoder265(new PushbackInputStream(new ByteArrayInputStream(buffer))))
				{
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decodeCABAC_bypass();
						assert b == bits[i];
					}
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
						encoder.encodeBitEqProb(bits[i]);
					}
				}
				buffer = baos.toByteArray();
			}

			{
				t = System.nanoTime();
				try (VP8Decoder decoder = new VP8Decoder(new ByteArrayInputStream(buffer)))
				{
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decodeBitEqProb();
						assert b == bits[i];
					}
				}
				t = System.nanoTime()-t;
			}

			System.out.println("VP8      - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try (ArithmeticEncoder encoder = new ArithmeticEncoder(baos))
				{
					for (int i = 0; i < bits.length; i++)
					{
						encoder.encodeEqProb(bits[i]);
					}
				}
				buffer = baos.toByteArray();
			}

			{
				t = System.nanoTime();
				try (ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(buffer)))
				{
					for (int i = 0; i < bits.length; i++)
					{
						int b = decoder.decodeEqProb();
						assert b == bits[i];
					}
				}
				t = System.nanoTime()-t;
			}

			System.out.println("Arith    - Size: "+buffer.length+", Time: "+t/1000000);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//			{
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				try (DiracEncoder encoder = new DiracEncoder(new BitOutputStream(baos), 1))
//				{
//					for (int i = 0; i < bits.length; i++)
//					{
//						encoder.encodeBit(bits[i], 0);
//					}
//				}
//				buffer = baos.toByteArray();
//			}
//
//			{
//				t = System.nanoTime();
//				try (DiracDecoder decoder = new DiracDecoder(new BitInputStream(new ByteArrayInputStream(buffer)), 1))
//				{
//					for (int i = 0; i < bits.length; i++)
//					{
//						int b = decoder.decodeBit(0) ? 1 : 0;
//						assert b == bits[i];
//					}
//				}
//				t = System.nanoTime()-t;
//			}
//
//			System.out.println("Dirac    - Size: "+buffer.length+", Time: "+t/1000000);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
