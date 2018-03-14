package test_all;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.terifan.compression.cabac.CabacContext;
import org.terifan.compression.cabac.CabacEncoder;
import static test_all.Shared.NATURAL_ORDER;
import static test_all.Shared.encodeZigZag32;


class Encoder 
{
	private CabacEncoder encoder;
	private State st;
	
	
	void encode(ByteArrayOutputStream baos, int[][][] aCoefficients) throws IOException
	{
		int blockCount = aCoefficients[0].length;

		int[] lastdc = new int[3];
		int[] compLookup = {0,0,0,0,1,2};

		encoder = new CabacEncoder(baos);
		State[] states = new State[]{new State(), new State(), new State()};

		for (int mcuIndex = 0; mcuIndex < aCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++)
			{
				int ci = compLookup[blockIndex];
				int[] block = aCoefficients[mcuIndex][blockIndex];
				st = states[ci];

				encodeDC(block[0] - lastdc[ci]);

				lastdc[ci] = block[0];

				int ke = 63;

				do
				{
					if (block[NATURAL_ORDER[ke]] != 0)
					{
						break;
					}
				}
				while (--ke != 0);

				for (int pixel = 1; pixel <= ke; pixel++)
				{
					encoder.encodeBit(0, st.stop);

					int i = pixel;
					while (pixel < ke)
					{
						if (block[NATURAL_ORDER[pixel]] != 0)
						{
							break;
						}
						encoder.encodeBit(0, st.run[i]);
						pixel++;
						i++;
					}

					encoder.encodeBit(1, st.run[i]);

					int coefficient = block[NATURAL_ORDER[pixel]];

					boolean neg = coefficient < 0;

					if (neg)
					{
						coefficient = -coefficient;
					}

					coefficient--;

					encodeAC(coefficient, pixel);

					encoder.encodeBit(neg ? 1 : 0, st.acsign);
				}

				if (ke < 64)
				{
					encoder.encodeBit(1, st.stop);
				}
			}
		}

		encoder.encodeFinal(1);

		encoder.stopEncoding();
	}


	private void encodeAC(int aCoefficient, int aPixel) throws IOException
	{
		if (false)
		{
			int v = aCoefficient+1;
			int m = 0;
			int i = 0;
			if ((v -= 1) != 0)
			{
				encoder.encodeBit(1, st.acmag[i]);
				m = 1;
				int v2 = v;
				if ((v2 >>= 1) != 0)
				{
					encoder.encodeBit(1, st.acmag[i]);
					m <<= 1;
					while ((v2 >>= 1) != 0)
					{
						encoder.encodeBit(1, st.acmag[i]);
						m <<= 1;
						i += 1;
					}
				}
			}
			encoder.encodeBit(0, st.acmag[i]);

			while ((m >>= 1) != 0)
			{
				encoder.encodeBit((m & v) != 0 ? 1 : 0, st.ac3);
			}
		}
		else if (true)
		{
			encoder.encodeExpGolomb(aCoefficient, 0, st.acmag2[aPixel], st.acres[aPixel]);
		}
		else
		{
			int i = 0;
			CabacContext[] ctx = st.acmag2[aPixel];
			while (aCoefficient > 0)
			{
				encoder.encodeBit(0, ctx[i]);
				aCoefficient--;
				i++;
			}
			encoder.encodeBit(1, ctx[i]);
		}
	}


	private void encodeDC(int aCoefficient) throws IOException
	{
		if (!true)
		{
			encoder.encodeExpGolomb(encodeZigZag32(aCoefficient), 0, st.dcmag, st.dc);
		}
		else
		{
			encoder.encodeBit(aCoefficient == 0 ? 1 : 0, st.dczero);

			if (aCoefficient != 0)
			{
				boolean neg = aCoefficient < 0;
				if (neg)
				{
					aCoefficient = -aCoefficient;
				}
				
				aCoefficient--;

				int S = 10;
				int i = 0;
				while (aCoefficient >= S)
				{
					encoder.encodeBit(0, st.dcmag[i]);
					aCoefficient -= S;
//					i++;
				}

				encoder.encodeBit(1, st.dcmag[i]);
				
				i = 0;
				while (aCoefficient > 0)
				{
					encoder.encodeBit(0, st.dc[i]);
					aCoefficient--;
					i++;
				}
				
				encoder.encodeBit(1, st.dc[i]);
				
				encoder.encodeBitEqProb(neg ? 1 : 0);
			}
		}
	}
}
