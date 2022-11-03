package org.terifan.compression.adaptivehuffman;

import java.io.IOException;
import java.io.InputStream;
import org.terifan.compression.io.BitOutputStream;


public class AdaptiveHuffmanEncoder
{
	private Model mModel;
	private BitOutputStream mOutput;
	private int mBitsPerSymbol;


	public AdaptiveHuffmanEncoder(BitOutputStream aOutput, int aBitsPerSymbol)
	{
		if (aBitsPerSymbol < 1 || aBitsPerSymbol > 30)
		{
			throw new IllegalArgumentException("aBitsPerSymbol must be between 1 and 30 bits.");
		}

		mBitsPerSymbol = aBitsPerSymbol;
		mModel = new Model();
		mOutput = aOutput;
	}


	public void encode(InputStream aInput) throws IOException
	{
		for (int c; (c = aInput.read()) != -1;)
		{
			encode(c);
		}
	}


	public void encode(int aSymbol) throws IOException
	{
		assert aSymbol >= 0 && aSymbol < (1 << mBitsPerSymbol);

		boolean newSymbol = !getCodeByTree(mModel.mRoot, aSymbol, 0, 0);

		if (newSymbol)
		{
			getCodeByTree(mModel.mRoot, Model.NEW, 0, 0);

			mOutput.writeBits(aSymbol, mBitsPerSymbol);
		}

		mModel.updateTree(aSymbol);
	}


	public void learn(int aSymbol) throws IOException
	{
		mModel.updateTree(aSymbol);
	}


	private boolean getCodeByTree(Node aNode, int aSymbol, int aCode, int aCodeLen) throws IOException
	{
		if (aNode.left != null || aNode.right != null)
		{
			if (aNode.left != null && getCodeByTree(aNode.left, aSymbol, aCode << 1, aCodeLen + 1))
			{
				return true;
			}
			if (aNode.right != null && getCodeByTree(aNode.right, aSymbol, (aCode << 1) + 1, aCodeLen + 1))
			{
				return true;
			}
		}
		else if (aSymbol == aNode.symbol)
		{
			mOutput.writeBits(aCode, aCodeLen);
			return true;
		}

		return false;
	}
}