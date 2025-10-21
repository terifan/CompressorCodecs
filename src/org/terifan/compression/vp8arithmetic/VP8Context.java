package org.terifan.compression.vp8arithmetic;


public class VP8Context
{
	private final int[] mTotal = new int[64];
	private final int[] mCount = new int[64];


	public VP8Context()
	{
	}


	public int prob(int aIndex)
	{
		return mTotal[aIndex] == 0 ? 128 : mCount[aIndex] * 255 / mTotal[aIndex];
	}


	public void advance(int aSymbol, int aIndex)
	{
		if (aSymbol == 0)
		{
			mCount[aIndex]++;
		}
		mTotal[aIndex]++;
	}


	public int next(int aSymbol, int aIndex)
	{
		int v = mTotal[aIndex] == 0 ? 128 : mCount[aIndex] * 255 / mTotal[aIndex];
		if (aSymbol == 0)
		{
			mCount[aIndex]++;
		}
		mTotal[aIndex]++;
		return v;
	}


	@Override
	public String toString()
	{
		String s = "";
		for (int i = 0; i < mTotal.length; i++)
		{
			if (mTotal[i] == 0)
			{
				break;
			}
			if (!s.isEmpty())
			{
				s += "|";
			}
			s += "%.2f".formatted(mCount[i]/(double)mTotal[i]).replace(',', '.');
//			s += mCount[i];
		}
		return s.trim();
	}
}
