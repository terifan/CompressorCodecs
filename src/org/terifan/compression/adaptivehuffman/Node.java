package org.terifan.compression.adaptivehuffman;


class Node
{
	Node left;
	Node right;
	Node parent;
	int symbol;
	int frequency;


	Node(int aLetter, int aFrequency)
	{
		frequency = aFrequency;
		symbol = aLetter;
	}
}
