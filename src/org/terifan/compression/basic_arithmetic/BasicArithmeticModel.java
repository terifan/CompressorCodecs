package org.terifan.compression.basic_arithmetic;


/*
 * If you are not familiar with arithmetic compression, you should read 
 * I. E. Witten, R. M. Neal, and J. G. Cleary, Communications of the ACM, 
 * Vol. 30, pp. 520-540 (1987), from which much have been borrowed.
 */
public class BasicArithmeticModel
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


	public BasicArithmeticModel()
	{
	}


	void increment(BasicArithmeticContext aContext, int aSymbol)
	{
		if (aContext.mSymbolCum[0] >= MAX_CUMULATIVE_FREQUENCY)
		{
			int c = 0;
			for (int i = aContext.mSymbolCount; i > 0; i--)
			{
				aContext.mSymbolCum[i] = c;
				c += (aContext.mSymbolFreq[i] = (aContext.mSymbolFreq[i] + 1) >> 1);
			}
			aContext.mSymbolCum[0] = c;
		}

		int i;
		for (i = aSymbol; aContext.mSymbolFreq[i] == aContext.mSymbolFreq[i - 1]; i--)
		{
		}

		if (i < aSymbol)
		{
			int ch_i = aContext.mSymbolToChar[i];
			int ch_sym = aContext.mSymbolToChar[aSymbol];
			aContext.mSymbolToChar[i] = ch_sym;
			aContext.mSymbolToChar[aSymbol] = ch_i;
			aContext.mCharToSymbol[ch_i] = aSymbol;
			aContext.mCharToSymbol[ch_sym] = i;
		}

		aContext.mSymbolFreq[i]++;

		while (--i >= 0)
		{
			aContext.mSymbolCum[i]++;
		}
	}
}