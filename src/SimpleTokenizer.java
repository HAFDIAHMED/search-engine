/*  
 *  This file is part of the computer assignment for the
 *  Information Retrieval course at KTH.
 * 
 *  First version: Johan Boye, 2010
 */  

package ir;

import java.io.Reader;
import java.io.IOException;


public class SimpleTokenizer {

    /** 
     *	The size of the buffer should be considerably larger than
     *  the anticipated length of the longest token.
     */
    public static final int BUFFER_LENGTH = 10000;

    /** The reader from where tokens are read. */
    Reader reader;
    
    /** 
     *  Characters are read @code{BUFFER_LENGTH} characters at a
     *  time into @code{buf}.
     */
    char[] buf = new char[BUFFER_LENGTH];

    /** The current position in the buffer. */
    int ptr = 0;

    /** Starting position of current token, or -1 if we're between tokens. */
    int tokenStart = -1;

    /** The next token to emit. */
    String nextTok;

    /** @code{true} if we've started reading tokens. */
    boolean started_reading = false;

    /** Handling of non-standard characters */
    static final char[] special_char =
    { 'á', 'à', 'â', 'å', 'ä', 'é', 'è', 'ê', 'í', 'ñ', 'ö', 'ô', 'ü', 'ú', 'ù', 'û', 'Å', 'Ä', 'Ö', 165, 164, 8222, 182, 184, 732, 8211, 195 };

    /** 
     *  What special characters should be translated into. 
     *  NB: This array should have the same size as the one above!
     */
    static final char[] translation =
    { 'a', 'a', 'a', 'å', 'ä', 'e', 'e', 'e', 'i', 'n', 'ö', 'o', 'ü', 'u', 'u', 'u', 'å', 'ä', 'ö', 'å', 'ä', 'ä', 'ö', 'ö', 'ö', 'ö', '#' };



    public SimpleTokenizer( Reader reader ) {
	this.reader = reader;
    }


    /**
     *  Normalizes strings by converting to lower-case and removing 
     *  diacritics. Non-normalizable characters are translated into
     *  whitespace.
     */
    public static String normalize( String s ) {
	char[] buf = s.toCharArray(); 
	for ( int i=0; i<buf.length; i++ ) {
	    if ( !normalize( buf, i )) {
		buf[i] = ' ';
	    }
	}
	return new String( buf );
    }


    /** 
     *  Normalizes letters by converting to lower-case and removing 
     *  diacritics. This method is also used for checking whether a 
     *  character can occur in a token or not.
     *
     *  @return code{true} if the (normalized counterpart of the) character
     *   can occur within a token, and @code{false} otherwise.
     */

    public static boolean normalize( char[] buf, int ptr ) {
	char c = buf[ptr];
	if (( c >= '0' && c <= '9' ) ||
	    ( c >= 'a' && c <= 'z' )) {
	    return true;
	}
	else if ( c >= 'A' && c <= 'Z' ) {
	    buf[ptr] = (char)(c+32);
	    return true;
	}
	else {
	    for ( int i=0; i<special_char.length; i++ ) {
		if ( special_char[i] == c ) {
		    buf[ptr] = translation[i];
		    return true;
		}
	    }
	    return false;
	}
    }


    /**
     *  @return the @code{true} if there are more tokens to be
     *  read, and @code{false} otherwise.
     */
    public boolean hasMoreTokens() throws IOException {
	if ( !started_reading ) {
	    nextTok = readToken();
	    started_reading = true;
	}
	return ( nextTok != null );
    }
    

    /**
     *  @return a String containing the next token, or @code{null} if there
     *  are no more tokens.
     */
    public String nextToken() throws IOException { 
	String s = null;
	if ( !started_reading ) {
	    s = readToken();
	    if ( s != null ) {
		s = s.replaceAll( "#", "" );
		nextTok = readToken();
	    }
	    return s;
	}
	else {
	    if ( nextTok != null ) {
		s = nextTok.replaceAll( "#", "" );
	    }
	    nextTok = readToken();
	    return s;
	}
    }


    /**
     *  Reads the next token. 
     */ 
    private String readToken() throws IOException {
	if ( started_reading && buf[ptr] == 0 ) {
	    // No more tokens to be read
	    return null;
	}
	if ( !started_reading ) {
	    refillBuffer( 0 );
	    started_reading = true;
	}
	String token_found = null;
	while ( buf[ptr] != 0 ) {
	    if ( tokenStart < 0 ) {
		if ( !normalize( buf, ptr )) {
		    // Skip whitespace etc.
		    ptr++;
		}
		else {
		    // A token starts here
		    tokenStart = ptr++;
		}
	    }
	    else {
		if ( normalize( buf, ptr )) {
		    // We're in the middle of a token
		    ptr++;
		}
		else {
		    // End of token
		    token_found = new String( buf, tokenStart, ptr-tokenStart );
		    tokenStart = -1;
		    ptr++;
		}
	    }
	    if ( ptr == BUFFER_LENGTH ) {
		// The buffer has been read, so refill it
		if ( tokenStart >= 0 ) {
		    // We're in the middle of a token. Copy the parts
		    // of the token we have read already into the 
		    // beginning of the buffer.
		    java.lang.System.arraycopy( buf, tokenStart, buf, 0, BUFFER_LENGTH-tokenStart );
		    refillBuffer( BUFFER_LENGTH-tokenStart );
		    ptr = BUFFER_LENGTH-tokenStart;
		    tokenStart = 0;
		}
		else {
		    refillBuffer( 0 );
		    ptr = 0;
		}
	    }
	    if ( token_found != null ) {
		return token_found;
	    }
	}
	return null;
    }


    /**
     *  Refills the buffer and adds end_of_file "\0" at the appropriate place.
     */
    private void refillBuffer( int start ) throws IOException {
	int chars_read = reader.read( buf, start, BUFFER_LENGTH-start );
	if ( chars_read < BUFFER_LENGTH-start ) {
	    buf[chars_read] = 0;
	}
    }
}





