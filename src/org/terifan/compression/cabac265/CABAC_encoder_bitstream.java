package org.terifan.compression.cabac265;

import static org.terifan.compression.cabac265.Cabac.*;


public class CABAC_encoder_bitstream
{
	int encBinCnt = 1;
	context_model[] mCtxModels;

	byte[] data_mem;
	int data_capacity;
	int data_size;
	char state; // for inserting emulation-prevention bytes

	// VLC
	int vlc_buffer;
	int vlc_buffer_len;

	// CABAC
	int range;
	int low;
	int bits_left;
	int buffered_byte;
	int num_buffered_bytes;


	CABAC_encoder_bitstream()
	{
		data_mem = null;
		data_capacity = 0;
		data_size = 0;
		state = 0;

		vlc_buffer_len = 0;

		init_CABAC();
	}


	void init_CABAC()
	{
		range = 510;
		low = 0;

		bits_left = 23;
		buffered_byte = 0xFF;
		num_buffered_bytes = 0;
	}


	int size()
	{
		return data_size;
	}


	byte[] data()
	{
		return data_mem;
	}


	void write_CABAC_term_bit(int bit)
	{
		range -= 2;

		if (bit != 0)
		{
			low += range;

			low <<= 7;
			range = 2 << 7;
			bits_left -= 7;
		}
		else if (range >= 256)
		{
			return;
		}
		else
		{
			low <<= 1;
			range <<= 1;
			bits_left--;
		}

		testAndWriteOut();
	}


	void write_CABAC_bypass(int bin)
	{
		encBinCnt++;

		low <<= 1;

		if (bin != 0)
		{
			low += range;
		}
		bits_left--;

		testAndWriteOut();
	}


	void testAndWriteOut()
	{
		if (bits_left < 12)
		{
			write_out();
		}
	}


	void write_CABAC_bit(int modelIdx, int bin)
	{
		context_model model = mCtxModels[modelIdx];

		encBinCnt++;

		int LPS = LPS_table[model.state][(range >> 6) - 4];
		range -= LPS;

		if (bin != model.MPSbit)
		{
			int num_bits = renorm_table[LPS >> 3];
			low = (low + range) << num_bits;
			range = LPS << num_bits;

			if (model.state == 0)
			{
				model.MPSbit = 1 - model.MPSbit;
			}

			model.state = next_state_LPS[model.state];

			bits_left -= num_bits;
		}
		else
		{
			model.state = next_state_MPS[model.state];

			// renorm
			if (range >= 256)
			{
				return;
			}

			low <<= 1;
			range <<= 1;
			bits_left--;
		}

		testAndWriteOut();
	}


	void write_out()
	{
		int leadByte = low >>> (24 - bits_left);
		bits_left += 8;
		low &= 0xffffffff >>> bits_left;

		if (leadByte == 0xff)
		{
			num_buffered_bytes++;
		}
		else
		{
			if (num_buffered_bytes > 0)
			{
				int carry = leadByte >> 8;
				int byte_ = buffered_byte + carry;
				buffered_byte = leadByte & 0xff;
				append_byte(byte_);

				byte_ = (0xff + carry) & 0xff;
				while (num_buffered_bytes > 1)
				{
					append_byte(byte_);
					num_buffered_bytes--;
				}
			}
			else
			{
				num_buffered_bytes = 1;
				buffered_byte = leadByte;
			}
		}
	}


	// output all remaining bits and fill with zeros to next byte boundary
	void flush_VLC()
	{
		while (vlc_buffer_len >= 8)
		{
			append_byte((vlc_buffer >> (vlc_buffer_len - 8)) & 0xFF);
			vlc_buffer_len -= 8;
		}

		if (vlc_buffer_len > 0)
		{
			append_byte(vlc_buffer << (8 - vlc_buffer_len));
			vlc_buffer_len = 0;
		}

		vlc_buffer = 0;
	}


	void skip_bits(int nBits)
	{
		while (nBits >= 8)
		{
			write_bits(0, 8);
			nBits -= 8;
		}

		if (nBits > 0)
		{
			write_bits(0, nBits);
		}
	}


	int number_free_bits_in_byte()
	{
		if ((vlc_buffer_len % 8) == 0)
		{
			return 0;
		}
		return 8 - (vlc_buffer_len % 8);
	}


	void check_size_and_resize(int nBytes)
	{
		if (data_size + nBytes > data_capacity)
		{ // 1 extra byte for stuffing
			if (data_capacity == 0)
			{
				data_capacity = INITIAL_CABAC_BUFFER_CAPACITY;
			}
			else
			{
				data_capacity *= 2;
			}

			data_mem = new byte[data_capacity];
		}
	}


	void append_byte(int byte_)
	{
		check_size_and_resize(2);

		// --- emulation prevention ---

		/* These byte sequences may never occur in the bitstream:
		 0x000000 / 0x000001 / 0x000002

		 Hence, we have to add a 0x03 before the third byte.
		 We also have to add a 0x03 for this sequence: 0x000003, because
		 the escape byte itself also has to be escaped.
		 */
		// S0 --(0)--> S1 --(0)--> S2 --(0,1,2,3)--> add stuffing
		if (byte_ <= 3)
		{
			/**/ if (state < 2 && byte_ == 0)
			{
				state++;
			}
			else if (state == 2 && byte_ <= 3)
			{
				data_mem[data_size++] = 3;

				if (byte_ == 0)
				{
					state = 1;
				}
				else
				{
					state = 0;
				}
			}
			else
			{
				state = 0;
			}
		}
		else
		{
			state = 0;
		}

		// write actual data byte
		data_mem[data_size++] = (byte)byte_;
	}


	void flush_CABAC()
	{
		if ((low >> (32 - bits_left)) != 0)
		{
			append_byte(buffered_byte + 1);
			while (num_buffered_bytes > 1)
			{
				append_byte(0x00);
				num_buffered_bytes--;
			}

			low -= 1 << (32 - bits_left);
		}
		else
		{
			if (num_buffered_bytes > 0)
			{
				append_byte(buffered_byte);
			}

			while (num_buffered_bytes > 1)
			{
				append_byte(0xff);
				num_buffered_bytes--;
			}
		}

		write_bits(low >> 8, 24 - bits_left);
	}


	void write_bits(int bits, int n)
	{
		vlc_buffer <<= n;
		vlc_buffer |= bits;
		vlc_buffer_len += n;

		while (vlc_buffer_len >= 8)
		{
			append_byte((vlc_buffer >> (vlc_buffer_len - 8)) & 0xFF);
			vlc_buffer_len -= 8;
		}
	}


	void reset()
	{
		data_size = 0;
		state = 0;

		vlc_buffer_len = 0;

		init_CABAC();
	}


	boolean modifies_context()
	{
		return true;
	}
}
