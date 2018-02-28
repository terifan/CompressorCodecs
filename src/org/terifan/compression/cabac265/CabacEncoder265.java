package org.terifan.compression.cabac265;

import static org.terifan.compression.cabac265.CabacConstants.*;


public class CabacEncoder265 extends CabacEncoderBitstream
{
	public void setContextModels(CabacModel[] models)
	{
		mCtxModels = models;
	}


	public void write_bit(int bit)
	{
		write_bits(bit, 1);
	}


	public void write_CABAC_TU_bypass(int value, int cMax)
	{
		for (int i = 0; i < value; i++)
		{
			write_CABAC_bypass(1);
		}

		if (value < cMax)
		{
			write_CABAC_bypass(0);
		}
	}


	public void write_CABAC_FL_bypass(int value, int n)
	{
		while (n > 0)
		{
			n--;
			write_CABAC_bypass(value & (1 << n));
		}
	}


	public float RDBits_for_CABAC_bin(int modelIdx, int bit)
	{
		CabacModel model = mCtxModels[modelIdx];
		int idx = model.state << 1;

		if (bit != model.MPSbit)
		{
			idx++;
		}

		return entropy_table[idx] / (float)(1 << 15);
	}


	public void write_CABAC_EGk(int val, int k)
	{
		while (val >= (1 << k))
		{
			write_CABAC_bypass(1);
			val = val - (1 << k);
			k++;
		}

		write_CABAC_bypass(0);

		while (k > 0)
		{
			k--;
			write_CABAC_bypass((val >>> k) & 1);
		}
	}


	public void write_uvlc(int value)
	{
		assert value >= 0;

		int nLeadingZeros = 0;
		int base = 0;
		int range = 1;

		while (value >= base + range)
		{
			base += range;
			range <<= 1;
			nLeadingZeros++;
		}

		write_bits((1 << nLeadingZeros) | (value - base), 2 * nLeadingZeros + 1);
	}


	public void write_svlc(int value)
	{
		if (value == 0)
		{
			write_bits(1, 1);
		}
		else if (value > 0)
		{
			write_uvlc(2 * value - 1);
		}
		else
		{
			write_uvlc(-2 * value);
		}
	}


	public void add_trailing_bits()
	{
		write_bit(1);
		int nZeros = number_free_bits_in_byte();
		write_bits(0, nZeros);
	}
}
