package org.terifan.compression.lzjb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import org.terifan.compression.vp8arithmetic.VP8Context;
import org.terifan.compression.vp8arithmetic.VP8Encoder;
import org.terifan.util.Console;


public class LZJBCompressor implements AutoCloseable
{
	final static int MATCH_MIN = 4;
//	final static int WINDOW_SIZE = 65536;

	private byte[] mWindow;
	private int mWindowOffset;

	private VP8Encoder mWriter;
	private VP8Context mContext0;
	private VP8Context mContext1;
	private VP8Context mContext2;
	private VP8Context mContext3;
	private VP8Context mContext4;
	private SymbolFrequency[] mSymbolFrequencys;
	private int mPreviousSym;


	public LZJBCompressor(OutputStream aOutputStream)
	{
		mWindow = new byte[1024 * 1024];

		mContext0 = new VP8Context();
		mContext1 = new VP8Context();
		mContext2 = new VP8Context();
		mContext3 = new VP8Context();
		mContext4 = new VP8Context();

		mSymbolFrequencys = new SymbolFrequency[256];
		for (int i = 0; i < 256; i++)
		{
			mSymbolFrequencys[i] = new SymbolFrequency(256, 1000000);
		}

		for (int i = 32; i < 128; i++)
		{
			for (int j = 32; j < 128; j++)
			{
				mSymbolFrequencys[i].update(j);
			}
		}

		mWriter = new VP8Encoder(aOutputStream);
	}


	@Override
	public void close() throws Exception
	{
	}


	public void compress(byte[] aInput) throws IOException
	{
		byte[] literals = new byte[1024];
		int literalCount = 0;

		Match result = new Match();
		for (int offset = 0; offset < aInput.length;)
		{
			findBestMatch(aInput, offset, result);

			if (result.length < MATCH_MIN)
			{
				if (literalCount == literals.length)
				{
					literals = Arrays.copyOfRange(literals, 0, literalCount * 3 / 2);
				}

				byte b = aInput[offset];
				mWindow[mWindowOffset++] = b;
				literals[literalCount++] = b;
				offset++;
			}
			else
			{
				Console.println("{lit:}{}", literalCount);

				if (offset == 0)
				{
				}

				writeLiterals(literals, literalCount);
				literalCount = 0;

				Console.println("{ref:}{} +{}", result.dist, result.length);

				System.arraycopy(aInput, offset, mWindow, mWindowOffset, result.length);
				mWindowOffset += result.length;

				writeReference(result.dist, result.length);

				for (int i = 0; i < result.length; i++)
				{
					int sym = aInput[offset + i];
					mSymbolFrequencys[mPreviousSym].update(sym);
					mPreviousSym = sym;
				}

				offset += result.length;
			}
		}

		if (literalCount > 0)
		{
			Console.println("{lit:}{}", literalCount);

			writeLiterals(literals, literalCount);
		}

		mWriter.restart();
	}


	private void findBestMatch(byte[] aInput, int aInputOffset, Match aResult)
	{
		int wsr = mWindowOffset;
		int wsl = 0;//Math.max(0, mWindowOffset - WINDOW_SIZE);
		int is = aInput.length;
		aResult.length = 0;
		aResult.dist = 0;
		for (int windowOffset = wsr - MATCH_MIN + 1; windowOffset >= wsl; windowOffset--)
		{
			int match = scan(windowOffset, aInputOffset, wsr, is, aInput, mWindow);
			if (match >= MATCH_MIN && match > aResult.length)
			{
				aResult.length = match;
				aResult.dist = wsr - windowOffset - match;
			}
		}
	}


	private static int scan(int aOffset, int aStartOffset, int aWs, int aIs, byte[] aInput, byte[] aWindow)
	{
		int match = 0;
		for (int wpos = aOffset, ipos = aStartOffset; wpos < aWs && ipos < aIs; ipos++, wpos++, match++)
		{
			if (aWindow[wpos] != aInput[ipos])
			{
				break;
			}
		}
		return match;
	}


	private void writeLiterals(byte[] aLiterals, int aLength) throws IOException
	{
//		mWriter.encodeBit(aLength == 0 ? 0 : 1, 0, mContext0);
//		if (aLength > 0)
//		{
//			mWriter.encodeExpGolomb(aLiteralCount, 0, mContext1);
//		}

		int S = 8;
		mWriter.encodeUnary(Math.min(aLength, S), mContext0);
		if (aLength >= S)
		{
			mWriter.encodeExpGolomb(aLength - S, 0, mContext1);
		}

		for (int i = 0; i < aLength; i++)
		{
			int sym = 0xff & aLiterals[i];

			mWriter.encodeExpGolomb(mSymbolFrequencys[mPreviousSym].encode(sym), 3, mContext4);

			mSymbolFrequencys[mPreviousSym].update(sym);
			mPreviousSym = sym;
		}
	}


	private void writeReference(int aDist, int aLength) throws IOException
	{
		mWriter.encodeExpGolomb(aLength - MATCH_MIN, 0, mContext2);
		mWriter.encodeExpGolomb(aDist, 0, mContext3);
	}


	private static class Match
	{
		int dist, length;


		@Override
		public String toString()
		{
			return "[" + dist + " +" + length + "]";
		}
	}
}
