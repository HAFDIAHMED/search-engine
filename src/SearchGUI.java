/**
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   First version: Johan Boye, 2012
 */

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *   A graphical interface to the information retrieval system.
 */
public class SearchGUI extends JFrame {

	/**  The indexer creating the search index. */
	Indexer indexer;

	/**  Directories that should be indexed. */
	LinkedList<String> dirNames = new LinkedList<String>();

	/**  Indices to be retrieved from disk. */
	LinkedList<String> indexFiles = new LinkedList<String>();

	/** Maximum number of indices we can read from disk. */
	public static final int MAX_NUMBER_OF_INDEX_FILES = 10;

	/**  The query type (either intersection, phrase, or ranked). */
	int queryType = Index.RANKED_QUERY;

	/**  The index type (either hashed or mega). */
	int indexType = Index.HASHED_INDEX;

	/**  Lock to prevent simultaneous access to the index. */
	Object indexLock = new Object();

	/** File containing link graph for PageRank */
	public String linksFile;

	/*
	 *   Common GUI resources
	 */
	public JTextField queryWindow = new JTextField("", 28);
	public JTextArea resultWindow = new JTextArea("", 23, 28);
	private JScrollPane resultPane = new JScrollPane(resultWindow);
	private Font queryFont = new Font("Arial", Font.BOLD, 20);
	private Font resultFont = new Font("Arial", Font.BOLD, 14);
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	JMenu optionsMenu = new JMenu("Search options");
	JMenuItem saveItem = new JMenuItem("Save index and exit");
	JMenuItem quitItem = new JMenuItem("Quit");
	JRadioButtonMenuItem intersectionItem = new JRadioButtonMenuItem("Intersection query");
	JRadioButtonMenuItem unionItem = new JRadioButtonMenuItem("Union query");
	JRadioButtonMenuItem phraseItem = new JRadioButtonMenuItem("Phrase query");
	JRadioButtonMenuItem rankedItem = new JRadioButtonMenuItem("Ranked retrieval");
	ButtonGroup queries = new ButtonGroup();
	Insets insets = new Insets(2, 3, 2, 3);


	/* ----------------------------------------------- */


	/*
	 *   Create the GUI.
	 */
	private void createGUI() {
		// GUI definition
		setSize(600, 650);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout(2, 2));
		getContentPane().add(p, BorderLayout.CENTER);

		// Top menu
		menuBar.add(fileMenu);
		menuBar.add(optionsMenu);
		fileMenu.add(saveItem);
		fileMenu.add(quitItem);
		optionsMenu.add(intersectionItem);
		optionsMenu.add(unionItem);
		optionsMenu.add(phraseItem);
		optionsMenu.add(rankedItem);
		queries.add(intersectionItem);
		queries.add(unionItem);
		queries.add(phraseItem);
		queries.add(rankedItem);
		rankedItem.setSelected(true);
		getContentPane().add(menuBar, BorderLayout.PAGE_START);

		// Query window
		queryWindow.setFont(queryFont);
		queryWindow.setMargin(insets);
		p.add(queryWindow, BorderLayout.PAGE_START);

		// Display area for search results
		resultWindow.setFont(resultFont);
		resultWindow.setEditable(false);
		resultWindow.setMargin(insets);
		p.add(resultPane, BorderLayout.CENTER);

		// Show the interface
		setVisible(true);

