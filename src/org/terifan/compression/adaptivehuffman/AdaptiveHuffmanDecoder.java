package org.terifan.compression.adaptivehuffman;

import java.io.IOException;
import java.io.OutputStream;
import org.terifan.compression.io.BitInputStream;


public class AdaptiveHuffmanDecoder
{
	private Model mModel;
	private int mBitsPerSymbol;
	private boolean mFirst;


	public AdaptiveHuffmanDecoder(int aBitsPerSymbol)
	{
		if (aBitsPerSymbol < 1 || aBitsPerSymbol > 30)
		{
			throw new IllegalArgumentException("aBitsPerSymbol must be between 1 and 30 bits.");
		}

		mBitsPerSymbol = aBitsPerSymbol;
		mModel = new Model();
		mFirst = true;
	}


	public void decode(BitInputStream aInput, OutputStream aOutput, int aLength) throws IOException
	{
		for (int i = 0; i < aLength; i++)
		{
			aOutput.write(decode(aInput));
		}
	}


	public int decode(BitInputStream aInput) throws IOException
	{
		if (mFirst)
		{
			int symbol = (int)aInput.readBits(mBitsPerSymbol);
			mModel.updateTree(symbol);
			mFirst = false;
			return symbol;
		}

		Node node = mModel.mRoot;

		for (;;)
		{
			if (aInput.readBit() == 0)
			{
				node = node.left;
			}
			else
			{
				node = node.right;
			}

			int symbol = node.symbol;

			if (symbol != -1)
			{
				if (symbol == Model.NEW)
				{
					symbol = (int)aInput.readBits(mBitsPerSymbol);
				}

				mModel.updateTree(symbol);
				return symbol;
			}
		}
	}


	public void learn(int aSymbol) throws IOException
	{
		mModel.updateTree(aSymbol);
		mFirst = false;
	}
}
