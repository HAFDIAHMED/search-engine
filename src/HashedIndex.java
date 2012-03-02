/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 *   This version: Victor Hallberg, Johan Stjernberg
 */  


import java.util.LinkedList;
import java.util.HashMap;
import java.util.Collections;

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
	 *  Searches the index for postings matching the query in @code{searchTerms}.
	 */
	public PostingsList search(LinkedList<String> searchTerms, int queryType) {
		PostingsList result = null;

		// Normal word queries
		if (queryType != Index.RANKED_QUERY) {
			for (String term : searchTerms) {
				if (result == null)
					result = getPostings(term);
			else if (queryType == Index.UNION_QUERY)
				result = result.unionWith(getPostings(term));
				else
					result = result.intersect(getPostings(term), queryType == Index.PHRASE_QUERY);
			}
		}
		// Ranked queries
		else {
			int numDocuments = docIDs.size();

			// Term frequency in query
			HashMap<String, Integer> termCounts = new HashMap<String, Integer>();
			for (String term : searchTerms) {
				Integer count = termCounts.get(term);          
				termCounts.put(term, (count == null) ? 1 : count + 1);
			}

			int numSearchTerms = searchTerms.size(); // total number of terms in search
			int numTerms = termCounts.size(); // number of distinct terms
			String[] terms = termCounts.keySet().toArray(new String[0]); // distinct terms
			double[] queryTFIDF = new double[numTerms];

			// Query for each term separately
			PostingsList[] termResults = new PostingsList[numTerms];

			// DocID -> array index mapping
			HashMap<Integer,Integer> resultDocIds = new HashMap<Integer,Integer>();
			int currentDocIdx = 0;

			// Query for each distinct term, compute TFIDF and create docID -> index mapping
			int idx = 0; // term index
			for (String term : terms) {
				termResults[idx] = getPostings(term);

				// Calculate term TFIDF
				int tf = termCounts.get(terms[idx]);
				int df = termResults[idx].size();
				double idf = (df < 1) ? 0 : Math.log10((double) numDocuments / df) + 1;
				queryTFIDF[idx] = tf * idf;

				// Determine index mapping for each document found
				for (PostingsEntry entry : termResults[idx].list) {
					Integer entryIndex = resultDocIds.get(entry.docID);
					if (entryIndex == null) // assign next available index if not seen yet
						resultDocIds.put(entry.docID, currentDocIdx++);
				}

				++idx;
			}

			int numResultDocIDs = resultDocIds.size();

			double[] scores = new double[numResultDocIDs];

			// Calculate scores for each document (in regards to each search term)
			idx = 0;
			for (PostingsList termResult : termResults) {
				for (PostingsEntry entry : termResult.list) {
					// Determine index mapping for document
					Integer entryIndex = resultDocIds.get(entry.docID);
					if (entryIndex == null) // assign next available index
						resultDocIds.put(entry.docID, currentDocIdx++);

					// Compute document score in regard to this term
					int dTF = entry.getFrequency();
					int dLength = docLengths.get("" + entry.docID);
					scores[entryIndex] += queryTFIDF[idx] * dTF / Math.sqrt(numSearchTerms) / Math.sqrt(dLength);
				}

				// Merge (union) each PostingsList
				result = (result == null) ? termResult : result.unionWith(termResult);

				++idx;
			}

			// Assign score to corresponding document (postings) entries
			for (PostingsEntry pe : result.list)
				pe.score = scores[resultDocIds.get(pe.docID)];

			// Sort documents according to their similarity score.
			Collections.sort(result.list);
		} // ranked queries

		return (result == null) ? new PostingsList() : result;
	}


	/**
	 *  No need for cleanup in a HashedIndex.
	 */
	public void cleanup() {
	}
}
