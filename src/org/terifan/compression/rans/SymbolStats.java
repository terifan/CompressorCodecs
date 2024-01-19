package org.terifan.compression.rans;

import java.util.Arrays;


public class SymbolStats
{
	private int mCumulative;
	private int mNumSymbols;
	private int[] mCounts;


	public SymbolStats()
	{
		this(256);
	}


	public SymbolStats(int aNumSymbols)
	{
		mNumSymbols = aNumSymbols;
		mCumulative = mNumSymbols;
		mCounts = new int[mNumSymbols];
		Arrays.fill(mCounts, 1);
	}


	public SymbolInfo findSymbol(int aCumFreq)
	{
		int sum = 0;
		int prevSum = 0;
		for (int symb = 0; symb < mNumSymbols; symb++)
		{
			prevSum = sum;
			sum += mCounts[symb];
			if (sum > aCumFreq)
			{
				return new SymbolInfo(mCounts[symb], prevSum, symb);
			}
		}
		return new SymbolInfo(mCounts[mNumSymbols - 1], prevSum, mNumSymbols - 1);
	}


	public int getScaleBits()
	{
		return Integer.numberOfTrailingZeros(Integer.highestOneBit(mCumulative)) + 1;
	}


	public SymbolInfo get(int aSymbol)
	{
		assert aSymbol < mNumSymbols;

		int sum = 0;
		for (int i = 0; i < aSymbol; i++)
		{
			sum += mCounts[i];
		}

		return new SymbolInfo(mCounts[aSymbol], sum, aSymbol);
	}


	public void update(int aSymbol)
	{
		assert aSymbol < mNumSymbols;

		mCounts[aSymbol]++;
		mCumulative++;
	}


	public void set(int aSymbol, int aCount)
	{
		assert aSymbol < mNumSymbols;

		mCumulative -= mCounts[aSymbol];
		mCounts[aSymbol] = aCount;
		mCumulative += aCount;
	}


	@Override
	public String toString()
	{
		return Arrays.toString(mCounts);
	}
}
