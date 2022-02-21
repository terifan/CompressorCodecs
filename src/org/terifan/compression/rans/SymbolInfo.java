package org.terifan.compression.rans;


public class SymbolInfo
{
	public int mFreq;
	public int mStart;
	public int mSymbol;


	public SymbolInfo(int aFreq, int aStart, int aSymbol)
	{
		mFreq = aFreq;
		mStart = aStart;
		mSymbol = aSymbol;
	}
}
