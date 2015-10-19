package org.terifan.compression.cabac_c;


public class CabacContext
{
	int MPS;
	int state;


	public void initContext(int qp, int[] ini)
	{
		int pstate = ((ini[0] * qp) >> 4) + ini[1];

		if (pstate >= 64)
		{
			pstate = Math.min(126, pstate);
			state = (pstate - 64);
			MPS = 1;
		}
		else
		{
			pstate = Math.max(1, pstate);
			state = (63 - pstate);
			MPS = 0;
		}
	}
}
