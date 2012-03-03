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
	int numDocs = 0;

	/**
	 *   Mapping from document names to document numbers.
	 */
	HashMap<String,Integer> docNumbers = new HashMap<String,Integer>();

	/**
	 *   Mapping from document numbers to document names
	 */
	String[] docNames = new String[MAX_NUMBER_OF_DOCS];

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
	HashMap<Integer,HashSet<Integer>> links = new HashMap<Integer,HashSet<Integer>>();

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
	
	/**
	 * Ranks for all documents after PageRank calculations.
	 */
	private double[] rank;
	
	/**
	 * VERBOSE = The probabilities for all documents are printed.
	 * NORMAL = The final results from all computations are printed.
	 * NONE = No ouput is printed.
	 */
	public enum OUTPUT { VERBOSE, NORMAL, NONE };
	
	/**
	 * Current choice of output type. Default is none.
	 */
	private OUTPUT outputType = OUTPUT.NONE;
	
	public enum ALGORITHM { 
		/** Run all algorithms and print comparison. */
		ALL, 
		
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

	/** If true, comparison will be made between page rank run and MC run.
	 * If false, rank will be saved by the algorithm passed to the constructor. */
	private boolean RUN_ALL = false;

	/* --------------------------------------------- */

	/*
	 *   Computes the pagerank of each document.
	 */
	void computePagerank(boolean approximate) {
		print(OUTPUT.NORMAL, "Computing PageRank starting");

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
			// Print page ranks of all documents
			
			if (outputType == OUTPUT.VERBOSE) {
				System.out.println("Before iteration " + iter + ":");
				for (int i = 0; i < numDocs; ++i)
					System.out.println(docNames[i] +
					                   "\t rank: " + String.format("%.5f", rank[i]) +
					                   "\t " + numOutLinks[i] + " outgoing");
				System.out.println("=================================================");
			}

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


		// Normalize ranks
		double sum = 0;
		for (int p = 0; p < numDocs; ++p)
			sum += rank[p];
		for (int i = 0; i < numDocs; ++i)
			rank[i] /= sum;

		// Print page ranks of all documents
		double total = 0.0;
		for (int i = 0; i < numDocs; ++i) {
			total += rank[i];
			print(OUTPUT.VERBOSE, docNames[i] +
					"\t rank: " + String.format("%.5f", rank[i]) +
					"\t " + numOutLinks[i] + " outgoing");			
		}
		print(OUTPUT.NORMAL, "Sum of all probabilities: " + total);
	}

	
	/**
	 *	Monte Carlo Complete Path implementation
	 *	Note: actual amount of path walks are runs * numDocs.
	 */
	private void mcCompletePathCyclicStart(int runs) {
		int[] hitCount = new int[numDocs];
		for (int i = 0; i < runs; i++) {
			for (int j = 0; j < numDocs; j++) {
				// start walk at j
				int curr = j;
				hitCount[curr]++;
				for (int k = 0; k < numDocs - 1; k++) {
					curr = mcMove(curr, false);
					hitCount[curr]++;
				}
			}
		}
		mcPrintAndSaveResult(hitCount, runs * numDocs * numDocs, "Complete Path (Cyclic Start)");
	}

	/**
	 *	Monte Carlo Complete Path Random start implementation.
	 */
	private void mcCompletePathRandomStart(int runs) {
		int[] hitCount = new int[numDocs];
		Random random = new Random();
		for (int i = 0; i < runs; i++) {
			// start walk at random
			int curr = random.nextInt(numDocs);
			hitCount[curr]++;
			for (int k = 0; k < numDocs - 1; k++) {
				curr = mcMove(curr, false);
				hitCount[curr]++;
			}
		}
		mcPrintAndSaveResult(hitCount, runs * numDocs, "Complete Path Random Start");
	}

	/**
	 *	Monte Carlo Complete Path Dangling Nodes implementation
	 *	Note: actual amount of path walks are runs * numDocs.
	 */
	private void mcCompletePathDanglingNodes(int runs) {
		int[] hitCount = new int[numDocs];
		int n = 0;
		for (int i = 0; i < runs; i++) {
			for (int j = 0; j < numDocs; j++) {
				// start walk at j
				int curr = j;
				hitCount[curr]++;
				n++;
				for (int k = 0; k < numDocs - 1; k++) {
					curr = mcMove(curr, true);
					if (curr == -1) {
						break;
					}
					hitCount[curr]++;
					n++;
				}
			}
		}
		mcPrintAndSaveResult(hitCount, n, "Complete Path Dangling Nodes");
	}
	
	/**
	 *	Monte Carlo End-Point Cyclic start implementation
	 *	Note: actual amount of path walks are runs * numDocs.
	 */
	private void mcEndPointCyclicStart(int runs) {
		int[] hitCount = new int[numDocs];
		for (int i = 0; i < runs; i++) {
			for (int j = 0; j < numDocs; j++) {
				// start walk at j
				int curr = j;
				for (int k = 0; k < numDocs - 1; k++) {
					curr = mcMove(curr, false);
				}
				hitCount[curr]++;
			}
		}
		mcPrintAndSaveResult(hitCount, runs * numDocs, "End-Point Cyclic Start");
	}
	
	/**
	 *	Monte Carlo End-Point Random start implementation.
	 */
	private void mcEndPointRandomStart(int runs) {
		int[] hitCount = new int[numDocs];
		Random random = new Random();
		for (int i = 0; i < runs; i++) {
			// start walk at random
			int curr = random.nextInt(numDocs);
			for (int k = 0; k < numDocs - 1; k++) {
				curr = mcMove(curr, false);
			}
			hitCount[curr]++;
		}
		mcPrintAndSaveResult(hitCount, runs, "End-Point Random Start");
	}


	/**
	 * Print the results of a Monte Carlo algorithm and/or saves it to this 
	 * PageRank object if not running all algorithms.
	 * 
	 * @param hitCount The amounts of hits for each document.
	 * @param totalHits The total amount of hits for all documents.
	 * @param name The name of this variant of the Monte Carlo algorithm.
	 */
	private void mcPrintAndSaveResult(int[] hitCount, int totalHits, String name) {
		if (!RUN_ALL) {
			rank = new double[numDocs];
		}
		
		print(OUTPUT.NORMAL, "=================================================");
		print(OUTPUT.NORMAL, "Monte Carlo " + name + " result");
		print(OUTPUT.NORMAL, "=================================================");
		double total = 0.0;
		double diff = 0.0, diffTotal = 0.0;
		double[] result = new double[numDocs];
		for (int i = 0; i < numDocs; i++) {
			result[i] = (double) hitCount[i] / totalHits;
			total += result[i];
			if (RUN_ALL) { // Calculate diff
				diff = Math.abs(rank[i]-result[i]);
				diffTotal += diff;
			} else { // Save result
				rank[i] = result[i];
			}
			print(OUTPUT.VERBOSE, docNames[i] +
				"\t rank: " + String.format("%.5f", result[i]) +
				"\t diff: " + (RUN_ALL ? String.format("%.5f", diff) : "[unknown]") +
				"\t " + numOutLinks[i] + " outgoing");
		}
		print(OUTPUT.NORMAL, "Sum of all probabilities: " + total);
		if (RUN_ALL)
			print(OUTPUT.NORMAL, "Total diff from Page Rank: " + diffTotal);
		print(OUTPUT.NORMAL, "=================================================");
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
		Random random = new Random();
		double chance = random.nextDouble();
		if (stopAtDangling && numOutLinks[currentDoc] == 0 )
			return -1; // stop walk here
		if (chance < BORED || numOutLinks[currentDoc] == 0) { 
			// Make a random move
			return random.nextInt(numDocs);
		}
		int nextI = random.nextInt(numOutLinks[currentDoc]);
		Iterator<Integer> outLinks = links.get(currentDoc).iterator();
		for(int i = 0; outLinks.hasNext(); i++) {
			Integer outLink = outLinks.next();
			if (i == nextI) {
				return outLink.intValue();
			}
		}
		System.err.println("Error: mcMove failed to generate a move.");
		System.exit(1);
		return -1; //error
	}

	
	private void print(OUTPUT type, String text) {
		if (outputType == type || outputType == OUTPUT.VERBOSE) {
			System.out.println(text);
		}
	}
	
	
	/* --------------------------------------------- */


	/**
	 * Default constructor. Read the file and compute page rank.
	 * 
	 * @param filename Location of file to read.
	 */
	public PageRank(String filename) {
		this(filename, ALGORITHM.PAGE_RANK);
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
		outputType = OUTPUT.NONE; // default is no output
		readDocs(filename);
		switch (alg) {
			case ALL:
				runAll();
				break;
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
				computePagerank(false);
				break;
		}
	}
	
	public void runAll() {
		outputType = OUTPUT.NORMAL;
		RUN_ALL = true;
		computePagerank(false);
		mcEndPointRandomStart(10 * numDocs);
		mcEndPointCyclicStart(10);
		mcCompletePathCyclicStart(10);
		mcCompletePathDanglingNodes(10);
		mcCompletePathRandomStart(10 * numDocs);
	}

	/**
	 * Fetch page rank for specific document.
	 * 
	 * @param document The name of the document.
	 * @return The page rank, or null if document was not found.
	 */
	public Double get(String document) {
		Integer docID = docNumbers.get(document);
		if (docID == null) {
			return null;
		}
		return new Double(rank[docID]);
	}

	/* --------------------------------------------- */



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
			print(OUTPUT.NORMAL, "Reading file... ");
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
			else 
				print(OUTPUT.NORMAL, "Done.");

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
		print(OUTPUT.NORMAL, "Read " + numDocs + " number of documents");
	}


	public static void main(String[] args) {
		if (args.length != 1)
			System.err.println("Please give the name of the links file");
		else 
			new PageRank(args[0], ALGORITHM.ALL);
	}
}
