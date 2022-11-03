package org.terifan.compression.rans;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.terifan.compression.rans.v2.RANSCodec;



public class Test
{
	public static void main(String ... args)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try ( InputStream in = RANSCodec.class.getResourceAsStream("book1.txt"))
			{
				byte[] buf = new byte[4096];
				for (int len; (len = in.read(buf)) > 0;)
				{
					baos.write(buf, 0, len);
				}
			}

			SymbolStats ds = new SymbolStats(256);
			String[] stats = " ,256,e,120,f,25,t,90,w,20,y,20,a,80,i,80,n,80,o,80,s,80,g,17,p,17,h,64,b,16,r,62,v,12,d,44,k,8,l,40,q,5,u,34,j,4,x,4,c,30,m,30,z,2".split(",");
			for (int i = 0; i < stats.length;)
			{
				char c = stats[i++].charAt(0);
				int f = Integer.parseInt(stats[i++]);
				ds.set(c, f);
			}

			RANSEncoder encoder = new RANSEncoder(ds);
			byte[] buf = baos.toByteArray();
			for (int i = 0; i < baos.size(); i++)
			{
				encoder.write(0xff & buf[i]);
			}

			byte[] encoded = encoder.finish();

			System.out.println(encoded.length);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
