package org.terifan.compression.rans;

import static org.terifan.compression.rans.DynamicStats.requiredBits;


public class SimpleStats implements SymbolStatistics
{
	private int mMax;
	private int mMaxBits;


	public SimpleStats()
	{
		this(255);
	}


	public SimpleStats(int aMax)
	{
		mMax = aMax;
		mMaxBits = requiredBits(aMax * (aMax + 1) / 2) + 2;
	}


	@Override
	public SymbolInfo findSymbol(int aCumFreq)
	{
		for (int s = 0; s <= mMax + 1; s++)
		{
			if ((s * (s + 1) / 2) > aCumFreq)
			{
				int symb = s - 1;
				return new SymbolInfo(symb + 1, symb * (symb + 1) / 2, symb);
			}
		}
		throw new RuntimeException("Invalid Frequency");
	}


	@Override
	public int getScaleBits()
	{
		return mMaxBits;
	}


	@Override
	public SymbolInfo get(int aSymbol)
	{
		return new SymbolInfo(aSymbol + 1, aSymbol * (aSymbol + 1) / 2, aSymbol);
	}


	@Override
	public void update(int aSymbol)
	{
	}
}