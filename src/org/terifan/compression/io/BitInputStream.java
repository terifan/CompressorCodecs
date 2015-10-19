package org.terifan.compression.io;

import java.io.IOException;
import java.io.InputStream;


/**
 * BitInputStream allow reading bits from the underlying stream.
 */
public class BitInputStream extends InputStream
{
	private InputStream mInputStream;
	private int mBitBuffer;
	private int mBitCount;
	private boolean mEOF;
	private boolean mReturnZeroOnEOF;


	public BitInputStream(InputStream aInputStream) throws IOException
	{
		mInputStream = aInputStream;
	}


	public int readBit() throws IOException
	{
		if (mBitCount == 0)
		{
			mBitBuffer = mInputStream.read();

			if (mBitBuffer == -1)
			{
				mEOF = true;
				if (mReturnZeroOnEOF) 
				{
					return 0;
				}
				throw new IOException("Premature end of stream");
			}

			mBitCount = 8;
		}

		mBitCount--;
		int output = 1 & (mBitBuffer >> mBitCount);
		mBitBuffer &= (1L << mBitCount) - 1;

		return output;
	}


	public long readBits(int aCount) throws IOException
	{
		long output = 0;

		while (aCount > mBitCount)
		{
			aCount -= mBitCount;
			output |= (long)mBitBuffer << aCount;
			mBitBuffer = mInputStream.read();
			mBitCount = 8;

			if (mBitBuffer == -1)
			{
				mBitCount = 0;
				mEOF = true;
				throw new IOException("Premature end of stream");
			}
		}

		if (aCount > 0)
		{
			mBitCount -= aCount;
			output |= mBitBuffer >> mBitCount;
			mBitBuffer &= (1L << mBitCount) - 1;
		}

		return output;
	}


	/**
	 * Read a variable number of bits to support all positive values in the range.
	 *
	 * E.g. reading a value in range of 100 will read 7 bits.
	 *
	 * @param aRange
	 *   the largest value that can be read
	 */
	public long readBitsInRange(long aRange) throws IOException
	{
		return readBits((int)Math.ceil(Math.log(aRange) / Math.log(2)));
	}


	public int readVariableInt(int aStep, int aIncrement, boolean aSigned) throws IOException
	{
		if (aStep <= 0 || aStep >= 32)
		{
			throw new IllegalArgumentException();
		}

		int result = 0;

		for (int len = 0;;)
		{
			int chunk = Math.max(1, Math.min(aStep, 32 - len));
			result |= readBits(chunk) << len;
			len += chunk;

			if (len >= 32 || readBit() == 0)
			{
				break;
			}

			aStep = Math.max(aStep + aIncrement, 1);
		}

		if (aSigned)
		{
			result = ((result) >>> 1) ^ -(result & 1);
		}

		return result;
	}


	public long readVariableLong(int aStep, int aIncrement, boolean aSigned) throws IOException
	{
		if (aStep <= 0 || aStep >= 64)
		{
			throw new IllegalArgumentException();
		}

		long result = 0;

		for (int len = 0;;)
		{
			int chunk = Math.max(1, Math.min(aStep, 64 - len));
			result |= readBits(chunk) << len;
			len += chunk;

			if (len >= 64 || readBit() == 0)
			{
				break;
			}

			aStep = Math.max(aStep + aIncrement, 1);
		}

		if (aSigned)
		{
			result = (result >>> 1) ^ -(result & 1);
		}

		return result;
	}


	@Override
	public int read() throws IOException
	{
		return (int)readBits(8);
	}


	@Override
	public int read(byte[] aBuffer) throws IOException
	{
		return read(aBuffer, 0, aBuffer.length);
	}


	@Override
	public int read(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		if (mBitCount == 0)
		{
			return mInputStream.read(aBuffer, aOffset, aLength);
		}

		int len = 0;

		try
		{
			while (aLength-- > 0)
			{
				aBuffer[aOffset++] = (byte)readBits(8);
				len++;
			}
		}
		catch (IOException e)
		{
			if (!mEOF)
			{
				throw e;
			}
		}

		return len;
	}


	public void skipBits(int n) throws IOException
	{
		for (int i = 0; i < n; i++)
		{
			readBit();
		}
	}


	public void align() throws IOException
	{
		while (mBitCount > 0)
		{
			readBit();
		}
	}


	public int getBitCount()
	{
		return mBitCount;
	}


	public void setReturnZeroOnEOF(boolean aState)
	{
		mReturnZeroOnEOF = aState;
	}


	public boolean isReturnZeroOnEOF()
	{
		return mReturnZeroOnEOF;
	}
}