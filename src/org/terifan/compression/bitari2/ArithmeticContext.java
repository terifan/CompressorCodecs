package org.terifan.compression.bitari2;


public final class ArithmeticContext
{
	int mSymbolFreq0;
	int mSymbolFreq1;

	
	public ArithmeticContext()
	{
		mSymbolFreq0 = 1;
		mSymbolFreq1 = 1;
	}
	
	
	void scale()
	{
		mSymbolFreq0 = (mSymbolFreq0 + 1) >> 1;
		mSymbolFreq1 = (mSymbolFreq1 + 1) >> 1;
	}
}