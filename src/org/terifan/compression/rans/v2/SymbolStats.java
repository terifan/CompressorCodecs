package org.terifan.compression.rans.v2;

import java.io.EOFException;
import java.util.Arrays;


public class SymbolStats
{
	final static int LOG2NSYMS = 8;
	final static int NSYMS = 1 << LOG2NSYMS;

	int[] freqs = new int[NSYMS];
	int[] cum_freqs = new int[NSYMS + 1];

	int[] divider = new int[NSYMS];
	int[] slot_adjust = new int[NSYMS * 2];
	int[] slot_freqs = new int[NSYMS * 2];
	int[] sym_id = new int[NSYMS * 2];

	int[] alias_remap;
	int prob_bits;


	public SymbolStats(int aProbBits)
	{
		prob_bits = aProbBits;
	}


	void count_freqs(byte[] aBuffer) throws EOFException
	{
		if (aBuffer == null)
		{
			Arrays.fill(freqs, 1);
		}
		else
		{
			Arrays.fill(freqs, 0);
			for (int i = 0; i < aBuffer.length; i++)
			{
				freqs[0xff & aBuffer[i]]++;
			}
		}
	}


	void calc_cum_freqs()
	{
		cum_freqs[0] = 0;
		for (int i = 0; i < NSYMS; i++)
		{
			cum_freqs[i + 1] = cum_freqs[i] + freqs[i];
		}
	}


	void normalize_freqs()
	{
		int target_total = 1 << prob_bits;

		assert (target_total >= NSYMS);

		calc_cum_freqs();
		int cur_total = cum_freqs[NSYMS];

		// resample distribution based on cumulative freqs
		for (int i = 1; i <= NSYMS; i++)
		{
			cum_freqs[i] = (int)(((long)target_total * cum_freqs[i]) / cur_total);
		}

		// if we nuked any non-0 frequency symbol to 0, we need to steal
		// the range to make the frequency nonzero from elsewhere.
		//
		// this is not at all optimal, i'm just doing the first thing that comes to mind.
		for (int i = 0; i < NSYMS; i++)
		{
			if (freqs[i] != 0 && cum_freqs[i + 1] == cum_freqs[i])
			{
				// symbol i was set to zero freq

				// find best symbol to steal frequency from (try to steal from low-freq ones)
				int best_freq = Integer.MAX_VALUE;
				int best_steal = -1;
				for (int j = 0; j < NSYMS; j++)
				{
					int freq = cum_freqs[j + 1] - cum_freqs[j];
					if (freq > 1 && freq < best_freq)
					{
						best_freq = freq;
						best_steal = j;
					}
				}
				assert (best_steal != -1);

				// and steal from it!
				if (best_steal < i)
				{
					for (int j = best_steal + 1; j <= i; j++)
					{
						cum_freqs[j]--;
					}
				}
				else
				{
					assert (best_steal > i);
					for (int j = i + 1; j <= best_steal; j++)
					{
						cum_freqs[j]++;
					}
				}
			}
		}

		// calculate updated freqs and make sure we didn't screw anything up
		assert (cum_freqs[0] == 0 && cum_freqs[NSYMS] == target_total);

		for (int i = 0; i < NSYMS; i++)
		{
			if (freqs[i] == 0)
			{
				assert (cum_freqs[i + 1] == cum_freqs[i]);
			}
			else
			{
				assert (cum_freqs[i + 1] > cum_freqs[i]);
			}

			// calc updated freq
			freqs[i] = cum_freqs[i + 1] - cum_freqs[i];
		}
	}


	void make_alias_table()
	{
		// verify that our distribution sum divides the number of buckets
		int sum = cum_freqs[NSYMS];
		assert (sum != 0 && (sum % NSYMS) == 0);
		assert (sum >= NSYMS);

		// target size in every bucket
		int tgt_sum = sum / NSYMS;

		// okay, prepare a sweep of vose's algorithm to distribute
		// the symbols into buckets
		int[] remaining = new int[NSYMS];
		for (int i = 0; i < NSYMS; i++)
		{
			remaining[i] = freqs[i];

			divider[i] = tgt_sum;
			sym_id[i * 2 + 0] = i;
			sym_id[i * 2 + 1] = i;
		}

		// a "small" symbol is one with less than tgt_sum slots left to distribute
		// a "large" symbol is one with >=tgt_sum slots.
		// find initial small/large buckets
		int cur_large = 0;
		int cur_small = 0;
		while (cur_large < NSYMS && remaining[cur_large] < tgt_sum)
		{
			cur_large++;
		}
		while (cur_small < NSYMS && remaining[cur_small] >= tgt_sum)
		{
			cur_small++;
		}

		// cur_small is definitely a small bucket
		// next_small *might* be.
		int next_small = cur_small + 1;

		// top up small buckets from large buckets until we're done
		// this might turn the large bucket we stole from into a small bucket itself.
		while (cur_large < NSYMS && cur_small < NSYMS)
		{
			// this bucket is split between cur_small and cur_large
			sym_id[cur_small * 2 + 0] = cur_large;
			divider[cur_small] = remaining[cur_small];

			// take the amount we took out of cur_large's bucket
			remaining[cur_large] -= tgt_sum - divider[cur_small];

			// if the large bucket is still large *or* we haven't processed it yet...
			if (remaining[cur_large] >= tgt_sum || next_small <= cur_large)
			{
				// find the next small bucket to process
				cur_small = next_small;
				while (cur_small < NSYMS && remaining[cur_small] >= tgt_sum)
				{
					cur_small++;
				}
				next_small = cur_small + 1;
			}
			else // the large bucket we just made small is behind us, need to back-track
			{
				cur_small = cur_large;
			}

			// if cur_large isn't large anymore, forward to a bucket that is
			while (cur_large < NSYMS && remaining[cur_large] < tgt_sum)
			{
				cur_large++;
			}
		}

		// okay, we now have our alias mapping; distribute the code slots in order
		int[] assigned = new int[NSYMS];
		alias_remap = new int[sum];

		for (int i = 0; i < NSYMS; i++)
		{
			int j = sym_id[i * 2 + 0];
			int sym0_height = divider[i];
			int sym1_height = tgt_sum - divider[i];
			int base0 = assigned[i];
			int base1 = assigned[j];
			int cbase0 = cum_freqs[i] + base0;
			int cbase1 = cum_freqs[j] + base1;

			divider[i] = i * tgt_sum + sym0_height;

			slot_freqs[i * 2 + 1] = freqs[i];
			slot_freqs[i * 2 + 0] = freqs[j];
			slot_adjust[i * 2 + 1] = i * tgt_sum - base0;
			slot_adjust[i * 2 + 0] = i * tgt_sum - (base1 - sym0_height);
			for (int k = 0; k < sym0_height; k++)
			{
				alias_remap[cbase0 + k] = k + i * tgt_sum;
			}
			for (int k = 0; k < sym1_height; k++)
			{
				alias_remap[cbase1 + k] = (k + sym0_height) + i * tgt_sum;
			}

			assigned[i] += sym0_height;
			assigned[j] += sym1_height;
		}

		// check that each symbol got the number of slots it needed
		for (int i = 0; i < NSYMS; i++)
		{
			assert (assigned[i] == freqs[i]);
		}
	}
}