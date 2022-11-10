package org.terifan.compression.io;

import java.io.IOException;
import java.io.InputStream;
import org.terifan.compression.cabac265.CabacContext265;


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


	@Override
	public int read() throws IOException
	{
		return (int)readBits(8);
	}


	@Override
	public void close() throws IOException
	{
		if (mInputStream != null)
		{
			mInputStream.close();
			mInputStream = null;
		}
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


	public int readGolomb(int aStep) throws IOException
	{
		int base = 0;
		int n = aStep;

		while (readBit() != 0)
		{
			base += 1 << n;
			n++;
		}

		int suffix = (int)readBits(n);

		return base + suffix;
	}
}