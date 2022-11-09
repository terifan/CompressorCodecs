package org.terifan.compression.cabac265;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.terifan.compression.util.Log;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			byte[] data;

			{
				CabacModel[] models = {
					new CabacModel()
				};

				ByteArrayOutputStream baos = new ByteArrayOutputStream();

				CabacEncoder265 encoder = new CabacEncoder265(baos);
				encoder.writeCABAC_EGk(8985, 2, models);
				encoder.writeCABAC_EGk(777, 2, models);
				encoder.writeCABAC_EGk(152, 2, models);
				encoder.writeCABAC_EGk(18, 2, models);
				encoder.writeCABAC_EGk(682, 2, models);
				encoder.encodeFinal(1);
				encoder.stopEncoding();

				data = baos.toByteArray();
			}

			Log.hexDump(data);

			{
				CabacModel[] models = {
					new CabacModel()
				};

				CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(data));
				Log.out.println(decoder.decodeCABAC_EGk_bypass(2));
				Log.out.println(decoder.decodeCABAC_EGk_bypass(2));
				Log.out.println(decoder.decodeCABAC_EGk_bypass(2));
				Log.out.println(decoder.decodeCABAC_EGk_bypass(2));
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
