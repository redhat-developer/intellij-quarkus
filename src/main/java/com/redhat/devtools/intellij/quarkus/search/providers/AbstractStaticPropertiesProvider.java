package com.redhat.devtools.intellij.quarkus.search.providers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.util.Query;
import com.redhat.devtools.intellij.quarkus.search.SearchContext;
import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4j.jsonrpc.json.adapters.EnumTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

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
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStaticPropertiesProvider.class);

	private final String path;

	private ConfigurationMetadata metadata;

	public AbstractStaticPropertiesProvider(String path) {
		this.path = path;
	}

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
	/**
	 * Collect static properties from the given context
	 *
	 * @param context the building scope context
	 */
	protected void collectStaticProperties(SearchContext context) {
		if (metadata == null) {
			try {
				metadata = getMetadata();
			} catch (IOException e) {
				LOGGER.warn(e.getLocalizedMessage(), e);
			}
		}
		if (metadata != null) {
			context.getCollector().merge(metadata);
		}
	}

	/**
	 * Returns a <code>ConfigurationMetadata</code> instance from
	 * the data stored from the json file located at <code>this.path</code>
	 *
	 * @return <code>ConfigurationMetadata</code> instance from
	 * the data stored from the json file located at <code>this.path</code>
	 * @throws IOException
	 */
	protected ConfigurationMetadata getMetadata() throws IOException {
		InputStream in = getInputStream();
		Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8.name());
		return createGson().fromJson(reader, ConfigurationMetadata.class);
	}

	/**
	 * Returns a <code>InputStream</code> instance that reads from the
	 * file located at <code>this.path</code>
	 *
	 * @return a <code>InputStream</code> instance that reads from the
	 * file located at <code>this.path</code>
	 * @throws IOException
	 */
	protected InputStream getInputStream() throws IOException {
		if (path == null || path.length() < 0) {
			return null;
		}
		InputStream stream = AbstractStaticPropertiesProvider.class.getResourceAsStream(path);
		if (stream == null) {
			stream = new FileInputStream(path);
		}
		return stream;
	}

	private static Gson createGson() {
		return new GsonBuilder().registerTypeAdapterFactory(new EnumTypeAdapter.Factory()).create();
	}


	@Override
	public void collectProperties(PsiModifierListOwner match, SearchContext context) {
		// Do nothing
	}

	@Override
	protected String[] getPatterns() {
		return null;
	}

	@Override
	protected Query<PsiModifierListOwner> createSearchPattern(SearchContext context, String pattern) {
		return null;
	}

}