package org.terifan.compression.lzw;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import testdata.LoadTestData.Source;
import static testdata.LoadTestData.loadTestData;


public class Test
{
	public static void main(String... args)
	{
		try
		{
			byte[] input = loadTestData(Source.BOOK);

			long t = System.currentTimeMillis();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (LZWOutputStream out = new LZWOutputStream(baos, 8, 14))
			{
				int p = 0;
				for (byte c : input)
				{
					out.write((0xff & c) - p + 128);
					p = 0xff & c;
				}
			}
			t = System.currentTimeMillis() - t;

			System.out.println("Compress: " + t + "ms");
			System.out.println("Size: " + baos.size() + " / " + input.length);

			t = System.currentTimeMillis();
			try (LZWInputStream in = new LZWInputStream(new ByteArrayInputStream(baos.toByteArray()), 8, 14))
			{
				int p = 0;
				baos = new ByteArrayOutputStream();
				for (int c; (c = in.read()) != -1;)
				{
					baos.write(c + p - 128);
					p = c + p - 128;
				}
			}
			t = System.currentTimeMillis() - t;

			System.out.println("Decompress: " + t + "ms");
			System.out.println("Match: " + Arrays.equals(input, baos.toByteArray()));
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
