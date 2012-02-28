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
	 *  Searches the index for postings matching the query in @code{terms}.
	 */
	public PostingsList search(LinkedList<String> terms, int queryType) {
		PostingsList result = null;
		System.out.println("===================================================");

		// Normal word queries
		if (queryType != Index.RANKED_QUERY) {
			for (String term : terms) {
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
			HashMap<String, Integer> termCount = new HashMap<String, Integer>();
			for (String term : terms) {
				Integer count = termCount.get(term);          
				termCount.put(term, (count == null) ? 1 : count + 1);
			}

			int numTerms = termCount.size();
			double[] idf = new double[numTerms];

			// Query for each term separately
			PostingsList[] termResults = new PostingsList[numTerms];
			HashMap<Integer,Integer> resultDocIds = new HashMap<Integer,Integer>();
			int currentDocIdx = 0;

			// Query for each distinct term and create docID -> index mapping
			int termIndex = 0;
			for (String term : termCount.keySet()) {
				termResults[termIndex] = getPostings(term);

				int df = termResults[termIndex].size();
				// TODO: tweak constant
				idf[termIndex] = (df > 0) ? Math.log10((double) numDocuments / df) + 1 : 0;
				System.out.println("idf(" + term + ")=" + idf[termIndex] + " df="+df + " numDocuments="+numDocuments);

				for (PostingsEntry entry : termResults[termIndex].list) {
					// Determine index mapping for document
					Integer idx = resultDocIds.get(entry.docID);
					if (idx == null) // assign next available index
						resultDocIds.put(entry.docID, currentDocIdx++);
				}

				++termIndex;
			}

			int numResultDocIDs = resultDocIds.size();

			// TFIDF vectors
			double[] qv = new double[numTerms];
			double[][] dv = new double[numResultDocIDs][numTerms];

			// Calculate TFIDF for query terms
			termIndex = 0;
			int termsLength = terms.size();
			for (String term : termCount.keySet()) {
				// TODO: fix
				qv[termIndex] = (double) termCount.get(term) / termsLength * idf[termIndex];
				System.out.println("qv(" + term + ") tfidf=" + qv[termIndex] + " freq=" + termCount.get(term) + " len="+termsLength);
				++termIndex;
			}

			// Calculate TFIDF values for each document (in regards to each search term)
			termIndex = 0;
			for (PostingsList termResult : termResults) {
				for (PostingsEntry entry : termResult.list) {
					// Determine index mapping for document
					Integer idx = resultDocIds.get(entry.docID);
					if (idx == null) // assign next available index
						resultDocIds.put(entry.docID, currentDocIdx++);

					// TF_a * IDF
					dv[idx][termIndex] = (double) entry.getFrequency() / docLengths.get("" + entry.docID) * idf[termIndex];
					System.out.println("dv(" + idx + ":" + terms.get(termIndex) + ") tfidf=" + dv[idx][termIndex] + " freq=" + entry.getFrequency() + " len="+docLengths.get("" + entry.docID));
				}

				// Merge (union) each PostingsList
				result = (result == null) ? termResult : result.unionWith(termResult);

				++termIndex;
			}

			// Calculate cos similarity of each document
			for (PostingsEntry pe : result.list) {
				double nom = .0, denom1 = .0, denom2 = .0;
				for (int i = 0; i < numTerms; ++i) {
					double di = dv[resultDocIds.get(pe.docID)][i];
					nom += qv[i] * di;
					denom1 += qv[i] * qv[i];
					denom2 += di * di;
				}
				pe.score = nom / (Math.sqrt(denom1) * Math.sqrt(denom2));
				System.out.println("docId=" + pe.docID + " score=" + pe.score + " nom="+nom + " denom1="+denom1 + " denom2="+denom2);
			}

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
