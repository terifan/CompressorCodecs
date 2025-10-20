package org.terifan.compression.lzjb;

import java.io.OutputStream;
import java.util.Arrays;


class ByteBlockOutputStream extends OutputStream
{
	int mOffset;
	byte[] mBuffer;


	public ByteBlockOutputStream()
	{
		mBuffer = new byte[512];
	}


	@Override
	public void write(int aByte)
	{
		if (mOffset == mBuffer.length)
		{
			mBuffer = Arrays.copyOfRange(mBuffer, 0, mOffset + 512);
		}

		mBuffer[mOffset++] = (byte)aByte;
	}


	@Override
	public void write(byte[] aBuffer, int aOffset, int aLength)
	{
		while (aLength > 0)
		{
			if (mOffset == mBuffer.length)
			{
				mBuffer = Arrays.copyOfRange(mBuffer, 0, mOffset + 512);
			}

			int s = Math.min(aLength, mBuffer.length - mOffset);

			System.arraycopy(aBuffer, aOffset, mBuffer, mOffset, s);

			mOffset += s;
			aOffset += s;
			aLength -= s;
		}
	}
}

