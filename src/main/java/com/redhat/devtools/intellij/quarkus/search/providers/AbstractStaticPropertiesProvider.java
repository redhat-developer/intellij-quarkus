package com.redhat.devtools.intellij.quarkus.search.providers;

import com.intellij.psi.PsiMember;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;

/**
 * Abstract class for static properties provider.
 * 
 * Static properties are properties that should be available when
 * a certain class is present in the classpath. As a result,
 * no search patterns are required.
 * 
 * @author Angelo ZERR
 *
 */
public abstract class AbstractStaticPropertiesProvider extends AbstractPropertiesProvider {

	@Override
	public final void beginSearch(SearchContext context) {
		if (isAdaptedFor(context)) {
			collectStaticProperties(context);
		}
	}

	/**
	 * Returns true if static properties must be collected for the given context and false
	 * otherwise.
	 * 
	 * @param context the building scope context
	 * @return
	 */
	protected abstract boolean isAdaptedFor(SearchContext context);

	/**
	 * Collect static properties from the given context
	 * 
	 * @param context the building scope context
	 */
	protected abstract void collectStaticProperties(SearchContext context);

	@Override
	public void collectProperties(PsiMember match, SearchContext context) {
		// Do nothing
	}

	@Override
	protected String[] getPatterns() {
		return null;
	}

	@Override
	protected Query<PsiMember> createSearchPattern(SearchContext context, String pattern) {
		return null;
	}

}