package test_all;

import org.terifan.compression.cabac.CabacContext;


class State
{
	CabacContext dclow = new CabacContext(0);
	CabacContext acsign = new CabacContext(0);
	CabacContext stop = new CabacContext(0);
	CabacContext dczero = new CabacContext(0);
	CabacContext ac3 = new CabacContext(0);
	CabacContext[] run = new CabacContext[65];
	CabacContext[] acmag = new CabacContext[10000];
	CabacContext[] dc = new CabacContext[1000];
	CabacContext[] dcmag = new CabacContext[1000];
	CabacContext[][] acmag2 = new CabacContext[64][1000];
	CabacContext[][] acres = new CabacContext[64][1000];


	public State()
	{
		for (int i = 0; i < run.length; i++) run[i] = new CabacContext(0);
		for (int i = 0; i < acmag.length; i++) acmag[i] = new CabacContext(0);
		for (int i = 0; i < dc.length; i++) dc[i] = new CabacContext(0);
		for (int i = 0; i < dcmag.length; i++) dcmag[i] = new CabacContext(0);
		for (int j = 0; j < acmag2.length; j++) for (int i = 0; i < acmag2[j].length; i++) acmag2[j][i] = new CabacContext(0);
		for (int j = 0; j < acres.length; j++) for (int i = 0; i < acres[j].length; i++) acres[j][i] = new CabacContext(0);
	}
}
