package org.terifan.compression.cabac265;

import static org.terifan.compression.cabac265.CabacConstants.*;


public class CABAC_encoder_estim_constant extends CABAC_encoder_estim
{
	@Override
	boolean modifies_context()
	{
		return false;
	}

	
	@Override
	void write_CABAC_bit(int modelIdx, int bit)
	{
		CabacModel model = mCtxModels[modelIdx];
		int idx = model.state << 1;

		if (bit != model.MPSbit)
		{
			idx++;
		}

		mFracBits += entropy_table[idx];
	}
}
