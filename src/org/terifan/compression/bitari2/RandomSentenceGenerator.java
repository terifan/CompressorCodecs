package org.terifan.compression.bitari2;

import java.util.Random;


/**
 * http://www.users.globalnet.co.uk/~pennck/random.htm
 */
class RandomSentenceGenerator
{
	private final static Random RND = new Random();


	/**
	 * Produces a random phrase.
	 */
	public static String next()
	{

		return next(RND);
	}


	/**
	 * Produces a slightly shorter random phrase.
	 */
	public static String nextShort()
	{
		return nextShort(RND);
	}


	/**
	 * Produces a random phrase, using the Random generator provided.
	 */
	public static String next(Random aRandom)
	{
		StringBuilder text = new StringBuilder();
		for (int j = 0; j < mPhrases.length; j++)
		{
			String s = mPhrases[j][aRandom.nextInt(mPhrases[j].length)];
			if (s.length() > 0)
			{
				if (text.length() > 0)
				{
					text.append(" ");
				}
				text.append(s);
			}
		}
		return text.toString();
	}


	/**
	 * Produces a slightly shorter random phrase, using the Random generator
	 * provided.
	 */
	public static String nextShort(Random aRandom)
	{
		StringBuilder text = new StringBuilder();
		for (int j = 1; j < mPhrases.length; j+=2)
		{
			String s = mPhrases[j][aRandom.nextInt(mPhrases[j].length)];
			if (s.length() > 0)
			{
				if (text.length() > 0)
				{
					text.append(" ");
				}
				text.append(s);
			}
		}
		return Character.toUpperCase(text.charAt(0)) + text.substring(1).trim();
	}


	private static String [][] mPhrases =
	{
		{
			"And so, the",
			"A",
			"Nobody knows why a",
			"Almost daily, a",
			"Sometimes, the",
			"However, this particular",
			"The",
			"Only one"
		},
		{
			"cat",
			"evil potato",
			"elephant",
			"monkey",
			"robot",
			"ninja",
			"nerd",
			"zombie",
			"teacher",
			"monster",
			"butler",
			"alien",
			"fox",
			"demon",
			"box",
			"hand"
		},
		{
			"regularly",
			"angrily",
			"nervously",
			"quickly",
			"politely",
			"quietly",
			"dramatically",
			""
		},
		{
			"destroyed a",
			"sat on the",
			"painted a",
			"captured the",
			"wrote about a",
			"kicked the",
			"screamed at a",
			"spat on the",
			"cleaned a",
			"laughed at the",
			"ate the"
		},
		{
			"green",
			"deadly",
			"flaming",
			"grumpy",
			"dancing",
			"electronic",
			"royal",
			"screaming",
			"insane",
			"secret",
			"confusing",
			"cloned",
			"original"
		},
		{
			"cake",
			"candle",
			"hair",
			"police car",
			"arm",
			"gardener",
			"pile of biscuits",
			"dead body",
			"printer",
			"chair",
			"book",
			"paper",
			"mirror",
			"balloon",
			"shampoo bottle",
			"coffin"
		}
	};
}