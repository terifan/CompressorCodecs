package test_all;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;
import org.terifan.compression.cabac.CabacContext;
import org.terifan.compression.cabac.CabacDecoder;
import org.terifan.compression.cabac.CabacEncoder;
import org.terifan.compression.dirac.DiracDecoder;
import org.terifan.compression.dirac.DiracEncoder;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class TestJPEGX
{
	public final static int[] NATURAL_ORDER =
	{
		0, 1, 8, 16, 9, 2, 3, 10,
		17, 24, 32, 25, 18, 11, 4, 5,
		12, 19, 26, 33, 40, 48, 41, 34,
		27, 20, 13, 6, 7, 14, 21, 28,
		35, 42, 49, 56, 57, 50, 43, 36,
		29, 22, 15, 23, 30, 37, 44, 51,
		58, 59, 52, 45, 38, 31, 39, 46,
		53, 60, 61, 54, 47, 55, 62, 63
	};

	public final static int[] ZIGZAG_ORDER =
	{
		0, 1, 5, 6, 14, 15, 27, 28,
		2, 4, 7, 13, 16, 26, 29, 42,
		3, 8, 12, 17, 25, 30, 41, 43,
		9, 11, 18, 24, 31, 40, 44, 53,
		10, 19, 23, 32, 39, 45, 52, 54,
		20, 22, 33, 38, 46, 51, 55, 60,
		21, 34, 37, 47, 50, 56, 59, 61,
		35, 36, 48, 49, 57, 58, 62, 63
	};


	public TestJPEGX(int[][][] aInputCoefficients) throws IOException
	{
		ByteArrayOutputStream baosDirac = new ByteArrayOutputStream();
		ByteArrayOutputStream baosCabac = new ByteArrayOutputStream();

		try (BitOutputStream bosDirac = new BitOutputStream(baosDirac))
		{
			encode(bosDirac, baosCabac, aInputCoefficients);
		}

		byte[] dataDirac = baosDirac.toByteArray();
		byte[] dataCabac = baosCabac.toByteArray();

		System.out.println(dataDirac.length);
		System.out.println(dataCabac.length);

		int[][][] outputCoefficients = new int[aInputCoefficients.length][aInputCoefficients[0].length][64];

		try (BitInputStream bis = new BitInputStream(new ByteArrayInputStream(dataDirac)))
		{
			decode(bis, new ByteArrayInputStream(dataCabac), outputCoefficients);
		}

		for (int mcuIndex = 0; mcuIndex < outputCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < outputCoefficients[0].length; blockIndex++)
			{
				if (!Arrays.equals(aInputCoefficients[mcuIndex][blockIndex], outputCoefficients[mcuIndex][blockIndex]))
//				if (mcuIndex==4535 && blockIndex==5)
				{
					System.out.println();
//					System.out.println(mcuIndex+" "+blockIndex);
					for (int i = 0; i < 64; i++)
					{
						/*if (aInputCoefficients[mcuIndex][blockIndex][i]!=0)*/ System.out.printf("%5d ", aInputCoefficients[mcuIndex][blockIndex][NATURAL_ORDER[i]]);
					}
					System.out.println();

					for (int i = 0; i < 64; i++)
					{
						/*if (outputCoefficients[mcuIndex][blockIndex][i]!=0)*/ System.out.printf("%5d ", outputCoefficients[mcuIndex][blockIndex][NATURAL_ORDER[i]]);
					}
					System.out.println();
				}
			}
		}
		
		System.out.println(Arrays.deepEquals(aInputCoefficients, outputCoefficients));
	}


	private void encode(BitOutputStream bos, ByteArrayOutputStream baos, int[][][] aCoefficients) throws IOException
	{
		int blockCount = aCoefficients[0].length;

		// amplitude + längd, terminator, hål

		// R----X
		// R--X
		// R--------X
		// R-----------R----------R----------X
		// R...........R.....R---------X

		int[] lastdc = new int[3];
		int[] compLookup = {0,0,0,0,1,2};

		DiracEncoder diracEncoder = new DiracEncoder(bos, 10000);

		CabacEncoder cabacEncoder = new CabacEncoder(baos);
		CabacContext context0 = new CabacContext(0);
		CabacContext context2 = new CabacContext(0);
		CabacContext context4 = new CabacContext(0);
		CabacContext context6 = new CabacContext(0);
		CabacContext context7 = new CabacContext(0);
		CabacContext context8 = new CabacContext(0);
		CabacContext[] context1 = new CabacContext[65];
		CabacContext[] context5 = new CabacContext[10000];
		CabacContext[] context9 = new CabacContext[1000];
		CabacContext[] context10 = new CabacContext[1000];
		for (int i = 0; i < context1.length; i++) context1[i] = new CabacContext(0);
		for (int i = 0; i < context5.length; i++) context5[i] = new CabacContext(0);
		for (int i = 0; i < context9.length; i++) context9[i] = new CabacContext(0);
		for (int i = 0; i < context10.length; i++) context10[i] = new CabacContext(0);

		for (int mcuIndex = 0; mcuIndex < aCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++)
			{
				int ci = compLookup[blockIndex];
				int[] block = aCoefficients[mcuIndex][blockIndex];

				{
				int coefficient = block[0] - lastdc[ci];
				
//				diracEncoder.encodeSInt(coefficient, 500, 7);
//				cabacEncoder.encodeExpGolomb(encodeZigZag32(coefficient), 4, context0);

				cabacEncoder.encodeBit(coefficient == 0 ? 1 : 0, context7);

				if (coefficient != 0)
				{
					if (coefficient > 0)
					{
						cabacEncoder.encodeBit(0, context6);
						diracEncoder.encodeBit(0, 8888);
					}
					else
					{
						coefficient = -coefficient;
						cabacEncoder.encodeBit(1, context6);
						diracEncoder.encodeBit(1, 8888);
					}

					coefficient--;

					int bin = 1500;
					int S = 10;
					int i = 1+coefficient/S;
					while (coefficient > S)
					{
						diracEncoder.encodeBit(0, bin);
						cabacEncoder.encodeBit(0, context10[i]);
						coefficient-=S;
						i--;
					}
					diracEncoder.encodeBit(1, bin);
					cabacEncoder.encodeBit(1, context10[0]);

					bin = 2000;
					i = 0;
					while (coefficient > 0)
					{
						diracEncoder.encodeBit(0, bin++);
						cabacEncoder.encodeBit(0, context9[coefficient]);
						coefficient--;
					}
					diracEncoder.encodeBit(1, bin);
					cabacEncoder.encodeBit(1, context9[i]);
				}
				
				lastdc[ci] = block[0];
				}

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
					diracEncoder.encodeBit(0, 3);
					cabacEncoder.encodeBit(0, context4);

					int bin = 250 + pixel;

					while (pixel < ke)
					{
						if (block[NATURAL_ORDER[pixel]] != 0)
						{
							break;
						}
						diracEncoder.encodeBit(0, bin++);
						cabacEncoder.encodeBit(0, context1[pixel]);

						pixel++;
					}

					diracEncoder.encodeBit(1, bin);
					cabacEncoder.encodeBit(1, context1[pixel]);

					int coefficient = block[NATURAL_ORDER[pixel]];

					if (coefficient > 0)
					{
						diracEncoder.encodeBit(0, 0);
						cabacEncoder.encodeBit(0, context2);
					}
					else
					{
						coefficient = -coefficient;
						diracEncoder.encodeBit(1, 0);
						cabacEncoder.encodeBit(1, context2);
					}

					coefficient--;

					bin = pixel < 10 ? 5 : 100;
//					int i = pixel < 5 ? 0 : pixel < 15 ? 100 : 500;
					while (coefficient > 0)
					{
						diracEncoder.encodeBit(0, bin++);
						cabacEncoder.encodeBit(0, context5[coefficient]);
						coefficient--;
					}
					diracEncoder.encodeBit(1, bin);
					cabacEncoder.encodeBit(1, context5[0]);
				}

				if (ke < 64)
				{
					diracEncoder.encodeBit(1, 3);
					cabacEncoder.encodeBit(1, context4);
				}
			}
		}

		cabacEncoder.encodeFinal(1);

		diracEncoder.stopEncoding();
		cabacEncoder.stopEncoding();
	}


	private void decode(BitInputStream bis, ByteArrayInputStream bais, int[][][] aCoefficients) throws IOException
	{
		int blockCount = aCoefficients[0].length;

		int[] lastdc = new int[3];
		int[] compLookup = {0,0,0,0,1,2};

		DiracDecoder diracDecoder = new DiracDecoder(bis, 1000);

		CabacDecoder cabacDecoder = new CabacDecoder(new PushbackInputStream(bais));
		CabacContext context0 = new CabacContext(0);
		CabacContext context2 = new CabacContext(0);
		CabacContext context4 = new CabacContext(0);
		CabacContext[] context1 = new CabacContext[65];
		CabacContext[] context5 = new CabacContext[1000];
		for (int i = 0; i < context1.length; i++) context1[i] = new CabacContext(0);
		for (int i = 0; i < context5.length; i++) context5[i] = new CabacContext(0);

		for (int mcuIndex = 0; mcuIndex < aCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++)
			{
				int ci = compLookup[blockIndex];
				int[] block = aCoefficients[mcuIndex][blockIndex];

				int v0 = diracDecoder.decodeSInt(500, 7);
				long v1 = decodeZigZag32((int)cabacDecoder.decodeExpGolomb(4, context0));
				
				assert v0 == v1 : v0+" == "+v1;

				block[0] = lastdc[ci] = lastdc[ci] + v0;

				int pixel = 1;

				for (;; pixel++)
				{
					int v10 = diracDecoder.decodeBit(3) ? 1 : 0;
					int v11 = cabacDecoder.decodeBit(context4);

					assert v10 == v11;
					if (v10 == 1) 
					{
						break;
					}
					
					int bin = 250;

					for (;;)
					{
						int v20 = diracDecoder.decodeBit(bin++) ? 1 : 0;
						int v21 = cabacDecoder.decodeBit(context1[pixel]);

						assert v20 == v21;
						if (v20 == 1) 
						{
							break;
						}

						block[NATURAL_ORDER[pixel++]] = 0;
					}

					int neg0 = diracDecoder.decodeBit(0) ? 1 : 0;
					int neg1 = cabacDecoder.decodeBit(context2);
					assert neg0 == neg1 : neg0+" == "+neg1;

					int coefficient = 1;

					bin = pixel < 10 ? 5 : 100;
					int i = pixel < 5 ? 0 : pixel < 15 ? 100 : 500;
					for (;;)
					{
						int v30 = diracDecoder.decodeBit(bin++) ? 1 : 0;
						int v31 = cabacDecoder.decodeBit(context5[i++]);

						assert v30 == v31;
						if (v30 == 1) 
						{
							break;
						}

						coefficient++;
					}
					
					coefficient = neg0 == 1 ? -coefficient : coefficient;

					block[NATURAL_ORDER[pixel]] = coefficient;
				}
			}
		}
	}


	private int encodeZigZag32(int aSigned)
	{
		return (aSigned << 1) ^ (aSigned >> 31);
	}


	private int decodeZigZag32(int aSigned)
	{
		return (aSigned >>> 1) ^ -(aSigned & 1);
	}


	private static int[][][] readImageData() throws IOException
	{
		int[][][] coefficients = new int[81 * 56][6][8 * 8];

		try (DataInputStream dis = new DataInputStream(new InflaterInputStream(TestAll.class.getResourceAsStream("swallowtail.jpg.data"))))
		{
			for (int[][] mcu : coefficients)
			{
				for (int[] block : mcu)
				{
					for (int pixel = 0; pixel < block.length; pixel++)
					{
						block[pixel] = dis.readShort();

//						System.out.printf("%5d", block[pixel]);
					}
//					System.out.println();
				}
			}
		}

		return coefficients;
	}


	public static void main(String... args)
	{
		try
		{
			int[][][] coefficients = readImageData();

			new TestJPEGX(coefficients);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
