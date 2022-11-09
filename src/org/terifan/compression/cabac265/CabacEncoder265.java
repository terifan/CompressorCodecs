package org.terifan.compression.cabac265;

import java.io.IOException;
import java.io.OutputStream;
import static org.terifan.compression.cabac265.CabacConstants.*;


public class CabacEncoder265 implements AutoCloseable
{
	private OutputStream mOutputStream;

	// VLC
	private int mVLCBuffer;
	private int mVLCBufferLen;

	// CABAC
	private int mRange;
	private int mLow;
	private int mBitsLeft;
	private int mBufferedByte;
	private int mNumBufferedBytes;


	public CabacEncoder265(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;

		mRange = 510;
		mLow = 0;

		mBitsLeft = 23;
		mBufferedByte = 0xFF;
		mNumBufferedBytes = 0;
	}


	public void writeBit(int aBit) throws IOException
	{
		writeBits(aBit, 1);
	}


	public void writeCABAC_TU_bypass(int aValue, int aMax) throws IOException
	{
		for (int i = 0; i < aValue; i++)
		{
			writeCABAC_bypass(1);
		}

		if (aValue < aMax)
		{
			writeCABAC_bypass(0);
		}
	}


	public void writeCABAC_TU(int aValue, CabacModel[] aModels) throws IOException
	{
		for (int i = 0; i < aValue; i++)
		{
			writeCABAC_bit(0, aModels[i]);
		}

		writeCABAC_bit(1, aModels[aValue]);
	}


	public void writeCABAC_FL_bypass(int aValue, int aLength) throws IOException
	{
		while (aLength > 0)
		{
			aLength--;
			writeCABAC_bypass(aValue & (1 << aLength));
		}
	}


	public float RDBits_for_CABAC_bin(int aBit, CabacModel aModel)
	{
		int idx = aModel.state << 1;

		if (aBit != aModel.MPSbit)
		{
			idx++;
		}

		return entropy_table[idx] / (float)(1 << 15);
	}


	public void writeCABAC_EGk_bypass(int aValue, int aStep) throws IOException
	{
		assert aValue >= 0;

		while (aValue >= (1 << aStep))
		{
			writeCABAC_bypass(1);
			aValue -= 1 << aStep;
			aStep++;
		}

		writeCABAC_bypass(0);

		while (aStep > 0)
		{
			aStep--;
			writeCABAC_bypass((aValue >>> aStep) & 1);
		}
	}


	public void writeCABAC_EGk(int aValue, int aStep, CabacModel[] aModels) throws IOException
	{
		assert aValue >= 0;

		int i = 0;

		while (aValue >= (1 << aStep))
		{
			writeCABAC_bit(1, aModels[i++]);
			aValue -= 1 << aStep;
			aStep++;
		}

		writeCABAC_bit(0, aModels[i]);

		while (aStep > 0)
		{
			aStep--;
			writeCABAC_bypass((aValue >>> aStep) & 1);
		}
	}


	public void writeCABAC_EGk(int aValue, int aStep, CabacModel[] aMagnitude, CabacModel[][] aValueModels) throws IOException
	{
		assert aValue >= 0;

		int i = 0;

		while (aValue >= (1 << aStep))
		{
			writeCABAC_bit(1, aMagnitude[i++]);
			aValue -= 1 << aStep;
			aStep++;
		}

		writeCABAC_bit(0, aMagnitude[i]);

		for (int j = 0; aStep > 0; j++)
		{
			aStep--;
			writeCABAC_bit((aValue >>> aStep) & 1, aValueModels[i][j]);
		}
	}


	public void writeUVLC(int aValue) throws IOException
	{
		assert aValue >= 0;

		int nLeadingZeros = 0;
		int base = 0;
		int range = 1;

		while (aValue >= base + range)
		{
			base += range;
			range <<= 1;
			nLeadingZeros++;
		}

		writeBits((1 << nLeadingZeros) | (aValue - base), 2 * nLeadingZeros + 1);
	}


	public void writeSVLC(int aValue) throws IOException
	{
		if (aValue == 0)
		{
			writeBits(1, 1);
		}
		else if (aValue > 0)
		{
			writeUVLC(2 * aValue - 1);
		}
		else
		{
			writeUVLC(-2 * aValue);
		}
	}


	public void addTrailingBits() throws IOException
	{
		writeBit(1);
		int nZeros = freeBitsInByteCount();
		writeBits(0, nZeros);
	}


