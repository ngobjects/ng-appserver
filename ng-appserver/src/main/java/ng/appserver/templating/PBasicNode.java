package ng.appserver.templating;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import ng.appserver.templating.NGDeclaration.NGBindingValue;

public record PBasicNode( NGDynamicHTMLTag tag ) implements PNode {

	public PBasicNode {
		Objects.requireNonNull( tag );
	}

	public boolean isInline() {
		return tag().declaration().isInline();
	}

	public String type() {
		return tag().declaration().type();
	}

	public Map<String, NGBindingValue> bindings() {
		return tag().declaration().bindings();
	}

	public List<PNode> children() {
		return tag().childrenWithStringsProcessedAndCombined();
	}
}