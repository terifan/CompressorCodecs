package org.terifan.compression.lzw;

import java.io.IOException;
import java.io.OutputStream;


public class LZWOutputStream extends OutputStream
{
	// stream
	private int mBitBufferLength;
	private int mBitBuffer;
	private OutputStream mOutputStream;
	// tree
	private Node[] mRootNodes;
	private Node mCurrentNode;
	private Node[] mCachedNodes;
	private int mNextFreeNodeIndex;
	// encoder
	private int mClearCode;
	private int mCodeLength;
	private int mDictionarySizeBits;
	private int mEOFCode;
	private int mInitialCodeSizeBits;
	private int mNextCode;
	private int mSymbolSize;
	private int mSwapCodeSize;


	/**
	 * Constructs a LZWOutputStream with symbol size of 8 bits and a dictionary
	 * size of 4096 entries.
	 */
	public LZWOutputStream(OutputStream aOutputStream) throws IOException
	{
		this(aOutputStream, 8, 4096, true);
	}


	/**
	 * Constructs a LZWOutputStream with symbol size of 8 bits and a dictionary
	 * size of 4096 entries.
	 */
	public LZWOutputStream(OutputStream aOutputStream, boolean aHeader) throws IOException
	{
		this(aOutputStream, 8, 4096, aHeader);
	}


	/**
	 * Constructs a LZWOutputStream with custom code size and dictionary size.
	 *
	 * The dictionary size controls compression efficiency; text-files can 
	 * be compressed with larger dictionaries (eg. 8192) while binary data such as an 
	 * executable should have a smaller dictionary (eg. 2048).
	 *
	 * @param aOutputStream
	 *    Underlaying output stream.
	 * @param aSymbolSize
	 *    Number of symbols that should be encoded. This value may range from 2 
	 *    to 4096.
	 * @param aDictionarySize
	 *    Number of entries in the dictionary. This value must be a power of 2 
	 *    and range from 256 to 131072. Note: The dictionary size must be 
	 *    greater than the symbol size.
	 */
	public LZWOutputStream(OutputStream aOutputStream, int aSymbolSize, int aDictionarySize, boolean aHeader) throws IOException
	{
		if (aSymbolSize < 1 || aSymbolSize > 12)
		{
			throw new IllegalArgumentException("aSymbolSize must be in range of 1 to 12 bits. Provided value is: " + aSymbolSize);
		}

		mOutputStream = aOutputStream;
		mDictionarySizeBits = (int) Math.ceil(Math.log(aDictionarySize) / Math.log(2));
		mSymbolSize = 1 << aSymbolSize;
		mInitialCodeSizeBits = (int) Math.ceil(Math.log(mSymbolSize + 2) / Math.log(2));

		if ((aDictionarySize & -aDictionarySize) != aDictionarySize)
		{
			throw new IllegalArgumentException("Dictionary size must be a power of 2: " + aDictionarySize);
		}
		if (mDictionarySizeBits < 8)
		{
			throw new IllegalArgumentException("Dictionary size is less than minimum allowed size: " + aDictionarySize);
		}
		if (mDictionarySizeBits > 17)
		{
			throw new IllegalArgumentException("Dictionary size exceeds maximum allowed size: " + aDictionarySize);
		}
		if ((mSymbolSize + 2) > aDictionarySize)
		{
			throw new IllegalArgumentException("Dictionary size must be increased to accommodate the number of symbols.");
		}

		mRootNodes = new Node[mSymbolSize];
		mClearCode = mSymbolSize;
		mEOFCode = mSymbolSize + 1;
		mCodeLength = mInitialCodeSizeBits;

		if (aHeader)
		{
			int a = mSymbolSize - 2;
			int b = (0xF0 & ((mSymbolSize - 2) >> 4)) | (mDictionarySizeBits - 2);
			int c = ((a ^ b) ^ ((a ^ b) << 4)) & 0xF0;
			int v = 1;
			mOutputStream.write((byte)(c | v)); // checksum + version
			mOutputStream.write((byte)a); // number of symbols
			mOutputStream.write((byte)b); // number of symbols + dictionary size
		}

		mCachedNodes = new Node[aDictionarySize];
		for (int i = mCachedNodes.length; --i >= 0;)
		{
			mCachedNodes[i] = new Node();
		}

		for (int i = mSymbolSize; --i >= 0;)
		{
			mRootNodes[i] = mCachedNodes[i].init(i);
		}

		clear();
	}


	/**
	 * Writes remaining compressed data to the output stream and closes the 
	 * underlying stream.
	 */
	@Override
	public void close() throws IOException
	{
		finish();

		mOutputStream.close();
		mOutputStream = null;
	}


	/**
	 * Finishes writing compressed data to the output stream without closing 
	 * the underlying stream.<p>
	 *
	 * It' not neccessary to close the stream if this metod is called.
	 */
	public void finish() throws IOException
	{
		writeBits(mCurrentNode.code, mCodeLength);
		writeBits(mEOFCode, mCodeLength);

		while (mBitBufferLength > 0)
		{
			mOutputStream.write(mBitBuffer & 255);
			mBitBuffer >>>= 8;
			mBitBufferLength -= 8;
		}
	}


