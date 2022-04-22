package ng.appserver.templating;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGAssociation;
import ng.appserver.NGConstantValueAssociation;

public class NGHelperFunctionDeclarationParser {

	private static Logger logger = LoggerFactory.getLogger( NGHelperFunctionDeclarationParser.class );

	private final Map<String, String> _quotedStrings = new HashMap<>();
	private static final int STATE_OUTSIDE = 0;
	private static final int STATE_INSIDE_COMMENT = 2;
	private static final String ESCAPED_QUOTE_STRING = "_WO_ESCAPED_QUOTE_";
	private static final String QUOTED_STRING_KEY = "_WODP_";

	public static Map<String, NGDeclaration> declarationsWithString( String declarationStr ) throws NGHelperFunctionDeclarationFormatException {
		final NGHelperFunctionDeclarationParser declarationParser = new NGHelperFunctionDeclarationParser();
		return declarationParser.parseDeclarations( declarationStr );
	}

	public Map<String, NGDeclaration> parseDeclarations( String declarationStr ) throws NGHelperFunctionDeclarationFormatException {
		String strWithoutComments = _removeOldStyleCommentsFromString( declarationStr );
		strWithoutComments = _removeNewStyleCommentsAndQuotedStringsFromString( strWithoutComments );
		return parseDeclarationsWithoutComments( strWithoutComments );
	}

	private String _removeOldStyleCommentsFromString( String str ) {
		StringBuilder stringbuffer = new StringBuilder( 100 );
		StringBuilder stringbuffer1 = new StringBuilder( 100 );
		StringTokenizer tokenizer = new StringTokenizer( str, "/", true );
		int state = NGHelperFunctionDeclarationParser.STATE_OUTSIDE;
		try {
			do {
				if( !tokenizer.hasMoreTokens() ) {
					break;
				}
				String token = tokenizer.nextToken();
				switch( state ) {
				case STATE_OUTSIDE:
					if( token.equals( "/" ) ) {
						token = tokenizer.nextToken();
						if( token.startsWith( "*" ) ) {
							state = NGHelperFunctionDeclarationParser.STATE_INSIDE_COMMENT;
							stringbuffer1.append( '/' );
							stringbuffer1.append( token );
						}
						else {
							stringbuffer.append( '/' );
							stringbuffer.append( token );
						}
					}
					else {
						stringbuffer.append( token );
					}
					break;

				case STATE_INSIDE_COMMENT:
					stringbuffer1.append( token );
					String s2 = stringbuffer1.toString();
					if( s2.endsWith( "*/" ) && !s2.equals( "/*/" ) ) {
						state = NGHelperFunctionDeclarationParser.STATE_OUTSIDE;
					}
					break;
				}
			}
			while( true );
		}
		catch( NoSuchElementException e ) {
			logger.debug( "Parsing failed.", e );
		}
		return stringbuffer.toString();
	}

	private String _removeNewStyleCommentsAndQuotedStringsFromString( String declarationsStr ) {
		String escapedQuoteStr = declarationsStr.replace( "\\\"", NGHelperFunctionDeclarationParser.ESCAPED_QUOTE_STRING );
		StringBuilder declarationWithoutCommentsBuffer = new StringBuilder( 100 );
		StringTokenizer tokenizer = new StringTokenizer( escapedQuoteStr, "/\"", true );
		try {
			while( tokenizer.hasMoreTokens() ) {
				String token = tokenizer.nextToken( "/\"" );
				if( token.equals( "/" ) ) {
					token = tokenizer.nextToken( "\n" );
					if( token.startsWith( "/" ) ) {
						token = token.replace( NGHelperFunctionDeclarationParser.ESCAPED_QUOTE_STRING, "\\\"" );
						declarationWithoutCommentsBuffer.append( '\n' );
						tokenizer.nextToken();
					}
					else {
						declarationWithoutCommentsBuffer.append( '/' );
						declarationWithoutCommentsBuffer.append( token );
					}
				}
				else if( token.equals( "\"" ) ) {
					token = tokenizer.nextToken( "\"" );
					if( token.equals( "\"" ) ) {
						token = "";
					}
					else {
						tokenizer.nextToken();
					}
					String quotedStringKey = NGHelperFunctionDeclarationParser.QUOTED_STRING_KEY + _quotedStrings.size();
					token = token.replace( NGHelperFunctionDeclarationParser.ESCAPED_QUOTE_STRING, "\"" );
					_quotedStrings.put( quotedStringKey, token );
					declarationWithoutCommentsBuffer.append( quotedStringKey );
				}
				else {
					declarationWithoutCommentsBuffer.append( token );
				}
			}
		}
		catch( NoSuchElementException e ) {
			logger.debug( "Parsing failed.", e );
		}
		return declarationWithoutCommentsBuffer.toString();
	}

