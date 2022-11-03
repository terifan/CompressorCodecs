package org.terifan.compression.rans;

import java.util.ArrayDeque;
import java.util.Arrays;


public class RANSEncoder
{
	private final static int RANS_BYTE_L = 1 << 23;

	private byte[] mOutput;
	private SymbolStats mStats;
	private ArrayDeque<StateInfo> mStack;
	private int mState;
	private int mPosition;


	public RANSEncoder(SymbolStats aStats)
	{
		mStats = aStats;
		mStack = new ArrayDeque<>();
		mState = RANS_BYTE_L;
		mOutput = new byte[4096];
	}


	public void write(int aSymbol)
	{
		write(aSymbol, mStats);
	}


	public void write(int aSymbol, SymbolStats aStats)
	{
		SymbolInfo symbInfo = aStats.get(aSymbol);
		mStack.push(new StateInfo(aStats.getScaleBits(), symbInfo.mStart, symbInfo.mFreq));
		aStats.update(aSymbol);
	}


	public byte[] finish()
	{
		while (!mStack.isEmpty())
		{
			StateInfo p = mStack.pop();
			int x = ransEncRenorm(p.mFreq, p.mScale);
			mState = ((x / p.mFreq) << p.mScale) + ((x % p.mFreq)) + p.mStart;
		}

		writeByte(0xff & mState);
		writeByte(0xff & (mState >>> 8));
		writeByte(0xff & (mState >>> 16));
		writeByte(0xff & (mState >>> 24));

		reverse();

		return Arrays.copyOfRange(mOutput, 0, mPosition);
	}


	private int ransEncRenorm(int aFreq, int aScaleBits)
	{
		int x = mState;
		int max = ((RANS_BYTE_L >>> aScaleBits) << 8) * aFreq;
		while (x >= max)
		{
			writeByte(0xff & x);
			x >>>= 8;
		}
		return x;
	}


	private void writeByte(int aByte)
	{
		if (mOutput.length == mPosition)
		{
			mOutput = Arrays.copyOfRange(mOutput, 0, mPosition * 3 / 2 + 1);
		}
		mOutput[mPosition++] = (byte)aByte;
	}


	private void reverse()
	{
		for (int i = 0, n = mPosition, j = mPosition - 1, end = n / 2; i < end; i++, j--)
		{
			byte tmp = mOutput[i];
			mOutput[i] = mOutput[j];
			mOutput[j] = tmp;
		}
	}
}
