package org.terifan.compression.test_suit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import org.terifan.compression.basic_arithmetic.BasicArithmeticContext;
import org.terifan.compression.basic_arithmetic.BasicArithmeticDecoder;
import org.terifan.compression.basic_arithmetic.BasicArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bwt.BWT;
import org.terifan.compression.cabac264.CabacContext264;
import org.terifan.compression.cabac264.CabacDecoder264;
import org.terifan.compression.cabac264.CabacEncoder264;
import org.terifan.compression.cabac265.CabacDecoder265;
import org.terifan.compression.cabac265.CabacEncoder265;
import org.terifan.compression.cabac265.CabacContext265;
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
			int[] data = _LoadTestData.loadTestDataInt(_LoadTestData.Source.LENNA_GRAY);

			System.out.printf("Dirac    %s%n", dirac(data, 12));
			System.out.printf("BasicAri %s%n", basicAri(data, 256));
			System.out.printf("BitAri   %s%n", bitari(data));
			System.out.printf("Cabac264 %s%n", cabac264(data));
			System.out.printf("Cabac265 %s%n", cabac265(data));

			System.out.println();

			for (int i = 0; i < data.length; i += 1024)
			{
				BWT.encode(data, i, Math.min(1024, data.length - i));
			}

			System.out.printf("Dirac    %s%n", dirac(data, 12));
			System.out.printf("BasicAri %s%n", basicAri(data, 256));
			System.out.printf("BitAri   %s%n", bitari(data));
			System.out.printf("Cabac264 %s%n", cabac264(data));
			System.out.printf("Cabac265 %s%n", cabac265(data));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static int dirac(int[] aInput, int aModelCount) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (DiracEncoder encoder = new DiracEncoder(new BitOutputStream(baos), aModelCount))
		{
			for (int i = 0, bin = 0; i < aInput.length; i++)
			{
				encoder.encodeUInt(aInput[i], bin, 10);
			}
		}

		int[] output = new int[aInput.length];

		try (DiracDecoder decoder = new DiracDecoder(new BitInputStream(new ByteArrayInputStream(baos.toByteArray())), aModelCount))
		{
			for (int i = 0, bin = 0; i < aInput.length; i++)
			{
				output[i] = decoder.decodeUInt(bin, 10);
			}
		}

		assertEquals(aInput, output);

		return baos.size();
	}


	private static int bitari(int[] aInput) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (ArithmeticEncoder encoder = new ArithmeticEncoder(baos))
		{
			ArithmeticContext[] context = {new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext()};

			for (int i = 0; i < aInput.length; i++)
			{
				encoder.encodeExpGolomb(aInput[i], 0, context);
			}
		}

		int[] output = new int[aInput.length];

		try (ArithmeticDecoder decoder = new ArithmeticDecoder(new PushbackInputStream(new ByteArrayInputStream(baos.toByteArray()))))
		{
			ArithmeticContext[] context = {new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext(),new ArithmeticContext()};

			for (int i = 0; i < aInput.length; i++)
			{
				output[i] = (int)decoder.decodeExpGolomb(0, context);
			}
		}

		assertEquals(aInput, output);

		return baos.size();
	}


	private static int cabac264(int[] aInput) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (CabacEncoder264 encoder = new CabacEncoder264(baos))
		{
			CabacContext264[] context = {new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0)};

			for (int i = 0; i < aInput.length; i++)
			{
				encoder.encodeExpGolomb(aInput[i], 0, context);
			}

			encoder.encodeFinal(1);
		}

		int[] output = new int[aInput.length];

		try (CabacDecoder264 decoder = new CabacDecoder264(new PushbackInputStream(new ByteArrayInputStream(baos.toByteArray()))))
		{
			CabacContext264[] context = {new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0),new CabacContext264(0)};

			for (int i = 0; i < aInput.length; i++)
			{
				output[i] = (int)decoder.decodeExpGolomb(0, context);
			}
		}

		assertEquals(aInput, output);

		return baos.size();
	}


	private static int cabac265(int[] aInput) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		{
			CabacContext265[] models = CabacContext265.create(20);

			try (CabacEncoder265 encoder = new CabacEncoder265(baos))
			{
				for (int i = 0; i < aInput.length; i++)
				{
					encoder.encodeCABAC_EGk(aInput[i], 0, 20, models);
				}

				encoder.encodeFinal(1);
			}
		}

		int[] output = new int[aInput.length];

		CabacContext265[] models = CabacContext265.create(20);
		try (CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(baos.toByteArray())))
		{
			for (int i = 0; i < aInput.length; i++)
			{
				output[i] = (int)decoder.decodeCABAC_EGk(0, 20, models);
			}
		}

		assertEquals(aInput, output);

		return baos.size();
	}


	private static int basicAri(int[] aInput, int aSymbolCount) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BasicArithmeticEncoder encoder = new BasicArithmeticEncoder(new BitOutputStream(baos)))
		{
			BasicArithmeticContext context = new BasicArithmeticContext(aSymbolCount, true);

			for (int i = 0; i < aInput.length; i++)
			{
				encoder.encode(aInput[i], context);
			}
		}

		int[] output = new int[aInput.length];

		try (BasicArithmeticDecoder decoder = new BasicArithmeticDecoder(new BitInputStream(new ByteArrayInputStream(baos.toByteArray()))))
		{
			BasicArithmeticContext context = new BasicArithmeticContext(aSymbolCount, true);

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
