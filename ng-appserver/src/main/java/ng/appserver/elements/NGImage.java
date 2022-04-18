package ng.appserver.elements;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ng.appserver.NGApplication;
import ng.appserver.NGAssociation;
import ng.appserver.NGComponent;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGResponse;

public class NGImage extends NGDynamicElement {

	/**
	 * For keeping the filename of the image
	 */
	private final NGAssociation _filenameAssociation;

	/**
	 * For storing associations that aren't part of the component's basic associations
	 */
	private Map<String, NGAssociation> _additionalAssociations;

	public NGImage( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( name, associations, template );
		_filenameAssociation = associations.get( "filename" );

		// Not exactly pretty, but let's work with this a little
		_additionalAssociations = new HashMap<>( associations );
		_additionalAssociations.remove( "filename" );
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {
		Objects.requireNonNull( response );
		Objects.requireNonNull( context );

		final NGComponent component = context.component();
		final String filename = (String)_filenameAssociation.valueInComponent( component );
		final Optional<String> relativeURL = NGApplication.application().resourceManager().urlForWebserverResourceNamed( filename );
		String urlString;

		if( relativeURL.isPresent() ) {
			urlString = relativeURL.get();
		}
		else {
			urlString = "ERROR_NOT_FOUND_" + filename;
		}

		final StringBuilder b = new StringBuilder();
		b.append( String.format( "<img src=\"%s\"", urlString ) );

		_additionalAssociations.forEach( ( name, ass ) -> {
			b.append( name );
			b.append( "=" );
			b.append( "\"" + ass.valueInComponent( component ) + "\"" );
		} );

		b.append( " />" );

		response.appendContentString( b.toString() );
	}
}
