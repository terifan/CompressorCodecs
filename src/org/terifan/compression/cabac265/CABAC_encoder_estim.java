package org.terifan.compression.cabac265;

import static org.terifan.compression.cabac265.CabacConstants.*;


public class CABAC_encoder_estim
{
	CabacModel[] mCtxModels;
	
	
	void reset()
	{
		mFracBits = 0;
	}


	int size()
	{
		return (int)(mFracBits >> (15 + 3));
	}


	long getFracBits()
	{
		return mFracBits;
	}


	float getRDBits()
	{
		return mFracBits / (float)(1 << 15);
	}

	
	void write_bits(int bits, int n)
	{
		mFracBits += n << 15;
	}


	void write_bit(int bit)
	{
		mFracBits += 1 << 15;
	}


	void write_startcode()
	{
		mFracBits += (1 << 15) * 8 * 3;
	}


	void skip_bits(int nBits)
	{
		mFracBits += nBits << 15;
	}


	int number_free_bits_in_byte()
	{
		return 0;
	}


	void write_CABAC_bypass(int bit)
	{
		mFracBits += 0x8000;
	}


	void write_CABAC_FL_bypass(int value, int nBits)
	{
		mFracBits += nBits << 15;
	}


	void write_CABAC_term_bit(int bit)
	{
		// not implemented (not needed)
	}


	boolean modifies_context()
	{
		return true;
	}

	long mFracBits;


	void write_CABAC_bit(int modelIdx, int bit)
	{
		CabacModel model = mCtxModels[modelIdx];

		int idx = model.state << 1;

		if (bit == model.MPSbit)
		{
			model.state = next_state_MPS[model.state];
		}
		else
		{
			idx++;
			if (model.state == 0)
			{
				model.MPSbit = 1 - model.MPSbit;
			}
			model.state = next_state_LPS[model.state];
		}

		mFracBits += entropy_table[idx];
	}
}
