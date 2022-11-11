package org.terifan.compression.cabac265;


public class CabacContext265
{
	int MPSbit;
	int state;


	public CabacContext265()
	{
		state = 7;
		MPSbit = 1;
	}


	public static CabacContext265[] create(int aDim0)
	{
		CabacContext265[] arr = new CabacContext265[aDim0];
		for (int i = 0; i < aDim0; i++)
		{
			arr[i] = new CabacContext265();
		}
		return arr;
	}


	public static CabacContext265[][] create(int aDim0, int aDim1)
	{
		CabacContext265[][] arr = new CabacContext265[aDim0][aDim1];
		for (int i = 0; i < aDim0; i++)
		{
			for (int j = 0; j < aDim1; j++)
			{
				arr[i][j] = new CabacContext265();
			}
		}
		return arr;
	}


	public static CabacContext265[][][] create(int aDim0, int aDim1, int aDim2)
	{
		CabacContext265[][][] arr = new CabacContext265[aDim0][aDim1][aDim2];
		for (int i = 0; i < aDim0; i++)
		{
			for (int j = 0; j < aDim1; j++)
			{
				for (int k = 0; k < aDim2; k++)
				{
					arr[i][j][k] = new CabacContext265();
				}
			}
		}
		return arr;
	}
}
