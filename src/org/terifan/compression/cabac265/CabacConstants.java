package org.terifan.compression.cabac265;


interface CabacConstants
{
	public static int INITIAL_CABAC_BUFFER_CAPACITY = 4096;

	final static int[][] LPS_table = new int[][]
	{
		{
			128, 176, 208, 240
		},
		{
			128, 167, 197, 227
		},
		{
			128, 158, 187, 216
		},
		{
			123, 150, 178, 205
		},
		{
			116, 142, 169, 195
		},
		{
			111, 135, 160, 185
		},
		{
			105, 128, 152, 175
		},
		{
			100, 122, 144, 166
		},
		{
			95, 116, 137, 158
		},
		{
			90, 110, 130, 150
		},
		{
			85, 104, 123, 142
		},
		{
			81, 99, 117, 135
		},
		{
			77, 94, 111, 128
		},
		{
			73, 89, 105, 122
		},
		{
			69, 85, 100, 116
		},
		{
			66, 80, 95, 110
		},
		{
			62, 76, 90, 104
		},
		{
			59, 72, 86, 99
		},
		{
			56, 69, 81, 94
		},
		{
			53, 65, 77, 89
		},
		{
			51, 62, 73, 85
		},
		{
			48, 59, 69, 80
		},
		{
			46, 56, 66, 76
		},
		{
			43, 53, 63, 72
		},
		{
			41, 50, 59, 69
		},
		{
			39, 48, 56, 65
		},
		{
			37, 45, 54, 62
		},
		{
			35, 43, 51, 59
		},
		{
			33, 41, 48, 56
		},
		{
			32, 39, 46, 53
		},
		{
			30, 37, 43, 50
		},
		{
			29, 35, 41, 48
		},
		{
			27, 33, 39, 45
		},
		{
			26, 31, 37, 43
		},
		{
			24, 30, 35, 41
		},
		{
			23, 28, 33, 39
		},
		{
			22, 27, 32, 37
		},
		{
			21, 26, 30, 35
		},
		{
			20, 24, 29, 33
		},
		{
			19, 23, 27, 31
		},
		{
			18, 22, 26, 30
		},
		{
			17, 21, 25, 28
		},
		{
			16, 20, 23, 27
		},
		{
			15, 19, 22, 25
		},
		{
			14, 18, 21, 24
		},
		{
			14, 17, 20, 23
		},
		{
			13, 16, 19, 22
		},
		{
			12, 15, 18, 21
		},
		{
			12, 14, 17, 20
		},
		{
			11, 14, 16, 19
		},
		{
			11, 13, 15, 18
		},
		{
			10, 12, 15, 17
		},
		{
			10, 12, 14, 16
		},
		{
			9, 11, 13, 15
		},
		{
			9, 11, 12, 14
		},
		{
			8, 10, 12, 14
		},
		{
			8, 9, 11, 13
		},
		{
			7, 9, 11, 12
		},
		{
			7, 9, 10, 12
		},
		{
			7, 8, 10, 11
		},
		{
			6, 8, 9, 11
		},
		{
			6, 7, 9, 10
		},
		{
			6, 7, 8, 9
		},
		{
			2, 2, 2, 2
		}
	};

	final static int[] renorm_table = new int[]
	{
		6, 5, 4, 4,
		3, 3, 3, 3,
		2, 2, 2, 2,
		2, 2, 2, 2,
		1, 1, 1, 1,
		1, 1, 1, 1,
		1, 1, 1, 1,
		1, 1, 1, 1
	};

	final static int[] next_state_MPS = new int[]
	{
		1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
		17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
		33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
		49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 62, 63
	};

	final static int[] next_state_LPS = new int[]
	{
		0, 0, 1, 2, 2, 4, 4, 5, 6, 7, 8, 9, 9, 11, 11, 12,
		13, 13, 15, 15, 16, 16, 18, 18, 19, 19, 21, 21, 22, 22, 23, 24,
		24, 25, 26, 26, 27, 27, 28, 29, 29, 30, 30, 30, 31, 32, 32, 33,
		33, 33, 34, 34, 35, 35, 35, 36, 36, 36, 37, 37, 37, 38, 38, 63
	};

