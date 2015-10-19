package org.terifan.io.compression.adaptivehuffman;


/**
 * Source: https://code.google.com/p/adaptive-huffman-coding/source/browse/trunk/AdaptiveHuffmanCoding/src/com/adaptivehuffman/
 */
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
