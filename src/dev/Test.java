package dev;

import java.util.Arrays;
import java.util.Base64;
import org.terifan.compression.rans.RANSEncoder;
import org.terifan.compression.rans.SymbolStats;


public class Test
{
	public static void main(String ... args)
	{
		try
		{
			int rating = 10;
			String[] tags = {"Red","Large","Wide"};
			String[] actors = {"Bobby Bob","Cindy Shine"};
			String title = "Opps I did it again";
			String description = "Bad girl";
			String producer = "Company";
			String series = "Booby";
			String volume = "BOB";
			String part = "4";
			int[] flags = {4, 7};
			int checksum = 0xd4;

			SymbolStats ds = new SymbolStats(256);
//			String[] stats = " ,100,*,200,e,150,f,25,t,90,w,20,y,20,a,80,i,80,n,80,o,80,s,80,g,17,p,17,h,64,b,16,r,62,v,12,d,44,k,8,l,40,q,5,u,34,j,4,x,4,c,30,m,30,z,2".split(",");
//			for (int i = 0; i < stats.length; i+=2)
//			{
//				ds.set(stats[i].charAt(0), Integer.parseInt(stats[i + 1]));
//			}
			String[] stats = "1,1,2,1,2,1,1,2,1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,4,1,1,1,1,1,1,1,1,1,13,1,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,7,3,1,1,1,1,1,1,1,1,2,1,1,2,1,1,2,2,1,1,1,2,1,1,1,1,1,1,1,1,1,4,5,1,5,5,1,3,2,5,1,1,2,2,4,6,2,1,3,1,1,1,1,1,1,5".split(",");
			for (int i = 0; i < stats.length; i++)
			{
				int j = Integer.parseInt(stats[i].trim());
				ds.set(i, j * j);
			}

			RANSEncoder encoder = new RANSEncoder(ds);
			encoder.write(rating);
			encoder.write(flags.length);
			encoder.write(flags);
			encoder.write((description + "*").getBytes("utf-8"));
			encoder.write((producer + "*").getBytes("utf-8"));
			encoder.write((volume + "*").getBytes("utf-8"));
			encoder.write((series + "*").getBytes("utf-8"));
			encoder.write((part + "*").getBytes("utf-8"));
			for (String v : tags)
			{
				encoder.write((v + "*").getBytes("utf-8"));
			}
			encoder.write('*');
			for (String v : actors)
			{
				encoder.write((v + "*").getBytes("utf-8"));
			}
			encoder.write('*');
			encoder.write(0xff & checksum);
			byte[] encoded = encoder.finish();
			System.out.println(ds);
			String s = Base64.getUrlEncoder().encodeToString(encoded).replace("=", "");

			String t = rating + ";" + description + ";" + Arrays.toString(actors).replace(", ", ",").replace("[", "").replace("]", ";") + Arrays.toString(tags).replace(", ", ",").replace("[", "").replace("]", ";") + Arrays.toString(flags).replace(", ", ",").replace("[", "").replace("]", ";") + producer + "," + series + "," + volume + "," + part + ";" + checksum;

			String a = title + " {" + t + "}";
			String b = title + " {" + s + "}";


			ds = new SymbolStats(256);
			stats = "4,1,1,1,1,1,1,1,1,1,1,1,8,1,1,1,2,3,3,1,3,1,1,2,1,1,1,7,1,1,1,1,1,1,7,3,1,1,1,1,1,1,1,1,2,1,1,2,1,1,2,2,1,1,1,2,1,1,1,1,1,1,1,1,1,4,5,1,5,5,1,3,2,5,1,1,2,2,4,6,2,1,3,1,1,1,1,1,1,5".split(",");
			for (int i = 0; i < stats.length; i++)
			{
				int j = Integer.parseInt(stats[i].trim());
				ds.set(32 + i, j * j);
			}
			encoder = new RANSEncoder(ds);
			encoder.write(t.getBytes("utf-8"));
			String c = title + " {" + Base64.getEncoder().encodeToString(encoder.finish()).replace("=", "") + "}";
			System.out.println(ds);

			System.out.println(a);
			System.out.println(b);
			System.out.println(c);
			System.out.println(a.length());
			System.out.println(b.length());
			System.out.println(c.length());
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
