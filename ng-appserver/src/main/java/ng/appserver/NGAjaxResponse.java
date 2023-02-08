package ng.appserver;

import java.util.List;

public class NGAjaxResponse extends NGResponse {

	private boolean shouldRender( NGContext context ) {
		final List<String> ucHeader = context.request().headers().get( "x-updatecontainerid" );

		if( ucHeader != null && !ucHeader.isEmpty() ) {
			if( !context.updateContainerIDs.contains( ucHeader.get( 0 ) ) ) {
				return false;
			}
		}

		return true;
	}

	private NGContext _context;

	public NGAjaxResponse( NGContext context ) {
		_context = context;
	}

	@Override
	public void appendContentString( String stringToAppend ) {
		if( shouldRender( _context ) ) {
			super.appendContentString( stringToAppend );
		}
	}
}