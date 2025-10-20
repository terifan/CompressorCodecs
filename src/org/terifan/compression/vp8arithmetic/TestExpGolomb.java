package org.terifan.compression.vp8arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class TestExpGolomb
{
//	public static void main(String ... args)
//	{
//		try
//		{
//			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
//			try (VP8Encoder writer = new VP8Encoder(baos1))
//			{
//				Random r = new Random(1);
//				for (int i = 0; i < 1000; i++)
//				{
//					int v = r.nextInt(1 << r.nextInt(31));
//					System.out.print(v + ", ");
//					writer.encodeExpGolomb(v, 3);
//				}
//				System.out.println();
//			}
//			System.out.println("-".repeat(100));
//			try (VP8Decoder reader = new VP8Decoder(new ByteArrayInputStream(baos1.toByteArray())))
//			{
//				for (int i = 0; i < 1000; i++)
//				{
//					System.out.print(reader.decodeExpGolomb(3) + ", ");
//				}
//				System.out.println();
//			}
//			System.out.println("-".repeat(100));
//			System.out.println(baos1.size());
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}
//
//
//	private static void print(int[] aArray)
//	{
//		for (int i = 0; i < aArray.length; i++)
//		{
//			System.out.print((i == 0 ? "" : ", ") + aArray[i]);
//		}
//	}
}
