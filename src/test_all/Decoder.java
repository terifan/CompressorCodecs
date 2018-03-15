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

				for (int pixel = 1; decoder.decodeBit(st.stop) == 0; pixel++)
				{
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
		boolean neg = decoder.decodeBit(st.acsign) == 1;

		int coefficient = 1;
		
		if (!true)
		{
			int m = 0;
			int i = 0;
			
			while (decoder.decodeBit(st.acmag[i++]) == 0)
			{
				m++;
			}

			int ix = 0; //(neg ? 3 : 0) + (m < 5 ? 0 : m < 10 ? 1 : 2);

			m = 1 << m;
			
			while ((m >>= 1) > 0)
			{
				coefficient += m * decoder.decodeBit(st.ac[ix]);
			}

			if(aPixel==1)System.out.print(coefficient+"\t");
		}
		else if (!true)
		{
			coefficient += (int)decoder.decodeExpGolomb(0, st.acmag2[aPixel], st.acres[aPixel]);
		}
		else
		{
			coefficient += decoder.decodeUnary(st.acmag[aPixel], st.acmag[64 + aPixel]);

//			for (int i = 0; decoder.decodeBit(st.acmag2[aPixel][i]) == 0; i++)
//			{
//				coefficient++;
//			}
		}

		return neg ? -coefficient : coefficient;
	}


	private int decodeDC() throws IOException
	{
		int coefficient;

		if (true)
		{
			coefficient = decodeZigZag32((int)decoder.decodeExpGolomb(0, st.dcmag, st.dc));
		}
		else if (true)
		{
			if (decoder.decodeBit(st.dczero) == 1)
			{
				coefficient = 0;
			}
			else
			{
				if (decoder.decodeBit(st.dcsign) == 0)
				{
					coefficient = (int)decoder.decodeExpGolomb(0, st.dcmag0, st.dc0) + 1;
				}
				else
				{
					coefficient = (int)decoder.decodeExpGolomb(0, st.dcmag1, st.dc1) * -1 - 1;
				}
			}
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
