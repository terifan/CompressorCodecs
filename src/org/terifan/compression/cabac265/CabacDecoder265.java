package org.terifan.compression.cabac265;

import java.io.IOException;
import java.io.InputStream;
import static org.terifan.compression.cabac265.CabacConstants.*;


// https://github.com/strukturag/libde265/blob/master/libde265/cabac.cc
public class CabacDecoder265 implements AutoCloseable
{
	private final static int MAX_PREFIX = 32;

	private InputStream mInputStream;
	private int mDecoderRange;
	private int mDecoderValue;
	private int mBitsNeeded;


	public CabacDecoder265(InputStream aInputStream) throws IOException
	{
		mInputStream = aInputStream;

		mDecoderRange = 510;
		mBitsNeeded = 8;

		mDecoderValue = mInputStream.read() << 8;
		mBitsNeeded -= 8;

		mDecoderValue |= mInputStream.read();
		mBitsNeeded -= 8;
	}


	public int decodeCABAC_bit(CabacContext265 aModel) throws IOException
	{
		int decodedBit;
		int LPS = LPS_table[aModel.state][(mDecoderRange >> 6) - 4];
		mDecoderRange -= LPS;

		int scaledRange = mDecoderRange << 7;

		if (mDecoderValue < scaledRange)
		{
			// MPS path
			decodedBit = aModel.MPSbit;
			aModel.state = next_state_MPS[aModel.state];

			if (scaledRange < (256 << 7))
			{
				// scaled range, highest bit (15) not set
				mDecoderRange = scaledRange >> 6; // shift range by one bit
				mDecoderValue <<= 1;               // shift value by one bit
				mBitsNeeded++;

				if (mBitsNeeded == 0)
				{
					mBitsNeeded = -8;
					int b = mInputStream.read();
					if (b >= 0)
					{
						mDecoderValue |= b;
					}
				}
			}
		}
		else
		{
			// LPS path
			mDecoderValue = (mDecoderValue - scaledRange);

			int numBits = renorm_table[LPS >> 3];
			mDecoderValue <<= numBits;
			mDecoderRange = LPS << numBits;  // this is always >= 0x100 except for state 63, but state 63 is never used

			int numBitsTab = renorm_table[LPS >> 3];

			assert numBits == numBitsTab;

			decodedBit = 1 - aModel.MPSbit;

			if (aModel.state == 0)
			{
				aModel.MPSbit = 1 - aModel.MPSbit;
			}
			aModel.state = next_state_LPS[aModel.state];

			mBitsNeeded += numBits;

			if (mBitsNeeded >= 0)
			{
				int b = mInputStream.read();
				if (b >= 0)
				{
					mDecoderValue |= b << mBitsNeeded;
				}

				mBitsNeeded -= 8;
			}
		}

		return decodedBit;
	}


	public int decodeCABAC_term_bit() throws IOException
	{
		mDecoderRange -= 2;
		int scaledRange = mDecoderRange << 7;

		if (mDecoderValue >= scaledRange)
		{
			return 1;
		}

		// there is a while loop in the standard, but it will always be executed only once
		if (scaledRange < (256 << 7))
		{
			mDecoderRange = scaledRange >> 6;
			mDecoderValue *= 2;

			mBitsNeeded++;
			if (mBitsNeeded == 0)
			{
				mBitsNeeded = -8;

				int b = mInputStream.read();
				if (b >= 0)
				{
					mDecoderValue += b;
				}
			}
		}

		return 0;
	}


	public int decodeCABAC_bypass() throws IOException
	{
		mDecoderValue <<= 1;
		mBitsNeeded++;

		if (mBitsNeeded >= 0)
		{
			int b = mInputStream.read();
			if (b >= 0)
			{
				mBitsNeeded = -8;
				mDecoderValue |= b;
			}
		}

		int bit;
		int scaledRange = mDecoderRange << 7;
		if (mDecoderValue >= scaledRange)
		{
			mDecoderValue -= scaledRange;
			bit = 1;
		}
		else
		{
			bit = 0;
		}

		return bit;
	}


