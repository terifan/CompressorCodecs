package org.terifan.compression.bitari2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;


public class ArithmeticTest
{
	public static void main(String... args)
	{
		try
		{
			Random rnd = new Random();
			long[] n =
			{
				0xffL & rnd.nextLong(),
				0xffffL & rnd.nextLong(),
				0xfffffffL & rnd.nextLong(),
				0xfffffffffffL & rnd.nextLong(),
				0xfffffffffffffffL & rnd.nextLong()
			};

			for (int i = 0; i < 16; i++)
			{
				test_encodeExpGolomb(Long.MIN_VALUE, i);
				test_encodeExpGolomb(Long.MAX_VALUE, i);
				test_encodeExpGolomb(Integer.MAX_VALUE, i);
				test_encodeExpGolomb(Integer.MIN_VALUE, i);
				test_encodeExpGolomb(-1, i);
				test_encodeExpGolomb(0, i);
				test_encodeExpGolomb(+1, i);
			}

			test_encodeBytesEqProb(128);
			test_encodeEqProb(Long.MAX_VALUE, 64);
			test_encodeEqProb(Long.MIN_VALUE, 64);
			test_encodeEqProb(Integer.MAX_VALUE, 32);
			test_encodeEqProb(Integer.MIN_VALUE, 32);

			for (int i = 0; i < n.length; i++)
			{
				test_encodeExpGolomb(n[i], 0);
				test_encodeExpGolomb(n[i], 8);
				test_encodeExpGolombEqProb(n[i], 0);
				test_encodeExpGolombEqProb(n[i], 8);
			}

			test1();
			test2();

			test_encodeProb();

			System.out.println("================================================================================================================================================");
			System.out.println("Success");
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test2() throws IOException
	{
		System.out.println("===== xxx ===========================================================================================================================================");

		double[] data = new double[]{
			48.991416692733765,
			48.991416692733765,
			48.991373777389526,
			48.991631269454956,
			48.991658091545105,
			48.989458680152893,
			48.992768526077271,
			48.985660672187805,
			48.976905941963196,
			48.970152139663696,
			48.965629935264587,
			48.960614204406738,
			48.957239985466   ,
			48.944869637489319,
			48.932204246520996,
			48.919823169708252,
			48.910049200057983,
			48.898993134498596,
			48.889621496200562,
			48.883870840072632,
			48.881564140319824,
			48.878206014633179,
			48.876382112503052,
			48.874649405479431,
			48.873823285102844,
			48.872868418693542,
			48.868995308876038,
			48.866066336631775,
			48.8641083240509  ,
			48.858464956283569,
			48.85296642780304 ,
			48.846040964126587,
			48.84096086025238 ,
			48.835644721984863,
			48.833177089691162,
			48.832527995109558,
			48.830398321151733,
			48.827877044677734,
			48.827211856842041,
			48.825725913047791,
			48.822169303894043,
			48.819733858108521,
			48.817722201347351,
			48.815925121307373
		};

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		{
			ArithmeticContext context = new ArithmeticContext();
			ArithmeticEncoder encoder = new ArithmeticEncoder(baos);

			long prev = 0;
			for (double input : data)
			{
				System.out.println(input);

				long value = Double.doubleToRawLongBits(input);

				value -= prev;
				value = encodeZigZag64(value);

				encoder.encodeExpGolomb(value >>> 32, 0, context);
				encoder.encodeEqProb(0xffffffffL & value, 32);

				prev = Double.doubleToRawLongBits(input);
			}
			encoder.stopEncoding();
		}

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		System.out.println(baos.size() + " / " + 8 * data.length);
		//Debug.hexDump(baos.toByteArray());

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ArithmeticContext context = new ArithmeticContext();
		ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));

		long prev = 0;
		for (double input : data)
		{
			long a = decoder.decodeExpGolomb(0, context);
			long b = decoder.decodeEqProb(32);
			long value = decodeZigZag64((a << 32) + b);
			value += prev;

			double output = Double.longBitsToDouble(value);

			System.out.println(output);

			if (output != input)
			{
				throw new InternalError(input+" != " +output);
			}

			prev = value;
		}
	}


