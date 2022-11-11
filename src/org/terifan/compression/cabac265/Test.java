package org.terifan.compression.cabac265;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import static org.terifan.compression.util.Log.hexDump;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			byte[] data;

			{
				CabacContext265[] ctxMagnitude = CabacContext265.create(10);
				CabacContext265[][] ctxValues = CabacContext265.create(10, 10);

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				try ( CabacEncoder265 encoder = new CabacEncoder265(baos))
				{
					Random rnd = new Random(2);
					for (int i = 0, v; i < 100; i++)
					{
						v = rnd.nextInt(1 << (1 + rnd.nextInt(6)));
						encoder.encodeCABAC_EGk(v, 0, 5, ctxMagnitude);

						v = rnd.nextInt(1 << (1 + rnd.nextInt(6)));
						encoder.encodeCABAC_EGk(v, 0, 5, ctxMagnitude, ctxValues);

						v = rnd.nextInt(1 << (1 + rnd.nextInt(6)));
						encoder.encodeCABAC_EGk_bypass(v, 0, 6);

						v = rnd.nextInt(10);
						encoder.encodeCABAC_TU(v, 10, ctxMagnitude);

						v = rnd.nextInt(10);
						encoder.encodeCABAC_TU_bypass(v, 10);
					}
					encoder.encodeFinal(1);
				}

				data = baos.toByteArray();
			}

			hexDump(data);

			{
				CabacContext265[] ctxMagnitude = CabacContext265.create(10);
				CabacContext265[][] ctxValues = CabacContext265.create(10, 10);

				try ( CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(data)))
				{
					Random rnd = new Random(2);
					for (int i = 0, v; i < 100; i++)
					{
						v = rnd.nextInt(1 << (1 + rnd.nextInt(6)));
						if (v != decoder.decodeCABAC_EGk(0, 5, ctxMagnitude))
						{
							throw new IllegalStateException();
						}

						v = rnd.nextInt(1 << (1 + rnd.nextInt(6)));
						if (v != decoder.decodeCABAC_EGk(0, 5, ctxMagnitude, ctxValues))
						{
							throw new IllegalStateException();
						}

						v = rnd.nextInt(1 << (1 + rnd.nextInt(6)));
						if (v != decoder.decodeCABAC_EGk_bypass(0, 6))
						{
							throw new IllegalStateException();
						}

						v = rnd.nextInt(10);
						if (v != decoder.decodeCABAC_TU(10, ctxMagnitude))
						{
							throw new IllegalStateException();
						}

						v = rnd.nextInt(10);
						if (v != decoder.decodeCABAC_TU_bypass(10))
						{
							throw new IllegalStateException();
						}
					}
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
