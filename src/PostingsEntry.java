/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.util.LinkedList;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
	public int docID;
	public LinkedList<Integer> offsets;
	public double score;

	public PostingsEntry(int docID) {
		this.docID = docID;
		this.score = 0;
		offsets = new LinkedList<Integer>();
	}

	public PostingsEntry(int docID, int offset) {
		this(docID);
		addOffset(offset);
	}

	public boolean addOffset(int offset) {
		return offsets.add(offset);
	}

	/**
	 *  PostingsEntries are compared by their score (only relevant 
	 *  in ranked retrieval).
	 *
	 *  The comparison is defined so that entries will be put in 
	 *  descending order.
	 */
	public int compareTo(PostingsEntry other) {
		return Double.compare(other.score, score);
	}
}

	
