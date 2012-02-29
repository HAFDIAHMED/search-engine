/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 *   This version: Victor Hallberg, Johan Stjernberg
 */  

import java.util.*;
import java.io.*;

public class PageRank {

	/**  
	 *   Maximal number of documents. We're assuming here that we
	 *   don't have more docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/** Number of documents */
	int numDocs = 0;

	/**
	 *   Mapping from document names to document numbers.
	 */
	Hashtable<String,Integer> docNumbers = new Hashtable<String,Integer>();

	/**
	 *   Mapping from document numbers to document names
	 */
	String[] docNames = new String[MAX_NUMBER_OF_DOCS];

	/**  
	 *   A memory-efficient representation of the transition matrix.
	 *   The outlinks are represented as a Hashtable, whose keys are
	 *   the numbers of the documents linked from.
	 *
	 *   The value corresponding to key i is a Hashtable whose keys are
	 *   all the numbers of documents j that i links to.
	 *
	 *   If there are no outlinks from i, then the value corresponding
	 *   key i is null.
	 */
	Hashtable<Integer,Hashtable<Integer,Boolean>> links = new Hashtable<Integer,Hashtable<Integer,Boolean>>();

	/**
	 *   The number of outlinks from each node.
	 */
	int[] numOutLinks = new int[MAX_NUMBER_OF_DOCS];

	/**
	 *   The number of documents with no outlinks.
	 */
	int numSinks = 0;

	/**
	 *   The probability that the surfer will be bored, stop
	 *   following links, and take a random jump somewhere.
	 */
	final static double BORED = 0.15;

	/**
	 *   Convergence criterion: Transition probabilities do not 
	 *   change more than EPSILON from one iteration to another.
	 */
	final static double EPSILON = 0.0001;

	/**
	 *   Never do more than this number of iterations regardless
	 *   of whether the transistion probabilities converge or not.
	 */
	final static int MAX_NUMBER_OF_ITERATIONS = 1000;


	/* --------------------------------------------- */

	/*
	 *   Computes the pagerank of each document.
	 */
	void computePagerank(boolean approximate) {
		System.out.println("computePagerank starting");

		// Rank array (x)
		double[] rank = new double[numDocs];
		double[] prev = new double[numDocs];

		// Starting probabilities
		double startingProb = (double) 1 / numDocs;
		for (int i = 0; i < numDocs; ++i)
			rank[i] = startingProb;
		//rank[0] = 1;

		int iter = 0;

		while (iter++ < MAX_NUMBER_OF_ITERATIONS) {
			// Print page ranks of all documents
			System.out.println("Before iteration " + iter + ":");
			for (int i = 0; i < numDocs; ++i)
				System.out.println(docNames[i] +
				                   "\t rank: " + String.format("%.5f", rank[i]) +
				                   "\t " + numOutLinks[i] + " outgoing");
			System.out.println("=================================================");

			// Reached stable state? (|x-x'| < epsilon)
			double res = 0.0;
			for (int i = 0; i < numDocs; ++i)
				res += Math.pow(rank[i] - prev[i], 2);
			if (Math.sqrt(res) <= EPSILON) {
				System.out.println("Reached stable state after " + iter + " iterations.");
				break;
			}

			// Use power iteration to compute x' = xG;
			// links -> hashtable[doc] -> hashtable[links in doc] -> true
			double[] next = new double[numDocs];

			for (int p = 0; p < numDocs; ++p) {
				Hashtable<Integer,Boolean> pOut = links.get(p);

				if (pOut == null)
					continue;

				// Increment page rank for all pages which p links to
				for (int q = 0; q < numDocs; ++q) {
					// Page p links to page q
					if (pOut.containsKey(q))
						next[q] += rank[p] * (1-BORED) / numOutLinks[p];
				}

				// Adjust page ranks according to formula to enable jumps due to boredom
				next[p] += BORED/numDocs;
				if (approximate)
					next[p] += (double) numSinks / numDocs / numDocs;
			}

			// x = x'
			prev = rank;
			rank = next;
		} // iterations


		// Normalize ranks
		double sum = 0;
		for (int p = 0; p < numDocs; ++p)
			sum += rank[p];
		for (int i = 0; i < numDocs; ++i)
			rank[i] /= sum;

		// Print page ranks of all documents
		for (int i = 0; i < numDocs; ++i)
			System.out.println(docNames[i] +
												 "\t rank: " + String.format("%.5f", rank[i]) +
												 "\t " + numOutLinks[i] + " outgoing");
	}

	
	/* --------------------------------------------- */


	public PageRank( String filename ) {
		readDocs( filename );
		computePagerank(false);
	}


	/* --------------------------------------------- */


	/**
	 *   Reads the documents and creates the docs table. When this method
	 *   finishes executing then the @code{numOutLinks} vector of outlinks is
	 *   initialised for each doc, and the @code{p} matrix is filled with
	 *   zeroes (that indicate direct links) and NO_LINK (if there is no
	 *   direct links.
	 */
	void readDocs( String filename ) {
		numDocs = 0;
		try {
			System.err.print( "Reading file... " );
			BufferedReader in = new BufferedReader( new FileReader( filename ));
			String line;
			while ((line = in.readLine()) != null && numDocs<MAX_NUMBER_OF_DOCS ) {
				int index = line.indexOf( ";" );
				String title = line.substring( 0, index );
				Integer fromdoc = docNumbers.get( title );
				//  Have we seen this document before?
				if ( fromdoc == null ) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = numDocs++;
					docNumbers.put( title, fromdoc );
					docNames[fromdoc] = title;
				}
				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
				while ( tok.hasMoreTokens() && numDocs<MAX_NUMBER_OF_DOCS ) {
					String otherTitle = tok.nextToken();
					Integer otherDoc = docNumbers.get( otherTitle );
					if ( otherDoc == null ) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = numDocs++;
						docNumbers.put( otherTitle, otherDoc );
						docNames[otherDoc] = otherTitle;
					}
					// Set the probability to 0 for now, to indicate that there is
					// a links from fromdoc to otherDoc.
					if ( links.get(fromdoc) == null ) {
						links.put(fromdoc, new Hashtable<Integer,Boolean>());
					}
					if ( links.get(fromdoc).get(otherDoc) == null ) {
						links.get(fromdoc).put( otherDoc, true );
						numOutLinks[fromdoc]++;
					}
				}
			}
			if ( numDocs >= MAX_NUMBER_OF_DOCS ) {
				System.err.print( "stopped reading since documents table is full. " );
			}
			else {
				System.err.print( "done. " );
			}
			// Compute the number of sinks.
			for ( int i=0; i<numDocs; i++ ) {
				if ( numOutLinks[i] == 0 )
					numSinks++;
			}
		}
		catch ( FileNotFoundException e ) {
			System.err.println( "File " + filename + " not found!" );
		}
		catch ( IOException e ) {
			System.err.println( "Error reading file " + filename );
		}
		System.err.println( "Read " + numDocs + " number of documents" );
	}


	public static void main( String[] args ) {
		if ( args.length != 1 ) {
			System.err.println( "Please give the name of the links file" );
		}
		else {
			new PageRank( args[0] );
		}
	}
}
