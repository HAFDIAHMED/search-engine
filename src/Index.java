/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  


import java.util.LinkedList;
import java.util.HashMap;

public interface Index {

	/* Index types */
	public static final int HASHED_INDEX = 0;
	public static final int MEGA_INDEX = 1;

	/* Query types */
	public static final int INTERSECTION_QUERY = 0;
	public static final int UNION_QUERY = 1;
	public static final int PHRASE_QUERY = 2;
	public static final int RANKED_QUERY = 4;
		
	public HashMap<String, String> docIDs = new HashMap<String,String>();
	public HashMap<String,Integer> docLengths = new HashMap<String,Integer>();

	public void setPageRank(PageRank p);

	public void insert( String token, int docID, int offset );
	public PostingsList getPostings( String token );
	public PostingsList search( LinkedList<String> searchterms, int queryType );
	public void cleanup();

}

