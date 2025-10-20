package org.terifan.compression.lzjb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import org.terifan.compression.util.Log;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			int n = 1;
			int sz0 = 190431;
			int sz1 = 1;

			Random rnd = new Random(1);
			byte[][] sourceA = new byte[n][];
			byte[][] sourceB = new byte[n][];
			int srcLen = 0;

			byte[] data = Files.readAllBytes(Paths.get("C:\\Users\\patrik\\Documents\\romeo_juliet.html"));
			for (int i = 0; i < n; i++)
			{
				int len = sz0 + rnd.nextInt(sz1);
				sourceA[i] = Arrays.copyOfRange(data, srcLen, srcLen + len);
				sourceB[i] = new byte[rnd.nextInt(sz1)];
				rnd.nextBytes(sourceB[i]);
				srcLen += len;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			LZJBCompressor dos = new LZJBCompressor(baos);
			for (int i = 0; i < n; i++)
			{
				dos.compress(sourceA[i]);
//				baos.write(sourceB[i]);
			}

			byte[] compressedData = baos.toByteArray();

			System.out.println(baos.size() + " / " + srcLen);

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
