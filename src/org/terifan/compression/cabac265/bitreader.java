package org.terifan.compression.cabac265;


public class bitreader
{
	int MAX_UVLC_LEADING_ZEROS = 20;
	int UVLC_ERROR = -99999;

	int dataOffset;
	byte[] data;
	int bytes_remaining;

	long nextbits; // left-aligned bits
	int nextbits_cnt;


	public bitreader(byte[] buffer, int len)
	{
		data = buffer;
		bytes_remaining = len;

		nextbits = 0;
		nextbits_cnt = 0;

		bitreader_refill();
	}


	void bitreader_refill()
	{
		int shift = 64 - nextbits_cnt;

		while (shift >= 8 && bytes_remaining > 0)
		{
			long newval = data[dataOffset++];
			bytes_remaining--;

			shift -= 8;
			newval <<= shift;
			nextbits |= newval;
		}

		nextbits_cnt = 64 - shift;
	}


	int get_bits(int n)
	{
		if (nextbits_cnt < n)
		{
			bitreader_refill();
		}

		long val = nextbits;
		val >>= 64 - n;

		nextbits <<= n;
		nextbits_cnt -= n;

		return (int)val;
	}


	int get_bits_fast(int n)
	{
		long val = nextbits;
		val >>>= 64 - n;

		nextbits <<= n;
		nextbits_cnt -= n;

		return (int)val;
	}


	int peek_bits(int n)
	{
		if (nextbits_cnt < n)
		{
			bitreader_refill();
		}

		long val = nextbits;
		val >>>= 64 - n;

		return (int)val;
	}


	void skip_bits(int n)
	{
		if (nextbits_cnt < n)
		{
			bitreader_refill();
		}

		nextbits <<= n;
		nextbits_cnt -= n;
	}


	void skip_bits_fast(int n)
	{
		nextbits <<= n;
		nextbits_cnt -= n;
	}


	void skip_to_byte_boundary()
	{
		int nskip = (nextbits_cnt & 7);

		nextbits <<= nskip;
		nextbits_cnt -= nskip;
	}


//	void prepare_for_CABAC()
//	{
//		skip_to_byte_boundary();
//
//		int rewind = nextbits_cnt / 8;
//		dataOffset -= rewind;
//		bytes_remaining += rewind;
//		nextbits = 0;
//		nextbits_cnt = 0;
//	}


	int get_uvlc()
	{
		int num_zeros = 0;

		while (get_bits(1) == 0)
		{
			num_zeros++;

			if (num_zeros > MAX_UVLC_LEADING_ZEROS)
			{
				return UVLC_ERROR;
			}
		}

		int offset = 0;
		if (num_zeros != 0)
		{
			offset = get_bits(num_zeros);
			int value = offset + (1 << num_zeros) - 1;
			assert (value > 0);
			return value;
		}
		else
		{
			return 0;
		}
	}


	int get_svlc()
	{
		int v = get_uvlc();
		if (v == 0)
		{
			return v;
		}
		if (v == UVLC_ERROR)
		{
			return UVLC_ERROR;
		}

		boolean negative = ((v & 1) == 0);
		return negative ? -v / 2 : (v + 1) / 2;
	}


	boolean check_rbsp_trailing_bits()
	{
		int stop_bit = get_bits(1);
		assert (stop_bit == 1);

		while (this.nextbits_cnt > 0 || this.bytes_remaining > 0)
		{
			int filler = get_bits(1);
			if (filler != 0)
			{
				return false;
			}
		}

		return true;
	}
}
