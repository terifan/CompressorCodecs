package org.terifan.compression.rans;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;


public class RANSDecoder
{
	private final static int RANS_BYTE_L = 1 << 23;

	private InputStream mInput;
	private SymbolStatistics mStats;
	private int mState;


	public RANSDecoder(InputStream aInput, SymbolStatistics aStats) throws IOException
	{
		mInput = aInput;
		mStats = aStats;
		mState = (readByte() << 24) | (readByte() << 16) | (readByte() << 8) | readByte();
	}


	public int read() throws IOException
	{
		return read(mStats);
	}


	public int read(SymbolStatistics aStats) throws IOException
	{
		int scaleBits = aStats.getScaleBits();
		SymbolInfo symbInfo = aStats.findSymbol(mState & ((1 << scaleBits) - 1));
		advance(scaleBits, symbInfo.mFreq, symbInfo.mStart);
		aStats.update(symbInfo.mSymbol);
		return symbInfo.mSymbol;
	}


	private void advance(int aScaleBits, int aFreq, int aStart) throws IOException
	{
		int x = aFreq * (mState >>> aScaleBits) + (mState & (1 << aScaleBits) - 1) - aStart;

		while (x < RANS_BYTE_L)
		{
			x = (x << 8) | readByte();
		}

		mState = x;
	}


	private int readByte() throws IOException
	{
		int b = mInput.read();
		if (b == -1)
		{
			throw new EOFException();
		}
		return b;
	}
}
