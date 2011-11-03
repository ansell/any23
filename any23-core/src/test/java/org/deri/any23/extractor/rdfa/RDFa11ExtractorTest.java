package org.deri.any23.extractor.rdfa;

import org.deri.any23.extractor.ErrorReporter;
import org.deri.any23.extractor.ExtractorFactory;
import org.deri.any23.rdf.RDFUtils;
import org.deri.any23.vocab.FOAF;
import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryException;

/**
 * Reference test class for {@link RDFa11Extractor} class.
 *
 * @author Michele Mostarda (mostarda@fbk.eu)
 */

public class RDFa11ExtractorTest extends AbstractRDFaExtractorTestCase {

    @Test
    public void testObjectResourceConversion() throws RepositoryException {
        assertExtracts("html/rdfa/object-resource-test.html");
        logger.debug(dumpModelToTurtle());
         assertContains(
                null,
                FOAF.getInstance().page,
                RDFUtils.uri("http://en.wikipedia.org/New_York")
        );
    }

    /**
     * This test checks the behavior of the <i>RDFa</i> extraction where the datatype
     * of a property is explicitly set.
     * For details see the <a href="http://www.w3.org/TR/rdfa-syntax/">RDFa in XHTML: Syntax and Processing</a>
     * recommendation.
     *
     * @throws RepositoryException
     */
    @Test
    public void testExplicitDatatypeDeclaration() throws RepositoryException {
        assertExtracts("html/rdfa/xmlliteral-datatype-test.html");
        logger.debug(dumpModelToTurtle());

        Literal literal = RDFUtils.literal(
                "<SPAN datatype=\"rdf:XMLLiteral\" property=\"foaf:name\">Albert <STRONG>Einstein</STRONG></SPAN>",
                RDF.XMLLITERAL
        );
        assertContains(
                RDFUtils.uri("http://dbpedia.org/resource/Albert_Einstein"),
                vFOAF.name,
                literal
        );
    }

    @Test
    public void testRelRevUse() throws RepositoryException {
        assertExtracts("html/rdfa/rel-rev-use.html");
        logger.debug(dumpModelToTurtle());

        assertContains(
                baseURI,
                RDFUtils.uri("http://bob.example.com/cite"),
                RDFUtils.uri("http://www.example.com/books/the_two_towers")
        );
        assertContains(
                RDFUtils.uri("http://www.example.com/books/the_two_towers"),
                RDFUtils.uri("http://bob.example.com/chapter"),
                baseURI
        );
    }

    /**
     * Tests that the default parser settings enable tolerance in data type parsing.
     */
    @Test
    public void testTolerantParsing() {
        assertExtracts("html/rdfa/oreilly-invalid-datatype.html");
        assertError(ErrorReporter.ErrorLevel.WARN, ".*Cannot map prefix \'mailto\'.*");
    }

    @Override
    protected ExtractorFactory<?> getExtractorFactory() {
        return RDFa11Extractor.factory;
    }

}
