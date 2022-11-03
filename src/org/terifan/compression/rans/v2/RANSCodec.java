package org.terifan.compression.rans.v2;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static org.terifan.compression.rans.v2.SymbolStats.LOG2NSYMS;


public class RANSCodec
{
	private final static int RANS_BYTE_L = 1 << 23;  // lower bound of our normalization interval


	private static class RansState
	{
		int state;


		public RansState()
		{
		}


		public RansState(int aState)
		{
			this.state = aState;
		}
	}
			RANSCodec.RansState r = new RANSCodec.RansState();

	// ---- rANS encoding/decoding with alias table

	void RansEncPutAlias(OutputStream pptr, SymbolStats syms, int s) throws IOException
	{
		int scale_bits = syms.prob_bits;

		// renormalize
		int freq = syms.freqs[s];
		RansState x = RansEncRenorm(pptr, freq, scale_bits);

		// x = C(s,x)
		// NOTE: alias_remap here could be replaced with e.g. a binary search.
		r.state = ((x.state / freq) << scale_bits) + syms.alias_remap[(x.state % freq) + syms.cum_freqs[s]];
	}


	int RansDecGetAlias(SymbolStats syms)
	{
		RansState x = r;

		int scale_bits = syms.prob_bits;

		// figure out symbol via alias table
		int mask = (1 << scale_bits) - 1; // constant for fixed scale_bits!
		int xm = x.state & mask;
		int bucket_id = xm >> (scale_bits - LOG2NSYMS);
		int bucket2 = bucket_id * 2;
		if (xm < syms.divider[bucket_id])
		{
			bucket2++;
		}

		// s, x = D(x)
		r.state = syms.slot_freqs[bucket2] * (x.state >> scale_bits) + xm - syms.slot_adjust[bucket2];
		return syms.sym_id[bucket2];
	}


	void RansEncInit()
	{
		r.state = RANS_BYTE_L;
	}


	private RansState RansEncRenorm(OutputStream pptr, int freq, int scale_bits) throws IOException
	{
		int x_max = ((RANS_BYTE_L >> scale_bits) << 8) * freq;
		RansState x = r;
		if (r.state >= x_max)
		{
			x = new RansState(x.state);
			do
			{
				pptr.write(x.state & 0xff);
				x.state >>= 8;
			}
			while (x.state >= x_max);
		}
		return x;
	}


	// Encodes a single symbol with range start "start" and frequency "freq".
	// All frequencies are assumed to sum to "1 << scale_bits", and the
	// resulting bytes get written to ptr (which is updated).
	//
	// NOTE: With rANS, you need to encode symbols in *reverse order*, i.e. from
	// beginning to end! Likewise, the output bytestream is written *backwards*:
	// ptr starts pointing at the end of the output buffer and keeps decrementing.
	void RansEncPut(OutputStream pptr, int start, int freq, int scale_bits) throws IOException
	{
		int x = r.state;

		// renormalize
		RansEncRenorm(pptr, freq, scale_bits);

		// x = C(s,x)
		r.state = ((x / freq) << scale_bits) + (x % freq) + start;
	}


	// Flushes the rANS encoder.
	void RansEncFlush(OutputStream pptr) throws IOException
	{
		int x = r.state;

		pptr.write(x >> 24);
		pptr.write(x >> 16);
		pptr.write(x >> 8);
		pptr.write(x >> 0);
	}


	// Initializes a rANS decoder.
	// Unlike the encoder, the decoder works forwards as you'd expect.
	void RansDecInit(InputStream pptr) throws IOException
	{
		int x;

		x = pptr.read() << 0;
		x |= pptr.read() << 8;
		x |= pptr.read() << 16;
		x |= pptr.read() << 24;

		r.state = x;
	}


	// Returns the current cumulative frequency (map it to a symbol yourself!)
	int RansDecGet(int scale_bits)
	{
		return r.state & ((1 << scale_bits) - 1);
	}


	// Advances in the bit stream by "popping" a single symbol with range start
	// "start" and frequency "freq". All frequencies are assumed to sum to "1 << scale_bits",
	// and the resulting bytes get written to ptr (which is updated).
	void RansDecAdvance(InputStream pptr, int start, int freq, int scale_bits) throws IOException
	{
		int mask = (1 << scale_bits) - 1;

		// s, x = D(x)
		int x = r.state;
		x = freq * (x >> scale_bits) + (x & mask) - start;

		// renormalize
		if (x < RANS_BYTE_L)
		{
			do
			{
				int c = pptr.read();
				if (c == -1) throw new EOFException();
				x = (x << 8) | c;
			}
			while (x < RANS_BYTE_L);
		}

		r.state = x;
	}

	// --------------------------------------------------------------------------

