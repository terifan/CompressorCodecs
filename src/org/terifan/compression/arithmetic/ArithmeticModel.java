package org.terifan.compression.arithmetic;


public class ArithmeticModel
{
	protected final static int CODE_VALUE_SIZE = 14;
	protected final static long Q1 = (1L << CODE_VALUE_SIZE); // Q1 must be sufficiently large, but not so large as the unsigned long 4 * Q1 * (Q1 - 1) overflows.
	protected final static long Q2 = (2 * Q1);
	protected final static long Q3 = (3 * Q1);
	protected final static long Q4 = (4 * Q1);
	protected final static long MAX_CUMULATIVE_FREQUENCY = (Q1 - 1);

	long mLow;
	long mHigh = Q4;
	long mValue;
	int mShifts;  // counts for magnifying low and high around Q2


	public ArithmeticModel()
	{
	}


	public void updateModel(FrequencyTable aFrequencyTable, int aSymbol)
	{
		if (!aFrequencyTable.isAdaptive())
		{
			return;
		}

		if (aFrequencyTable.mSymbolCum[0] >= MAX_CUMULATIVE_FREQUENCY)
		{
			int c = 0;
			for (int i = aFrequencyTable.mSymbolCount; i > 0; i--)
			{
				aFrequencyTable.mSymbolCum[i] = c;
				c += (aFrequencyTable.mSymbolFreq[i] = (aFrequencyTable.mSymbolFreq[i] + 1) >> 1);
			}
			aFrequencyTable.mSymbolCum[0] = c;
		}

		int i;
		for (i = aSymbol; aFrequencyTable.mSymbolFreq[i] == aFrequencyTable.mSymbolFreq[i - 1]; i--)
		{
		}

		if (i < aSymbol)
		{
			int ch_i = aFrequencyTable.mSymbolToChar[i];
			int ch_sym = aFrequencyTable.mSymbolToChar[aSymbol];
			aFrequencyTable.mSymbolToChar[i] = ch_sym;
			aFrequencyTable.mSymbolToChar[aSymbol] = ch_i;
			aFrequencyTable.mCharToSymbol[ch_i] = aSymbol;
			aFrequencyTable.mCharToSymbol[ch_sym] = i;
		}

		aFrequencyTable.mSymbolFreq[i]++;

		while (--i >= 0)
		{
			aFrequencyTable.mSymbolCum[i]++;
		}
	}
}