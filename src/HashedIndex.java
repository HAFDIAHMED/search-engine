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

	public static double logw(double tf) {
		return (tf <= 0) ? 0 : 1 + Math.log10(tf);
	}

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
			System.out.println("===================================================");
			int numDocuments = docIDs.size();

			// Term frequency in query
			HashMap<String, Integer> termCounts = new HashMap<String, Integer>();
			for (String term : searchTerms) {
				Integer count = termCounts.get(term);          
				termCounts.put(term, (count == null) ? 1 : count + 1);
			}

			int termsLength = searchTerms.size();
			int numTerms = termCounts.size();
			String[] terms = termCounts.keySet().toArray(new String[0]); // unique terms
			double[] idf = new double[numTerms];

			// Query for each term separately
			PostingsList[] termResults = new PostingsList[numTerms];
			HashMap<Integer,Integer> resultDocIds = new HashMap<Integer,Integer>();
			int currentDocIdx = 0;

			// Query for each distinct term and create docID -> index mapping
			// Also compute IDF for each term
			int termIndex = 0;
			for (String term : terms) {
				termResults[termIndex] = getPostings(term);

				// Calculate IDF terms
				int df = termResults[termIndex].size();
				idf[termIndex] = (df < 1) ? 0 : Math.log10((double) numDocuments / df) + 1;

				for (PostingsEntry entry : termResults[termIndex].list) {
					// Determine index mapping for document
					Integer idx = resultDocIds.get(entry.docID);
					if (idx == null) // assign next available index
						resultDocIds.put(entry.docID, currentDocIdx++);
				}

				++termIndex;
			}

			int numResultDocIDs = resultDocIds.size();

			double[] scores = new double[numResultDocIDs];

			// Calculate scores for each document (in regards to each search term)
			termIndex = 0;
			for (PostingsList termResult : termResults) {
				for (PostingsEntry entry : termResult.list) {
					// Determine index mapping for document
					Integer idx = resultDocIds.get(entry.docID);
					if (idx == null) // assign next available index
						resultDocIds.put(entry.docID, currentDocIdx++);

					// Compute document score in regard to this term
					int qTF = termCounts.get(terms[termIndex]);
					int dTF = entry.getFrequency();
					int dLength = docLengths.get("" + entry.docID);
					scores[idx] += qTF / Math.sqrt(termsLength) * dTF / Math.sqrt(dLength) * idf[termIndex];
				}

				// Merge (union) each PostingsList
				result = (result == null) ? termResult : result.unionWith(termResult);

				++termIndex;
			}

			// Assign score to corresponding document (postings) entries
			for (PostingsEntry pe : result.list)
				pe.score = scores[resultDocIds.get(pe.docID)];

			// Sort documents according to their similarity score.
			Collections.sort(result.list);
		}

		return (result == null) ? new PostingsList() : result;
	}


	/**
	 *  No need for cleanup in a HashedIndex.
	 */
	public void cleanup() {
	}
}
