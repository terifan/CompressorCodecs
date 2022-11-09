package org.terifan.compression.cabac264;

import java.io.IOException;
import java.io.OutputStream;
import static org.terifan.compression.cabac264.CabacConstants.*;


public class CabacEncoder
{
	private final static int BITS_TO_LOAD = 16;
	private final static int MAX_BITS = 26;             //(B_BITS + BITS_TO_LOAD)
	private final static int ONE = 0x04000000;          //(1 << MAX_BITS)
	private final static int HALF = 0x01FE;             //(1 << (B_BITS-1)) - 2
	private final static int QUARTER = 0x0100;          //(1 << (B_BITS-2))
	private final static int MIN_BITS_TO_GO = 0;
	private final static int B_LOAD_MASK = 0xFFFF;      // ((1<<BITS_TO_LOAD) - 1)

	private OutputStream mOutputStream;

	private int mBitsToGo;
	private int mRange;
	private int mLow;
	private int mBitBuffer;
	private int mPbuf;
	private int mChunksOutstanding;


	public CabacEncoder(OutputStream aOutputStream)
	{
		mLow = 0;
		mChunksOutstanding = 0;
		mBitBuffer = 0;
		mPbuf = -1;
		mBitsToGo = BITS_TO_LOAD + 1;
		mOutputStream = aOutputStream;

		mRange = HALF;
	}


	public void encodeBit(int aBit, CabacContext aContext) throws IOException
	{
		int low = mLow;
		int bl = mBitsToGo;
		int rLPS = rLPS_table_64x4[aContext.state][(mRange >> 6) & 3];

		mRange -= rLPS;

		if (aBit == aContext.MPS)
		{
			aContext.state = AC_next_state_MPS_64[aContext.state];

			if (mRange >= QUARTER)
			{
				return;
			}
			else
			{
				mRange <<= 1;
				if (--bl > MIN_BITS_TO_GO)
				{
					mBitsToGo = bl;
					return;
				}
			}
		}
		else
		{
			int renorm = renorm_table_32[(rLPS >> 3) & 0x1F];

			low += mRange << bl;
			mRange = (rLPS << renorm);
			bl -= renorm;

			if (aContext.state == 0)
			{
				aContext.MPS ^= 0x01;
			}

			aContext.state = AC_next_state_LPS_64[aContext.state];

			if (low >= ONE)
			{
				low -= ONE;
				propagateCarry();
			}

			if (bl > MIN_BITS_TO_GO)
			{
				mLow = low;
				mBitsToGo = bl;
				return;
			}
		}

		mLow = (low << BITS_TO_LOAD) & (ONE - 1);
		low = (low >> (MAX_BITS - BITS_TO_LOAD)) & B_LOAD_MASK; // mask out the 8/16 MSBs for output

		if (low < B_LOAD_MASK) // no carry possible, output now
		{
			putLastChunkPlusOutstanding(low);
		}
		else          // low == "FF.."; keep it, may affect future carry
		{
			mChunksOutstanding++;
		}
		mBitsToGo = bl + BITS_TO_LOAD;
	}


	public void encodeBitEqProb(int aBit) throws IOException
	{
		assert aBit == 0 || aBit == 1;

		int low = mLow;
		mBitsToGo--;

		if (aBit != 0)
		{
			low += mRange << mBitsToGo;
			if (low >= ONE) // output of carry needed
			{
				low -= ONE;
				propagateCarry();
			}
		}
		if (mBitsToGo == MIN_BITS_TO_GO)  // renorm needed
		{
			mLow = (low << BITS_TO_LOAD) & (ONE - 1);
			low = (low >> (MAX_BITS - BITS_TO_LOAD)) & B_LOAD_MASK; // mask out the 8/16 MSBs for output
			if (low < B_LOAD_MASK)      // no carry possible, output now
			{
				putLastChunkPlusOutstanding(low);
			}
			else          // low == "FF"; keep it, may affect future carry
			{
				mChunksOutstanding++;
			}

			mBitsToGo = BITS_TO_LOAD;
		}
		else         // no renorm needed
		{
			mLow = low;
		}
	}


