package ng.appserver;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles requests for the "stateful" part of the framework.
 *
 * The basics:
 *
 * - URLs for "stateful requests" contain only numbers, separated by a period (eg. 3.4.12.5)
 * - The first number is the "contextID". In simplistic terms, this identifies an instance of a page/NGComponent (e.g. '3')
 * - The remaining numbers comprise the "senderID". It identifies a method invoked on the page/NGComponent instance, usually by clicking a link or submitting a form (e.g. '4.12.5')
 * - The stateful pages are stored in the user's session, basically a Map that maps contextIDs to page instances.
 */

public class NGComponentRequestHandler extends NGRequestHandler {

	private static Logger logger = LoggerFactory.getLogger( NGComponentRequestHandler.class );

	/**
	 * The default path prefix for this request handler
	 */
	public static String DEFAULT_PATH = "/wo/";

	@Override
	public NGResponse handleRequest( NGRequest request ) {

		// CHECKME: Request validation may affect performance, but it's nice to have during the development phase // Hugi 2023-01-08
		validateRequest( request );

		// At this point the request's context is a freshly created one
		final NGContext context = request.context();

		// Just some sanity checking for development
		if( context == null ) {
			throw new IllegalStateException( "The request's context is null. This should never happen" );
		}

		// We need to use the session to gain access to the page cache
		final NGSession session = context.session();

		// Just some sanity checking for development
		if( session == null ) {
			throw new IllegalStateException( "The context's session is null. That should never happen" );
		}

		// Now let's try to restore the page from the cache, using the contextID provided by the URL
		final NGComponent originalPage = session.restorePageFromCache( context._originatingContextID() );

		// No page found in cache. If this happens, the page has probably been pushed out of the session's page cache.
		if( originalPage == null ) {
			throw new NGPageRestorationException( request, "No page found in the page cache for contextID %s. The page has probably been pushed out of the session's page cache".formatted( context._originatingContextID() ) );
		}

		// Since we're working with the page we can safely assume it's become relevant again, so we give it another shot at life by moving it to the top of the page cache
		session.retainPageWithContextIDInCache( context._originatingContextID() );

		logger.debug( "Page restored from cache is: " + originalPage.getClass() );

		// Push the page in the context
		context.setPage( originalPage );
		context.setComponent( originalPage );
		context.page().awakeInContext( request.context() );

		logger.debug( "About to perform takeValuesfromRequest in context {} on page {} ", context._originatingContextID(), originalPage );

		// We only perform the takeValuesFromRequest phase if there are actual form values to read
		if( !request.formValues().isEmpty() ) {
			originalPage.takeValuesFromRequest( request, context );
		}

		logger.debug( "About to perform invokeAction on element {} in context {} on page {} ", context.senderID(), context._originatingContextID(), originalPage );

		// We now invoke the action on the original page instance
		final NGActionResults actionInvocationResults = originalPage.invokeAction( request, context );

		logger.debug( "Action invocation returned {}", actionInvocationResults );

		// The response we will eventually return
		final NGResponse response;

		// The response returned by an action can be
		// - null : Meaning we're working within a page/staying on the same page instance
		// - An instance of NGComponent : In which case that becomes the new page of this context
		// - Everything else that implements NGActionResults : which we just allow to do it's own thing (by invoking generateResponse() on it)

		if( actionInvocationResults == null ) {
			// If an action returns null, we're staying on the same page
			logger.debug( "Action method returned null, invoking generateResponse on the original page" );
			response = originalPage.generateResponse();
		}
		else if( actionInvocationResults instanceof NGComponent newPage ) {
			// If an action method returns an NGComponent, that's our new page in this context. We set it, and return it
			newPage.awakeInContext( context );
			response = newPage.generateResponse();
		}
		else {
			// If this is not an NGComponent, we don't need to take any special action and just invoke generateResponse() on the action's results
			response = actionInvocationResults.generateResponse();
		}

		// Just a little self-documenting sanity checking
		if( response == null ) {
			throw new IllegalStateException( "Response is null. This should never happen" );
		}

		return response;
	}

	/**
	 * Validates the request. @throws a RuntimeException if an invalid value is encountered
	 */
	private static void validateRequest( NGRequest request ) {
		Objects.requireNonNull( request );

		if( request.parsedURI().length() != 2 ) {
			throw new IllegalArgumentException( "Component requests always contain only two path elements, the request handler key, and the identifier" );
		}

		final String identifier = request.parsedURI().getString( 1 );

		// Ensure the URL contains at least one period
		if( !identifier.contains( "." ) ) {
			throw new IllegalArgumentException( "The identifier contains no periods" );
		}

		// Ensure the URL starts with a numeric
		if( !Character.isDigit( identifier.charAt( 0 ) ) ) {
			throw new IllegalArgumentException( "The identifier must start with a digit" );
		}

		// Ensure the URL starts with a numeric
		if( !Character.isDigit( identifier.charAt( identifier.length() - 1 ) ) ) {
			throw new IllegalArgumentException( "The identifier must end with a digit" );
		}

		// Ensure the identifier contains only digits and periods
		for( char c : identifier.toCharArray() ) {
			if( !Character.isDigit( c ) && !(c == '.') ) {
				throw new IllegalArgumentException( "Illegal character '%s' in identifier '%s'".formatted( c, identifier ) );
			}
		}
	}
}