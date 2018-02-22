package org.terifan.compression.lzw;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;


public class LZWInputStream extends InputStream
{
	// stream
	private int mBitBufferLength;
	private int mBitBuffer;
	private InputStream mInputStream;
	// decoder
	private int mCodeSize;
	private int mDictionarySize;
	private int mClearCode;
	private int mEOFCode;
	private int mInitialCodeSize;
	private int mSwapCode;
	// tree
	private int mNextCode;
	private int mOldCode;
	private byte[] mInitial;
	private int[] mLength;
	private int[] mPrefix;
	private byte[] mSuffix;
	// output
	private byte[] mBuffer;
	private int mBufferOffset;
	private int mBufferLength;


	public LZWInputStream(InputStream aInputStream, int aInitialCodeSizeBits, int aDictionarySizeBits) throws IOException
	{
		mInputStream = aInputStream;
		mInitialCodeSize = aInitialCodeSizeBits;
		mDictionarySize = 1 << aDictionarySizeBits;
		mClearCode = 1 << aInitialCodeSizeBits;
		mEOFCode = mClearCode + 1;

		mPrefix = new int[mDictionarySize];
		mSuffix = new byte[mDictionarySize];
		mInitial = new byte[mDictionarySize];
		mLength = new int[mDictionarySize];

		mBuffer = new byte[4096];
		mOldCode = -1;

		clear();
	}


	@Override
	public void close() throws IOException
	{
		mInitial = null;
		mLength = null;
		mPrefix = null;
		mSuffix = null;

		if (mInputStream != null)
		{
			mInputStream.close();
			mInputStream = null;
		}
	}


	@Override
	public int read() throws IOException
	{
		if (mBufferOffset == mBufferLength)
		{
			if (mInputStream == null)
			{
				return -1;
			}
			decodeBuffer();
		}
		if (mBufferLength == 0)
		{
			return -1;
		}

		return 255 & mBuffer[mBufferOffset++];
	}


	@Override
	public int read(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		int read = 0;
		while (aLength > 0)
		{
			if (mBufferOffset == mBufferLength)
			{
				if (mInputStream == null)
				{
					return read == 0 ? -1 : read;
				}
				decodeBuffer();
			}
			if (mBufferLength == 0)
			{
				return read == 0 ? -1 : read;
			}

			int len = Math.min(mBufferLength - mBufferOffset, aLength);

			System.arraycopy(mBuffer, mBufferOffset, aBuffer, aOffset, len);

			aLength -= len;
			aOffset += len;
			mBufferOffset += len;
			read += len;
		}

		return read;
	}


	private void decodeBuffer() throws IOException
	{
		mBufferOffset = 0;
		mBufferLength = 0;

		if (mOldCode == -1)
		{
			do
			{
				mOldCode = readBits(mCodeSize);
			} while (mOldCode == mClearCode);
			mBuffer[mBufferLength++] = (byte) mOldCode;
		}

		while (mBufferLength < 1024)
		{
			int code = readBits(mCodeSize);
			int newSuffix;

			if (code == mClearCode)
			{
				clear();
				do
				{
					code = readBits(mCodeSize);
				} while (code == mClearCode);
				if (code == mEOFCode)
				{
					close();
					return;
				}
				mBuffer[mBufferLength++] = (byte) code;
				mOldCode = code;
			}
			else if (code == mEOFCode)
			{
				close();
				return;
			}
			else
			{
				if (code < mNextCode)
				{
					newSuffix = code;
				}
				else
				{
					newSuffix = mOldCode;
					if (code != mNextCode)
					{
						throw new IOException("code out of range: " + code);
					}
				}

				mPrefix[mNextCode] = mOldCode;
				mSuffix[mNextCode] = mInitial[newSuffix];
				mInitial[mNextCode] = mInitial[mOldCode];
				mLength[mNextCode] = mLength[mOldCode] + 1;
				mNextCode++;

				if (mNextCode == mSwapCode && mNextCode < mDictionarySize)
				{
					mCodeSize++;
					mSwapCode <<= 1;
				}

				int len = mLength[code];

				mBufferLength += len;

				if (mBuffer.length < mBufferLength)
				{
					byte[] tmp = new byte[mBufferLength * 3 / 2];
					System.arraycopy(mBuffer, 0, tmp, 0, mBuffer.length);
					mBuffer = tmp;
				}

				int c = code;
				for (int i = 1; i <= len; i++)
				{
					mBuffer[mBufferLength - i] = mSuffix[c];
					c = mPrefix[c];
				}

				mOldCode = code;
			}
		}
	}


	private void clear()
	{
		int num = 1 << mInitialCodeSize;

		for (int i = 0; i < num; i++)
		{
			mPrefix[i] = -1;
			mSuffix[i] = (byte) i;
			mInitial[i] = (byte) i;
			mLength[i] = 1;
		}

		for (int i = num; i < mDictionarySize; i++)
		{
			mPrefix[i] = -1;
			mLength[i] = 1;
		}

		mCodeSize = mInitialCodeSize+1;
		mSwapCode = 1 << mCodeSize;
		mNextCode = (1 << mInitialCodeSize) + 2;
	}


	private int readBits(int aLength) throws IOException
	{
		while (mBitBufferLength < aLength)
		{
			int in = mInputStream.read();
			if (in == -1)
			{
				throw new EOFException();
			}

			mBitBuffer += in << mBitBufferLength;
			mBitBufferLength += 8;
		}

		int code = mBitBuffer & ((1 << aLength) - 1);

		mBitBuffer >>= aLength;
		mBitBufferLength -= aLength;

		return code;
	}
}