	public int decodeCABAC_TU_bypass(int aMax) throws IOException
	{
		for (int i = 0; i < aMax; i++)
		{
			if (decodeCABAC_bypass() == 0)
			{
				return i;
			}
		}

		return aMax;
	}


	public int decodeCABAC_TU(int aMax, CabacContext265 aModel) throws IOException
	{
		for (int i = 0; i < aMax; i++)
		{
			if (decodeCABAC_bit(aModel) == 0)
			{
				return i;
			}
		}

		return aMax;
	}


	public int decodeCABAC_TU(CabacContext265[] aModel) throws IOException
	{
		for (int i = 0; ; i++)
		{
			if (decodeCABAC_bit(aModel[i]) == 0)
			{
				return i;
			}
		}
	}


	public int decodeCABAC_FL_bypass_parallel(int aLength) throws IOException
	{
		mDecoderValue <<= aLength;
		mBitsNeeded += aLength;

		if (mBitsNeeded >= 0)
		{
			int b = mInputStream.read();
			if (b >= 0)
			{
				int input = b;
				input <<= mBitsNeeded;

				mBitsNeeded -= 8;
				mDecoderValue |= input;
			}
		}

		int scaledRange = mDecoderRange << 7;
		int value = mDecoderValue / scaledRange;
		if (/*unlikely*/(value >= (1 << aLength)))
		{
			value = (1 << aLength) - 1;
		}

		// may happen with broken bitstreams
		mDecoderValue -= value * scaledRange;

		return value;
	}


	public int decodeCABAC_FL_bypass(int aLength) throws IOException
	{
		int value = 0;

		if (aLength <= 8)
		{
			if (aLength == 0)
			{
				return 0;
			}
			value = decodeCABAC_FL_bypass_parallel(aLength);
		}
		else
		{
			value = decodeCABAC_FL_bypass_parallel(8);
			aLength -= 8;

			while (aLength-- != 0)
			{
				value <<= 1;
				value |= decodeCABAC_bypass();
			}
		}

		return value;
	}


	public int decodeCABAC_TR_bypass(int aRiceParam, int aTRMax) throws IOException
	{
		int prefix = decodeCABAC_TU_bypass(aTRMax >> aRiceParam);
		if (prefix == 4)
		{
			// TODO check: constant 4 only works for coefficient decoding
			return aTRMax;
		}

		int suffix = decodeCABAC_FL_bypass(aRiceParam);

		return (prefix << aRiceParam) | suffix;
	}


	public int decodeCABAC_EGk_bypass(int aStep) throws IOException
	{
		int base = 0;
		int n = aStep;

		while (decodeCABAC_bypass() != 0)
		{
			base += 1 << n;
			n++;

			if (n == aStep + MAX_PREFIX)
			{
				System.out.println("err");
				return 0; // TODO: error
			}
		}

		int suffix = decodeCABAC_FL_bypass(n);

		return base + suffix;
	}


	public int decodeCABAC_EGk(int aStep, CabacContext265[] aModels) throws IOException
	{
		int base = 0;
		int n = aStep;
		int i = 0;

		while (decodeCABAC_bit(aModels[i++]) != 0)
		{
			base += 1 << n;
			n++;

			if (n == aStep + MAX_PREFIX)
			{
				System.out.println("err");
				return 0; // TODO: error
			}
		}

		int suffix = decodeCABAC_FL_bypass(n);

		return base + suffix;
	}


	public int decodeCABAC_EGk(int aStep, CabacContext265[] aMagnitudeModels, CabacContext265[][] aValueModels) throws IOException
	{
		int base = 0;
		int n = aStep;
		int i = 0;

		while (decodeCABAC_bit(aMagnitudeModels[i++]) != 0)
		{
			base += 1 << n;
			n++;

			if (n == aStep + MAX_PREFIX)
			{
				System.out.println("err");
				return 0; // TODO: error
			}
		}

		int suffix = 0;
		for (int j = 0, k = n; j < n; j++)
		{
			suffix += decodeCABAC_bit(aValueModels[i][j]) << --k;
		}

		return base + suffix;
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
}
