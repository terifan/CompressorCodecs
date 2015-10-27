package org.terifan.compression.io;

import java.io.IOException;
import java.io.OutputStream;


/**
 * BitOutputStream writes bits to the underlying byte stream.
 */
public class BitOutputStream implements AutoCloseable //extends OutputStream
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


//	/**
//	 * Write a variable number of bits to support all positive values in the range.
//	 *
//	 * E.g. writing a value in range of 100 will write 7 bits.
//	 *
//	 * @param aValue
//	 *   the value to write, greater or equal to zero
//	 * @param aRange
//	 *   the largest value that can be written
//	 */
//	public void writeBitsInRange(long aValue, long aRange) throws IOException
//	{
//		assert aValue >= 0 && aValue <= aRange;
//
//		writeBits(aValue, (int)Math.ceil(Math.log(aRange) / Math.log(2)));
//	}
//
//
//	/**
//	 * Encodes a number using a variable number of bits.
//	 *
//	 * E.g. value 319 encoded with step size 3 and increment 1 output this to the stream: 111 (1) 0111 (1) 00010 (0)
//	 *
//	 * @param aValue
//	 *   the value to encode
//	 * @param aStep
//	 *   the initial step size, between 1 and 32 bits
//	 * @param aIncrement
//	 *   for each chunk encoded the step size increase with this number, can be negative
//	 * @param aSigned
//	 *   true if the the input value may be signed
//	 */
//	public void writeVariableInt(int aValue, int aStep, int aIncrement, boolean aSigned) throws IOException
//	{
//		if (aStep <= 0 || aStep >= 32)
//		{
//			throw new IllegalArgumentException();
//		}
//
//		if (aSigned)
//		{
//			aValue = (aValue << 1) ^ (aValue >> 31);
//		}
//
//		for (int len = 0;;)
//		{
//			int chunk = Math.max(1, Math.min(aStep, 32 - len));
//			writeBits(aValue, chunk);
//			len += chunk;
//			aValue >>>= chunk;
//
//			if (aValue == 0)
//			{
//				if (len < 32)
//				{
//					writeBit(0);
//				}
//				break;
//			}
//
//			writeBit(1);
//
//			aStep += aIncrement;
//		}
//	}
//
//
//	/**
//	 * Encodes a number using a variable number of bits.
//	 *
//	 * @param aValue
//	 *   the value to encode
//	 * @param aStep
//	 *   the initial step size, between 1 and 64 bits
//	 * @param aIncrement
//	 *   for each chunk encoded the step size increase with this number, can be negative
//	 * @param aSigned
//	 *   true if the the input value may be signed
//	 */
//	public void writeVariableLong(long aValue, int aStep, int aIncrement, boolean aSigned) throws IOException
//	{
//		if (aStep <= 0 || aStep >= 64)
//		{
//			throw new IllegalArgumentException();
//		}
//
//		if (aSigned)
//		{
//			aValue = (aValue << 1) ^ (aValue >> 63);
//		}
//
//		for (int len = 0;;)
//		{
//			int chunk = Math.max(1, Math.min(aStep, 64 - len));
//			writeBits(aValue, chunk);
//			len += chunk;
//			aValue >>>= chunk;
//
//			if (aValue == 0)
//			{
//				if (len < 64)
//				{
//					writeBit(0);
//				}
//				break;
//			}
//
//			writeBit(1);
//
//			aStep += aIncrement;
//		}
//	}
//
//
//	@Override
//	public void write(int aByte) throws IOException
//	{
//		writeBits(0xff & aByte, 8);
//	}
//
//
//	@Override
//	public void write(byte[] aBuffer) throws IOException
//	{
//		write(aBuffer, 0, aBuffer.length);
//	}
//
//
//	@Override
//	public void write(byte[] aBuffer, int aOffset, int aLength) throws IOException
//	{
//		if (mBitsToGo == 8)
//		{
//			mOutputStream.write(aBuffer, aOffset, aLength);
//		}
//		else
//		{
//			while (aLength-- > 0)
//			{
//				writeBits(aBuffer[aOffset++] & 0xFF, 8);
//			}
//		}
//	}


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
		return 8-mBitsToGo;
	}
}