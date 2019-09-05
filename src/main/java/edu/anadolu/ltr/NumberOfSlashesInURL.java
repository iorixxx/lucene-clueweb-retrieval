package edu.anadolu.ltr;

import org.apache.commons.lang3.StringUtils;

public class NumberOfSlashesInURL implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {

        if (StringUtils.isEmpty(base.url))
            return 0;
        return StringUtils.countMatches(base.url, "/");
    }
}
