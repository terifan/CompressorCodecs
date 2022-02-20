package org.terifan.compression.rans;

import java.io.IOException;
import java.io.OutputStream;


public class RANSEncoder
{
	private final static int RANS_BYTE_L = 1 << 23;

	private OutputStream mOutput;
	private SymbolStatistics mStats;
	private IntStack mStartHistory = new IntStack();
	private IntStack mFreqHistory = new IntStack();
	private IntStack mScaleBitsHistory = new IntStack();
	private int mState = RANS_BYTE_L;


	public RANSEncoder(OutputStream aOutput, SymbolStatistics aStats)
	{
		mOutput = aOutput;
		mStats = aStats;
	}


	public void write(int aChar)
	{
		write(aChar, mStats);
	}


	public void write(int aSymbol, SymbolStatistics aStats)
	{
		int scaleBits = aStats.getScaleBits();
		SymbolInfo symbInfo = aStats.get(aSymbol);
		aStats.update(aSymbol);
		mScaleBitsHistory.push(scaleBits);
		mStartHistory.push(symbInfo.mStart);
		mFreqHistory.push(symbInfo.mFreq);
	}


	public void finish() throws IOException
	{
		int count = mStartHistory.getCount();
		for (int i = 0; i < count; i++)
		{
			// x = C(s,x)
			int freq = mFreqHistory.pop();
			int start = mStartHistory.pop();
			int scaleBits = mScaleBitsHistory.pop();
			int x = ransEncRenorm(freq, scaleBits);
			mState = (Integer.divideUnsigned(x, freq) << scaleBits) + (Integer.remainderUnsigned(x, freq)) + start;
		}

		flushState();
		mStats.finish();
	}


	private void flushState() throws IOException
	{
		mOutput.write(mState);
		mOutput.write(mState >>> 8);
		mOutput.write(mState >>> 16);
		mOutput.write(mState >>> 24);
		mOutput.close();
	}


	private int ransEncRenorm(int aFreq, int aScaleBits) throws IOException
	{
		assert (aFreq != 0);

		int x = mState;
		int x_max = ((RANS_BYTE_L >>> aScaleBits) << 8) * aFreq;

		while (Integer.compareUnsigned(x, x_max) >= 0)
		{
			mOutput.write(x); // OutputStream.write() only writes the Least Significant 8 bits
			x >>>= 8;
		}
		return x;
	}
}
