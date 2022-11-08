package org.terifan.compression.cabac265;

import java.io.IOException;
import java.io.OutputStream;
import static org.terifan.compression.cabac265.CabacConstants.*;


public class CabacEncoder265
{
	private OutputStream mOutputStream;
	private CabacModel[] mCtxModels;

	// VLC
	private int vlc_buffer;
	private int vlc_buffer_len;

	// CABAC
	private int range;
	private int low;
	private int bits_left;
	private int buffered_byte;
	private int num_buffered_bytes;


	public CabacEncoder265(OutputStream aOutputStream)
	{
		mOutputStream = aOutputStream;

		range = 510;
		low = 0;

		bits_left = 23;
		buffered_byte = 0xFF;
		num_buffered_bytes = 0;
	}


	public void setContextModels(CabacModel[] models)
	{
		mCtxModels = models;
	}


	public void write_bit(int bit) throws IOException
	{
		write_bits(bit, 1);
	}


	public void write_CABAC_TU_bypass(int value, int cMax) throws IOException
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


	public void write_CABAC_FL_bypass(int value, int n) throws IOException
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


	public void write_CABAC_EGk(int val, int k) throws IOException
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


	public void write_CABAC_EGk(int val, int k, CabacModel[] aModel) throws IOException
	{
		int i = 0;

		while (val >= (1 << k))
		{
			write_CABAC_bit(aModel[i++], 1);
			val = val - (1 << k);
			k++;
		}

		write_CABAC_bit(aModel[i], 0);

		while (k > 0)
		{
			k--;
			write_CABAC_bypass((val >>> k) & 1);
		}
	}


	public void write_CABAC_EGk(int val, int k, CabacModel aModel) throws IOException
	{
		int i = 0;

		while (val >= (1 << k))
		{
			write_CABAC_bit(aModel, 1);
			val = val - (1 << k);
			k++;
		}

		write_CABAC_bit(aModel, 0);

		while (k > 0)
		{
			k--;
			write_CABAC_bypass((val >>> k) & 1);
		}
	}


	public void write_uvlc(int value) throws IOException
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


	public void write_svlc(int value) throws IOException
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


	public void add_trailing_bits() throws IOException
	{
		write_bit(1);
		int nZeros = number_free_bits_in_byte();
		write_bits(0, nZeros);
	}


	public void encodeFinal(int bit) throws IOException
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


	public void write_CABAC_bypass(int bin) throws IOException
	{
		low <<= 1;

		if (bin != 0)
		{
			low += range;
		}
		bits_left--;

		testAndWriteOut();
	}


	public void testAndWriteOut() throws IOException
	{
		if (bits_left < 12)
		{
			write_out();
		}
	}


	public void write_CABAC_bit(CabacModel model, int bin) throws IOException
	{
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


	public void write_out() throws IOException
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
	public void flush_VLC() throws IOException
	{
		while (vlc_buffer_len >= 8)
		{
			append_byte((vlc_buffer >>> (vlc_buffer_len - 8)) & 0xFF);
			vlc_buffer_len -= 8;
		}

		if (vlc_buffer_len > 0)
		{
			append_byte(vlc_buffer << (8 - vlc_buffer_len));
			vlc_buffer_len = 0;
		}

		vlc_buffer = 0;
	}


	public void skip_bits(int nBits) throws IOException
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


	public int number_free_bits_in_byte()
	{
		if ((vlc_buffer_len % 8) == 0)
		{
			return 0;
		}
		return 8 - (vlc_buffer_len % 8);
	}


	public void append_byte(int byte_) throws IOException
	{
		mOutputStream.write(byte_);
	}


	public void stopEncoding() throws IOException
	{
		if ((low >>> (32 - bits_left)) != 0)
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

		write_bits(low >>> 8, 24 - bits_left);
	}


	public void write_bits(int bits, int n) throws IOException
	{
		vlc_buffer <<= n;
		vlc_buffer |= bits;
		vlc_buffer_len += n;

		while (vlc_buffer_len >= 8)
		{
			append_byte((vlc_buffer >>> (vlc_buffer_len - 8)) & 0xFF);
			vlc_buffer_len -= 8;
		}
	}
}
