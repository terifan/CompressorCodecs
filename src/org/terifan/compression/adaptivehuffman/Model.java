package org.terifan.compression.adaptivehuffman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Source: https://code.google.com/p/adaptive-huffman-coding/source/browse/trunk/AdaptiveHuffmanCoding/src/com/adaptivehuffman/
 */
class Model
{
	static int NEW = Integer.MAX_VALUE;
	private Node mNytNode;
	private HashSet<Integer> mAlreadyExist;
	private HashMap<Integer,Node> mNodeLookup;
	Node mRoot;

	/**
	 * Store all the nodes in the tree from left to right, bottom to top.
	 */
	private ArrayList<Node> mNodeList;


	public Model()
	{
		mAlreadyExist = new HashSet<>();
		mNodeList = new ArrayList<>();
		mNodeLookup = new HashMap<>();

		mNytNode = new Node(NEW, 0);
		mNytNode.parent = null;
		mNodeList.add(mNytNode);
		mRoot = mNytNode;
	}


	/**
	 * It's a very important method that is used for updating the structure of tree after read
	 * each symbol. Called both during encoding and decoding.
	 *
	 * @param c the next character
	 */
	public void updateTree(int c)
	{
		Node toBeAdd;

		if (mAlreadyExist.contains(c))
		{
			toBeAdd = mNodeLookup.get(c);
		}
		else
		{
			// if the character is not yet existed, create two nodes. The one is for the new character,
			// the other is for its father node.
			Node innerNode = new Node(-1, 1); //inner node with null letter
			Node newNode = new Node(c, 1); //stores symbol
			
			mNodeLookup.put(c, newNode);

			// pay attention to the linking process among nodes.
			innerNode.left = mNytNode;
			innerNode.right = newNode;
			innerNode.parent = mNytNode.parent;
			if (mNytNode.parent != null)//In the first time the nyt node is root.
			{
				mNytNode.parent.left = innerNode;
			}
			else
			{
				mRoot = innerNode;
			}
			mNytNode.parent = innerNode;
			newNode.parent = innerNode;

			// the following two lines assure the right order in nodeList
			mNodeList.add(1, innerNode);
			mNodeList.add(1, newNode);

			mAlreadyExist.add(c);

			toBeAdd = innerNode.parent;
		}

		// loop until all parent nodes are incremented.
		while (toBeAdd != null)
		{
			Node bigNode = findBigNode(toBeAdd.frequency);

			if (toBeAdd != bigNode && toBeAdd.parent != bigNode && bigNode.parent != toBeAdd)
			{
				swapNode(toBeAdd, bigNode);
			}

			toBeAdd.frequency++;
			toBeAdd = toBeAdd.parent;
		}
	}


	/**
	 * Swap two nodes. Note that we should swap nodes but not only values, because the subtree
	 * is also needed to be swapped.
	 *
	 * @param n1 the node of which the frequency is to be incremented.
	 * @param n2 the biggest node in the block.
	 */
	private void swapNode(Node n1, Node n2)
	{
		// note that n1<n2
		int i1 = mNodeList.indexOf(n1);
		int i2 = mNodeList.indexOf(n2);
		mNodeList.remove(n1);
		mNodeList.remove(n2);
		mNodeList.add(i1, n2);
		mNodeList.add(i2, n1);

		Node p1 = n1.parent;
		Node p2 = n2.parent;

		if (p1 == p2)
		{
			p1.left = n2;
			p1.right = n1;
		}
		else
		{
			if (p1.left == n1)
			{
				p1.left = n2;
			}
			else
			{
				p1.right = n2;
			}

			if (p2.left == n2)
			{
				p2.left = n1;
			}
			else
			{
				p2.right = n1;
			}
		}

		n1.parent = p2;
		n2.parent = p1;
	}


	/**
	 * Find the node with biggest index in a certain block. Just look for the first node with the
	 * same frequency from the back.
	 */
	private Node findBigNode(int aFrequency)
	{
		Node temp = null;

		for (int i = mNodeList.size(); --i >= 0;)
		{
			temp = mNodeList.get(i);

			if (temp.frequency == aFrequency)
			{
				break;
			}
		}

		return temp;
	}
}
