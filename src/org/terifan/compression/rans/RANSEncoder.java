package org.terifan.compression.rans;

import java.io.IOException;
import java.io.OutputStream;


public class RANSEncoder
{
	private final static int RANS_BYTE_L = 1 << 23;

	private OutputStream mOutput;
	private SymbolStatistics mStats;
	private Stack mStack;
	private int mState;


	public RANSEncoder(OutputStream aOutput, SymbolStatistics aStats)
	{
		mOutput = aOutput;
		mStats = aStats;

		mStack = new Stack();
		mState = RANS_BYTE_L;
	}


	public void write(int aSymbol)
	{
		write(aSymbol, mStats);
	}


	public void write(int aSymbol, SymbolStatistics aStats)
	{
		SymbolInfo symbInfo = aStats.get(aSymbol);
		mStack.push(new StateInfo(aStats.getScaleBits(), symbInfo.mStart, symbInfo.mFreq));
		aStats.update(aSymbol);
	}


	public void finish() throws IOException
	{
		for (int i = 0, count = mStack.size(); i < count; i++)
		{
			StateInfo p = mStack.pop();
			int x = ransEncRenorm(p.mFreq, p.mScale);
			mState = ((x / p.mFreq) << p.mScale) + ((x % p.mFreq)) + p.mStart;
		}

		mOutput.write(0xff & mState);
		mOutput.write(0xff & (mState >>> 8));
		mOutput.write(0xff & (mState >>> 16));
		mOutput.write(0xff & (mState >>> 24));
		mOutput.close();
	}


	private int ransEncRenorm(int aFreq, int aScaleBits) throws IOException
	{
		int x = mState;
		int max = ((RANS_BYTE_L >>> aScaleBits) << 8) * aFreq;

		while (Integer.compareUnsigned(x, max) >= 0)
		{
			mOutput.write(0xff & x);
			x >>>= 8;
		}
		return x;
	}
}
