/*
 * Copyright 2008-2010 Digital Enterprise Research Institute (DERI)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deri.any23.extractor.html;

import org.deri.any23.extractor.ErrorReporter;
import org.deri.any23.extractor.ExtractionException;
import org.deri.any23.extractor.ExtractorFactory;
import org.deri.any23.extractor.SingleDocumentExtraction;
import org.deri.any23.extractor.SingleDocumentExtractionReport;
import org.deri.any23.parser.NQuadsWriter;
import org.deri.any23.rdf.RDFUtils;
import org.deri.any23.writer.RepositoryWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract class used to write {@link org.deri.any23.extractor.Extractor} specific
 * test cases.
 */
public abstract class AbstractExtractorTestCase {

    /**
     * Base test document.
     */
    protected static URI baseURI = RDFUtils.uri("http://bob.example.com/");

    /**
     * Internal connection used to collect extraction results.
     */
    private RepositoryConnection conn;

    /**
     * The latest generated report.
     */
    private SingleDocumentExtractionReport report;

    /**
     * Constructor.
     */
    public AbstractExtractorTestCase() {
        super();
    }

    /**
     * @return the factory of the extractor to be tested.
     */
    protected abstract ExtractorFactory<?> getExtractorFactory();

    /**
     * Test case initialization.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        Sail store = new MemoryStore();
        store.initialize();
        conn = new SailRepository(store).getConnection();
    }

    /**
     * Test case resources release.
     *
     * @throws RepositoryException
     */
    @After
    public void tearDown() throws RepositoryException {
        conn.close();
        conn   = null;
        report = null;
    }

    /**
     * @return the connection to the memory repository.
     */
    public RepositoryConnection getConnection() {
        return conn;
    }

    /**
     * @return the last generated report.
     */
    public SingleDocumentExtractionReport getReport() {
        return report;
    }

