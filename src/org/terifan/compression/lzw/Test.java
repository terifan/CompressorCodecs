package org.terifan.compression.lzw;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;


public class Test
{
	public static void main(String... args)
	{
		try
		{
//			File f = new File("d:/jvm-app-0.log");
//			byte[] input = new byte[(int) f.length()];
//			try (FileInputStream fin = new FileInputStream(f))
//			{
//				fin.read(input);
//			}

			byte [] input = "The quick brown fox jumped over the lazy dog.".getBytes();

			long t = System.nanoTime();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (LZWOutputStream out = new LZWOutputStream(baos, 8, 12))
			{
				out.write(input);
			}
			t = System.nanoTime() - t;

			System.out.println("Compress: " + t / 1000000 + "ms");
			System.out.println("Size: " + baos.size() + " / " + input.length);

			t = System.nanoTime();
			try (LZWInputStream in = new LZWInputStream(new ByteArrayInputStream(baos.toByteArray()), 8, 12))
			{
				baos = new ByteArrayOutputStream();
				for (int c; (c = in.read()) != -1;)
				{
					System.out.print((char)c);
					baos.write(c);
				}
			}
			t = System.nanoTime() - t;

			System.out.println("Decompress: " + t / 1000000 + "ms");
			System.out.println("Match: " + Arrays.equals(input, baos.toByteArray()));
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