	// That's all you need for a full encoder; below here are some utility
	// functions with extra convenience or optimizations.
	// Encoder symbol description
	// This (admittedly odd) selection of parameters was chosen to make
	// RansEncPutSymbol as cheap as possible.
	// Initializes an encoder symbol to start "start" and frequency "freq"
	static void RansEncSymbolInit(RansEncSymbol s, int start, int freq, int scale_bits)
	{
		assert scale_bits <= 16;
		assert start <= (1 << scale_bits);
		assert freq <= (1 << scale_bits) - start;

		// Say M := 1 << scale_bits.
		//
		// The original encoder does:
		//   x_new = (x/freq)*M + start + (x%freq)
		//
		// The fast encoder does (schematically):
		//   q     = mul_hi(x, rcp_freq) >> rcp_shift   (division)
		//   r     = x - q*freq                         (remainder)
		//   x_new = q*M + bias + r                     (new x)
		// plugging in r into x_new yields:
		//   x_new = bias + x + q*(M - freq)
		//        =: bias + x + q*cmpl_freq             (*)
		//
		// and we can just precompute cmpl_freq. Now we just need to
		// set up our parameters such that the original encoder and
		// the fast encoder agree.
		s.x_max = ((RANS_BYTE_L >> scale_bits) << 8) * freq;
		s.cmpl_freq = (int)((1 << scale_bits) - freq);
		if (freq < 2)
		{
			// freq=0 symbols are never valid to encode, so it doesn't matter what
			// we set our values to.
			//
			// freq=1 is tricky, since the reciprocal of 1 is 1; unfortunately,
			// our fixed-point reciprocal approximation can only multiply by values
			// smaller than 1.
			//
			// So we use the "next best thing": rcp_freq=0xffffffff, rcp_shift=0.
			// This gives:
			//   q = mul_hi(x, rcp_freq) >> rcp_shift
			//     = mul_hi(x, (1<<32) - 1)) >> 0
			//     = floor(x - x/(2^32))
			//     = x - 1 if 1 <= x < 2^32
			// and we know that x>0 (x=0 is never in a valid normalization interval).
			//
			// So we now need to choose the other parameters such that
			//   x_new = x*M + start
			// plug it in:
			//     x*M + start                   (desired result)
			//   = bias + x + q*cmpl_freq        (*)
			//   = bias + x + (x - 1)*(M - 1)    (plug in q=x-1, cmpl_freq)
			//   = bias + 1 + (x - 1)*M
			//   = x*M + (bias + 1 - M)
			//
			// so we have start = bias + 1 - M, or equivalently
			//   bias = start + M - 1.
			s.rcp_freq = Integer.MAX_VALUE;
			s.rcp_shift = 0;
			s.bias = start + (1 << scale_bits) - 1;
		}
		else
		{
			// Alverson, "Integer Division using reciprocals"
			// shift=ceil(log2(freq))
			int shift = 0;
			while (freq > (1 << shift))
			{
				shift++;
			}

			s.rcp_freq = (int)(((1L << (shift + 31)) + freq - 1) / freq);
			s.rcp_shift = shift - 1;

			// With these values, 'q' is the correct quotient, so we
			// have bias=start.
			s.bias = start;
		}
	}


	// Initialize a decoder symbol to start "start" and frequency "freq"
	static void RansDecSymbolInit(RansDecSymbol s, int start, int freq)
	{
		assert start <= (1 << 16);
		assert freq <= (1 << 16) - start;
		s.start = (int)start;
		s.freq = (int)freq;
	}


	// Encodes a given symbol. This is faster than straight RansEnc since we can do
	// multiplications instead of a divide.
	//
	// See RansEncSymbolInit for a description of how this works.
	void RansEncPutSymbol(OutputStream pptr, RansEncSymbol sym) throws IOException
	{
		assert sym.x_max != 0; // can't encode symbol with freq=0

		// renormalize
		int x = r.state;
		int x_max = sym.x_max;
		if (x >= x_max)
		{
			do
			{
				pptr.write(x & 0xff);
				x >>= 8;
			}
			while (x >= x_max);
		}

		// x = C(s,x)
		// NOTE: written this way so we get a 32-bit "multiply high" when
		// available. If you're on a 64-bit platform with cheap multiplies
		// (e.g. x64), just bake the +32 into rcp_shift.
		int q = (int)(((long)x * sym.rcp_freq) >> 32) >> sym.rcp_shift;
		r.state = x + sym.bias + q * sym.cmpl_freq;
	}


	// Equivalent to RansDecAdvance that takes a symbol.
	void RansDecAdvanceSymbol(InputStream pptr, RansDecSymbol sym, int scale_bits) throws IOException
	{
		RansDecAdvance(pptr, sym.start, sym.freq, scale_bits);
	}


	// Advances in the bit stream by "popping" a single symbol with range start
	// "start" and frequency "freq". All frequencies are assumed to sum to "1 << scale_bits".
	// No renormalization or output happens.
	void RansDecAdvanceStep(int start, int freq, int scale_bits)
	{
		int mask = (1 << scale_bits) - 1;

		// s, x = D(x)
		int x = r.state;
		r.state = freq * (x >> scale_bits) + (x & mask) - start;
	}


	// Equivalent to RansDecAdvanceStep that takes a symbol.
	void RansDecAdvanceSymbolStep(RansDecSymbol sym, int scale_bits)
	{
		RansDecAdvanceStep(sym.start, sym.freq, scale_bits);
	}


	// Renormalize.
	void RansDecRenorm(InputStream pptr) throws IOException
	{
		// renormalize
		int x = r.state;
		if (x < RANS_BYTE_L)
		{
			do
			{
				x = (x << 8) | pptr.read();
			}
			while (x < RANS_BYTE_L);
		}

		r.state = x;
	}
}