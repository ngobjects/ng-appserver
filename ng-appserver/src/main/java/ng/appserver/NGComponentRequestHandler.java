package ng.appserver;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NGComponentRequestHandler extends NGRequestHandler {

	private static Logger logger = LoggerFactory.getLogger( NGComponentRequestHandler.class );

	@Override
	public NGResponse handleRequest( NGRequest request ) {
		// at this point, this point, the request's context is a freshly created one
		final NGContext context = request.context();

		//		logger.debug( "request.context: " + context );
		//		logger.debug( "request.originatingContext: " + context.originatingContext() );
		logger.debug( "pageCache: " + _pageCache );

		final String pageKey = pageCacheKey( context.originatingContext().contextID(), context.senderID().toString() );

		logger.debug( "Our pageKey is: " + pageKey );

		// Now let's try to restore the page from the cache
		final NGComponent originalPage = restorePageFromCache( pageKey );

		if( originalPage == null ) {
			throw new IllegalStateException( "No page found in cache" );
		}

		logger.info( "Page restored from cache is: " + originalPage.getClass() );

		// At this point, we must know what page we're working with.
		context.setPage( originalPage );
		context.setCurrentComponent( originalPage );
		context.page().awakeInContext( request.context() );

		logger.debug( "About to perform takeValuesfromRequest in context {} on page {} ", context.originatingContext().contextID(), originalPage );
		originalPage.takeValuesFromRequest( request, context );

		logger.debug( "About to perform invokeAction on element {} in context {} on page {} ", context.senderID(), context.originatingContext().contextID(), originalPage );

		// This is what we got from invoking the action on the original page.
		final NGActionResults actionInvocationResults = originalPage.invokeAction( request, context );

		logger.debug( "Action invocation returned {}", actionInvocationResults );

		// And this is what we're going to eventually return.
		NGResponse response; // FIXME: Don't initialize to null.

		// If action results are null, we're returning the same page
		if( actionInvocationResults == null ) {
			logger.debug( "Action method returned null, invoking generateResponse on the original page" );
			//			savePage( context.contextID(), originalPage );
			response = originalPage.generateResponse();
		}
		else if( actionInvocationResults instanceof NGComponent newPage ) {
			context.setPage( newPage );
			context.setCurrentComponent( newPage );
			newPage.awakeInContext( context );

			// Since we've switched pages, we need to save this page in the current context
			//			savePage( context.contextID(), ngc );

			response = newPage.generateResponse();
		}
		else {
			// FIXME: The return of an action might not be a WOComponent, handle that
			throw new IllegalStateException( "You returned something that's not a page. We don't support that yet" );
		}

		if( response == null ) {
			throw new IllegalStateException( "Response is null, there's something we haven't handled yet" );
		}

		return response;
	}

	public static String pageCacheKey( String contextID, String senderID ) {
		String key = contextID;

		//		if( senderID != null ) {
		//			key = key + "." + senderID;
		//		}

		return key;
	}

	/**
	 * FIXME: OK, this is horrible, but we're going to start out with out pageCache here. This belongs in the session, really.
	 *
	 * The page cache is going to have to keep track of
	 *
	 *  1. The originating context ID
	 *  2. The elementID the page originates from (for example, the click of a link)
	 *
	 *  So, let's just for now store the page as an accumulation of the entire string after the request handler key
	 */
	public static Map<String, NGComponent> _pageCache = new HashMap<>();

	public static void savePage( String key, NGComponent component ) {
		logger.debug( "Saving page {} in cache with key {} ", component.getClass(), key );
		_pageCache.put( key, component );
	}

	public static NGComponent restorePageFromCache( String key ) {
		logger.debug( "Restoring page from cache with key: " + key );
		return _pageCache.get( key );
	}
}