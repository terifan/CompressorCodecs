package org.terifan.compression.lzjb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import org.terifan.compression.util.Log;
import org.terifan.compression.vp8arithmetic.VP8Context;
import org.terifan.compression.vp8arithmetic.VP8Decoder;
import org.terifan.compression.vp8arithmetic.VP8Encoder;
import org.terifan.io.Streams;
import org.terifan.util.Console;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			Random rnd = new Random(1);

			byte[] data = Files.readAllBytes(Paths.get("C:\\Users\\patrik\\Documents\\Books\\Romeo & Juliet.html"));
//			byte[] data = Files.readAllBytes(Paths.get("j:\\Documents\\Books\\Lolita.html"));

			int n = 2;
			int sz1 = 1;
			int sz0 = 50;//data.length / n - sz1;
			int srcLen = 0;

			byte[][] sourceA = new byte[n][];
			byte[][] sourceB = new byte[n][];

			for (int i = 0; i < n; i++)
			{
				int len = sz0 + rnd.nextInt(sz1);

				System.out.println(data.length + " " + len + " " + srcLen + " " + (srcLen + len));

				sourceA[i] = Arrays.copyOfRange(data, srcLen, Math.min(data.length, srcLen + len));
				sourceB[i] = new byte[rnd.nextInt(sz1)];
				rnd.nextBytes(sourceB[i]);
				srcLen += len;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (LZJBCompressor dos = new LZJBCompressor(baos))
			{
				for (int i = 0; i < n; i++)
				{
					dos.compress(sourceA[i]);
//					baos.write(sourceB[i]);
				}
			}

			byte[] compressedData = baos.toByteArray();

			Console.println("{input:}{} => {output:}{}", srcLen, baos.size());

//			Log.hexDump(compressedData, 24);
			LZJBDecompressor in = new LZJBDecompressor(new ByteArrayInputStream(compressedData));
			for (int i = 0; i < n; i++)
			{
				byte[] inputA = in.decompress(sourceA[i].length);
//				byte[] inputB = bais.readNBytes(sourceB[i].length);

				System.out.println(Arrays.equals(inputA, sourceA[i]));
//				System.out.println(Arrays.equals(inputB, sourceB[i]));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
