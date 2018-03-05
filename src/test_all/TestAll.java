package test_all;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Random;
import org.terifan.compression.basic_arithmetic.BasicArithmeticContext;
import org.terifan.compression.basic_arithmetic.BasicArithmeticDecoder;
import org.terifan.compression.basic_arithmetic.BasicArithmeticEncoder;
import org.terifan.compression.cabac.CabacContext;
import org.terifan.compression.cabac.CabacDecoder;
import org.terifan.compression.cabac.CabacEncoder;
import org.terifan.compression.dirac.DiracDecoder;
import org.terifan.compression.dirac.DiracEncoder;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class TestAll
{
	public static void main(String... args)
	{
		try
		{
			Random rnd = new Random(1);

			int symbolCountN = 4;
			int symbolCount = 1 << symbolCountN;
			int[] input = new int[65536];

			for (int i = 0; i < input.length; i++)
			{
				input[i] = rnd.nextInt(4 << rnd.nextInt(symbolCountN - 1));
			}

			for (int i = 1; i < input.length; i++)
			{
				if (input[i - 1] == 1 << (symbolCountN - 1))
				{
					input[i] = 0;
				}
			}

			System.out.printf("dirac %s%n", dirac(input, symbolCount));
			System.out.printf("arith %s%n", arith(input, symbolCount));
			System.out.printf("cabac %s%n", cabac(input, symbolCount));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static int dirac(int[] aInput, int aSymbolCount) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BitOutputStream bos = new BitOutputStream(baos))
		{
			DiracEncoder encoder = new DiracEncoder(bos, aSymbolCount);

			for (int i = 0, bin = 0; i < aInput.length; i++)
			{
				encoder.encodeUInt(aInput[i], bin, 10);
				bin = 0 * aInput[i];
			}

			encoder.stopEncoding();
		}

		int[] output = new int[aInput.length];

		try (BitInputStream bis = new BitInputStream(new ByteArrayInputStream(baos.toByteArray())))
		{
			DiracDecoder decoder = new DiracDecoder(bis, aSymbolCount);

			for (int i = 0, bin = 0; i < aInput.length; i++)
			{
				output[i] = decoder.decodeUInt(bin, 10);
				bin = 0 * output[i];
			}
		}

		assertEquals(aInput, output);

		return baos.size();
	}


	private static int cabac(int[] aInput, int aSymbolCount) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		{
			CabacEncoder encoder = new CabacEncoder(baos);
			CabacContext context = new CabacContext(0);

			for (int i = 0; i < aInput.length; i++)
			{
				encoder.encodeExpGolomb(aInput[i], 0, context);
			}

			encoder.encodeFinal(1);
			encoder.stopEncoding();
		}

		int[] output = new int[aInput.length];

		{
			CabacDecoder decoder = new CabacDecoder(new PushbackInputStream(new ByteArrayInputStream(baos.toByteArray())));
			CabacContext context = new CabacContext(0);

			for (int i = 0; i < aInput.length; i++)
			{
				output[i] = (int)decoder.decodeExpGolomb(0, context);
			}
		}

		assertEquals(aInput, output);

		return baos.size();
	}


	private static int arith(int[] aInput, int aSymbolCount) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BitOutputStream bitOutputStream = new BitOutputStream(baos))
		{
			BasicArithmeticContext context = new BasicArithmeticContext(aSymbolCount, true);
			BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(bitOutputStream);

			for (int i = 0; i < aInput.length; i++)
			{
				encoder.encode(aInput[i], context);
			}

			encoder.stopEncoding();
		}

		int[] output = new int[aInput.length];

		{
			BasicArithmeticContext context = new BasicArithmeticContext(aSymbolCount, true);
			BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(baos.toByteArray())));

			for (int i = 0; i < aInput.length; i++)
			{
				output[i] = decoder.decode(context);
			}
		}

		assertEquals(aInput, output);

		return baos.size();
	}


	private static void assertEquals(int[] aExpected, int[] aActual)
	{
		for (int i = 0; i < aExpected.length; i++)
		{
			if (aExpected[i] != aActual[i])
			{
				throw new IllegalArgumentException("Error at offset " + i + ", was: " + aActual[i] + ", expected " + aExpected[i]);
			}
		}
	}
}
