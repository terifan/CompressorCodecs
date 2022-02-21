package org.terifan.compression.rans;

import java.util.Arrays;


public class DynamicStats implements SymbolStatistics
{
	private int mTotalCount;
	private int mNumSymbols;
	private int[] mCounts;


	public DynamicStats()
	{
		this(256);
	}


	public DynamicStats(int aNumSymbols)
	{
		mNumSymbols = aNumSymbols;
		mTotalCount = mNumSymbols;
		mCounts = new int[mNumSymbols];
		Arrays.fill(mCounts, 1);
	}


	@Override
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


	@Override
	public int getScaleBits()
	{
		return Integer.numberOfTrailingZeros(Integer.highestOneBit(mTotalCount)) + 1;
	}


	@Override
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


	@Override
	public void update(int aSymbol)
	{
		assert aSymbol < mNumSymbols;

		mCounts[aSymbol]++;
		mTotalCount++;
	}


	public void set(int aSymbol, int aCount)
	{
		assert aSymbol < mNumSymbols;

		mTotalCount -= mCounts[aSymbol];
		mCounts[aSymbol] = aCount;
		mTotalCount += aCount;
	}
}
