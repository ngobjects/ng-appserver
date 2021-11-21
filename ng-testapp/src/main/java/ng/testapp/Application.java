package ng.testapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ng.appserver.NGApplication;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;

public class Application extends NGApplication {

	private static Logger logger = LoggerFactory.getLogger( Application.class );

	public static void main( String[] args ) {
		new NGApplication().run( args, Application.class );
	}

	@Override
	public String adaptorClassName() {
		return ng.adaptor.jetty.NGAdaptorJetty.class.getName();
	}

	@Override
	public NGResponse dispatchRequest( NGRequest request ) {
		logger.info( "uri: {} ", request.uri() );
		logger.info( "method: {} ", request.method() );
		logger.info( "headers: {} ", request.headers() );
		logger.info( "formValues: {} ", request.formValues() );
		logger.info( "contentString: {}", request.contentString() );
		return super.dispatchRequest( request );
	}
}