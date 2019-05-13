package edu.anadolu.analysis;


import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.TypeTokenFilter;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory class for {@link TypeTokenFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="latin" class="solr.TextField" positionIncrementGap="100"&gt;
 * &lt;analyzer&gt;
 * &lt;tokenizer class="solr.ICUTokenizerFactory"/&gt;
 * &lt;tokenizer class="solr.ScriptAsTypeTokenFilterFactory"/&gt;
 * &lt;filter class="solr.FilterTypeTokenFilterFactory" types="Latin" useWhitelist="true"/&gt;
 * &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class FilterTypeTokenFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    private final boolean useWhitelist;
    private final String types;
    private Set<String> stopTypes;

    /**
     * Creates a new TypeTokenFilterFactory
     */
    public FilterTypeTokenFilterFactory(Map<String, String> args) {
        super(args);
        types = require(args, "types");
        useWhitelist = getBoolean(args, "useWhitelist", false);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public void inform(ResourceLoader loader) {
        List<String> files = splitFileNames(types);
        if (files.size() > 0) {
            stopTypes = files.stream().map(String::trim).collect(Collectors.toSet());
        }
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new TypeTokenFilter(input, stopTypes, useWhitelist);
    }
}
