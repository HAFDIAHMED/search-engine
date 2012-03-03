/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version:  Johan Boye, 2012
 *   This version: Victor Hallberg, Johan Stjernberg
 */  

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

public class PageRank {

	/**  
	 *   Maximal number of documents. We're assuming here that we
	 *   don't have more docs than we can keep in main memory.
	 */
	final static int MAX_NUMBER_OF_DOCS = 2000000;

	/** Number of documents */
	public int numDocs = 0;
	
	/**
	 * Ranks for all documents after PageRank calculations.
	 */
	public double[] rank;

	/**
	 *   Mapping from document names to document numbers.
	 */
	private HashMap<String,Integer> docNumbers = new HashMap<String,Integer>();

	/**
	 *   Mapping from document numbers to document names
	 */
	private String[] docNames = new String[MAX_NUMBER_OF_DOCS];

	/**  
	 *   A memory-efficient representation of the transition matrix.
	 *   The outlinks are represented as a HashMap, whose keys are
	 *   the numbers of the documents linked from.
	 *
	 *   The value corresponding to key i is a HashMap whose keys are
	 *   all the numbers of documents j that i links to.
	 *
	 *   If there are no outlinks from i, then the value corresponding
	 *   key i is null.
	 */
	private HashMap<Integer,HashSet<Integer>> links = new HashMap<Integer,HashSet<Integer>>();

	/**
	 *   The number of outlinks from each node.
	 */
	private int[] numOutLinks = new int[MAX_NUMBER_OF_DOCS];

	/**
	 *   The number of documents with no outlinks.
	 */
	private int numSinks = 0;

	/**
	 *   The probability that the surfer will be bored, stop
	 *   following links, and take a random jump somewhere.
	 */
	private final static double BORED = 0.15;

	/**
	 *   Convergence criterion: Transition probabilities do not 
	 *   change more than EPSILON from one iteration to another.
	 */
	private final static double EPSILON = 0.0001;

	/**
	 *   Never do more than this number of iterations regardless
	 *   of whether the transistion probabilities converge or not.
	 */
	private final static int MAX_NUMBER_OF_ITERATIONS = 1000;
	
	public enum ALGORITHM { 
		/** Page rank. */
		PAGE_RANK, 
		
		/** Monte Carlo End-Point Random start */
		MC_END_RANDOM, 
		
		/** Monte Carlo End-Point Cyclic start */
		MC_END_CYCLIC, 
		
		/** Monte Carlo Complete Path Cyclic start */
		MC_COMPLETE_CYCLIC, 
		
		/** Monte Carlo Complete Path Dangling start */
		MC_COMPLETE_DANGLING, 
		
		/** Monte Carlo Complete path Random start */
		MC_COMPLETE_RANDOM
	};

	/** Random number generator */
	Random random = new Random();

	/**
	* VERBOSE = The probabilities for all documents are printed.
	* NORMAL = The final results from all computations are printed.
	* NONE = No ouput is printed.
	*/
	public enum OUTPUT { VERBOSE, NORMAL, NONE };

	/**
	* Current choice of output type. Default is none.
	*/
	private OUTPUT outputType = OUTPUT.NORMAL;

	/* --------------------------------------------- */

	/**
	 * Fetch page rank for specific document.
	 * 
	 * @param document The name of the document.
	 * @return The page rank, or null if document was not found.
	 */
	public Double get(String document) {
		Integer docID = docNumbers.get(document);
		return (docID == null) ? null : new Double(rank[docID]);
	}

