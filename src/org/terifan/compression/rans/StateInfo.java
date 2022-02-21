package org.terifan.compression.rans;


public class StateInfo
{
	public int mScale;
	public int mStart;
	public int mFreq;


	public StateInfo(int aScale, int aStart, int aFreq)
	{
		mScale = aScale;
		mStart = aStart;
		mFreq = aFreq;
	}
}
