package test_all;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import org.terifan.compression.cabac.CabacContext;
import org.terifan.compression.cabac.CabacDecoder;
import static test_all.Shared.NATURAL_ORDER;
import static test_all.Shared.decodeZigZag32;


class Decoder 
{
	private CabacDecoder decoder;
	private State st;

	void decode(ByteArrayInputStream bais, int[][][] aCoefficients) throws IOException
	{
		int blockCount = aCoefficients[0].length;

		int[] lastdc = new int[3];
		int[] compLookup = {0,0,0,0,1,2};

		decoder = new CabacDecoder(new PushbackInputStream(bais));
		State[] states = {new State(), new State(), new State()};

		for (int mcuIndex = 0; mcuIndex < aCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++)
			{
				int ci = compLookup[blockIndex];
				int[] block = aCoefficients[mcuIndex][blockIndex];
				st = states[ci];

				block[0] = lastdc[ci] = lastdc[ci] + decodeDC();

				for (int pixel = 1;; pixel++)
				{
					if (decoder.decodeBit(st.stop) == 1)
					{
						break;
					}

					int i = pixel;
					while (decoder.decodeBit(st.run[i]) == 0)
					{
						block[NATURAL_ORDER[i]] = 0;
						i++;
						pixel++;
					}

					block[NATURAL_ORDER[pixel]] = decodeAC(pixel);
				}
			}
		}
	}


	private int decodeAC(int aPixel) throws IOException
	{
		int coefficient = 1;
		
		if (true)
		{
			coefficient += (int)decoder.decodeExpGolomb(0, st.acmag2[aPixel], st.acres[aPixel]);
		}
		else
		{
			int i = 0;
			CabacContext[] ctx = st.acmag2[aPixel / 5];
			while (decoder.decodeBit(ctx[i]) == 0)
			{
				coefficient++;
				i++;
			}
		}

		return decoder.decodeBit(st.acsign) == 1 ? -coefficient : coefficient;
	}


	private int decodeDC() throws IOException
	{
		int coefficient;
		
		if (!true)
		{
			coefficient = decodeZigZag32((int)decoder.decodeExpGolomb(0, st.dcmag, st.dc));
		}
		else
		{
			if (decoder.decodeBit(st.dczero) == 1)
			{
				return 0;
			}

			coefficient = 1;

			int S = 10;
			int i = 0;
			while (decoder.decodeBit(st.dcmag[i]) == 0)
			{
				coefficient += S;
//				i++;
			}
			i = 0;
			while (decoder.decodeBit(st.dc[i]) == 0)
			{
				coefficient++;
				i++;
			}

			coefficient = (decoder.decodeBitEqProb() == 1 ? -coefficient : coefficient);
		}

		return coefficient;
	}
}