		Action search = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					// Normalize the search string and turn it into a linked list
					String searchstring = SimpleTokenizer.normalize(queryWindow.getText());
					StringTokenizer tok = new StringTokenizer(searchstring);
					LinkedList<String> searchterms = new LinkedList<String>();
					while (tok.hasMoreTokens()) {
						searchterms.add(tok.nextToken());
					}
					// Search and print results. Access to the index is synchronized since
					// we don't want to search at the same time we're indexing new files
					// (this might corrupt the index).
					PostingsList p;
					synchronized (indexLock) {
						p = indexer.index.search(searchterms, queryType);
					}
					StringBuffer buf = new StringBuffer();
					buf.append(searchstring + ": ");
					if (p != null) {
						buf.append(p.size() + " matching documents\n\n");
						for (int i=0; i<p.size(); i++) {
							PostingsEntry pe = p.get(i);
							String filename = indexer.index.docIDs.get("" + pe.docID);
							buf.append(String.format(
								"%6s  %s  ",
								i + 1,
								(filename == null ? pe.docID : filename)
							));
							if (queryType == Index.RANKED_QUERY)
								buf.append(String.format("(%.3f)", pe.score));
							else
								buf.append("(" + pe.offsets.size() + ")");
							buf.append("\n");
						}
					}
					else {
						buf.append("0 matching documents\n");
					}
					resultWindow.setText(buf.toString());
					resultWindow.setCaretPosition(0);
				}
			};

		queryWindow.registerKeyboardAction(search,
											"",
											KeyStroke.getKeyStroke("ENTER"),
											JComponent.WHEN_FOCUSED);
		
		Action saveAndQuit = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					resultWindow.setText("\n  Saving index...");
					indexer.index.cleanup();
					System.exit(0);
				}
			};
		saveItem.addActionListener(saveAndQuit);
		
		
		Action quit = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			};
		quitItem.addActionListener(quit);

		
		Action setIntersectionQuery = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					queryType = Index.INTERSECTION_QUERY;
				}
			};
		intersectionItem.addActionListener(setIntersectionQuery);

		Action setUnionQuery = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					queryType = Index.UNION_QUERY;
				}
			};
		unionItem.addActionListener(setUnionQuery);
				
		Action setPhraseQuery = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					queryType = Index.PHRASE_QUERY;
				}
			};
		phraseItem.addActionListener(setPhraseQuery);
				
		Action setRankedQuery = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					queryType = Index.RANKED_QUERY;
				}
			};
		rankedItem.addActionListener(setRankedQuery);

	}

 
	/* ----------------------------------------------- */
   

	/**
	 *   Calls the indexer to index the chosen directory structure.
	 *   Access to the index is synchronized since we don't want to 
	 *   search at the same time we're indexing new files (this might 
	 *   corrupt the index).
	 */
	private void index() {
		synchronized (indexLock) {
			resultWindow.setText("Indexing, please wait...");
			for (int i=0; i<dirNames.size(); i++) {
				File dokDir = new File(dirNames.get(i));
				indexer.processFiles(dokDir);
			}
			resultWindow.setText("Done!");
		}
	};


	/* ----------------------------------------------- */


	/**
	 *   Decodes the command line arguments.
	 */
	private void decodeArgs(String[] args) {
		String linksFile = null;
		int i=0, j=0;

		while (i < args.length) {
			if ("-i".equals(args[i])) {
				i++;
				if (j++ >= MAX_NUMBER_OF_INDEX_FILES) {
					System.err.println("Too many index files specified");
					break;
				}
				if (i < args.length)
					indexFiles.add(args[i++]);
			}
			else if ("-r".equals(args[i])) {
				if (++i < args.length)
					linksFile = args[i++];
			}
			else if ("-d".equals(args[i])) {
				if (++i < args.length)
					dirNames.add(args[i++]);
			}
			else if ("-m".equals(args[i])) {
				i++;
				indexType = Index.MEGA_INDEX;
			}
			else {
				System.err.println("Unknown option: " + args[i]);
				break;
			}
		}
		//  It might take a long time to create a MegaIndex. Meanwhile no searches
		//  should be carried out (it would result in a NullPointerException).
		//  Therefore the access to the index must be synchronized.
		synchronized (indexLock) {
			if (indexType == Index.HASHED_INDEX) {
				indexer = new Indexer();
			}
			else {
				resultWindow.setText("Creating MegaIndex, please wait...");
				indexer = new Indexer(indexFiles);
				resultWindow.setText("Done!");
			}
			if (linksFile != null) {
				resultWindow.setText("Generating PageRank...");
				indexer.index.setPageRank(new PageRank(linksFile, PageRank.ALGORITHM.MC_COMPLETE_CYCLIC));
				resultWindow.setText("Done!");
			}
		}
	}


	/* ----------------------------------------------- */


	public static void main(String[] args) {
		SearchGUI s = new SearchGUI();
		s.createGUI();
		s.decodeArgs(args);
		s.index();
	}

}
