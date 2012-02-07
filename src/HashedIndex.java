/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import java.util.LinkedList;
import java.util.HashMap;

/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {
	/** The index as a hashtable. */
	private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

	/**
	 *  Inserts this token in the index.
	 */
	public void insert(String token, int docID, int offset) {
		PostingsList list = index.get(token);

		if (list == null) {
			list = new PostingsList();
			index.put(token, list);
		}

		//System.out.println("inserting " + token + ": " + docID + ":" + offset);

		list.add(docID, offset);
	}

	/**
	 *  Returns the postings for a specific term, or null
	 *  if the term is not in the index.
	 */
	public PostingsList getPostings(String token) {
		return index.get(token) == null ? new PostingsList() : index.get(token);
	}

	/**
	 *  Searches the index for postings matching the query in @code{searchterms}.
	 */
	public PostingsList search(LinkedList<String> searchterms, int queryType) {
		PostingsList result = null;
		//System.out.println(index.keySet().size());

		// Word queries
		for (String term : searchterms) {
			if (result == null)
				result = getPostings(term);
			else
				result = result.intersect(getPostings(term), queryType == Index.PHRASE_QUERY);
		}

		return (result == null) ? new PostingsList() : result;
	}


	/**
	 *  No need for cleanup in a HashedIndex.
	 */
	public void cleanup() {
	}
}