    /**
     * Performs data extraction over the content of a file
     * and assert that the extraction was correct.
     *
     * @param fileName
     */
    public void assertExtracts(String fileName) {
        try {
            extract(fileName);
        } catch (ExtractionException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Asserts that the extracted triples contain the pattern <code>(_ p o)</code>.
     *
     * @param p predicate
     * @param o object.
     * @throws RepositoryException
     */
    public void assertContains(URI p, Resource o) throws RepositoryException {
        assertContains(null, p, o);
    }

    /**
     * Asserts that the extracted triples contain the pattern <code>(_ p o)</code>.
     *
     * @param p predicate
     * @param o object.
     * @throws RepositoryException
     */
    public void assertContains(URI p, String o) throws RepositoryException {
        assertContains(null, p, RDFUtils.literal(o));
    }

    /**
     * Asserts that the extracted triples contain the pattern <code>(_ p o)</code>.
     *
     * @param p predicate
     * @param o object.
     * @throws RepositoryException
     */
    public void assertNotContains(URI p, Resource o) throws RepositoryException {
        assertNotContains(null, p, o);
    }

    /**
     * Asserts that the extracted triples contain the pattern <code>(s p o)</code>.
     *
     * @param s subject.
     * @param p predicate.
     * @param o object.
     * @throws RepositoryException
     */
    public void assertContains(Resource s, URI p, Value o) throws RepositoryException {
        Assert.assertTrue(
                getFailedExtractionMessage() +
                        String.format("Cannot find triple (%s %s %s)", s, p, o),
                conn.hasStatement(s, p, o, false));
    }

    /**
     * Asserts that the extracted triples contain the pattern <code>(s p o)</code>.
     *
     * @param s subject.
     * @param p predicate.
     * @param o object.
     * @throws RepositoryException
     */
    public void assertNotContains(Resource s, URI p, String o) throws RepositoryException {
        Assert.assertFalse(getFailedExtractionMessage(), conn.hasStatement(s, p, RDFUtils.literal(o), false));
    }

    /**
     * Asserts that the extracted triples contain the pattern <code>(s p o)</code>.
     *
     * @param s subject.
     * @param p predicate.
     * @param o object.
     * @throws RepositoryException
     */
    public void assertNotContains(Resource s, URI p, Resource o) throws RepositoryException {
        Assert.assertFalse(getFailedExtractionMessage(), conn.hasStatement(s, p, o, false));
    }

    /**
     * Asserts that the model contains at least a statement.
     *
     * @throws RepositoryException
     */
    public void assertModelNotEmpty() throws RepositoryException {
        Assert.assertFalse(
                "The model is expected to be empty." + getFailedExtractionMessage(),
                conn.isEmpty()
        );
    }

    /**
     * Asserts that the model doesn't contain the pattern <code>(s p o)</code>
     *
     * @param s subject.
     * @param p predicate.
     * @param o object.
     * @throws RepositoryException
     */
    public void assertNotContains(Resource s, URI p, Literal o) throws RepositoryException {
        Assert.assertFalse(getFailedExtractionMessage(), conn.hasStatement(s, p, o, false));
    }

    /**
     * Asserts that the model is expected to contains no statements.
     *
     * @throws RepositoryException
     */
    public void assertModelEmpty() throws RepositoryException {
        Assert.assertTrue(getFailedExtractionMessage(), conn.isEmpty());
    }

    /**
     * Returns the blank subject matching the pattern <code>(_:b p o)</code>, it is expected to exists and be just one.
     *
     * @param p predicate.
     * @param o object.
     * @return the matching blank subject.
     * @throws RepositoryException
     */
    public Resource findExactlyOneBlankSubject(URI p, Value o) throws RepositoryException {
        RepositoryResult<Statement> it = conn.getStatements(null, p, o, false);
        try {
            Assert.assertTrue(getFailedExtractionMessage(), it.hasNext());
            Statement stmt = it.next();
            Resource result = stmt.getSubject();
            Assert.assertTrue(getFailedExtractionMessage(), result instanceof BNode);
            Assert.assertFalse(getFailedExtractionMessage(), it.hasNext());
            return result;
        } finally {
            it.close();
        }
    }

    /**
     * Returns the object matching the pattern <code>(s p o)</code>, it is expected to exists and be just one.
     *
     * @param s subject.
     * @param p predicate.
     * @return the matching object.
     * @throws RepositoryException
     */
    public Value findExactlyOneObject(Resource s, URI p) throws RepositoryException {
        RepositoryResult<Statement> it = conn.getStatements(s, p, null, false);
        try {
            Assert.assertTrue(getFailedExtractionMessage(), it.hasNext());
            return it.next().getObject();
        } finally {
            it.close();
        }
    }

    /**
     * Returns all the subjects matching the pattern <code>(s? p o)</code>.
     *
     * @param p predicate.
     * @param o object.
     * @return list of matching subjects.
     * @throws RepositoryException
     */
    public List<Resource> findSubjects(URI p, Value o) throws RepositoryException {
        RepositoryResult<Statement> it = conn.getStatements(null, p, o, false);
        List<Resource> subjects = new ArrayList<Resource>();
        try {
            Statement statement;
            while( it.hasNext() ) {
                statement = it.next();
                subjects.add( statement.getSubject() );
            }
        } finally {
            it.close();
        }
        return subjects;
    }

    /**
     * Returns all the objects matching the pattern <code>(s p ?o)</code>.
     *
     * @param s predicate.
     * @param p predicate.
     * @return list of matching objects.
     * @throws RepositoryException
     */
    public List<Value> findObjects(Resource s, URI p) throws RepositoryException {
        RepositoryResult<Statement> it = conn.getStatements(s, p, null, false);
        List<Value> objects = new ArrayList<Value>();
        try {
            Statement statement;
            while( it.hasNext() ) {
                statement = it.next();
                objects.add( statement.getObject() );
            }
        } finally {
            it.close();
        }
        return objects;
    }

    /**
     * Asserts that an error has been produced by the processed {@link org.deri.any23.extractor.Extractor}.
     *
     * @param level expected error level
     * @param errorRegex regex matching the expected human readable error message.
     */
    public void assertError(ErrorReporter.ErrorLevel level, String errorRegex) {
        final Collection<ErrorReporter.Error> errors = getErrors( getExtractorFactory().getExtractorName() );
        boolean found = false;
        for(ErrorReporter.Error error : errors) {
            if(error.getLevel() == level && error.getMessage().matches(errorRegex)) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(
                String.format("Cannot find error with level %s matching expression '%s'", level, errorRegex),
                found
        );
    }

    protected void extract(String name) throws ExtractionException, IOException {
        SingleDocumentExtraction ex = new SingleDocumentExtraction(
                new HTMLFixture(name).getOpener(baseURI.toString()),
                getExtractorFactory(), new RepositoryWriter(conn));
        // TODO: MimeType detector to null forces the execution of all extractors, but extraction
        //       tests should be based on mimetype detection.
        ex.setMIMETypeDetector(null);
        report = ex.run();
    }

    /**
     * Dumps the extracted model in <i>Turtle</i> format.
     *
     * @return a string containing the model in Turtle.
     * @throws RepositoryException
     */
    protected String dumpModelToTurtle() throws RepositoryException {
        StringWriter w = new StringWriter();
        try {
            conn.export(new TurtleWriter(w));
            return w.toString();
        } catch (RDFHandlerException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Dumps the extracted model in <i>NQuads</i> format.
     *
     * @return a string containing the model in NQuads.
     * @throws RepositoryException
     */
    protected String dumpModelToNQuads() throws RepositoryException {
        StringWriter w = new StringWriter();
        try {
            conn.export(new NQuadsWriter(w));
            return w.toString();
        } catch (RDFHandlerException ex) {
            throw new RuntimeException(ex);
        }
    }

     /**
     * Dumps the extracted model in <i>RDFXML</i> format.
     *
     * @return a string containing the model in RDFXML.
     * @throws RepositoryException
     */
    protected String dumpModelToRDFXML() throws RepositoryException {
        StringWriter w = new StringWriter();
        try {
            conn.export(new RDFXMLWriter(w));
            return w.toString();
        } catch (RDFHandlerException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected List<Statement> dumpAsListOfStatements() throws RepositoryException {
        List<Statement> result = conn.getStatements(null, null, null, false).asList();
        conn.remove(null, null, null, new Resource[]{});
        return result;
    }

    protected String dumpHumanReadableTriples() throws RepositoryException {
        StringBuilder sb = new StringBuilder();
        RepositoryResult<Statement> result = conn.getStatements(null, null, null, false);
        while(result.hasNext()) {
            Statement statement = result.next();
            sb.append(String.format("%s %s %s %s\n",
                    statement.getSubject(),
                    statement.getPredicate(),
                    statement.getObject(),
                    statement.getContext()
                    )
            );
            
        }
        return sb.toString();
    }

    protected void assertContains(Statement statement) throws RepositoryException {
        if(statement.getSubject() instanceof BNode) {
            conn.hasStatement(
                    statement.getSubject() instanceof  BNode ? null : statement.getSubject(),
                    statement.getPredicate(),
                    statement.getObject() instanceof BNode   ? null : statement.getObject(),
                    false
            );
        }
    }

    protected void assertContains(Resource s, URI p, String o) throws RepositoryException {
        assertContains(s, p, RDFUtils.literal(o));
    }

    protected void assertContains(Resource s, URI p, String o, String lang) throws RepositoryException {
        assertContains(s, p, RDFUtils.literal(o, lang));
    }

    protected RepositoryResult<Statement> getStatements(Resource s, URI p, Value o)
    throws RepositoryException {
        return conn.getStatements(s, p, o, false);
    }

    protected int getStatementsSize(Resource s, URI p, Value o)
    throws RepositoryException {
        RepositoryResult<Statement> result = getStatements(s, p, o);
        int count = 0;
        try {
            while (result.hasNext()) {
                result.next();
                count++;
            }
        } finally {
            result.close();
        }
        return count;
    }

    protected void assertStatementsSize(Resource subject, URI prop, Value obj, int expected)
    throws RepositoryException {
        Assert.assertEquals(expected, getStatementsSize(subject, prop, obj) );
    }

    protected void assertStatementsSize(URI p, Value o, int expected) throws RepositoryException {
        assertStatementsSize(null, p, o, expected);
    }

    protected void assertStatementsSize(URI p, String o, int expected) throws RepositoryException {
        assertStatementsSize(p, o == null ? null : RDFUtils.literal(o), expected );
    }

    protected void assertNotFound(Resource s, URI p) throws RepositoryException {
         RepositoryResult<Statement> statements = conn.getStatements(s, p, null, true);
        try {
            Assert.assertFalse("Expected no statements.", statements.hasNext());
        } finally {
            statements.close();
        }
    }

    protected Value findObject(Resource s, URI p) throws RepositoryException {
        RepositoryResult<Statement> statements = conn.getStatements(s, p, null, true);
        try {
            junit.framework.Assert.assertTrue("Expected at least a statement.", statements.hasNext());
            return (statements.next().getObject());
        } finally {
            statements.close();
        }
    }

    protected Resource findObjectAsResource(Resource s, URI p) throws RepositoryException {
        return (Resource) findObject(s, p);
    }

    protected String findObjectAsLiteral(Resource s, URI p) throws RepositoryException {
        return findObject(s, p).stringValue();
    }

    protected Collection<ErrorReporter.Error> getErrors(String extractorName) {
        for(
                Map.Entry<String, Collection<ErrorReporter.Error>> errorEntry
                :
                report.getExtractorToErrors().entrySet()
        ) {
            if(errorEntry.getKey().equals(extractorName)) {
                return errorEntry.getValue();
            }
        }
        return Collections.emptyList();
    }

    private String getFailedExtractionMessage() throws RepositoryException {
        return "Assertion failed! Extracted triples:\n" + dumpModelToTurtle();
    }

}