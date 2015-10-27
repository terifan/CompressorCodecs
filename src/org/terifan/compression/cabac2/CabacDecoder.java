package org.terifan.compression.cabac2;

import java.io.IOException;
import java.io.PushbackInputStream;
import static org.terifan.compression.cabac2.Shared.*;


public class CabacDecoder
{
	private final static int HALF = 0x01FE;    //(1 << (B_BITS-1)) - 2
	private final static int QUARTER = 0x0100; //(1 << (B_BITS-2))

	private PushbackInputStream mInputStream;
	private int mValue;
	private int mBitsLeft;
	private int mRange;
	private int mOutOfBoundsCount;


	public CabacDecoder(PushbackInputStream aInputStream) throws IOException
	{
		mInputStream = aInputStream;

		mValue = getByte();
		mValue = (mValue << 16) | getWord();
		mBitsLeft = 15;
		mRange = HALF;
	}


	private int getByte() throws IOException
	{
		int i = mInputStream.read();
		if (i == -1)
		{
			if (++mOutOfBoundsCount > 2)
			{
				throw new IOException("Reading beyond the end of the stream.");
			}
			return 0;
		}
		return i;
	}


	private int getWord() throws IOException
	{
		return (getByte() << 8) | getByte();
	}


	public int decodeBit(CabacContext aContext) throws IOException
	{
		int bit = aContext.MPS;
		int rLPS = rLPS_table_64x4[aContext.state][(mRange >> 6) & 0x03];

		mRange -= rLPS;

		if (mValue < (mRange << mBitsLeft))
		{
			aContext.state = AC_next_state_MPS_64[aContext.state];
			if (mRange >= QUARTER)
			{
				return bit;
			}
			else
			{
				mRange <<= 1;
				mBitsLeft--;
			}
		}
		else
		{
			int renorm = renorm_table_32[(rLPS >> 3) & 0x1F];
			mValue -= mRange << mBitsLeft;
			mRange = rLPS << renorm;
			mBitsLeft -= renorm;

			bit ^= 0x01;
			if (aContext.state == 0)
			{
				aContext.MPS ^= 0x01;
			}

			aContext.state = AC_next_state_LPS_64[aContext.state];
		}

		if (mBitsLeft > 0)
		{
			return bit;
		}

		mValue <<= 16;
		mValue |= getWord();
		mBitsLeft += 16;
		return bit;
	}


	public int decodeBitEqProb() throws IOException
	{
		if (--mBitsLeft == 0)
		{
			mValue = (mValue << 16) | getWord();
			mBitsLeft = 16;
		}

		int tmp_value = mValue - (mRange << mBitsLeft);

		if (tmp_value < 0)
		{
			return 0;
		}
		else
		{
			mValue = tmp_value;
			return 1;
		}
	}


	public long decodeExpGolombEqProb(int aStep) throws IOException
	{
		int x = decodeBitEqProb();

		long result = 0;

		while (decodeBitEqProb() == 0)
		{
			result += 1L << aStep;
			aStep++;
		}

		long binarySymbol = 0;
		while (aStep-- > 0)
		{
			if (decodeBitEqProb() == 1)
			{
				binarySymbol |= 1L << aStep;
			}
		}

		return ((result + binarySymbol) << 1) + x;
	}


	public long decodeExpGolomb(int aStep, CabacContext aContext) throws IOException
	{
		int x = decodeBitEqProb();

		long result = 0;

		while (decodeBit(aContext) == 0)
		{
			result += 1L << aStep;
			aStep++;
		}

		long binarySymbol = 0;
		while (aStep-- > 0)
		{
			if (decodeBitEqProb() == 1)
			{
				binarySymbol |= 1L << aStep;
			}
		}

		return ((result + binarySymbol) << 1) + x;
	}


	public int decodeUnary(CabacContext ctx) throws IOException
	{
		int symbol = 0;
		while (decodeBit(ctx) == 0)
		{
			symbol++;
		}

		return symbol;
	}


	public int decodeUnary(CabacContext ctx0, CabacContext ctx1) throws IOException
	{
		if (decodeBit(ctx0) == 0)
		{
			return 0;
		}
		else
		{
			int symbol = 1;

			while (decodeBit(ctx1) == 0)
			{
				symbol++;
			}

			return symbol;
		}
	}


	public long decodeUnaryGolomb(int aStep, CabacContext ctx) throws IOException
	{
		assert aStep > 0 && aStep <= 64;

		long symbol = 0;
		int len = 0;

		while (decodeBit(ctx) == 0)
		{
			for (int i = 0; i < aStep; i++, len++)
			{
				if (decodeBitEqProb() == 1)
				{
					symbol |= 1L << len;
				}
			}
		}

		return symbol;
	}


	public void decodeBytesEqProb(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		for (int i = 0; i < aLength; i++)
		{
			int b = 0;
			for (int j = 8; --j >= 0;)
			{
				b += decodeBitEqProb() << j;
			}
			aBuffer[aOffset + i] = (byte)b;
		}
	}


	public long decodeEqProb(int aLength) throws IOException
	{
		long value = 0;
		for (int i = 0; i < aLength; i++)
		{
			value <<= 1;
			value += decodeBitEqProb();
		}
		return value;
	}


	public int decodeFinal() throws IOException
	{
		int range = mRange - 2;
		int value = mValue;
		value -= (range << mBitsLeft);

		try
		{
			if (value < 0)
			{
				if (range >= QUARTER)
				{
					mRange = range;
					return 0;
				}
				else
				{
					mRange = (range << 1);
					if (--mBitsLeft > 0)
					{
						return 0;
					}
					else
					{
						mValue = (mValue << 16) | getWord();
						mBitsLeft = 16;
						return 0;
					}
				}
			}
			else
			{
				return 1;
			}
		}
		finally
		{
			if (mBitsLeft >= 16)
			{
				mInputStream.unread(mValue & 0xFF);
				mInputStream.unread((mValue >> 8) & 0xFF);
			}
			else if (mBitsLeft >= 8)
			{
				mInputStream.unread(mValue & 0xFF);
			}
		}
	}
}
