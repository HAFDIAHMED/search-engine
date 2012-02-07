/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2010
 *   Second version: Johan Boye, 2012
 */  

package ir;

import com.larvalabs.megamap.MegaMapManager;
import com.larvalabs.megamap.MegaMap;
import com.larvalabs.megamap.MegaMapException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Set;


public class MegaIndex implements Index {

	/** 
	 *  The index as a hash map that can also extend to secondary 
	 *		memory if necessary. 
	 */
	private MegaMap index;


	/** 
	 *  The MegaMapManager is the user's entry point for creating and
	 *  saving MegaMaps on disk.
	 */
	private MegaMapManager manager;


	/** The directory where to place index files on disk. */
	private static final String path = "./index";


	/**
	 *  Create a new index and invent a name for it.
	 */
	public MegaIndex() {
		try {
			manager = MegaMapManager.getMegaMapManager();
			index = manager.createMegaMap( generateFilename(), path, true, false );
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}


	/**
	 *  Create a MegaIndex, possibly from a list of smaller
	 *  indexes.
	 */
	public MegaIndex( LinkedList<String> indexfiles ) {
		try {
			manager = MegaMapManager.getMegaMapManager();
			if ( indexfiles.size() == 0 ) {
				// No index file names specified. Construct a new index and
				// invent a name for it.
				index = manager.createMegaMap( generateFilename(), path, true, false );
				
			}
			else if ( indexfiles.size() == 1 ) {
				// Read the specified index from file
				index = manager.createMegaMap( indexfiles.get(0), path, true, false );
				HashMap<String,String> m = (HashMap<String,String>)index.get( "..docIDs" );
				if ( m == null ) {
					System.err.println( "Couldn't retrieve docIDs" );
				}
				else {
					docIDs.putAll( m );
				}
			}
			else {
				// Merge the specified index files into a large index.
				MegaMap[] indexesToBeMerged = new MegaMap[indexfiles.size()];
				for ( int k=0; k<indexfiles.size(); k++ ) {
					System.err.println( indexfiles.get(k) );
					indexesToBeMerged[k] = manager.createMegaMap( indexfiles.get(k), path, true, false );
				}
				index = merge( indexesToBeMerged );
				for ( int k=0; k<indexfiles.size(); k++ ) {
					manager.removeMegaMap( indexfiles.get(k) );
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 *  Generates unique names for index files
	 */
	private String generateFilename() {
		String s = "" + Math.abs((new java.util.Date()).hashCode());
		System.err.println(s);
		return s;
	}


	/**
	 *   It is ABSOLUTELY ESSENTIAL to run this method before terminating 
	 *   the JVM, otherwise the index files might become corrupted.
	 */
	public void cleanup() {
		// Save the docID-filename association list in the MegaMap as well
		index.put("..docIDs", docIDs);
		// Shutdown the MegaMap thread gracefully
		manager.shutdown();
	}

	/**
	 *  Merges several indexes into one.
	 */
	MegaMap merge(MegaMap[] indexes) {
		try {
			MegaMap res = manager.createMegaMap(generateFilename(), path, true, false);
			for (MegaMap map : indexes)
				combineMaps(res, map);
			return res;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Mutates target map by adding from-entries. */
	private void combineMaps(MegaMap map, MegaMap from) {
		Iterator<String> it = from.getKeys().iterator();

		while (it.hasNext()) {
			String key = it.next();

			if (key.equals("..docIDs")) {
				try {
					HashMap<String,String> m = (HashMap<String,String>) from.get("..docIDs");
					docIDs.putAll(m);
				} catch (Exception e) {
					System.err.println("Couldn't retrieve docIDs");
				}
				continue;
			}

			PostingsList fromVal = null;
			try {
				fromVal = (PostingsList) from.get(key);
			} catch (MegaMapException e) {
				continue;
			}
			try {
				PostingsList mapVal = (PostingsList) map.get(key);
				map.put(key, mapVal.unionWith(fromVal));
			} catch (Exception e) {
				map.put(key, fromVal);
			}
		}
	}

	/**
	 *  Inserts this token in the index.
	 */
	public void insert(String token, int docID, int offset) {
		PostingsList list = null;

		try {
			list = (PostingsList) index.get(token);
		} catch(MegaMapException e) {}

		if (list == null) {
			list = new PostingsList();
			index.put(token, list);
		}

		//System.out.println("inserting " + token + ": " + docID + ":" + offset);

		list.add(docID, offset);
		//index.put(token, list); // TODO: necessary?
	}

	/**
	 *  Returns the postings for a specific term, or null
	 *  if the term is not in the index.
	 */
	public PostingsList getPostings(String token) {
		try {
			PostingsList ret = (PostingsList) index.get(token);
			if (ret == null) throw new Exception();
			return ret;
		} catch(Exception e) {
			return new PostingsList();
		}
	}

	/**
	 *  Searches the index for postings matching the query in @code{searchterms}.
	 */
	public PostingsList search(LinkedList<String> searchterms, int queryType) {
		PostingsList result = null;
		//System.out.println(index.getKeys().size());

		// Word queries
		for (String term : searchterms) {
			if (result == null)
				result = getPostings(term);
			else
				result = result.intersect(getPostings(term), queryType == Index.PHRASE_QUERY);
		}

		return (result == null) ? new PostingsList() : result;
	}
}

