package org.terifan.compression.lzjb;

import java.util.Arrays;



public class SymbolFrequency
{
	private final int mCumFreqLimit;
	private final int[] mFreq;
	private final int[] mOrderToSym;
	private final int[] mSymToOrder;
	private int mCumFreq;


	public SymbolFrequency(int aSymbolCount, int aCumFreqLimit)
	{
		mCumFreqLimit = aCumFreqLimit;
		mFreq = new int[aSymbolCount];
		mOrderToSym = new int[aSymbolCount];
		mSymToOrder = new int[aSymbolCount];
		for (int i = 0; i < aSymbolCount; i++)
		{
			mOrderToSym[i] = i;
			mSymToOrder[i] = i;
		}
	}


	public int encode(int aOrder)
	{
		return mSymToOrder[aOrder];
	}


	public int decode(int aOrder)
	{
		return mOrderToSym[aOrder];
	}


	public void update(int aSymbol)
	{
		int f = ++mFreq[aSymbol];
		mCumFreq++;

		if (mCumFreq > mCumFreqLimit)
		{
			for (int i = 0; i < mFreq.length; i++)
			{
				int d = mFreq[i] / 2;
				mFreq[i] -= d;
				mCumFreq -= d;
			}
		}

		for (;;)
		{
			int o = mSymToOrder[aSymbol];
			if (o == 0)
			{
				break;
			}
			int s = mOrderToSym[o - 1];
			if (mFreq[s] >= f)
			{
				break;
			}
			mOrderToSym[o - 1] = aSymbol;
			mOrderToSym[o] = s;
			mSymToOrder[aSymbol] = o - 1;
			mSymToOrder[s] = o;
		}
	}


	@Override
	public String toString()
	{
		String str = "";
		for (int i = 0; i < mFreq.length; i++)
		{
			if (!str.isEmpty())
			{
				str += ", ";
			}
			int t = mOrderToSym[i];
			str += t + "=" + mFreq[t];
		}

		return str+"\n"+Arrays.toString(mFreq)+"\n"+Arrays.toString(mSymToOrder)+"\n"+Arrays.toString(mOrderToSym);
	}


	public static void main(String... args)
	{
		try
		{
//			Random rnd = new Random(1);
//			SymbolFrequency freq = new SymbolFrequency(16, 10000);
//			for (int i = 0; i < 1000; i++)
//			{
//				freq.add(rnd.nextInt(16));
//			}
//			System.out.println(freq);
//

			SymbolFrequency freq = new SymbolFrequency(4, 10000);
			System.out.println(freq);
			freq.update(2);
			System.out.println(freq);
			freq.update(2);
			System.out.println(freq);
			freq.update(3);
			System.out.println(freq);
			freq.update(3);
			System.out.println(freq);
			freq.update(3);
			System.out.println(freq);
			freq.update(0);
			System.out.println(freq);
			freq.update(0);
			System.out.println(freq);
			freq.update(0);
			System.out.println(freq);

			System.out.println(freq.encode(0));
			System.out.println(freq.encode(1));
			System.out.println(freq.encode(2));
			System.out.println(freq.encode(3));

			System.out.println(freq.encode(freq.decode(0)));
			System.out.println(freq.encode(freq.decode(1)));
			System.out.println(freq.encode(freq.decode(2)));
			System.out.println(freq.encode(freq.decode(3)));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
