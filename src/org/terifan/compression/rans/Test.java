package org.terifan.compression.rans;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import org.terifan.compression.test_suit._LoadTestData;


public class Test
{
	public static void main(String ... args)
	{
		try
		{
			byte[] data = _LoadTestData.loadTestData(_LoadTestData.Source.BOOK);

			SymbolStats ds1 = new SymbolStats(256);
			SymbolStats ds2 = new SymbolStats(256);
			String[] stats = " ,256,e,120,f,25,t,90,w,20,y,20,a,80,i,80,n,80,o,80,s,80,g,17,p,17,h,64,b,16,r,62,v,12,d,44,k,8,l,40,q,5,u,34,j,4,x,4,c,30,m,30,z,2".split(",");
			for (int i = 0; i < stats.length;)
			{
				char c = stats[i++].charAt(0);
				int f = Integer.parseInt(stats[i++]);
				ds1.set(c, f);
				ds2.set(c, f);
			}

			RANSEncoder encoder = new RANSEncoder(ds1);
			for (int i = 0; i < data.length; i++)
			{
				encoder.write(0xff & data[i]);
			}

			byte[] encoded = encoder.finish();

			RANSDecoder decoder = new RANSDecoder(new ByteArrayInputStream(encoded), ds2);
			byte[] decoded = new byte[data.length];
			for (int i = 0; i < data.length; i++)
			{
				decoded[i] = (byte)decoder.read();
			}

//			System.out.println(new String(decoded));

			System.out.println(Arrays.equals(data, decoded));

			System.out.println(data.length);
			System.out.println(encoded.length);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
