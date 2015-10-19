package org.terifan.compression.cabac_c;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;
import java.util.Random;


public class Test2
{
	public static void main(String... args)
	{
		try
		{
			for(;;)
			{
				int seed = new Random().nextInt(Integer.MAX_VALUE);
				System.out.println(seed);

				Random rnd = new Random(seed);
				int L = 10;
				byte[][] input = new byte[L][];
				byte[][] output = new byte[L][];

				for(int testRun = 0; testRun < 10000; testRun++)
				{
					for (int i = 0; i < L; i++)
					{
						input[i]=new byte[rnd.nextInt(100)];
						output[i] = new byte[input[i].length];
						for (int j = 0; j < input[i].length; j++) input[i][j] = (byte)rnd.nextInt(100);
					}

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					for (int i = 0; i < L; i++)
					{
						CabacEncoder encoder = new CabacEncoder(baos);
						CabacContext encoderCtx0 = new CabacContext();
						CabacContext encoderCtx1 = new CabacContext();
						CabacContext encoderCtx2 = new CabacContext();
						for (int j = 0; j < input[i].length; j++) encoder.encodeUnary(input[i][j], encoderCtx0, encoderCtx1);
						for (int j = 0; j < input[i].length; j++) encoder.encodeExpGolomb(input[i][j], 0, encoderCtx2);
						encoder.encodeFinal(1);
						encoder.stopEncoding();
					}

					byte[] buffer = baos.toByteArray();

					PushbackInputStream bais = new PushbackInputStream(new ByteArrayInputStream(buffer), 2);
					for (int i = 0; i < L; i++)
					{
						CabacDecoder decoder = new CabacDecoder(bais);
						CabacContext decoderCtx0 = new CabacContext();
						CabacContext decoderCtx1 = new CabacContext();
						CabacContext decoderCtx2 = new CabacContext();
						for (int j = 0; j < output[i].length; j++) output[i][j] = (byte)decoder.decodeUnary(decoderCtx0, decoderCtx1);
						for (int j = 0; j < output[i].length; j++) if (output[i][j] != decoder.decodeExpGolomb(0, decoderCtx2)) throw new Error(""+seed);
						int f = decoder.decodeFinal();
						if (f != 1) throw new Error(""+seed);
					}

//					System.out.println(Arrays.equals(input[1], output[1]));
//					System.out.println(Arrays.asList(input[1]));
//					System.out.println(Arrays.asList(output[1]));

					for (int i = 0; i < L; i++)
					{
						if (!Arrays.equals(input[i], output[i])) throw new Error(""+seed);
					}
				}
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
