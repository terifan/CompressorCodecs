package org.terifan.compression.cabac265;

import static org.terifan.compression.cabac265.Cabac.*;


public class CABAC_decoder
{
	byte[] bitstream;
	int bitstream_curr;
	int bitstream_end;

	int decoder_range;
	int decoder_value;
	int bits_needed;

	int logcnt = 1;


	CABAC_decoder(byte[] bitstream, int length)
	{
		this.bitstream = bitstream;
		bitstream_curr = 0;
		bitstream_end = length;

		decoder_range = 510;
		bits_needed = 8;

		decoder_value = 0;

		if (length > 0)
		{
			decoder_value = (0xff & bitstream[bitstream_curr++]) << 8;
			bits_needed -= 8;
		}
		if (length > 1)
		{
			decoder_value |= (0xff & bitstream[bitstream_curr++]);
			bits_needed -= 8;
		}
	}


	int decode_CABAC_bit(CABAC_decoder decoder, context_model model)
	{
		int decoded_bit;
		int LPS = LPS_table[model.state][(decoder.decoder_range >> 6) - 4];
		decoder.decoder_range -= LPS;

		int scaled_range = decoder.decoder_range << 7;

		if (decoder.decoder_value < scaled_range)
		{
			// MPS path
			decoded_bit = model.MPSbit;
			model.state = next_state_MPS[model.state];

			if (scaled_range < (256 << 7))
			{
				// scaled range, highest bit (15) not set
				decoder.decoder_range = scaled_range >> 6; // shift range by one bit
				decoder.decoder_value <<= 1;               // shift value by one bit
				decoder.bits_needed++;

				if (decoder.bits_needed == 0)
				{
					decoder.bits_needed = -8;
					if (decoder.bitstream_curr < decoder.bitstream_end)
					{
						decoder.decoder_value |= 0xff & decoder.bitstream[decoder.bitstream_curr++];
					}
				}
			}
		}
		else
		{
			// LPS path
			decoder.decoder_value = (decoder.decoder_value - scaled_range);

			int num_bits = renorm_table[LPS >> 3];
			decoder.decoder_value <<= num_bits;
			decoder.decoder_range = LPS << num_bits;  // this is always >= 0x100 except for state 63, but state 63 is never used

			int num_bitsTab = renorm_table[LPS >> 3];

			assert (num_bits == num_bitsTab);

			decoded_bit = 1 - model.MPSbit;

			if (model.state == 0)
			{
				model.MPSbit = 1 - model.MPSbit;
			}
			model.state = next_state_LPS[model.state];

			decoder.bits_needed += num_bits;

			if (decoder.bits_needed >= 0)
			{
				if (decoder.bitstream_curr < decoder.bitstream_end)
				{
					decoder.decoder_value |= (0xff & decoder.bitstream[decoder.bitstream_curr++]) << decoder.bits_needed;
				}

				decoder.bits_needed -= 8;
			}
		}

		logcnt++;

		return decoded_bit;
	}


	int decode_CABAC_term_bit(CABAC_decoder decoder)
	{
		decoder.decoder_range -= 2;
		int scaledRange = decoder.decoder_range << 7;

		if (decoder.decoder_value >= scaledRange)
		{
			return 1;
		}
		else
		{
			// there is a while loop in the standard, but it will always be executed only once

			if (scaledRange < (256 << 7))
			{
				decoder.decoder_range = scaledRange >> 6;
				decoder.decoder_value *= 2;

				decoder.bits_needed++;
				if (decoder.bits_needed == 0)
				{
					decoder.bits_needed = -8;

					if (decoder.bitstream_curr < decoder.bitstream_end)
					{
						decoder.decoder_value += 0xff & decoder.bitstream[decoder.bitstream_curr++];
					}
				}
			}

			return 0;
		}
	}


	int decode_CABAC_bypass(CABAC_decoder decoder)
	{
		decoder.decoder_value <<= 1;
		decoder.bits_needed++;

		if (decoder.bits_needed >= 0)
		{
			if (decoder.bitstream_end > decoder.bitstream_curr)
			{
				decoder.bits_needed = -8;
				decoder.decoder_value |= 0xff & decoder.bitstream[decoder.bitstream_curr++];
			}
		}

		int bit;
		int scaled_range = decoder.decoder_range << 7;
		if (decoder.decoder_value >= scaled_range)
		{
			decoder.decoder_value -= scaled_range;
			bit = 1;
		}
		else
		{
			bit = 0;
		}

		logcnt++;

		return bit;
	}


	int decode_CABAC_TU_bypass(CABAC_decoder decoder, int cMax)
	{
		for (int i = 0; i < cMax; i++)
		{
			int bit = decode_CABAC_bypass(decoder);
			if (bit == 0)
			{
				return i;
			}
		}

		return cMax;
	}


	int decode_CABAC_TU(CABAC_decoder decoder, int cMax, context_model model)
	{
		for (int i = 0; i < cMax; i++)
		{
			int bit = decode_CABAC_bit(decoder, model);
			if (bit == 0)
			{
				return i;
			}
		}

		return cMax;
	}


	int decode_CABAC_FL_bypass_parallel(CABAC_decoder decoder, int nBits)
	{
		decoder.decoder_value <<= nBits;
		decoder.bits_needed += nBits;

		if (decoder.bits_needed >= 0)
		{
			if (decoder.bitstream_end > decoder.bitstream_curr)
			{
				int input = 0xff & decoder.bitstream[decoder.bitstream_curr++];
				input <<= decoder.bits_needed;

				decoder.bits_needed -= 8;
				decoder.decoder_value |= input;
			}
		}

		int scaled_range = decoder.decoder_range << 7;
		int value = decoder.decoder_value / scaled_range;
		if (/*unlikely*/(value >= (1 << nBits)))
		{
			value = (1 << nBits) - 1;
		} 
	
		// may happen with broken bitstreams
		decoder.decoder_value -= value * scaled_range;

		logcnt += nBits;

		return value;
	}


	int decode_CABAC_FL_bypass(CABAC_decoder decoder, int nBits)
	{
		int value = 0;

		if (/*likely*/(nBits <= 8))
		{
			if (nBits == 0)
			{
				return 0;
			}
			else
			{
				value = decode_CABAC_FL_bypass_parallel(decoder, nBits);
			}
		}
		else
		{
			value = decode_CABAC_FL_bypass_parallel(decoder, 8);
			nBits -= 8;

			while (nBits-- != 0)
			{
				value <<= 1;
				value |= decode_CABAC_bypass(decoder);
			}
		}

		return value;
	}


	int decode_CABAC_TR_bypass(CABAC_decoder decoder, int cRiceParam, int cTRMax)
	{
		int prefix = decode_CABAC_TU_bypass(decoder, cTRMax >> cRiceParam);
		if (prefix == 4)
		{
			// TODO check: constant 4 only works for coefficient decoding
			return cTRMax;
		}

		int suffix = decode_CABAC_FL_bypass(decoder, cRiceParam);

		return (prefix << cRiceParam) | suffix;
	}

	final static int MAX_PREFIX = 32;


	int decode_CABAC_EGk_bypass(CABAC_decoder decoder, int k)
	{
		int base = 0;
		int n = k;

		for (;;)
		{
			int bit = decode_CABAC_bypass(decoder);
			if (bit == 0)
			{
				break;
			}
			else
			{
				base += 1 << n;
				n++;
			}

			if (n == k + MAX_PREFIX)
			{
				return 0; // TODO: error
			}
		}

		int suffix = decode_CABAC_FL_bypass(decoder, n);
		return base + suffix;
	}
}