	final static int[] entropy_table =
	{
		// -------------------- 200 --------------------
		/* state= 0 */ 0x07d13 /* 0.977164 */, 0x08255 /* 1.018237 */,
		/* state= 1 */ 0x07738 /* 0.931417 */, 0x086ef /* 1.054179 */,
		/* state= 2 */ 0x0702b /* 0.876323 */, 0x0935a /* 1.151195 */,
		/* state= 3 */ 0x069e6 /* 0.827333 */, 0x09c7f /* 1.222650 */,
		/* state= 4 */ 0x062e8 /* 0.772716 */, 0x0a2c7 /* 1.271708 */,
		/* state= 5 */ 0x05c18 /* 0.719488 */, 0x0ae25 /* 1.360532 */,
		/* state= 6 */ 0x05632 /* 0.673414 */, 0x0b724 /* 1.430793 */,
		/* state= 7 */ 0x05144 /* 0.634904 */, 0x0c05d /* 1.502850 */,
		/* state= 8 */ 0x04bdf /* 0.592754 */, 0x0ccf2 /* 1.601145 */,
		/* state= 9 */ 0x0478d /* 0.559012 */, 0x0d57b /* 1.667843 */,
		/* state=10 */ 0x042ad /* 0.520924 */, 0x0de81 /* 1.738336 */,
		/* state=11 */ 0x03f4d /* 0.494564 */, 0x0e4b8 /* 1.786871 */,
		/* state=12 */ 0x03a9d /* 0.457945 */, 0x0f471 /* 1.909721 */,
		/* state=13 */ 0x037d5 /* 0.436201 */, 0x0fc56 /* 1.971385 */,
		/* state=14 */ 0x034c2 /* 0.412177 */, 0x10236 /* 2.017284 */,
		/* state=15 */ 0x031a6 /* 0.387895 */, 0x10d5c /* 2.104394 */,
		/* state=16 */ 0x02e62 /* 0.362383 */, 0x11b34 /* 2.212552 */,
		/* state=17 */ 0x02c20 /* 0.344752 */, 0x120b4 /* 2.255512 */,
		/* state=18 */ 0x029b8 /* 0.325943 */, 0x1294d /* 2.322672 */,
		/* state=19 */ 0x02791 /* 0.309143 */, 0x135e1 /* 2.420959 */,
		/* state=20 */ 0x02562 /* 0.292057 */, 0x13e37 /* 2.486077 */,
		/* state=21 */ 0x0230d /* 0.273846 */, 0x144fd /* 2.539000 */,
		/* state=22 */ 0x02193 /* 0.262308 */, 0x150c9 /* 2.631150 */,
		/* state=23 */ 0x01f5d /* 0.245026 */, 0x15ca0 /* 2.723641 */,
		/* state=24 */ 0x01de7 /* 0.233617 */, 0x162f9 /* 2.773246 */,
		/* state=25 */ 0x01c2f /* 0.220208 */, 0x16d99 /* 2.856259 */,
		/* state=26 */ 0x01a8e /* 0.207459 */, 0x17a93 /* 2.957634 */,
		/* state=27 */ 0x0195a /* 0.198065 */, 0x18051 /* 3.002477 */,
		/* state=28 */ 0x01809 /* 0.187778 */, 0x18764 /* 3.057759 */,
		/* state=29 */ 0x0164a /* 0.174144 */, 0x19460 /* 3.159206 */,
		/* state=30 */ 0x01539 /* 0.165824 */, 0x19f20 /* 3.243181 */,
		/* state=31 */ 0x01452 /* 0.158756 */, 0x1a465 /* 3.284334 */,
		/* state=32 */ 0x0133b /* 0.150261 */, 0x1b422 /* 3.407303 */,
		/* state=33 */ 0x0120c /* 0.140995 */, 0x1bce5 /* 3.475767 */,
		/* state=34 */ 0x01110 /* 0.133315 */, 0x1c394 /* 3.527962 */,
		/* state=35 */ 0x0104d /* 0.127371 */, 0x1d059 /* 3.627736 */,
		/* state=36 */ 0x00f8b /* 0.121451 */, 0x1d74b /* 3.681983 */,
		/* state=37 */ 0x00ef4 /* 0.116829 */, 0x1dfd0 /* 3.748540 */,
		/* state=38 */ 0x00e10 /* 0.109864 */, 0x1e6d3 /* 3.803335 */,
		/* state=39 */ 0x00d3f /* 0.103507 */, 0x1f925 /* 3.946462 */,
		/* state=40 */ 0x00cc4 /* 0.099758 */, 0x1fda7 /* 3.981667 */,
		/* state=41 */ 0x00c42 /* 0.095792 */, 0x203f8 /* 4.031012 */,
		/* state=42 */ 0x00b78 /* 0.089610 */, 0x20f7d /* 4.121014 */,
		/* state=43 */ 0x00afc /* 0.085830 */, 0x21dd6 /* 4.233102 */,
		/* state=44 */ 0x00a5e /* 0.081009 */, 0x22419 /* 4.282016 */,
		/* state=45 */ 0x00a1b /* 0.078950 */, 0x22a5e /* 4.331015 */,
		/* state=46 */ 0x00989 /* 0.074514 */, 0x23756 /* 4.432323 */,
		/* state=47 */ 0x0091b /* 0.071166 */, 0x24225 /* 4.516775 */,
		/* state=48 */ 0x008cf /* 0.068837 */, 0x2471a /* 4.555487 */,
		/* state=49 */ 0x00859 /* 0.065234 */, 0x25313 /* 4.649048 */,
		/* state=50 */ 0x00814 /* 0.063140 */, 0x25d67 /* 4.729721 */,
		/* state=51 */ 0x007b6 /* 0.060272 */, 0x2651f /* 4.790028 */,
		/* state=52 */ 0x0076e /* 0.058057 */, 0x2687c /* 4.816294 */,
		/* state=53 */ 0x00707 /* 0.054924 */, 0x27da7 /* 4.981661 */,
		/* state=54 */ 0x006d5 /* 0.053378 */, 0x28172 /* 5.011294 */,
		/* state=55 */ 0x00659 /* 0.049617 */, 0x28948 /* 5.072512 */,
		/* state=56 */ 0x00617 /* 0.047598 */, 0x297c5 /* 5.185722 */,
		/* state=57 */ 0x005dd /* 0.045814 */, 0x2a2df /* 5.272434 */,
		/* state=58 */ 0x005c1 /* 0.044965 */, 0x2a581 /* 5.293019 */,
		/* state=59 */ 0x00574 /* 0.042619 */, 0x2ad59 /* 5.354304 */,
		/* state=60 */ 0x0053b /* 0.040882 */, 0x2bba5 /* 5.465973 */,
		/* state=61 */ 0x0050c /* 0.039448 */, 0x2c596 /* 5.543651 */,
		/* state=62 */ 0x004e9 /* 0.038377 */, 0x2cd88 /* 5.605741 */,
		0x00400, 0x2d000
	/* dummy, should never be used */
	};


	static int clip3(int aLow, int aHigh, int aValue)
	{
		return ((aValue) < (aLow) ? (aLow) : (aValue) > (aHigh) ? (aHigh) : (aValue));
	}
}
