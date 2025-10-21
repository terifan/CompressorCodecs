package org.terifan.compression.vp8arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			testEncodeExpGolomb();
			System.out.println("-".repeat(150));
			testEncodeUnary();
			System.out.println("-".repeat(150));
			testEncodeBit();
			System.out.println("-".repeat(150));
			testEncodeVLC();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void testEncodeExpGolomb()
	{
		try
		{
			Random rnd = new Random();

			long[] in = new long[10];

			int[] order = new int[in.length];
			long[] out = new long[in.length];
			for (int i = 0; i < in.length; i++)
			{
				in[i] = rnd.nextLong(1L << rnd.nextInt(1 << rnd.nextInt(6)));
				order[i] = rnd.nextInt(4 << rnd.nextInt(4));
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					writer.encodeExpGolomb(in[i], order[i], context);
				}

				System.out.println(context);
			}

			int ln = 0;
			for (long v : in)
			{
				ln += Math.ceil(v == 0 ? 1 : Math.log(v) / Math.log(2) / 7) * 8;
			}
			System.out.println("*** input:" + ln / 8 + " => output:" + baos.size());

			try (VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos.toByteArray())))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					out[i] = reader.decodeExpGolomb(order[i], context);
				}
				System.out.println(context);
			}

			for (int i = 0; i < in.length; i++)
			{
				System.out.printf("%5s %19d %-19d %d%n", out[i] == in[i], in[i], out[i], order[i]);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void testEncodeUnary()
	{
		try
		{
			Random rnd = new Random();

			int[] in = new int[10];

			int[] out = new int[in.length];
			for (int i = 0; i < in.length; i++)
			{
				in[i] = rnd.nextInt(1 << rnd.nextInt(4));
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					writer.encodeUnary(in[i], context);
				}

				System.out.println(context);
			}

			int ln = 0;
			for (long v : in)
			{
				ln += Math.ceil(v == 0 ? 1 : Math.log(v) / Math.log(2) / 7) * 8;
			}
			System.out.println("*** input:" + ln / 8 + " => output:" + baos.size());

			try (VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos.toByteArray())))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					out[i] = reader.decodeUnary(context);
				}
				System.out.println(context);
			}

			for (int i = 0; i < in.length; i++)
			{
				System.out.printf("%5s %19d %d%n", out[i] == in[i], in[i], out[i]);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void testEncodeBit()
	{
		try
		{
			Random rnd = new Random();

			int[] in = new int[8 * 15];

			int[] out = new int[in.length];
			for (int i = 0; i < in.length; i++)
			{
				in[i] = rnd.nextInt(100) < 95 ? 0 : 1;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					writer.encodeBit(in[i], 0, context);
				}

				System.out.println(context);
			}

			System.out.println("*** input:" + (in.length / 8) + " => output:" + baos.size());

			try (VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos.toByteArray())))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					out[i] = reader.decodeBit(0, context);
				}
				System.out.println(context);
			}

			System.out.println(Arrays.equals(in, out));

			for (int i = 0; i < in.length; i++)
			{
				System.out.printf("%d", in[i]);
			}
			System.out.println();
			for (int i = 0; i < in.length; i++)
			{
				System.out.printf("%s", out[i] == in[i] ? "." : "#");
			}
			System.out.println();
			for (int i = 0; i < in.length; i++)
			{
				System.out.printf("%d", out[i]);
			}
			System.out.println();
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}


	public static void testEncodeVLC()
	{
		try
		{
			Random rnd = new Random();

			long[] in = new long[10000];

			long[] out = new long[in.length];
			for (int i = 0; i < in.length; i++)
			{
				in[i] = rnd.nextLong(4L << rnd.nextInt(12));
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (VP8Encoder writer = new VP8Encoder(baos))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					writer.encodeVLC(in[i], 3, context);
				}

				System.out.println(context);
			}

			int ln = 0;
			for (long v : in)
			{
				ln += Math.ceil(v == 0 ? 1 : Math.log(v) / Math.log(2) / 7) * 8;
			}
			System.out.println("*** input:" + ln / 8 + " => output:" + baos.size());

			try (VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos.toByteArray())))
			{
				VP8Context context = new VP8Context();
				for (int i = 0; i < in.length; i++)
				{
					out[i] = reader.decodeVLC(3, context);
				}
				System.out.println(context);
			}

			for (int i = 0; i < in.length; i++)
			{
//				System.out.printf("%5s %19d %d%n", out[i] == in[i], in[i], out[i]);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
