package org.terifan.compression.dirac;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class Test 
{
	public static void main(String... args)
	{
		try
		{
			Random rnd = new Random(1);

			int[] values = {
				Integer.MAX_VALUE,
				-1,
				0,
				1,
				Integer.MIN_VALUE,
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt(),
				rnd.nextInt()
			};

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try (BitOutputStream bos = new BitOutputStream(baos))
			{
				DiracEncoder encoder = new DiracEncoder(bos, 8);
				for (int i : values)
				{
					encoder.encodeSInt(i, 0, 0);
				}
				encoder.stopEncoding();
			}

//			Debug.hexDump(baos.toByteArray());

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			BitInputStream bis = new BitInputStream(bais);

			DiracDecoder decoder = new DiracDecoder(bis, 8);
			for (int i : values)
			{
				long v = decoder.decodeSInt(0, 0);
				System.out.printf("%12d  %12d  %5s\n", i, v, i == v);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