	private Map<String, NGDeclaration> parseDeclarationsWithoutComments( String declarationWithoutComment ) throws NGHelperFunctionDeclarationFormatException {
		final Map<String, NGDeclaration> declarations = new HashMap<>();
		final Map<String, String> rawDeclarations = _rawDeclarationsWithoutComment( declarationWithoutComment );

		String tagName;
		NGDeclaration declaration;

		final Enumeration<String> rawDeclarationHeaderEnum = Collections.enumeration( rawDeclarations.keySet() );

		while( rawDeclarationHeaderEnum.hasMoreElements() ) {
			String declarationHeader = rawDeclarationHeaderEnum.nextElement();
			String declarationBody = rawDeclarations.get( declarationHeader );
			int colonIndex = declarationHeader.indexOf( ':' );

			if( colonIndex < 0 ) {
				throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Missing ':' for declaration:\n" + declarationHeader + " " + declarationBody );
			}

			tagName = declarationHeader.substring( 0, colonIndex ).trim();

			if( tagName.length() == 0 ) {
				throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Missing tag name for declaration:\n" + declarationHeader + " " + declarationBody );
			}

			if( declarations.get( tagName ) != null ) {
				throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Duplicate tag name '" + tagName + "' in declaration:\n" + declarationBody );
			}

			String type = declarationHeader.substring( colonIndex + 1 ).trim();

			if( type.length() == 0 ) {
				throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Missing element name for declaration:\n" + declarationHeader + " " + declarationBody );
			}

			Map<String, NGAssociation> associations = _associationsForDictionaryString( declarationHeader, declarationBody );
			declaration = NGHelperFunctionParser.createDeclaration( tagName, type, associations );
			declarations.put( tagName, declaration );
		}

		return declarations;
	}

	private Map<String, NGAssociation> _associationsForDictionaryString( String declarationHeader, String declarationBody ) throws NGHelperFunctionDeclarationFormatException {
		final Map<String, NGAssociation> associations = new HashMap<>();
		String trimmedDeclarationBody = declarationBody.trim();

		if( !trimmedDeclarationBody.startsWith( "{" ) && !trimmedDeclarationBody.endsWith( "}" ) ) {
			throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Internal inconsistency : invalid dictionary for declaration:\n" + declarationHeader + " " + declarationBody );
		}

		int declarationBodyLength = trimmedDeclarationBody.length();

		if( declarationBodyLength <= 2 ) {
			return associations;
		}

		trimmedDeclarationBody = trimmedDeclarationBody.substring( 1, declarationBodyLength - 1 ).trim();
		List<String> bindings = Arrays.asList( trimmedDeclarationBody.split( ";" ) );
		Enumeration<String> bindingsEnum = Collections.enumeration( bindings );

		do {
			if( !bindingsEnum.hasMoreElements() ) {
				break;
			}
			String binding = bindingsEnum.nextElement().trim();
			if( binding.length() != 0 ) {
				int equalsIndex = binding.indexOf( '=' );
				if( equalsIndex < 0 ) {
					throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Invalid line. No equal in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}
				String key = binding.substring( 0, equalsIndex ).trim();
				if( key.length() == 0 ) {
					throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Missing binding in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}
				String value = binding.substring( equalsIndex + 1 ).trim();
				if( value.length() == 0 ) {
					throw new NGHelperFunctionDeclarationFormatException( "<WOHelperFunctionDeclarationParser> Missing value in line:\n" + binding + "\nfor declaration:\n" + declarationHeader + " " + declarationBody );
				}
				NGAssociation association = NGHelperFunctionDeclarationParser._associationWithKey( value, _quotedStrings );
				String quotedString = _quotedStrings.get( key );
				if( quotedString != null ) {
					associations.put( quotedString, association );
				}
				else {
					associations.put( key, association );
				}
			}
		}
		while( true );
		// if (log.isDebugEnabled()) {
		// log.debug("Parsed '" + s + "' declarations:\n" + nsmutabledictionary
		// + "\n--------");
		// }
		return associations;
	}

