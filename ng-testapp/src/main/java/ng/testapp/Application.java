package ng.testapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGCookie;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class Application extends NGApplication {

	private static Logger logger = LoggerFactory.getLogger( Application.class );

	public static void main( String[] args ) {
		NGApplication.run( args, Application.class );
	}

	public Application() {
		routeTable().map( "/some-route", ( request ) -> {
			NGResponse response = new NGResponse( "Oh look, a response!", 200 );
			response.addCookie( new NGCookie( "nafn", "Hugi" ) );
			return response;
		} );
	}

	@Override
	public NGResponse dispatchRequest( NGRequest request ) {
		logger.info( "uri: {} ", request.uri() );
		logger.info( "method: {} ", request.method() );
		logger.info( "headers: {} ", request.headers() );
		logger.info( "cookieValues: {} ", request.cookieValues() );
		logger.info( "formValues: {} ", request.formValues() );
		logger.info( "contentString: {}", request.contentString() );
		return super.dispatchRequest( request );
	}
}