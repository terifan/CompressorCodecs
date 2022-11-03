package org.terifan.compression.rans.v2;


class RansEncSymbol{
	int x_max;     // (Exclusive) upper bound of pre-normalization interval
	int rcp_freq;  // Fixed-point reciprocal frequency
	int bias;      // Bias
	int cmpl_freq; // Complement of frequency: (1 << scale_bits) - freq
	int rcp_shift; // Reciprocal shift
}
