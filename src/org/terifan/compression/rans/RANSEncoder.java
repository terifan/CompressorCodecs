package org.terifan.compression.rans;

import java.io.IOException;
import java.io.OutputStream;


public class RANSEncoder
{
	private final static int RANS_BYTE_L = 1 << 23;

	private OutputStream mOutput;
	private SymbolStatistics mStats;
	private IntStack mStartHistory;
	private IntStack mFreqHistory;
	private IntStack mScaleBitsHistory;
	private int mState;


	public RANSEncoder(OutputStream aOutput, SymbolStatistics aStats)
	{
		mOutput = aOutput;
		mStats = aStats;

		mScaleBitsHistory = new IntStack();
		mFreqHistory = new IntStack();
		mStartHistory = new IntStack();
		mState = RANS_BYTE_L;
	}


	public void write(int aSymbol)
	{
		write(aSymbol, mStats);
	}


	public void write(int aSymbol, SymbolStatistics aStats)
	{
		SymbolInfo symbInfo = aStats.get(aSymbol);
		mScaleBitsHistory.push(aStats.getScaleBits());
		mStartHistory.push(symbInfo.mStart);
		mFreqHistory.push(symbInfo.mFreq);
		aStats.update(aSymbol);
	}


	public void finish() throws IOException
	{
		for (int i = 0, count = mStartHistory.size(); i < count; i++)
		{
			int freq = mFreqHistory.pop();
			int start = mStartHistory.pop();
			int scaleBits = mScaleBitsHistory.pop();
			int x = ransEncRenorm(freq, scaleBits);
			mState = (Integer.divideUnsigned(x, freq) << scaleBits) + (Integer.remainderUnsigned(x, freq)) + start;
		}

		mOutput.write(mState);
		mOutput.write(mState >>> 8);
		mOutput.write(mState >>> 16);
		mOutput.write(mState >>> 24);
		mOutput.close();
	}


	private int ransEncRenorm(int aFreq, int aScaleBits) throws IOException
	{
		assert aFreq != 0;

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