	public static NGAssociation _associationWithKey( String associationValue, Map<String, String> quotedStrings ) {
		NGAssociation association = null;
		if( associationValue != null && associationValue.startsWith( "~" ) ) {
			int associationValueLength = associationValue.length();
			StringBuilder value = new StringBuilder();
			int lastIndex = 0;
			int index = 0;
			while( (index = associationValue.indexOf( NGHelperFunctionDeclarationParser.QUOTED_STRING_KEY, lastIndex )) != -1 ) {
				value.append( associationValue.substring( lastIndex, index ) );
				int wodpValueStartIndex = index + NGHelperFunctionDeclarationParser.QUOTED_STRING_KEY.length();
				int wodpValueEndIndex = wodpValueStartIndex;
				for( ; wodpValueEndIndex < associationValueLength && Character.isDigit( associationValue.charAt( wodpValueEndIndex ) ); wodpValueEndIndex++ ) {
					// do nothing
				}
				String wodpKey = NGHelperFunctionDeclarationParser.QUOTED_STRING_KEY + associationValue.substring( wodpValueStartIndex, wodpValueEndIndex );
				String quotedString = quotedStrings.get( wodpKey );
				if( quotedString != null ) {
					quotedString = quotedString.replaceAll( "\\\"", "\\\\\"" );
					value.append( "\"" );
					value.append( quotedString );
					value.append( "\"" );
				}
				lastIndex = wodpValueEndIndex;
			}
			value.append( associationValue.substring( lastIndex ) );
			associationValue = value.toString();
			association = NGHelperFunctionAssociation.associationWithValue( associationValue );
		}
		else {
			String quotedString = quotedStrings.get( associationValue );
			// MS: WO 5.4 converts \n to an actual newline. I don't know if WO 5.3 does, too, but let's go ahead and be compatible with them as long as nobody is yelling.
			if( quotedString != null ) {
				int backslashIndex = quotedString.indexOf( '\\' );
				if( backslashIndex != -1 ) {
					StringBuilder sb = new StringBuilder( quotedString );
					int length = sb.length();
					for( int i = backslashIndex; i < length; i++ ) {
						char ch = sb.charAt( i );
						if( ch == '\\' && i < length ) {
							char nextCh = sb.charAt( i + 1 );
							if( nextCh == 'n' ) {
								sb.replace( i, i + 2, "\n" );
							}
							else if( nextCh == 'r' ) {
								sb.replace( i, i + 2, "\r" );
							}
							else if( nextCh == 't' ) {
								sb.replace( i, i + 2, "\t" );
							}
							else {
								sb.replace( i, i + 2, String.valueOf( nextCh ) );
							}
							length--;
						}
					}
					quotedString = sb.toString();
				}
				association = NGHelperFunctionAssociation.associationWithValue( quotedString );
			}
			else if( _NGUtilities.isNumber( associationValue ) ) {
				Number number = null;
				if( associationValue != null && associationValue.contains( "." ) ) {
					number = Double.valueOf( associationValue );
				}
				else {
					number = Integer.parseInt( associationValue );
				}
				association = NGHelperFunctionAssociation.associationWithValue( number );
			}
			else if( "true".equalsIgnoreCase( associationValue ) || "yes".equalsIgnoreCase( associationValue ) ) {
				association = NGConstantValueAssociation.TRUE;
			}
			else if( "false".equalsIgnoreCase( associationValue ) || "no".equalsIgnoreCase( associationValue ) || "nil".equalsIgnoreCase( associationValue ) || "null".equalsIgnoreCase( associationValue ) ) {
				association = NGConstantValueAssociation.FALSE;
			}
			else {
				association = NGHelperFunctionAssociation.associationWithKeyPath( associationValue );
			}
		}
		return association;
	}

	private Map<String, String> _rawDeclarationsWithoutComment( String declarationStr ) {
		Map<String, String> declarations = new HashMap<>();
		StringBuilder declarationWithoutCommentBuffer = new StringBuilder( 100 );
		StringTokenizer tokenizer = new StringTokenizer( declarationStr, "{", true );
		try {
			while( tokenizer.hasMoreTokens() ) {
				String token = tokenizer.nextToken( "{" );
				if( token.equals( "{" ) ) {
					token = tokenizer.nextToken( "}" );
					if( token.equals( "}" ) ) {
						token = "";
					}
					else {
						tokenizer.nextToken();
					}
					String declarationWithoutComment = declarationWithoutCommentBuffer.toString();
					if( declarationWithoutComment.startsWith( ";" ) ) {
						declarationWithoutComment = declarationWithoutComment.substring( 1 );
					}
					declarations.put( declarationWithoutComment.trim(), "{" + token + "}" );
					declarationWithoutCommentBuffer.setLength( 0 );
				}
				else {
					declarationWithoutCommentBuffer.append( token );
				}
			}
		}
		catch( NoSuchElementException e ) {
			logger.debug( "Failed to parse.", e );
		}
		return declarations;
	}

	@Override
	public String toString() {
		return "<WOHelperFunctionDeclarationParser quotedStrings = " + _quotedStrings.toString() + ">";
	}
}