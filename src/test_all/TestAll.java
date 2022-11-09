package test_all;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Random;
import java.util.zip.InflaterInputStream;
import org.terifan.compression.basic_arithmetic.BasicArithmeticContext;
import org.terifan.compression.basic_arithmetic.BasicArithmeticDecoder;
import org.terifan.compression.basic_arithmetic.BasicArithmeticEncoder;
import org.terifan.compression.bitari.ArithmeticContext;
import org.terifan.compression.bitari.ArithmeticDecoder;
import org.terifan.compression.bitari.ArithmeticEncoder;
import org.terifan.compression.bwt.BWT;
import org.terifan.compression.cabac.CabacContext;
import org.terifan.compression.cabac.CabacDecoder;
import org.terifan.compression.cabac.CabacEncoder;
import org.terifan.compression.cabac265.CabacDecoder265;
import org.terifan.compression.cabac265.CabacEncoder265;
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
//			Random rnd = new Random();
//
//			int[] input = new int[100000];
//
//			int[] bs = {2,2,2,2,2,2,2,2,2,2,4,4,4,4,4,8};
//
//			int symbolCount = 1 << bs[bs.length - 1];
//
//			for (int i = 0; i < input.length; i++)
//			{
//				input[i] = rnd.nextInt(1 << bs[rnd.nextInt(bs.length)]);
//			}

			int[] unsigned = new int[81 * 56 * 6 * 8 * 8];
			int[] signed = new int[81 * 56 * 6 * 8 * 8];
			try (DataInputStream dis = new DataInputStream(new InflaterInputStream(TestAll.class.getResourceAsStream("swallowtail.jpg.data"))))
			{
				for (int i = 0; i < unsigned.length; i++)
				{
					signed[i] = dis.readShort();
					unsigned[i] = (signed[i] << 1) ^ (signed[i] >> 31);

//					System.out.printf("%5d", signed[i]);
//					if ((i%64)==63)System.out.println();
				}
			}

			int symbolCount = 2048;

			System.out.printf("dirac(S) %s%n", dirac(signed, 10*symbolCount, true));
			System.out.printf("dirac    %s%n", dirac(unsigned, 10*symbolCount, false));
			System.out.printf("arith    %s%n", arith(unsigned, symbolCount));
			System.out.printf("cabac264 %s%n", cabac(unsigned, symbolCount));
			System.out.printf("cabac265 %s%n", cabac265(unsigned, symbolCount));

			System.out.println();

			for (int i = 0; i < unsigned.length; i += 1024)
			{
				BWT.encode(unsigned, i, Math.min(1024, unsigned.length - i));
				BWT.encode(signed, i, Math.min(1024, unsigned.length - i));
			}

			System.out.printf("dirac(S) %s%n", dirac(signed, 10*symbolCount, true));
			System.out.printf("dirac    %s%n", dirac(unsigned, 10*symbolCount, false));
			System.out.printf("arith    %s%n", arith(unsigned, symbolCount));
			System.out.printf("cabac264 %s%n", cabac(unsigned, symbolCount));
			System.out.printf("cabac265 %s%n", cabac265(unsigned, symbolCount));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static int dirac(int[] aInput, int aSymbolCount, boolean aSigned) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BitOutputStream bos = new BitOutputStream(baos))
		{
			DiracEncoder encoder = new DiracEncoder(bos, aSymbolCount);

			for (int i = 0, bin = 0; i < aInput.length; i++)
			{
				if (aSigned)
				encoder.encodeSInt(aInput[i], bin, 10);
				else
				encoder.encodeUInt(aInput[i], bin, 10);

				bin = 0*10*Math.abs(aInput[i]);
			}

			encoder.stopEncoding();
		}

		int[] output = new int[aInput.length];

		try (BitInputStream bis = new BitInputStream(new ByteArrayInputStream(baos.toByteArray())))
		{
			DiracDecoder decoder = new DiracDecoder(bis, aSymbolCount);

			for (int i = 0, bin = 0; i < aInput.length; i++)
			{
				if (aSigned)
				output[i] = decoder.decodeSInt(bin, 10);
				else
				output[i] = decoder.decodeUInt(bin, 10);

				bin = 0*10*Math.abs(output[i]);
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


	private static int cabac265(int[] aInput, int aSymbolCount) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		{
			CabacEncoder265 encoder = new CabacEncoder265(baos);

			for (int i = 0; i < aInput.length; i++)
			{
				encoder.writeCABAC_EGk_bypass(aInput[i], 0);
			}

			encoder.encodeFinal(1);
			encoder.stopEncoding();
		}

		int[] output = new int[aInput.length];

		{
			CabacDecoder265 decoder = new CabacDecoder265(new ByteArrayInputStream(baos.toByteArray()));

			for (int i = 0; i < aInput.length; i++)
			{
				output[i] = (int)decoder.decodeCABAC_EGk_bypass(0);
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

			encoder.close();
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


	private static int bitarith(int[] aInput, int aSymbolCount) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		{
			ArithmeticContext[] context = new ArithmeticContext[20];
			for (int i = 0; i < context.length; i++)
			{
				context[i] = new ArithmeticContext();
			}

			ArithmeticEncoder encoder = new ArithmeticEncoder(baos);

			for (int i = 0; i < aInput.length; i++)
			{
				encoder.encodeExpGolomb(aInput[i], 1, context);
			}

			encoder.stopEncoding();
		}

		int[] output = new int[aInput.length];

		{
			ArithmeticContext[] context = new ArithmeticContext[20];
			for (int i = 0; i < context.length; i++)
			{
				context[i] = new ArithmeticContext();
			}

			ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));

			for (int i = 0; i < aInput.length; i++)
			{
				output[i] = (int)decoder.decodeExpGolomb(1, context);
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
