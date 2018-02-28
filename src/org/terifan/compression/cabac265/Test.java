package org.terifan.compression.cabac265;

import java.util.Arrays;
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

				CabacEncoder265 encoder = new CabacEncoder265();
				encoder.setContextModels(models);
				encoder.write_CABAC_EGk(8985, 2);
				encoder.write_CABAC_EGk(777, 2);
				encoder.write_CABAC_EGk(152, 2);
				encoder.write_CABAC_EGk(18, 2);
				encoder.write_CABAC_EGk(682, 2);
				encoder.write_CABAC_term_bit(1);
//				encoder.add_trailing_bits();
				encoder.flush_CABAC();

				data = Arrays.copyOfRange(encoder.data(), 0, encoder.size());
			}

			Log.hexDump(data);

			{
				CabacModel[] models = {
					new CabacModel()
				};

				CabacDecoder265 decoder = new CabacDecoder265(data, data.length);
				Log.out.println(decoder.decode_CABAC_EGk_bypass(2));
				Log.out.println(decoder.decode_CABAC_EGk_bypass(2));
				Log.out.println(decoder.decode_CABAC_EGk_bypass(2));
				Log.out.println(decoder.decode_CABAC_EGk_bypass(2));
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