	/**
	 * Writes a byte to the compressed output stream. The general contract 
	 * for write is that one byte is written to the output stream. The byte 
	 * to be written is the eight low-order bits of the argument b. The 24 
	 * high-order bits of b are ignored.<p>
	 *
	 * Note: Use the writeSymbol method to write codes greater than 255.
	 *
	 * @param b
	 *    The byte value.
	 */
	@Override
	public void write(int b) throws IOException
	{
		writeSymbol(b & 255);
	}


	/**
	 * Writes b.length bytes from the specified byte array to the compressed 
	 * output stream. The general contract for write(b) is that it should have 
	 * exactly the same effect as the call write(b, 0, b.length)<p>
	 *
	 * Note: Use the writeSymbol method to write codes greater than 255.
	 *
	 * @param b
	 *    The byte values.
	 */
	@Override
	public void write(byte[] b) throws IOException
	{
		for (int i = 0, len = b.length; i < len; i++)
		{
			writeSymbol(b[i] & 255);
		}
	}


	/**
	 * Writes len bytes from the specified byte array starting at offset off 
	 * to this output stream.<p>
	 *
	 * Note: Use the writeSymbol method to write codes greater than 255.
	 *
	 * @param b
	 *    The byte values.
	 * @param off
	 *    The start offset in the data.
	 * @param len
	 *    The number of bytes to write.
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		while (len-- > 0)
		{
			writeSymbol(b[off++] & 255);
		}
	}


	/**
	 * Writes the specified symbol to the compressed output stream.
	 *
	 * @param aValue
	 *    The symbol value.
	 */
	public void writeSymbol(int aValue) throws IOException
	{
		if (aValue < 0 || aValue >= mSymbolSize)
		{
			throw new IllegalArgumentException("Provided code exceeds maximum code value (" + mSymbolSize + "): " + aValue);
		}

		Node nextNode;

		if (mCurrentNode != null)
		{
			nextNode = mCurrentNode.subnodes[aValue];
		}
		else
		{
			nextNode = mRootNodes[aValue];
		}

		if (nextNode == null)
		{
			writeBits(mCurrentNode.code, mCodeLength);

			if (mNextCode == mSwapCodeSize)
			{
				if (mCodeLength == mDictionarySizeBits)
				{
					writeBits(mClearCode, mCodeLength);
					clear();
				}
				else
				{
					mSwapCodeSize <<= 1;
					mCodeLength++;
				}
			}

			if (mCurrentNode != null)
			{
				mCurrentNode.subnodes[aValue] = mCachedNodes[mNextFreeNodeIndex++].init(mNextCode++);
			}

			mCurrentNode = mRootNodes[aValue];
		}
		else
		{
			mCurrentNode = nextNode;
		}
	}


	public void learnWord(String aWord) throws IOException
	{
		// ap
		// app
		// appl
		// apple

		Node node = mRootNodes[aWord.charAt(0)];
		
		for (int i = 1; i < aWord.length(); i++)
		{
			int c = aWord.charAt(i);

			Node nextNode = node.subnodes[c];

			if (nextNode == null)
			{
				Node newNode = mCachedNodes[mNextFreeNodeIndex++].init(mNextCode++);
				node.subnodes[c] = newNode;
				nextNode = newNode;
			}

			node = nextNode;
		}
	}


	/**
	 * Clears the encoder state, called every time the codebook becomes full.
	 */
	private void clear()
	{
		for (int index = 0; index < mSymbolSize; index++)
		{
			mRootNodes[index].clear();
		}

		mNextCode = mEOFCode + 1;
		mCodeLength = mInitialCodeSizeBits;
		mSwapCodeSize = 1 << mCodeLength;
		mNextFreeNodeIndex = 256;

		mCurrentNode = null;
	}


	/**
	 * Writes a variable length code the output stream. The code is written to 
	 * an internal buffer and only written to output when the buffer is full. 
	 */
	private void writeBits(int aCode, int aLength) throws IOException
	{
		mBitBuffer |= aCode << mBitBufferLength;
		mBitBufferLength += aLength;

		while (mBitBufferLength >= 8)
		{
			mOutputStream.write(mBitBuffer & 255);
			mBitBuffer >>>= 8;
			mBitBufferLength -= 8;
		}
	}


	/**
	 * Class used in the LZW compression.
	 */
	private class Node
	{
		int code;
		Node[] subnodes;


		private Node()
		{
			subnodes = new Node[mSymbolSize];
		}


		Node init(int code)
		{
			this.code = code;
			for (int i = mSymbolSize; --i >= 0;)
			{
				subnodes[i] = null;
			}
			return this;
		}


		void clear()
		{
			for (int i = mSymbolSize; --i >= 0;)
			{
				subnodes[i] = null;
			}
		}
	}
}
