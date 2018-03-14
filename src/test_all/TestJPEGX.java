package test_all;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;


public class TestJPEGX
{
	public TestJPEGX(int[][][] aInputCoefficients) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		new Encoder().encode(baos, aInputCoefficients);

		byte[] data = baos.toByteArray();

		System.out.println(data.length);

		int[][][] outputCoefficients = new int[aInputCoefficients.length][aInputCoefficients[0].length][64];

		new Decoder().decode(new ByteArrayInputStream(data), outputCoefficients);

		for (int mcuIndex = 0; mcuIndex < outputCoefficients.length; mcuIndex++)
		{
			for (int blockIndex = 0; blockIndex < outputCoefficients[0].length; blockIndex++)
			{
				if (!Arrays.equals(aInputCoefficients[mcuIndex][blockIndex], outputCoefficients[mcuIndex][blockIndex]))
				{
//					System.out.println();
//					for (int i = 0; i < 64; i++)
//					{
//						/*if (aInputCoefficients[mcuIndex][blockIndex][i]!=0)*/ System.out.printf("%5d ", aInputCoefficients[mcuIndex][blockIndex][NATURAL_ORDER[i]]);
//					}
//					System.out.println();
//
//					for (int i = 0; i < 64; i++)
//					{
//						/*if (outputCoefficients[mcuIndex][blockIndex][i]!=0)*/ System.out.printf("%5d ", outputCoefficients[mcuIndex][blockIndex][NATURAL_ORDER[i]]);
//					}
//					System.out.println();
				}
			}
		}

		System.out.println(Arrays.deepEquals(aInputCoefficients, outputCoefficients));
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
