package org.terifan.compression.lzjb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import org.terifan.compression.vp8arithmetic.VP8Context;
import org.terifan.compression.vp8arithmetic.VP8Encoder;

// 78305

public class LZJBCompressor implements AutoCloseable
{
	final static int MATCH_MIN = 4;
	final static int WINDOW_SIZE = 65536;

	private byte[] mWindow;
	private int mWindowOffset;

	private VP8Encoder mWriter;
	private VP8Context mContext0;
	private VP8Context mContext1;
	private VP8Context mContext2;
	private VP8Context mContext3;


	public LZJBCompressor(OutputStream aOutputStream)
	{
		mWindow = new byte[1024 * 1024];

		mContext0 = new VP8Context();
		mContext1 = new VP8Context();
		mContext2 = new VP8Context();
		mContext3 = new VP8Context();

		mWriter = new VP8Encoder(aOutputStream);
	}


	@Override
	public void close() throws Exception
	{
		mWriter.close();
	}


	public void compress(byte[] aInput) throws IOException
	{
		byte[] literals = new byte[MATCH_MIN];
		int literalCount = 0;

		Match result = new Match();
		for (int offset = 0; offset < aInput.length;)
		{
			findBestMatch(aInput, offset, result);

			if (literalCount == MATCH_MIN || literalCount > 0 && result.length >= MATCH_MIN)
			{
				writeLiterals(literals, literalCount);
				literalCount = 0;
			}

			if (result.length < MATCH_MIN)
			{
				byte b = aInput[offset++];
				mWindow[mWindowOffset++] = b;
				literals[literalCount++] = b;
			}
			else
			{
				result.length = Math.min(result.length, 255);

				System.arraycopy(aInput, offset, mWindow, mWindowOffset, result.length);
				mWindowOffset += result.length;

				writeReference(result.dist, result.length);

				offset += result.length;
			}
		}

		if (literalCount > 0)
		{
			writeLiterals(literals, literalCount);
		}

//		System.out.println("*".repeat(100));
//		Log.hexDump(Arrays.copyOfRange(mWindow, 0, mWindowOffset),24);
//		System.out.println("*".repeat(100));
	}


	private void findBestMatch(byte[] aInput, int aInputOffset, Match aResult)
	{
		int wsr = mWindowOffset;
		int wsl = Math.max(0, mWindowOffset - WINDOW_SIZE);
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


	private void writeLiterals(byte[] aLiterals, int aLiteralCount) throws IOException
	{
//		System.out.print(","+aLiteralCount +",");
		mWriter.encodeBit(0, 0, mContext0);
		mWriter.encodeUnary(aLiteralCount - 1, mContext1);
		for (int i = 0; i < aLiteralCount; i++)
		{
			mWriter.encodeValue(0xff & aLiterals[i], 8);
		}
	}


	private void writeReference(int aDist, int aLength) throws IOException
	{
//		System.out.print("["+aDist+" "+(aLength - MATCH_MIN) +"]");
		mWriter.encodeBit(1, 0, mContext0);
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
