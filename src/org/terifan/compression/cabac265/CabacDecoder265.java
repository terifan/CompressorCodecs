package org.terifan.compression.cabac265;

import java.io.IOException;
import java.io.InputStream;
import static org.terifan.compression.cabac265.CabacConstants.*;


// https://github.com/strukturag/libde265/blob/master/libde265/cabac.cc
public class CabacDecoder265
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


	public int decodeCABAC_bit(CabacModel aModel) throws IOException
	{
		int decoded_bit;
		int LPS = LPS_table[aModel.state][(mDecoderRange >> 6) - 4];
		mDecoderRange -= LPS;

		int scaled_range = mDecoderRange << 7;

		if (mDecoderValue < scaled_range)
		{
			// MPS path
			decoded_bit = aModel.MPSbit;
			aModel.state = next_state_MPS[aModel.state];

			if (scaled_range < (256 << 7))
			{
				// scaled range, highest bit (15) not set
				mDecoderRange = scaled_range >> 6; // shift range by one bit
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
			mDecoderValue = (mDecoderValue - scaled_range);

			int num_bits = renorm_table[LPS >> 3];
			mDecoderValue <<= num_bits;
			mDecoderRange = LPS << num_bits;  // this is always >= 0x100 except for state 63, but state 63 is never used

			int num_bitsTab = renorm_table[LPS >> 3];

			assert (num_bits == num_bitsTab);

			decoded_bit = 1 - aModel.MPSbit;

			if (aModel.state == 0)
			{
				aModel.MPSbit = 1 - aModel.MPSbit;
			}
			aModel.state = next_state_LPS[aModel.state];

			mBitsNeeded += num_bits;

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

		return decoded_bit;
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
		int scaled_range = mDecoderRange << 7;
		if (mDecoderValue >= scaled_range)
		{
			mDecoderValue -= scaled_range;
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
			int bit = decodeCABAC_bypass();
			if (bit == 0)
			{
				return i;
			}
		}

		return aMax;
	}


	public int decodeCABAC_TU(int aMax, CabacModel aModel) throws IOException
	{
		for (int i = 0; i < aMax; i++)
		{
			int bit = decodeCABAC_bit(aModel);
			if (bit == 0)
			{
				return i;
			}
		}

		return aMax;
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

		int scaled_range = mDecoderRange << 7;
		int value = mDecoderValue / scaled_range;
		if (/*unlikely*/(value >= (1 << aLength)))
		{
			value = (1 << aLength) - 1;
		}

		// may happen with broken bitstreams
		mDecoderValue -= value * scaled_range;

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


	public int decodeCABAC_EGk(int aStep, CabacModel[] aModels) throws IOException
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
}
