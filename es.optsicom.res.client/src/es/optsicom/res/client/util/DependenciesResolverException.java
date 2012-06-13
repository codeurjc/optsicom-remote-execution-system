package es.optsicom.res.client.util;

public class DependenciesResolverException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DependenciesResolverException() {
		super("Remote execution doesn't support dependencies not included in the workspace");
	}
}
