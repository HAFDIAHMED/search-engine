/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   First version: Johan Boye, 2012
 */  


package ir;

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
	int queryType = Index.INTERSECTION_QUERY;

	/**  The index type (either hashed or mega). */
	int indexType = Index.HASHED_INDEX;

	/**  Lock to prevent simultaneous access to the index. */
	Object indexLock = new Object();

	/**  Directory from which the code is compiled and run. */
	public static final String homeDir = "D:/Dropbox/mogelshark/ir/hw1";


	/*
	 *   The nice logotype
	 */
	static final String IPIC = homeDir + "/pics/i.jpg";
	static final String RPIC = homeDir + "/pics/r.jpg";
	static final String TPIC = homeDir + "/pics/t.jpg";
	static final String WPIC = homeDir + "/pics/w.jpg";
	static final String EPIC = homeDir + "/pics/e.jpg";
	static final String LPIC = homeDir + "/pics/l.jpg";
	static final String VPIC = homeDir + "/pics/v.jpg";
	static final String BLANKPIC = homeDir + "/pics/blank.jpg";


	/*  
	 *   Common GUI resources
	 */
	public JTextField queryWindow = new JTextField( "", 28 );
	public JTextArea resultWindow = new JTextArea( "", 23, 28 );
	private JScrollPane resultPane = new JScrollPane( resultWindow );
	private Font queryFont = new Font( "Arial", Font.BOLD, 24 );
	private Font resultFont = new Font( "Arial", Font.BOLD, 16 );
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu( "File" );
	JMenu optionsMenu = new JMenu( "Search options" );
	JMenuItem saveItem = new JMenuItem( "Save index and exit" );
	JMenuItem quitItem = new JMenuItem( "Quit" );
	JRadioButtonMenuItem intersectionItem = new JRadioButtonMenuItem( "Intersection query" );
	JRadioButtonMenuItem phraseItem = new JRadioButtonMenuItem( "Phrase query" );
	JRadioButtonMenuItem rankedItem = new JRadioButtonMenuItem( "Ranked retrieval" );
	ButtonGroup queries = new ButtonGroup();


	/* ----------------------------------------------- */


	/*
	 *   Create the GUI.
	 */
	private void createGUI() {
		// GUI definition
		setSize( 600, 650 );
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		getContentPane().add(p, BorderLayout.CENTER);
		// Top menu
		menuBar.add( fileMenu );
		menuBar.add( optionsMenu );
		fileMenu.add( saveItem );
		fileMenu.add( quitItem );
		optionsMenu.add( intersectionItem );
		optionsMenu.add( phraseItem );
		optionsMenu.add( rankedItem );
		queries.add( intersectionItem );
		queries.add( phraseItem );
		queries.add( rankedItem );
		intersectionItem.setSelected( true );
		p.add( menuBar );
		// Logo
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS));
		p1.add( new JLabel( new ImageIcon( IPIC )));
		p1.add( new JLabel( new ImageIcon( RPIC )));
		p1.add( new JLabel( new ImageIcon( BLANKPIC )));
		p1.add( new JLabel( new ImageIcon( TPIC )));
		p1.add( new JLabel( new ImageIcon( WPIC )));
		p1.add( new JLabel( new ImageIcon( EPIC )));
		p1.add( new JLabel( new ImageIcon( LPIC )));
		p1.add( new JLabel( new ImageIcon( VPIC )));
		p1.add( new JLabel( new ImageIcon( EPIC )));
		p.add( p1 );
		JPanel p3 = new JPanel();
		// Search box
		p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
		p3.add( new JLabel( new ImageIcon( BLANKPIC )));
		p3.add( queryWindow );
		queryWindow.setFont( queryFont );
		p3.add( new JLabel( new ImageIcon( BLANKPIC )));
		p.add( p3 );
		// Display area for search results
		p.add( resultPane );
		resultWindow.setFont( resultFont );
		// Show the interface
		setVisible( true );

		Action search = new AbstractAction() {
				public void actionPerformed( ActionEvent e ) {
					// Normalize the search string and turn it into a linked list
					String searchstring = SimpleTokenizer.normalize( queryWindow.getText() );
					StringTokenizer tok = new StringTokenizer( searchstring );
					LinkedList<String> searchterms = new LinkedList<String>();
					while ( tok.hasMoreTokens() ) {
						searchterms.add( tok.nextToken() );
					}
					// Search and print results. Access to the index is synchronized since
					// we don't want to search at the same time we're indexing new files
					// (this might corrupt the index).
					PostingsList p;
					synchronized ( indexLock ) {
						p = indexer.index.search( searchterms, queryType );
					}
					StringBuffer buf = new StringBuffer();
					if ( p != null ) {
						buf.append( "\nFound " + p.size() + " matching document(s)\n\n" );
						for ( int i=0; i<p.size(); i++ ) {
							buf.append( " " + i + ". " );
							String filename = indexer.index.docIDs.get( "" + p.get(i).docID );
							if ( filename == null ) {
								buf.append( "" + p.get(i).docID );
							}
							else {
								buf.append( filename );
							}
							if ( queryType == Index.RANKED_QUERY ) {
								buf.append( "   " + String.format( "%.3f", p.get(i).score ));
							}
							buf.append( "  (" + p.get(i).offsets.size() + ")\n" );
						}
					}
					else {
						buf.append( "\nFound 0 matching document(s)\n\n" );
					}
					resultWindow.setText( buf.toString() );
					resultWindow.setCaretPosition( 0 );
				}
			};

		queryWindow.registerKeyboardAction( search,
											"",
											KeyStroke.getKeyStroke( "ENTER" ),
											JComponent.WHEN_FOCUSED );
		
		Action saveAndQuit = new AbstractAction() {
				public void actionPerformed( ActionEvent e ) {
					resultWindow.setText( "\n  Saving index..." );
					indexer.index.cleanup();
					System.exit( 0 );
				}
			};
		saveItem.addActionListener( saveAndQuit );
		
		
		Action quit = new AbstractAction() {
				public void actionPerformed( ActionEvent e ) {
					System.exit( 0 );
				}
			};
		quitItem.addActionListener( quit );

		
		Action setIntersectionQuery = new AbstractAction() {
				public void actionPerformed( ActionEvent e ) {
					queryType = Index.INTERSECTION_QUERY;
				}
			};
		intersectionItem.addActionListener( setIntersectionQuery );
				
		Action setPhraseQuery = new AbstractAction() {
				public void actionPerformed( ActionEvent e ) {
					queryType = Index.PHRASE_QUERY;
				}
			};
		phraseItem.addActionListener( setPhraseQuery );
				
		Action setRankedQuery = new AbstractAction() {
				public void actionPerformed( ActionEvent e ) {
					queryType = Index.RANKED_QUERY;
				}
			};
		rankedItem.addActionListener( setRankedQuery );

	}

 
	/* ----------------------------------------------- */
   

	/**
	 *   Calls the indexer to index the chosen directory structure.
	 *   Access to the index is synchronized since we don't want to 
	 *   search at the same time we're indexing new files (this might 
	 *   corrupt the index).
	 */
	private void index() {
		synchronized ( indexLock ) {
			resultWindow.setText( "\n  Indexing, please wait..." );
			for ( int i=0; i<dirNames.size(); i++ ) {
				File dokDir = new File( dirNames.get( i ));
				indexer.processFiles( dokDir );
			}
			resultWindow.setText( "\n  Done!" );
		}
	};


	/* ----------------------------------------------- */


	/**
	 *   Decodes the command line arguments.
	 */
	private void decodeArgs( String[] args ) {
		int i=0, j=0;
		while ( i < args.length ) {
			if ( "-i".equals( args[i] )) {
				i++;
				if ( j++ >= MAX_NUMBER_OF_INDEX_FILES ) {
					System.err.println( "Too many index files specified" );
					break;
				}
				if ( i < args.length ) {
					indexFiles.add( args[i++] );
				}
			} 
			else if ( "-d".equals( args[i] )) {
				i++;
				if ( i < args.length ) {
					dirNames.add( args[i++] );
				}
			}
			else if ( "-m".equals( args[i] )) {
				i++;
				indexType = Index.MEGA_INDEX;
			}
			else {
				System.err.println( "Unknown option: " + args[i] );
				break;
			}
		}
		//  It might take a long time to create a MegaIndex. Meanwhile no searches
		//  should be carried out (it would result in a NullPointerException).
		//  Therefore the access to the index must be synchronized.
		synchronized ( indexLock ) {
			if ( indexType == Index.HASHED_INDEX ) {
				indexer = new Indexer();
			}
			else {
				resultWindow.setText( "\n  Creating MegaIndex, please wait... " );
				indexer = new Indexer( indexFiles );
				resultWindow.setText( "\n  Done!" );
			}
		}
	}									


	/* ----------------------------------------------- */


	public static void main( String[] args ) {
		SearchGUI s = new SearchGUI();
		s.createGUI();
		s.decodeArgs( args );
		s.index();
	}

}
