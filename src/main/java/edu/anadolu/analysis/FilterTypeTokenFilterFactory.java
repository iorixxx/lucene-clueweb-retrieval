
package edu.anadolu.analysis;


import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Factory class for {@link TypeTokenFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="chars" class="solr.TextField" positionIncrementGap="100"&gt;
 * &lt;analyzer&gt;
 * &lt;tokenizer class="solr.StandardTokenizerFactory"/&gt;
 * &lt;filter class="solr.TypeTokenFilterFactory" types="Latin"
 * useWhitelist="false"/&gt;
 * &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class FilterTypeTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    private final boolean useWhitelist;
    private final String stopTypesFiles;
    private Set<String> stopTypes;

    /**
     * Creates a new TypeTokenFilterFactory
     */
    public FilterTypeTokenFilterFactory(Map<String, String> args) {
        super(args);
        stopTypesFiles = require(args, "types");
        useWhitelist = getBoolean(args, "useWhitelist", false);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        List<String> files = splitFileNames(stopTypesFiles);
        if (files.size() > 0) {
            stopTypes = new HashSet<>();

            stopTypes.addAll(files);

        }
    }

    public Set<String> getStopTypes() {
        return stopTypes;
    }

    @Override
    public TokenStream create(TokenStream input) {
        final TokenStream filter = new TypeTokenFilter(input, stopTypes, useWhitelist);
        return filter;
    }
}
