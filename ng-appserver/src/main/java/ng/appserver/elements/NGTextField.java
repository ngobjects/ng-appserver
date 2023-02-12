package ng.appserver.elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ng.appserver.NGAssociation;
import ng.appserver.NGContext;
import ng.appserver.NGDynamicElement;
import ng.appserver.NGElement;
import ng.appserver.NGRequest;
import ng.appserver.NGResponse;
import ng.appserver.privates.NGHTMLUtilities;

public class NGTextField extends NGDynamicElement {

	/**
	 * 'name' attribute of the text field. If not specified, will be populated using the elementID
	 */
	private NGAssociation _nameAssociation;

	/**
	 * The value for the field. This is a bidirectional binding that will also pass the value upstrem.
	 */
	private NGAssociation _valueAssociation;

	public NGTextField( String name, Map<String, NGAssociation> associations, NGElement template ) {
		super( null, null, null );
		_nameAssociation = associations.get( "name" );
		_valueAssociation = associations.get( "value" );
	}

	@Override
	public void takeValuesFromRequest( NGRequest request, NGContext context ) {
		// FIXME: we should only grab values from the request if it's containing form is being submitted. We probably need to pass that info along from NGForm (or perhaps NGSubmitButton?)
		// FIXME: In that case, we're going to want to push the field's value through the value binding

		final List<String> valuesFromRequest = request.formValues().get( nameFromCurrentElementId( context ) ); // FIXME: Account for multiple values/no value

		if( valuesFromRequest != null ) { // FIXME: Should formValues return an empty list or null if not present? // Hugi 2022-06-08
			String valueFromRequest = valuesFromRequest.get( 0 );
			_valueAssociation.setValue( valueFromRequest, context.component() );
		}
	}

	@Override
	public void appendToResponse( final NGResponse response, final NGContext context ) {

		final String name;

		if( _nameAssociation != null ) {
			name = (String)_nameAssociation.valueInComponent( context.component() );
		}
		else {
			name = nameFromCurrentElementId( context );
		}

		final String value = (String)_valueAssociation.valueInComponent( context.component() ); // FIXME: This value might need to be converted/formatted

		final Map<String, String> attributes = new HashMap<>();

		attributes.put( "type", "text" );

		if( name != null ) {
			attributes.put( "name", name );
		}

		if( value != null ) {
			attributes.put( "value", value );
		}

		final String tagString = NGHTMLUtilities.createElementStringWithAttributes( "input", attributes, true );
		response.appendContentString( tagString );
	}

	/**
	 * @return A unique name for this text field, based on the NGContext's elementId
	 */
	private String nameFromCurrentElementId( final NGContext context ) {
		return context.elementID().toString();
	}
}