package org.terifan.compression.rans;

import java.util.Arrays;


final public class IntStack
{
	private int[] mBuffer;
	private int mPosition;


	public IntStack()
	{
		mBuffer = new int[4096];
	}


	public void push(int aValue)
	{
		if (mBuffer.length == mPosition)
		{
			mBuffer = Arrays.copyOfRange(mBuffer, 0, mBuffer.length * 3 / 2);
		}
		mBuffer[mPosition++] = aValue;
	}


	public int pop()
	{
		if (mPosition <= 0)
		{
			throw new RuntimeException("Underflow");
		}

		return mBuffer[--mPosition];
	}


	public int size()
	{
		return mPosition;
	}
}
