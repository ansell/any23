package org.deri.any23.extractor.rdfa;

import org.deri.any23.extractor.ExtractorFactory;
import org.junit.Ignore;

/**
 * Reference test class for {@link RDFa11Extractor} class.
 *
 * @author Michele Mostarda (mostarda@fbk.eu)
 */

@Ignore
public class RDFa11ExtractorTest extends AbstractRDFaExtractorTestCase {

    @Override
    protected ExtractorFactory<?> getExtractorFactory() {
        return RDFa11Extractor.factory;
    }

}