	public void encodeUnary(int aSymbol, CabacContext aContext) throws IOException
	{
		assert aSymbol >= 0;

		int l = aSymbol;
		while (l-- > 0)
		{
			encodeBit(0, aContext);
		}
		encodeBit(1, aContext);
	}


	public void encodeUnary(int aSymbol, CabacContext aContext0, CabacContext aContext1) throws IOException
	{
		if (aSymbol == 0)
		{
			encodeBit(0, aContext0);
		}
		else
		{
			encodeBit(1, aContext0);
			int l = aSymbol;
			while (--l > 0)
			{
				encodeBit(0, aContext1);
			}
			encodeBit(1, aContext1);
		}
	}


	public void encodeExpGolombEqProb(long aSymbol, int aStep) throws IOException
	{
		encodeBitEqProb((int)aSymbol & 1);
		aSymbol >>>= 1;

		while (aSymbol >= (1L << aStep))
		{
			encodeBitEqProb(0);

			aSymbol -= 1L << aStep;
			aStep++;
		}

		encodeBitEqProb(1);

		while (aStep-- > 0)
		{
			encodeBitEqProb((int)(aSymbol >>> aStep) & 1);
		}
	}


	public void encodeExpGolomb(long aSymbol, int aStep, CabacContext aContext) throws IOException
	{
		assert aSymbol >= 0;

		while (aSymbol >= (1L << aStep))
		{
			encodeBit(0, aContext);

			aSymbol -= 1L << aStep;
			aStep++;
		}

		encodeBit(1, aContext);

		while (aStep-- > 0)
		{
			encodeBitEqProb((int)(aSymbol >>> aStep) & 1);
		}
	}


	public void encodeExpGolomb(int aSymbol, int aStep, CabacContext[] aContext) throws IOException
	{
		assert aSymbol >= 0;

		int i = 0;

		while (aSymbol >= (1 << aStep))
		{
			encodeBit(0, aContext[i++]);
			aSymbol -= 1 << aStep;
			aStep++;
		}

		encodeBit(1, aContext[i]);

		while (aStep-- > 0)
		{
			encodeBitEqProb((aSymbol >>> aStep) & 1);
		}
	}


	public void encodeUnaryGolomb(long aSymbol, int aStep, CabacContext aContext) throws IOException
	{
		assert aSymbol >= 0;

		if (aStep <= 0 || aStep > 64)
		{
			throw new IllegalArgumentException();
		}

		int len = 0;

		while (aSymbol != 0)
		{
			encodeBit(0, aContext);
			for (int i = 0; i < aStep; i++, len++)
			{
				encodeBitEqProb((int)aSymbol & 1);
				aSymbol >>>= 1;
			}
		}

		if (len < 64)
		{
			encodeBit(1, aContext);
		}
	}


	public void encodeBytesEqProb(byte[] aBuffer) throws IOException
	{
		encodeBytesEqProb(aBuffer, 0, aBuffer.length);
	}


	public void encodeBytesEqProb(byte[] aBuffer, int aOffset, int aLength) throws IOException
	{
		for (int i = 0; i < aLength; i++)
		{
			int b = 0xff & aBuffer[aOffset + i];
			for (int j = 8; --j >= 0;)
			{
				encodeBitEqProb((b >> j) & 1);
			}
		}
	}


	public void encodeEqProb(long aValue, int aLength) throws IOException
	{
		for (int i = aLength; --i >= 0;)
		{
			encodeBitEqProb((int)(aValue >>> i) & 1);
		}
	}


