package org.terifan.compression.rans;


public class VerySimpleStats implements SymbolStatistics
{
	@Override
	public SymbolInfo findSymbol(int aCumFreq)
	{
		return new SymbolInfo(1, aCumFreq, aCumFreq);
	}


	@Override
	public int getScaleBits()
	{
		return 8;
	}


	@Override
	public SymbolInfo get(int aSymbol)
	{
		return new SymbolInfo(1, aSymbol, aSymbol);
	}


	@Override
	public void update(int aSymbol)
	{
	}


	@Override
	public void finish()
	{
	}
}
