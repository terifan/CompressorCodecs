package org.terifan.compression.lzjb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import static org.terifan.compression.lzjb.LZJBCompressor.MATCH_MIN;
import org.terifan.compression.vp8arithmetic.VP8Context;
import org.terifan.compression.vp8arithmetic.VP8Decoder;
import org.terifan.util.Console;


public class LZJBDecompressor
{
	private byte[] mWindow;
	private int mWindowOffset;

	private VP8Decoder mReader;
	private VP8Context mContext0;
	private VP8Context mContext1;
	private VP8Context mContext2;
	private VP8Context mContext3;
	private VP8Context mContext4;
	private SymbolFrequency[] mSymbolFrequencys;
	private int mPreviousSym;


	public LZJBDecompressor(InputStream aInputStream)
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

		mReader = new VP8Decoder(aInputStream);
	}


	public byte[] decompress(int aOutputLength) throws IOException
	{
		return decompress(new byte[aOutputLength], 0, aOutputLength);
	}


	public byte[] decompress(byte[] aOutput, int aOutputOffset, int aOutputLength) throws IOException
	{
		while (aOutputLength > 0)
		{
			int len = mReader.decodeUnary(mContext0);
			int S = 8;
			if (len >= S)
			{
				len += mReader.decodeExpGolomb(0, mContext1);
			}

			if (len > 0)
			{
				Console.println("{lit:}{}", len);

				for (int i = 0; i < len; i++)
				{
					int sym = mSymbolFrequencys[mPreviousSym].decode((int)mReader.decodeExpGolomb(3, mContext4));
					mSymbolFrequencys[mPreviousSym].update(sym);
					mPreviousSym = sym;

					mWindow[mWindowOffset + i] = (byte)sym;
				}

				System.arraycopy(mWindow, mWindowOffset, aOutput, aOutputOffset, len);

				mWindowOffset += len;
				aOutputOffset += len;
				aOutputLength -= len;
			}

			if (aOutputLength > 0)
			{
				len = (int)(mReader.decodeExpGolomb(0, mContext2) + MATCH_MIN);
				int dist = (int)mReader.decodeExpGolomb(0, mContext3);

				Console.println("{ref:}{} +{}", dist, len);

				int offset = mWindowOffset - dist - len;
				System.arraycopy(mWindow, offset, aOutput, aOutputOffset, len);
				System.arraycopy(mWindow, offset, mWindow, mWindowOffset, len);

				for (int i = 0; i < len; i++)
				{
					int sym = 0xff & mWindow[offset + i];
					mSymbolFrequencys[mPreviousSym].update(sym);
					mPreviousSym = sym;
				}

				mWindowOffset += len;
				aOutputOffset += len;
				aOutputLength -= len;
			}
		}

		mReader.restart();

		return aOutput;
	}
}