	private static void test1() throws IOException
	{
		System.out.println("===== xxx ===========================================================================================================================================");

		byte[] input = RandomSentenceGenerator.next(new Random()).getBytes("UTF-16");

		//Debug.hexDump(input);

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		{
		ArithmeticContext context1 = new ArithmeticContext();
		ArithmeticContext context2 = new ArithmeticContext();
		ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
		encoder.encodeExpGolomb(input.length, 0, context1);
		for (int i = 0; i < input.length; i+=2)
		{
			encoder.encodeExpGolomb(255 & input[i], 0, context1);
			encoder.encodeExpGolomb(255 & input[i+1], 4, context2);
//			encoder.encodeExpGolomb((((255 & input[i])<<8)+(255 & input[i+1])), 0, context);
		}
		encoder.stopEncoding();
		}

		//Debug.hexDump(baos.toByteArray());

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		byte[] output = new byte[input.length];

		ArithmeticContext context1 = new ArithmeticContext();
		ArithmeticContext context2 = new ArithmeticContext();
		ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));
		int len = (int)decoder.decodeExpGolomb(0, context1);
		for (int i = 0; i < len; i+=2)
		{
			output[i] = (byte)decoder.decodeExpGolomb(0, context1);
			output[i+1] = (byte)decoder.decodeExpGolomb(4, context2);
		}

		//Debug.hexDump(output);

		if (!Arrays.equals(input, output)) throw new IOException("FAILED");
	}


	private static void test_encodeExpGolombEqProb(long input, int aStep) throws IOException
	{
		System.out.println("===== encodeExpGolombEqProb ===========================================================================================================================================");

		System.out.println(input);

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		{
		ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
		encoder.encodeExpGolombEqProb(input, aStep);
		encoder.stopEncoding();
		}

		//Debug.hexDump(baos.toByteArray());

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));
		long output = decoder.decodeExpGolombEqProb(aStep);

		System.out.println(output);

		if (input != output) throw new IOException("FAILED");
	}


	private static void test_encodeExpGolomb(long input, int aStep) throws IOException
	{
		System.out.println("===== encodeExpGolomb ===========================================================================================================================================");

		System.out.println(input);

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		{
		ArithmeticContext context = new ArithmeticContext();
		ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
		encoder.encodeExpGolomb(input, aStep, context);
		encoder.stopEncoding();
		}

		//Debug.hexDump(baos.toByteArray());

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ArithmeticContext context = new ArithmeticContext();
		ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));
		long output = decoder.decodeExpGolomb(aStep, context);

		System.out.println(output);

		if (input != output) throw new IOException("FAILED");
	}


	private static void test_encodeEqProb(long aInput, int aLength) throws IOException
	{
		System.out.println("===== encodeEqProb ===========================================================================================================================================");

		System.out.println(aInput);

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		{
		ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
		encoder.encodeEqProb(encodeZigZag64(aInput), aLength);
		encoder.stopEncoding();
		}

		//Debug.hexDump(baos.toByteArray());

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));
		long output = decodeZigZag64(decoder.decodeEqProb(aLength));

		System.out.println(output);

		if (aInput != output) throw new IOException("FAILED");
	}


	private static void test_encodeProb() throws IOException
	{
//		System.out.println("===== encodeProb ===========================================================================================================================================");
//
////		System.out.println(aInput);
//
//		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");
//
//		String text = RandomSentenceGenerator.next(new Random());
//		int[] input = stringToInts(text);
//		System.out.println(text);
//
//		ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
//		DeflaterOutputStream dos = new DeflaterOutputStream(baos2, new Deflater(Deflater.BEST_COMPRESSION, true));
//		dos.write(text.getBytes());
//		dos.close();
//		Debug.hexDump(baos2.toByteArray());
//		System.out.println(baos2.size()+"/"+input.length);
//
//		int window = 16;
//
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		{
//		ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
//		ArithmeticContext[] context = new ArithmeticContext[1<<window];
//		for (int i = 0; i < context.length; i++)
//		{
//			context[i] = new ArithmeticContext();
//		}
//
//		{
//			ArithmeticEncoder xxx = new ArithmeticEncoder(new ByteArrayOutputStream());
//			for (String s : Dic.words)
//				xxx.encodeProb(stringToInts(s), 8, window, context);
//		}
//
//		encoder.encodeProb(input, 8, window, context);
//		encoder.stopEncoding();
//		}
//
//		Debug.hexDump(baos.toByteArray());
//		System.out.println(baos.size()+"/"+input.length);
//
//		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");
//
//		ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));
//		ArithmeticContext[] context = new ArithmeticContext[1<<window];
//		for (int i = 0; i < context.length; i++)
//		{
//			context[i] = new ArithmeticContext();
//		}
//
//		{
//			ArithmeticEncoder xxx = new ArithmeticEncoder(new ByteArrayOutputStream());
//			for (String s : Dic.words)
//				xxx.encodeProb(stringToInts(s), 8, window, context);
//		}
//
//		int[] output = new int[input.length];
//		decoder.decodeProb(output, output.length, 8, window, context);
//
//		if (!Arrays.equals(input, output)) throw new IOException("FAILED");
	}

	private static int[] stringToInts(String text)
	{
		int[] input = new int[text.length()];
		for (int i = 0; i < text.length(); i++)
		{
			input[i] = text.charAt(i);
		}
		return input;
	}


	private static void test_encodeBytesEqProb(int aLength) throws IOException
	{
		System.out.println("===== encodeBytesEqProb ===========================================================================================================================================");

		byte[] input = new byte[aLength];
		new Random().nextBytes(input);

		//Debug.hexDump(input);

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ArithmeticEncoder encoder = new ArithmeticEncoder(baos);
		encoder.encodeBytesEqProb(input, 0, input.length);
		encoder.stopEncoding();

		//Debug.hexDump(baos.toByteArray());

		System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------");

		byte[] output = new byte[input.length];

		ArithmeticDecoder decoder = new ArithmeticDecoder(new ByteArrayInputStream(baos.toByteArray()));
		decoder.decodeBytesEqProb(output, 0, output.length);

		//Debug.hexDump(output);

		if (!Arrays.equals(input, output)) throw new IOException("FAILED");
	}


	static int decodeZigZag32(int n)
	{
		return ((n >>> 1) ^ -(n & 1));
	}


	static long encodeZigZag64(long n)
	{
		// Note:  the right-shift must be arithmetic
		return (n << 1) ^ (n >> 63);
	}


	static long decodeZigZag64(long n)
	{
		return (n >>> 1) ^ -(n & 1);
	}
}
