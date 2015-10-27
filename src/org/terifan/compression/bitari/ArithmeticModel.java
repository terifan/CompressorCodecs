package org.terifan.compression.bitari;


interface ArithmeticModel
{
	int CODE_VALUE_SIZE = 14;
	int Q1 = 1 << CODE_VALUE_SIZE; // Q1 must be sufficiently large, but not so large as the unsigned long 4 * Q1 * (Q1-1) overflows.
	int Q2 = 2 * Q1;
	int Q3 = 3 * Q1;
	int Q4 = 4 * Q1;
	int MAX_CUMULATIVE_FREQUENCY = 1 << 9; //Q1 - 1;
	int GOLOMB_EXP_START = 13;
}
