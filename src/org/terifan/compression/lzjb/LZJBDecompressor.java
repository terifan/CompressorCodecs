package org.terifan.compression.lzjb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import static org.terifan.compression.lzjb.LZJBCompressor.MATCH_MIN;
import org.terifan.compression.vp8arithmetic.VP8Context;
import org.terifan.compression.vp8arithmetic.VP8Decoder;


public class LZJBDecompressor
{
	private byte[] mWindow;
	private int mWindowOffset;
	private VP8Decoder mReader;
	private VP8Context mContext0;
	private VP8Context mContext1;
	private VP8Context mContext2;
	private VP8Context mContext3;


	public LZJBDecompressor(InputStream aInputStream)
	{
		mWindow = new byte[1024 * 1024];
		Arrays.fill(mWindow, (byte)' ');

		mContext0 = new VP8Context();
		mContext1 = new VP8Context();
		mContext2 = new VP8Context();
		mContext3 = new VP8Context();

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
			if (mReader.decodeBit(mContext0) == 0)
			{
				int count = mReader.decodeUnary(mContext1) + 1;

				for (int i = 0; i <= count; i++)
				{
					byte c = (byte)mReader.decodeValue(8);
					aOutput[aOutputOffset++] = c;
					mWindow[mWindowOffset++] = c;
					aOutputLength--;
				}
			}
			else
			{
				int len = (int)mReader.decodeExpGolomb(0, mContext2) + MATCH_MIN;
				int dist = (int)mReader.decodeExpGolomb(7, mContext3);

				int offset = mWindowOffset - dist - len;
				System.arraycopy(mWindow, offset, aOutput, aOutputOffset, len);
				System.arraycopy(mWindow, offset, mWindow, mWindowOffset, len);
				mWindowOffset += len;
				aOutputOffset += len;
				aOutputLength -= len;
			}
		}

		return aOutput;
	}
}
