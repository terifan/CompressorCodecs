package org.terifan.compression.rans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class InMemoryEncoder
{
	private ByteArrayOutputStream mOutput;
	private RANSEncoder mEncoder;


	public InMemoryEncoder(SymbolStatistics aStats)
	{
		mOutput = new ByteArrayOutputStream();
		mEncoder = new RANSEncoder(mOutput, aStats);
	}


	public void write(int aChar)
	{
		mEncoder.write(aChar);
	}


	public byte[] finish() throws IOException
	{
		mEncoder.finish();
		byte[] codedBytes = mOutput.toByteArray();
		reverse(codedBytes);
		return codedBytes;
	}


	private void reverse(byte[] aBytes)
	{
		int N = aBytes.length;
		int NBy2 = N / 2;
		for (int i = 0; i < NBy2; i++)
		{
			byte tmp = aBytes[i];
			aBytes[i] = aBytes[N - 1 - i];
			aBytes[N - 1 - i] = tmp;
		}
	}
}
