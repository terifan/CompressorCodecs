package org.terifan.compression.cabac265;

import java.io.IOException;
import java.io.InputStream;
import static org.terifan.compression.cabac265.CabacConstants.*;


// https://github.com/strukturag/libde265/blob/master/libde265/cabac.cc
public class CabacDecoder265
{
	private final static int MAX_PREFIX = 32;

	private InputStream mInputStream;
	private int decoder_range;
	private int decoder_value;
	private int bits_needed;


	public CabacDecoder265(InputStream aInputStream) throws IOException
	{
		mInputStream = aInputStream;

		decoder_range = 510;
		bits_needed = 8;

		decoder_value = mInputStream.read() << 8;
		bits_needed -= 8;

		decoder_value |= mInputStream.read();
		bits_needed -= 8;
	}


	public int decode_CABAC_bit(CabacModel model) throws IOException
	{
		int decoded_bit;
		int LPS = LPS_table[model.state][(decoder_range >> 6) - 4];
		decoder_range -= LPS;

		int scaled_range = decoder_range << 7;

		if (decoder_value < scaled_range)
		{
			// MPS path
			decoded_bit = model.MPSbit;
			model.state = next_state_MPS[model.state];

			if (scaled_range < (256 << 7))
			{
				// scaled range, highest bit (15) not set
				decoder_range = scaled_range >> 6; // shift range by one bit
				decoder_value <<= 1;               // shift value by one bit
				bits_needed++;

				if (bits_needed == 0)
				{
					bits_needed = -8;
					int b = mInputStream.read();
					if (b >= 0)
					{
						decoder_value |= b;
					}
				}
			}
		}
		else
		{
			// LPS path
			decoder_value = (decoder_value - scaled_range);

			int num_bits = renorm_table[LPS >> 3];
			decoder_value <<= num_bits;
			decoder_range = LPS << num_bits;  // this is always >= 0x100 except for state 63, but state 63 is never used

			int num_bitsTab = renorm_table[LPS >> 3];

			assert (num_bits == num_bitsTab);

			decoded_bit = 1 - model.MPSbit;

			if (model.state == 0)
			{
				model.MPSbit = 1 - model.MPSbit;
			}
			model.state = next_state_LPS[model.state];

			bits_needed += num_bits;

			if (bits_needed >= 0)
			{
				int b = mInputStream.read();
				if (b >= 0)
				{
					decoder_value |= b << bits_needed;
				}

				bits_needed -= 8;
			}
		}

		return decoded_bit;
	}


	public int decode_CABAC_term_bit() throws IOException
	{
		decoder_range -= 2;
		int scaledRange = decoder_range << 7;

		if (decoder_value >= scaledRange)
		{
			return 1;
		}

		// there is a while loop in the standard, but it will always be executed only once
		if (scaledRange < (256 << 7))
		{
			decoder_range = scaledRange >> 6;
			decoder_value *= 2;

			bits_needed++;
			if (bits_needed == 0)
			{
				bits_needed = -8;

				int b = mInputStream.read();
				if (b >= 0)
				{
					decoder_value += b;
				}
			}
		}

		return 0;
	}


	public int decode_CABAC_bypass() throws IOException
	{
		decoder_value <<= 1;
		bits_needed++;

		if (bits_needed >= 0)
		{
			int b = mInputStream.read();
			if (b >= 0)
			{
				bits_needed = -8;
				decoder_value |= b;
			}
		}

		int bit;
		int scaled_range = decoder_range << 7;
		if (decoder_value >= scaled_range)
		{
			decoder_value -= scaled_range;
			bit = 1;
		}
		else
		{
			bit = 0;
		}

		return bit;
	}


	public int decode_CABAC_TU_bypass(int cMax) throws IOException
	{
		for (int i = 0; i < cMax; i++)
		{
			int bit = decode_CABAC_bypass();
			if (bit == 0)
			{
				return i;
			}
		}

		return cMax;
	}


	public int decode_CABAC_TU(int cMax, CabacModel model) throws IOException
	{
		for (int i = 0; i < cMax; i++)
		{
			int bit = decode_CABAC_bit(model);
			if (bit == 0)
			{
				return i;
			}
		}

		return cMax;
	}


	public int decode_CABAC_FL_bypass_parallel(int nBits) throws IOException
	{
		decoder_value <<= nBits;
		bits_needed += nBits;

		if (bits_needed >= 0)
		{
			int b = mInputStream.read();
			if (b >= 0)
			{
				int input = b;
				input <<= bits_needed;

				bits_needed -= 8;
				decoder_value |= input;
			}
		}

		int scaled_range = decoder_range << 7;
		int value = decoder_value / scaled_range;
		if (/*unlikely*/(value >= (1 << nBits)))
		{
			value = (1 << nBits) - 1;
		}

		// may happen with broken bitstreams
		decoder_value -= value * scaled_range;

		return value;
	}


	public int decode_CABAC_FL_bypass(int nBits) throws IOException
	{
		int value = 0;

		if (nBits <= 8)
		{
			if (nBits == 0)
			{
				return 0;
			}
			value = decode_CABAC_FL_bypass_parallel(nBits);
		}
		else
		{
			value = decode_CABAC_FL_bypass_parallel(8);
			nBits -= 8;

			while (nBits-- != 0)
			{
				value <<= 1;
				value |= decode_CABAC_bypass();
			}
		}

		return value;
	}


	public int decode_CABAC_TR_bypass(int cRiceParam, int cTRMax) throws IOException
	{
		int prefix = decode_CABAC_TU_bypass(cTRMax >> cRiceParam);
		if (prefix == 4)
		{
			// TODO check: constant 4 only works for coefficient decoding
			return cTRMax;
		}

		int suffix = decode_CABAC_FL_bypass(cRiceParam);

		return (prefix << cRiceParam) | suffix;
	}


	public int decode_CABAC_EGk_bypass(int k) throws IOException
	{
		int base = 0;
		int n = k;

		while (decode_CABAC_bypass() != 0)
		{
			base += 1 << n;
			n++;

			if (n == k + MAX_PREFIX)
			{
				System.out.println("err");
				return 0; // TODO: error
			}
		}

		int suffix = decode_CABAC_FL_bypass(n);

		return base + suffix;
	}


	public int decode_CABAC_EGk(int k, CabacModel[] aModels) throws IOException
	{
		int base = 0;
		int n = k;
		int i = 0;

		while (decode_CABAC_bit(aModels[i++]) != 0)
		{
			base += 1 << n;
			n++;

			if (n == k + MAX_PREFIX)
			{
				System.out.println("err");
				return 0; // TODO: error
			}
		}

		int suffix = decode_CABAC_FL_bypass(n);

		return base + suffix;
	}


	public int decode_CABAC_EGk(int k, CabacModel aModels) throws IOException
	{
		int base = 0;
		int n = k;

		while (decode_CABAC_bit(aModels) != 0)
		{
			base += 1 << n;
			n++;

			if (n == k + MAX_PREFIX)
			{
				System.out.println("err");
				return 0; // TODO: error
			}
		}

		int suffix = decode_CABAC_FL_bypass(n);

		return base + suffix;
	}
}
