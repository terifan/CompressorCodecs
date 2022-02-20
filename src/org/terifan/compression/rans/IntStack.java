package org.terifan.compression.rans;


final public class IntStack
{
	private static final int MAX_SIZE = 8 * 1024 * 1024;

	final private int[] mBuffer = new int[MAX_SIZE];
	private int mSize = 0;


	public void push(int value)
	{
		if (mSize >= MAX_SIZE)
		{
			throw new RuntimeException("Exceeded capacity");
		}
		else
		{
			mBuffer[mSize++] = value;
		}
	}


	public int pop()
	{
		if (mSize <= 0)
		{
			throw new RuntimeException("Underflow");
		}
		else
		{
			return mBuffer[--mSize];
		}

	}


	public int getCount()
	{
		return mSize;
	}


	/**
	 * O(N) implementation. Useful for testing
	 */
	public int dequeue()
	{
		if (mSize <= 0)
		{
			throw new RuntimeException("Underflow");
		}
		else
		{
			int result = mBuffer[0];
			for (int i = 0; i < mSize - 1; i++)
			{
				mBuffer[i] = mBuffer[i + 1];
			}
			return result;
		}
	}
}
