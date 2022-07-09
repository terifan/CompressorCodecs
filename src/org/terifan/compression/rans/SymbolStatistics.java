package org.terifan.compression.rans;


public interface SymbolStatistics
{
	SymbolInfo findSymbol(int aCumFreq);


	int getScaleBits();


	SymbolInfo get(int aSymbol);


	void update(int aSymbol);
}
