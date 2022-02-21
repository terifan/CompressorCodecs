package org.terifan.compression.rans;

import java.util.Arrays;


class Stack
{
	private StateInfo[] mBuffer;
	private int mPosition;


	Stack()
	{
		mBuffer = new StateInfo[4096];
	}


	public void push(StateInfo aValue)
	{
		if (mBuffer.length == mPosition)
		{
			mBuffer = Arrays.copyOfRange(mBuffer, 0, mBuffer.length * 3 / 2);
		}
		mBuffer[mPosition++] = aValue;
	}


	public StateInfo pop()
	{
		if (mPosition <= 0)
		{
			throw new IllegalStateException("Underflow");
		}

		return mBuffer[--mPosition];
	}


	public int size()
	{
		return mPosition;
	}
}
