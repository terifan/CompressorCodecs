package org.terifan.compression.cabac265;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import static org.terifan.compression.util.Log.hexDump;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			byte[] data;

			{
				CabacContext265[] models = {
					new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265()
				};

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				try (CabacEncoder265 encoder = new CabacEncoder265(baos))
				{
					encoder.encodeCABAC_EGk(8985, 2, models);
					encoder.encodeCABAC_EGk(777, 2, models);
					encoder.encodeCABAC_EGk(152, 2, models);
					encoder.encodeCABAC_EGk(18, 2, models);
					encoder.encodeCABAC_EGk(682, 2, models);
					encoder.encodeFinal(1);
				}

				data = baos.toByteArray();
			}

			hexDump(data);

			{
				CabacContext265[] models = {
					new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265(),new CabacContext265()
				};

				try (CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(data)))
				{
					System.out.println(decoder.decodeCABAC_EGk(2, models));
					System.out.println(decoder.decodeCABAC_EGk(2, models));
					System.out.println(decoder.decodeCABAC_EGk(2, models));
					System.out.println(decoder.decodeCABAC_EGk(2, models));
					System.out.println(decoder.decodeCABAC_EGk(2, models));
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