	/*
	 *   Computes the pagerank of each document.
	 */
	private void computeRank(boolean approximate) {
		print(OUTPUT.NORMAL, "Probabilistic PageRank " + (approximate ? "(approximate)" : "(exact)"));

		// Rank array (x)
		rank = new double[numDocs];
		double[] prev = new double[numDocs]; // used for stable state checking

		// Starting probabilities
		double startingProb = (double) 1 / numDocs;
		for (int i = 0; i < numDocs; ++i)
			rank[i] = startingProb;
		//rank[0] = 1;

		int iter = 0;

		while (iter++ < MAX_NUMBER_OF_ITERATIONS || !approximate) {
			// Reached stable state? (|x-x'| < epsilon)
			double res = 0.0;
			for (int i = 0; i < numDocs; ++i)
				res += Math.pow(rank[i] - prev[i], 2);
			if (Math.sqrt(res) <= EPSILON) {
				print(OUTPUT.NORMAL, "Reached stable state after " + iter + " iterations.");
				break;
			}

			// Use power iteration to compute x' = xG;
			// links -> hashtable[doc] -> hashtable[links in doc] -> true
			double[] next = new double[numDocs];

			for (int p = 0; p < numDocs; ++p) {
				HashSet<Integer> pOut = links.get(p);

				if (pOut == null)
					continue;

				// Increment page rank for all pages which p links to
				for (int q = 0; q < numDocs; ++q) {
					// Page p links to page q
					if (pOut.contains(q))
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

		double sum = 0;
		for (int p = 0; p < numDocs; ++p)
			sum += rank[p];
		normalizeRank(sum);

		// Print page ranks of all documents
		/*
		if (outputType != OUTPUT.NONE) {
			double total = 0.0;
			for (int i = 0; i < numDocs; ++i) {
				total += rank[i];
				print(OUTPUT.VERBOSE, docNames[i] +
						"\t rank: " + String.format("%.5f", rank[i]) +
						"\t " + numOutLinks[i] + " outgoing");			
			}
			print(OUTPUT.NORMAL, "Sum of all probabilities: " + total);
		}
		*/
	}

	/**
	 *  Normalizes rank to a uniform distribution (sums up to 1.0)
	 */
	private void normalizeRank(double denom) {
		for (int i = 0; i < numDocs; ++i)
			rank[i] /= denom;
	}

	/**
	 *	Monte Carlo Complete Path implementation
	 *	Note: actual amount of path walks are runs * numDocs.
	 */
	private void mcCompletePathCyclicStart(int runs) {
		print(OUTPUT.NORMAL, "Monte Carlo Complete Path (" + runs + " runs)");

		rank = new double[numDocs];
		for (int i = 0; i < runs; i++) {
			for (int j = 0; j < numDocs; j++) {
				// start walk at j
				int curr = j;
				rank[curr]++;
				for (int k = 0; k < numDocs - 1; k++) {
					curr = mcMove(curr, false);
					rank[curr]++;
				}
			}
		}
		normalizeRank(runs * numDocs * numDocs);
	}

	/**
	 *	Monte Carlo Complete Path Random start implementation.
	 */
	private void mcCompletePathRandomStart(int runs) {
		print(OUTPUT.NORMAL, "Monte Carlo Complete Path Random start (" + runs + " runs)");

		rank = new double[numDocs];
		for (int i = 0; i < runs; i++) {
			// start walk at random
			int curr = random.nextInt(numDocs);
			rank[curr]++;
			for (int k = 0; k < numDocs - 1; k++) {
				curr = mcMove(curr, false);
				rank[curr]++;
			}
		}
		normalizeRank(n);
	}

	/**
	 *	Monte Carlo Complete Path Dangling Nodes implementation
	 *	Note: actual amount of path walks are runs * numDocs.
	 */
	private void mcCompletePathDanglingNodes(int runs) {
		print(OUTPUT.NORMAL, "Monte Carlo Complete Path Dangling Nodes (" + runs + " runs)");

		int[] rank = new int[numDocs];
		int n = 0;
		for (int i = 0; i < runs; i++) {
			for (int j = 0; j < numDocs; j++) {
				// start walk at j
				int curr = j;
				rank[curr]++;
				n++;
				for (int k = 0; k < numDocs - 1; k++) {
					curr = mcMove(curr, true);
					if (curr == -1)
						break;
					rank[curr]++;
					n++;
				}
			}
		}
		normalizeRank(n);
	}
	
	/**
	 *	Monte Carlo End-Point Cyclic start implementation
	 *	Note: actual amount of path walks are runs * numDocs.
	 */
	private void mcEndPointCyclicStart(int runs) {
		print(OUTPUT.NORMAL, "Monte Carlo End-Point Cyclic start (" + runs + " runs)");

		int[] rank = new int[numDocs];
		for (int i = 0; i < runs; i++) {
			for (int j = 0; j < numDocs; j++) {
				// start walk at j
				int curr = j;
				for (int k = 0; k < numDocs - 1; k++) {
					curr = mcMove(curr, false);
				}
				rank[curr]++;
			}
		}
		normalizeRank(runs * numDocs);
	}
	
	/**
	 *	Monte Carlo End-Point Random start implementation.
	 */
	private void mcEndPointRandomStart(int runs) {
		print(OUTPUT.NORMAL, "Monte Carlo End-Point Random start (" + runs + " runs)");

		int[] rank = new int[numDocs];
		for (int i = 0; i < runs; i++) {
			// start walk at random
			int curr = random.nextInt(numDocs);
			for (int k = 0; k < numDocs - 1; k++) {
				curr = mcMove(curr, false);
			}
			rank[curr]++;
		}
		normalizeRank(runs);
	}

	/**
	 * Make a Monte Carlo move.
	 * 
	 * @param currentDoc The document to move from.
	 * @param stopAtDangling Whether or not to stop if a dangling node is found.
	 * @return The document to move to. Can be the same as currentDoc.
	 * 		-1 if the node is dangling. System.exit(1) if no move is found.
	 */
	private int mcMove(int currentDoc, boolean stopAtDangling) {
		double chance = random.nextDouble();

		if (stopAtDangling && numOutLinks[currentDoc] == 0 )
			return -1; // stop walk here

		// Make a random move if bored
		if (chance < BORED || numOutLinks[currentDoc] == 0) 
			return random.nextInt(numDocs);

		int nextI = random.nextInt(numOutLinks[currentDoc]);
		Iterator<Integer> outLinks = links.get(currentDoc).iterator();
		for (int i = 0; outLinks.hasNext(); i++) {
			Integer outLink = outLinks.next();
			if (i == nextI)
				return outLink.intValue();
		}

		System.err.println("Error: mcMove failed to generate a move.");
		System.exit(1);
		return -1; //error
	}

	/**
	 * Default constructor. Read the file and compute page rank.
	 * 
	 * @param filename Location of file to read.
	 */
	public PageRank(String filename) {
		this(filename, ALGORITHM.MC_COMPLETE_CYCLIC);
	}
	
	/**
	 * Constructor. Read the file and compute page rank.
	 * 
	 * @param filename Location of file to read.
	 * @param alg The algorithm to use when calculating page rank or
	 * 			ALGORITHM.ALL if a comparison between all algorithms
	 * 			should be made.
	 */
	public PageRank(String filename, ALGORITHM alg) {
		print(OUTPUT.NORMAL, "Computing PageRank from " + filename);
		readDocs(filename);
		switch (alg) {
			case MC_COMPLETE_CYCLIC:
				mcCompletePathCyclicStart(10);
				break;
			case MC_COMPLETE_DANGLING:
				mcCompletePathDanglingNodes(10);
				break;
			case MC_COMPLETE_RANDOM:
				mcCompletePathRandomStart(10 * numDocs);
				break;
			case MC_END_CYCLIC:
				mcEndPointCyclicStart(10);
				break;
			case MC_END_RANDOM:
				mcEndPointRandomStart(10 * numDocs);
				break;
			default:
				computeRank(false);
				break;
		}
	}

	/**
	 *   Reads the documents and creates the docs table. When this method
	 *   finishes executing then the @code{numOutLinks} vector of outlinks is
	 *   initialised for each doc, and the @code{p} matrix is filled with
	 *   zeroes (that indicate direct links) and NO_LINK (if there is no
	 *   direct links.
	 */
	void readDocs(String filename) {
		numDocs = 0;
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;

			while ((line = in.readLine()) != null && numDocs < MAX_NUMBER_OF_DOCS) {
				int index = line.indexOf(";");
				if (index < 0) continue;
				String title = line.substring(0, index);
				Integer fromdoc = docNumbers.get(title);

				//  Have we seen this document before?
				if (fromdoc == null) {
					// This is a previously unseen doc, so add it to the table.
					fromdoc = numDocs++;
					docNumbers.put(title, fromdoc);
					docNames[fromdoc] = title;
				}

				// Check all outlinks.
				StringTokenizer tok = new StringTokenizer(line.substring(index+1), ",");
				while (tok.hasMoreTokens() && numDocs < MAX_NUMBER_OF_DOCS) {
					String otherTitle = tok.nextToken();

					Integer otherDoc = docNumbers.get(otherTitle);
					if (otherDoc == null) {
						// This is a previousy unseen doc, so add it to the table.
						otherDoc = numDocs++;
						docNumbers.put(otherTitle, otherDoc);
						docNames[otherDoc] = otherTitle;
					}

					// Set the probability to 0 for now, to indicate that there is
					// a links from fromdoc to otherDoc.
					HashSet<Integer> outLinks = links.get(fromdoc);
					if (outLinks == null) {
						outLinks = new HashSet<Integer>();
						links.put(fromdoc, outLinks);
					}
					if (!outLinks.contains(otherDoc)) {
						outLinks.add(otherDoc);
						numOutLinks[fromdoc]++;
					}
				}
			}
			if (numDocs >= MAX_NUMBER_OF_DOCS)
				System.err.print("stopped reading since documents table is full. ");

			// Compute the number of sinks.
			for (int i=0; i<numDocs; i++) {
				if (numOutLinks[i] == 0)
					numSinks++;
			}
		}
		catch (FileNotFoundException e) {
			System.err.println("File " + filename + " not found!");
		}
		catch (IOException e) {
			System.err.println("Error reading file " + filename);
		}
		print(OUTPUT.NORMAL, "Read " + numDocs + " documents");
	}

	private void print(OUTPUT type, String text) {
		if (outputType == type || outputType == OUTPUT.VERBOSE)
			System.out.println(text);
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Please give the name of the links file");
			return;
		}
	}
}
