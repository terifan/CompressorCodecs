package org.terifan.compression.test_suit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class TestDynamic
{
	public static void main(String... args)
	{
		try
		{
			int BITS = 14;
			int STATIC = 5;
			boolean dynamic = false;

			int[] values = new int[1000];
			Random rnd = new Random(3);
			for (int i = 0; i < values.length; i++)
			{
				values[i] = rnd.nextInt(1 << rnd.nextInt(BITS));
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (BitOutputStream bos = new BitOutputStream(baos))
			{
				int next = 0;
				int prev1 = 0;
				int prev2 = 0;
				for (int i : values)
				{
					int cur = 0;
					int v = i;
					while (v >= (1 << cur))
					{
						v -= 1 << cur;
						cur++;
					}

					System.out.printf("%8d %2d %2d ", i, next, cur);
					bos.writeGolomb(i, dynamic ? next : STATIC);

//					next = cur;
//					next = (cur+prev1)/2;
					next = (cur+prev1+prev2)/3;
					prev2 = prev1;
					prev1 = cur;
				}
			}

			System.out.println("==============================================");
			System.out.println(baos.size());

			try (BitInputStream bis = new BitInputStream(new ByteArrayInputStream(baos.toByteArray())))
			{
				int next = 0;
				int prev1 = 0;
				int prev2 = 0;

				for (int expected : values)
				{
					int v = bis.readGolomb(dynamic ? next : STATIC);

					if (v != expected)
					{
						System.out.println("err");
					}

					int cur = 0;
					while (v >= (1 << cur))
					{
						v -= 1 << cur;
						cur++;
					}

//					next = cur;
//					next = (cur+prev)/2;
					next = (cur+prev1+prev2)/3;
					prev2 = prev1;
					prev1 = cur;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
