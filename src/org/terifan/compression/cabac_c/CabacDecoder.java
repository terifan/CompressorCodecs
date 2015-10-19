package org.terifan.compression.cabac_c;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;


public class CabacDecoder implements CabacModel
{
	private final static int HALF = 0x01FE;    //(1 << (B_BITS-1)) - 2
	private final static int QUARTER = 0x0100; //(1 << (B_BITS-2))
	
	private PushbackInputStream mInputStream;
	private int mValue;
	private int mBitsLeft;
	private int mRange;


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
		return i == -1 ? 0 : i;
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


	public int decodeExpGolombEqProb(int K) throws IOException
	{
		int L, binarySymbol;
		int result = 0;
		binarySymbol = 0;
		do
		{
			L = decodeBitEqProb();
			if (L == 1)
			{
				result += 1 << K;
				K++;
			}
		}
		while (L != 0);
		
		while (K != 0)
		{
			K--;
			if (decodeBitEqProb() == 1)
			{
				binarySymbol |= 1 << K;
			}
		}

		return result + binarySymbol;
	}
	
	
	public long decodeExpGolomb(int K, CabacContext aContext) throws IOException
	{
		int x = decodeBitEqProb();

		int L;
		long binarySymbol = 0;
		long result = 0;

		do
		{
			L = decodeBit(aContext);
			if (L == 1)
			{
				result += 1L << K;
				K++;
			}
		}
		while (L != 0);

		while (K != 0)
		{
			K--;
			if (decodeBitEqProb() == 1)
			{
				binarySymbol |= 1L << K;
			}
		}

		return ((result + binarySymbol) << 1) + x;
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

			while (decodeBit(ctx1) == 1)
			{
				symbol++;
			}

			return symbol;
		}
	}

	
	public int decodeFinal() throws IOException
	{
		int range = mRange - 2;
		int value = mValue;
		value -= (range << mBitsLeft);

		try{
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
		}finally
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