	public void encodeFinal(int aBit) throws IOException
	{
		mRange -= 2;

		if (aBit != 0)
		{
			mLow += mRange;

			mLow <<= 7;
			mRange = 2 << 7;
			mBitsLeft -= 7;
		}
		else if (mRange >= 256)
		{
			return;
		}
		else
		{
			mLow <<= 1;
			mRange <<= 1;
			mBitsLeft--;
		}

		testAndWriteOut();
	}


	public void writeCABAC_bypass(int aBit) throws IOException
	{
		mLow <<= 1;

		if (aBit != 0)
		{
			mLow += mRange;
		}
		mBitsLeft--;

		testAndWriteOut();
	}


	public void testAndWriteOut() throws IOException
	{
		if (mBitsLeft < 12)
		{
			writeOut();
		}
	}


	public void writeCABAC_bit(int aBit, CabacModel aModel) throws IOException
	{
		int LPS = LPS_table[aModel.state][(mRange >> 6) - 4];
		mRange -= LPS;

		if (aBit != aModel.MPSbit)
		{
			int numBits = renorm_table[LPS >> 3];
			mLow = (mLow + mRange) << numBits;
			mRange = LPS << numBits;

			if (aModel.state == 0)
			{
				aModel.MPSbit = 1 - aModel.MPSbit;
			}

			aModel.state = next_state_LPS[aModel.state];

			mBitsLeft -= numBits;
		}
		else
		{
			aModel.state = next_state_MPS[aModel.state];

			// renorm
			if (mRange >= 256)
			{
				return;
			}

			mLow <<= 1;
			mRange <<= 1;
			mBitsLeft--;
		}

		testAndWriteOut();
	}


	public void writeOut() throws IOException
	{
		int leadByte = mLow >>> (24 - mBitsLeft);
		mBitsLeft += 8;
		mLow &= 0xffffffff >>> mBitsLeft;

		if (leadByte == 0xff)
		{
			mNumBufferedBytes++;
		}
		else
		{
			if (mNumBufferedBytes > 0)
			{
				int carry = leadByte >> 8;
				int symbol = mBufferedByte + carry;
				mBufferedByte = leadByte & 0xff;
				appendByte(symbol);

				symbol = (0xff + carry) & 0xff;
				while (mNumBufferedBytes > 1)
				{
					appendByte(symbol);
					mNumBufferedBytes--;
				}
			}
			else
			{
				mNumBufferedBytes = 1;
				mBufferedByte = leadByte;
			}
		}
	}


	// output all remaining bits and fill with zeros to next byte boundary
	public void flushVLC() throws IOException
	{
		while (mVLCBufferLen >= 8)
		{
			appendByte((mVLCBuffer >>> (mVLCBufferLen - 8)) & 0xFF);
			mVLCBufferLen -= 8;
		}

		if (mVLCBufferLen > 0)
		{
			appendByte(mVLCBuffer << (8 - mVLCBufferLen));
			mVLCBufferLen = 0;
		}

		mVLCBuffer = 0;
	}


	public void skipBits(int aLength) throws IOException
	{
		while (aLength >= 8)
		{
			writeBits(0, 8);
			aLength -= 8;
		}

		if (aLength > 0)
		{
			writeBits(0, aLength);
		}
	}


	public int freeBitsInByteCount()
	{
		if ((mVLCBufferLen % 8) == 0)
		{
			return 0;
		}
		return 8 - (mVLCBufferLen % 8);
	}


	public void appendByte(int aByte) throws IOException
	{
		mOutputStream.write(aByte);
	}


	@Override
	public void close() throws IOException
	{
		if (mOutputStream != null)
		{
			if ((mLow >>> (32 - mBitsLeft)) != 0)
			{
				appendByte(mBufferedByte + 1);
				while (mNumBufferedBytes > 1)
				{
					appendByte(0x00);
					mNumBufferedBytes--;
				}

				mLow -= 1 << (32 - mBitsLeft);
			}
			else
			{
				if (mNumBufferedBytes > 0)
				{
					appendByte(mBufferedByte);
				}

				while (mNumBufferedBytes > 1)
				{
					appendByte(0xff);
					mNumBufferedBytes--;
				}
			}

			writeBits(mLow >>> 8, 24 - mBitsLeft);

			mOutputStream.close();
			mOutputStream = null;
		}
	}


	public void writeBits(int aBits, int aLength) throws IOException
	{
		mVLCBuffer <<= aLength;
		mVLCBuffer |= aBits;
		mVLCBufferLen += aLength;

		while (mVLCBufferLen >= 8)
		{
			appendByte((mVLCBuffer >>> (mVLCBufferLen - 8)) & 0xFF);
			mVLCBufferLen -= 8;
		}
	}
}
