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
//	public static void main(String ... args)
//	{
//		try
//		{
//			ByteArrayOutputStream aOutputStream = new ByteArrayOutputStream();
//			{
//		VP8Encoder mWriter = new VP8Encoder(aOutputStream);
//		VP8Context mContext0;
//			mContext0 = new VP8Context();
//		mWriter.encodeBit(0, 0, mContext0);
//		mWriter.close();
//			}
//
//		VP8Decoder mReader = new VP8Decoder(new ByteArrayInputStream(aOutputStream.toByteArray()));
//	VP8Context mContext0;
//		mContext0 = new VP8Context();
//		if (mReader.decodeBit(0, mContext0) == 0) System.out.println("#");
//}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}

	public static void main(String... args)
	{
		try
		{
			Random rnd = new Random(1);

			byte[] data = Files.readAllBytes(Paths.get("C:\\Users\\patrik\\Documents\\Books\\Romeo & Juliet.html"));
//			byte[] data = Files.readAllBytes(Paths.get("j:\\Documents\\Books\\Lolita.html"));

			int n = 1;
			int sz0 = data.length;
			int sz1 = 1;
			int srcLen = 0;

			byte[][] sourceA = new byte[n][];
//			byte[][] sourceB = new byte[n][];

			for (int i = 0; i < n; i++)
			{
				int len = sz0 + rnd.nextInt(sz1);
				sourceA[i] = Arrays.copyOfRange(data, srcLen, srcLen + len);
//				sourceB[i] = new byte[rnd.nextInt(sz1)];
//				rnd.nextBytes(sourceB[i]);
				srcLen += len;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (LZJBCompressor dos = new LZJBCompressor(baos))
			{
				for (int i = 0; i < n; i++)
				{
					dos.compress(sourceA[i]);
//				baos.write(sourceB[i]);
				}
			}

			byte[] compressedData = baos.toByteArray();

			Console.println("{input:}{} => {output:}{}", srcLen, baos.size());

//			Log.hexDump(compressedData, 24);
			ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
			LZJBDecompressor in = new LZJBDecompressor(bais);
			for (int i = 0; i < n; i++)
			{
				byte[] inputA = in.decompress(sourceA[i].length);
//				byte[] inputB = bais.readNBytes(sourceB[i].length);

//System.out.println("-".repeat(100));
//System.out.println(new String(sourceA[0]));
//System.out.println("-".repeat(100));
//System.out.println(new String(inputA));
//System.out.println("-".repeat(100));
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