	public void encodeFinal(int aBit) throws IOException
	{
		int range = mRange - 2;
		int low = mLow;
		int bl = mBitsToGo;

		if (aBit == 0)
		{
			if (range >= QUARTER)
			{
				mRange = range;
				return;
			}
			else
			{
				range <<= 1;
				if (--bl > MIN_BITS_TO_GO)
				{
					mRange = range;
					mBitsToGo = bl;
					return;
				}
			}
		}
		else
		{
			low += (range << bl);
			range = 2;

			if (low >= ONE)
			{
				low -= ONE;
				propagateCarry();
			}
			bl -= 7;

			range <<= 7;
			if (bl > MIN_BITS_TO_GO)
			{
				mRange = range;
				mLow = low;
				mBitsToGo = bl;
				return;
			}
		}

		mLow = (low << BITS_TO_LOAD) & (ONE - 1);
		low = (low >> (MAX_BITS - BITS_TO_LOAD)) & B_LOAD_MASK;
		if (low < B_LOAD_MASK)
		{
			putLastChunkPlusOutstanding(low);
		}
		else
		{
			mChunksOutstanding++;
		}

		mRange = range;
		bl += BITS_TO_LOAD;
		mBitsToGo = bl;
	}


	private void putOneByteFinal(int b) throws IOException
	{
		mOutputStream.write(b);
	}


	private void putBuffer() throws IOException
	{
		while (mPbuf >= 0)
		{
			mOutputStream.write((mBitBuffer >> (mPbuf << 3)) & 0xFF);
			mPbuf--;
		}
		mBitBuffer = 0;
	}


	private void putOneByte(int b) throws IOException
	{
		if (mPbuf >= 3)
		{
			putBuffer();
		}
		mBitBuffer <<= 8;
		mBitBuffer += b;
		mPbuf++;
	}


	private void putOneWord(int b) throws IOException
	{
		if (mPbuf >= 3)
		{
			putBuffer();
		}
		mBitBuffer <<= 16;
		mBitBuffer += b;
		mPbuf += 2;
	}


	private void propagateCarry() throws IOException
	{
		mBitBuffer++;
		while (mChunksOutstanding > 0)
		{
			putOneWord(0);
			mChunksOutstanding--;
		}
	}


	private void putLastChunkPlusOutstanding(int l) throws IOException
	{
		while (mChunksOutstanding > 0)
		{
			putOneWord(0xFFFF);
			mChunksOutstanding--;
		}
		putOneWord(l);
	}


	private void putLastChunkPlusOutstandingFinal(int l) throws IOException
	{
		while (mChunksOutstanding > 0)
		{
			putOneWord(0xFFFF);
			mChunksOutstanding--;
		}
		putOneByte(l);
	}


	public void stopEncoding() throws IOException
	{
		int remainingBits = BITS_TO_LOAD - mBitsToGo; // output (2 + remaining) bits for terminating the codeword + one stop bit
		int mask;

		if (remainingBits <= 5) // one terminating byte
		{
			mask = (255 - ((1 << (6 - remainingBits)) - 1));
			mLow = (mLow >> (MAX_BITS - 8)) & mask;
			mLow += (1 << (5 - remainingBits));       // put the terminating stop bit '1'

			putLastChunkPlusOutstandingFinal(mLow);
			putBuffer();
		}
		else if (remainingBits <= 13)            // two terminating bytes
		{
			putLastChunkPlusOutstandingFinal(((mLow >> (MAX_BITS - 8)) & 0xFF)); // mask out the 8 MSBs for output
			putBuffer();

			if (remainingBits > 6)
			{
				mask = (255 - ((1 << (14 - remainingBits)) - 1));
				mLow = (mLow >> (MAX_BITS - 16)) & mask;
				mLow += (1 << (13 - remainingBits));     // put the terminating stop bit '1'
				putOneByteFinal(mLow);
			}
			else
			{
				putOneByteFinal(128); // second byte contains terminating stop bit '1' only
			}
		}
		else             // three terminating bytes
		{
			putLastChunkPlusOutstanding(((mLow >> (MAX_BITS - BITS_TO_LOAD)) & B_LOAD_MASK)); // mask out the 16 MSBs for output
			putBuffer();

			if (remainingBits > 14)
			{
				mask = (255 - ((1 << (22 - remainingBits)) - 1));
				mLow = (mLow >> (MAX_BITS - 24)) & mask;
				mLow += 1 << (21 - remainingBits);       // put the terminating stop bit '1'
				putOneByteFinal(mLow);
			}
			else
			{
				putOneByteFinal(128); // third byte contains terminating stop bit '1' only
			}
		}
		mBitsToGo = 8;
	}
}
