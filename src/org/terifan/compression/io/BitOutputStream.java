package org.terifan.compression.io;

import java.io.IOException;
import java.io.OutputStream;


/**
 * BitOutputStream writes bits to the underlying byte stream.
 */
public class BitOutputStream extends OutputStream
{
	private OutputStream mOutputStream;
	private int mBitsToGo;
	private int mBitBuffer;


	public BitOutputStream(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;
		mBitBuffer = 0;
		mBitsToGo = 8;
	}


	public void writeBit(int aBit) throws IOException
	{
		mBitBuffer |= aBit << --mBitsToGo;

		if (mBitsToGo == 0)
		{
			mOutputStream.write(mBitBuffer & 0xFF);
			mBitBuffer = 0;
			mBitsToGo = 8;
		}
	}


	public void writeBits(int aValue, int aLength) throws IOException
	{
		while (aLength-- > 0)
		{
			writeBit((aValue >>> aLength) & 1);
		}
	}


	public void writeBits(long aValue, int aLength) throws IOException
	{
		if (aLength > 32)
		{
			writeBits((int)(aValue >>> 32), aLength - 32);
		}

		writeBits((int)(aValue), Math.min(aLength, 32));
	}


	@Override
	public void write(int aByte) throws IOException
	{
		writeBits(0xff & aByte, 8);
	}


	@Override
	public void write(byte[] aBuffer) throws IOException
	{
		write(aBuffer, 0, aBuffer.length);
	}


	@Override
	public void write(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		if (mBitsToGo == 8)
		{
			mOutputStream.write(aBuffer, aOffset, aLength);
		}
		else
		{
			while (aLength-- > 0)
			{
				writeBits(aBuffer[aOffset++] & 0xFF, 8);
			}
		}
	}


	public void finish() throws IOException
	{
		align();
	}


	@Override
	public void close() throws IOException
	{
		if (mOutputStream != null)
		{
			finish();

			mOutputStream.close();
			mOutputStream = null;
		}
	}


	public void align() throws IOException
	{
		if (mBitsToGo < 8)
		{
			writeBits(0, mBitsToGo);
		}
	}


	public int getBitCount()
	{
		return 8 - mBitsToGo;
	}


	public void writeGolomb(int aValue, int aMinLen) throws IOException
	{
		assert aValue >= 0;

		while (aValue >= (1 << aMinLen))
		{
			System.out.print(0);
			writeBit(0);
			aValue -= 1 << aMinLen;
			aMinLen++;
		}

		System.out.print(1);
		writeBit(1);

		while (aMinLen > 0)
		{
			System.out.print('x');
			aMinLen--;
			writeBit((aValue >>> aMinLen) & 1);
		}
		System.out.println();
	}
}
