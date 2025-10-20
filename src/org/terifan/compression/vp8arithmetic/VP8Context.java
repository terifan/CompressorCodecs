package org.terifan.compression.vp8arithmetic;


public class VP8Context
{
	private final int[] mTotal = new int[64];
	private final int[] mCount = new int[64];


	public VP8Context()
	{
	}


	public int advance(int aSymbol, int aIndex)
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
		for (int i = 0; i < 64; i++)
		{
			if (mTotal[i] == 0) break;
			s+=mCount[i]+",";
		}
		return s;
	}
}
