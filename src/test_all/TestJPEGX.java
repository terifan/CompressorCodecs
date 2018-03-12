package test_all;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BitOutputStream bos = new BitOutputStream(baos))
		{
			encode(bos, aInputCoefficients);
		}

		byte[] data = baos.toByteArray();

		System.out.println(data.length);

		int[][][] outputCoefficients = new int[aInputCoefficients.length][aInputCoefficients[0].length][64];

		try (BitInputStream bis = new BitInputStream(new ByteArrayInputStream(data)))
		{
			decode(bis, outputCoefficients);
		}

		System.out.println(Arrays.deepEquals(aInputCoefficients, outputCoefficients));
	}


	private void encode(BitOutputStream bos, int[][][] aCoefficients) throws IOException
	{
		boolean mtf = false;

		int blockCount = aCoefficients[0].length;

//		int[][] order = new int[blockCount][2048 + 1 + 2048];
//		for (int i = 1, j = 1; i <= 2048; i++)
//		{
//			order[0][j++] = -i;
//			order[0][j++] =  i;
//		}
//		for (int i = 1; i < blockCount; i++)
//		{
//			order[i] = order[0].clone();
//		}

		// amplitude + längd, terminator, hål

		// R----X
		// R--X
		// R--------X
		// R-----------R----------R----------X
		// R...........R.....R---------X

		int[] lastdc = new int[3];
		int[] compLookup = {0,0,0,0,1,2};

		DiracEncoder encoder = new DiracEncoder(bos, 1000);

		for (int mcuIndex = 0; mcuIndex < aCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++)
			{
				int ci = compLookup[blockIndex];
				int[] block = aCoefficients[mcuIndex][blockIndex];

				{
				int coefficient = block[0] - lastdc[ci];

				encoder.encodeSInt(coefficient, 500, 7);

				lastdc[ci] = block[0];
				}

				for (int pixel = 1; pixel < 64; pixel++)
				{
					int run = 0;
					for (int i = pixel; i < 64; i++, run++)
					{
						if (block[NATURAL_ORDER[i]] != 0)
						{
							break;
						}
					}

					if (pixel + run == 64)
					{
						int bin = 40 + pixel;
						for (int i = 0; i < 16; i++)
						{
							encoder.encodeBit(1, bin++);
						}
						break;
					}

					int bin = 40 + pixel;
					for (int i = 0; pixel < 64 && i < run; pixel++, i++)
					{
						encoder.encodeBit(1, bin++);
					}

					encoder.encodeBit(0, bin);

					int coefficient = block[NATURAL_ORDER[pixel]];

//					System.out.printf("%5d ", coefficient);

					if (coefficient > 0)
					{
						encoder.encodeBit(0, 0);
					}
					else
					{
						coefficient = -coefficient;
						encoder.encodeBit(1, 0);
					}

					coefficient--;

					bin = 2;
					int m = 0;

					if (coefficient != 0)
					{
						m = 1;

						for (int v = coefficient; v > 0; v >>= 1)
						{
							encoder.encodeBit(1, bin++);
							m <<= 1;
						}
					}

					encoder.encodeBit(0, bin);

					while ((m >>= 1) != 0)
					{
						encoder.encodeBit(coefficient & m, 1);
					}
				}

//				System.out.println();

//					int symbol = coefficient;
//
//					if (mtf)
//					{
//						for (int z = 0; z < order[blockIndex].length; z++)
//						{
//							if (order[blockIndex][z] == coefficient)
//							{
//								symbol = z;
//								break;
//							}
//						}
//					}
//
//					System.out.printf("%5d ", symbol);
//
//					encoder.encodeSInt(symbol, bin, 10);
//
//					if (mtf)
//					{
//						for (int z = symbol; z > 0; z--)
//						{
//							order[blockIndex][z] = order[blockIndex][z - 1];
//						}
//						order[blockIndex][0] = coefficient;
//					}
//				}
//				System.out.println();
			}
		}

		encoder.stopEncoding();
	}


	private void decode(BitInputStream bis, int[][][] aCoefficients) throws IOException
	{
		boolean mtf = false;

		int blockCount = aCoefficients[0].length;
//		int[][] order = new int[blockCount][2048 + 1 + 2048];
//		for (int i = 1, j = 1; i <= 2048; i++)
//		{
//			order[0][j++] = -i;
//			order[0][j++] =  i;
//		}
//		for (int i = 1; i < blockCount; i++)
//		{
//			order[i] = order[0].clone();
//		}

		int[] lastdc = new int[3];
		int[] compLookup = {0,0,0,0,1,2};

		DiracDecoder decoder = new DiracDecoder(bis, 1000);

		for (int mcuIndex = 0; mcuIndex < aCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < blockCount; blockIndex++)
			{
				int ci = compLookup[blockIndex];
				int[] block = aCoefficients[mcuIndex][blockIndex];

				block[0] = lastdc[ci] = lastdc[ci] + decoder.decodeSInt(500, 7);

				for (int pixel = 1; pixel < 64; pixel++)
				{
					int run = 0;

					int bin = 40 + pixel;
					for (int i = 0; i < 16; i++, run++)
					{
						if (!decoder.decodeBit(bin++))
						{
							break;
						}
					}

					while (run-- > 0)
					{
						block[NATURAL_ORDER[pixel++]] = 0;
					}
					if (run == 16)
					{
						break;
					}

					boolean neg = decoder.decodeBit(0);

					bin = 2;
					int m = 0;

					if (decoder.decodeBit(bin++))
					{
						m = 1;

						while (decoder.decodeBit(bin++))
						{
							m <<= 1;
						}
					}

					int coefficient = 0;

					while ((m >>= 1) != 0)
					{
						coefficient += decoder.decodeBit(1) ? m : 0;
					}

					block[NATURAL_ORDER[pixel]] = neg ? -coefficient - 1 : coefficient + 1;
				}

//				for (int pixel = 0, bin = 0*12 * blockIndex; pixel < 64; pixel++)
//				{
//					int symbol = decoder.decodeSInt(bin, 10);
//					int coefficient = symbol;
//
//					if (mtf)
//					{
//						coefficient = order[blockIndex][symbol];
//					}
//
//					block[NATURAL_ORDER[pixel]] = coefficient;
//
//					if (mtf)
//					{
//						for (int z = symbol; z > 0; z--)
//						{
//							order[blockIndex][z] = order[blockIndex][z - 1];
//						}
//						order[blockIndex][0] = coefficient;
//					}
//				}
			}
		}
	}


	private int unsigned(int aSigned)
	{
		return (aSigned << 1) ^ (aSigned >> 31);
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
