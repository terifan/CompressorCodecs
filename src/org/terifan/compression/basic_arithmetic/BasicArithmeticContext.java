package org.terifan.compression.basic_arithmetic;


public class BasicArithmeticContext
{
	private final boolean mAdaptive;

	int mSymbolCount;
	int[] mCharToSymbol;
	int[] mSymbolToChar;
	int[] mSymbolFreq; // frequency for symbols
	int[] mSymbolCum; // cumulative freq for symbols


	public BasicArithmeticContext(int aSymbolCount, boolean aAdaptive)
	{
		this(aSymbolCount, aAdaptive, null);
	}


	public BasicArithmeticContext(int[] aInitialFrequencies, boolean aAdaptive)
	{
		this(aInitialFrequencies.length, aAdaptive, aInitialFrequencies);
	}


	private BasicArithmeticContext(int aSymbolCount, boolean aAdaptive, int[] aInitialFrequencies)
	{
		mAdaptive = aAdaptive;
		mSymbolCount = aSymbolCount;

		mCharToSymbol = new int[mSymbolCount];
		mSymbolToChar = new int[mSymbolCount + 1];
		mSymbolFreq = new int[mSymbolCount + 1];
		mSymbolCum = new int[mSymbolCount + 1];

		mSymbolCum[mSymbolCount] = 0;
		for (int symbol = mSymbolCount; symbol >= 1; symbol--)
		{
			int character = symbol - 1;
			mCharToSymbol[character] = symbol;
			mSymbolToChar[symbol] = character;
			mSymbolFreq[symbol] = aInitialFrequencies == null ? 1 : 1 + aInitialFrequencies[character];
			mSymbolCum[symbol - 1] = mSymbolCum[symbol] + mSymbolFreq[symbol];
			
			if ((long)mSymbolCum[symbol] + mSymbolFreq[symbol] >= BasicArithmeticModel.Q2)
			{
				throw new IllegalArgumentException("Cumulative frequencies too high: " + ((long)mSymbolCum[symbol] + mSymbolFreq[symbol]) + ", max: " + (BasicArithmeticModel.Q2 - 1));
			}
		}

		mSymbolFreq[0] = 0;
	}


	public boolean isAdaptive()
	{
		return mAdaptive;
	}
}