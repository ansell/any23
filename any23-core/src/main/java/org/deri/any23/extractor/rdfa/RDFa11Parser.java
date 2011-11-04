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

package org.deri.any23.extractor.rdfa;

import org.deri.any23.extractor.ErrorReporter;
import org.deri.any23.extractor.ExtractionResult;
import org.deri.any23.extractor.html.DomUtils;
import org.deri.any23.rdf.RDFUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Michele Mostarda (mostarda@fbk.eu)
 */
public class RDFa11Parser {

    private static final Logger logger = LoggerFactory.getLogger(RDFa11Parser.class);

    public static final String CURIE_SEPARATOR      = ":";
    public static final String URI_PREFIX_SEPARATOR = ":";
    public static final String URI_SCHEMA_SEPARATOR = "://";
    public static final String URI_PATH_SEPARATOR   = "/";

    public static final String HEAD_TAG = "HEAD";
    public static final String BODY_TAG = "BODY";

    public static final String XMLNS_ATTRIBUTE    = "xmlns";
    public static final String XML_LANG_ATTRIBUTE = "xml:lang";

    public static final String REL_ATTRIBUTE      = "rel";
    public static final String REV_ATTRIBUTE      = "rev";

    public static final String ABOUT_ATTRIBUTE    = "about";
    public static final String SRC_ATTRIBUTE      = "src";
    public static final String RESOURCE_ATTRIBUTE = "resource";
    public static final String HREF_ATTRIBUTE     = "href";

    public static final String[] SUBJECT_ATTRIBUTES = {
            ABOUT_ATTRIBUTE,
            SRC_ATTRIBUTE,
            RESOURCE_ATTRIBUTE,
            HREF_ATTRIBUTE
    };

    public static final String PREFIX_ATTRIBUTE   = "prefix";
    public static final String TYPEOF_ATTRIBUTE   = "typeof";
    public static final String PROPERTY_ATTRIBUTE = "property";
    public static final String DATATYPE_ATTRIBUTE = "datatype";
    public static final String CONTENT_ATTRIBUTE  = "content";

    public static final String XML_LITERAL_DATATYPE = "rdf:XMLLiteral";

    public static final String XMLNS_DEFAULT = "http://www.w3.org/1999/xhtml";

    private ErrorReporter errorReporter;

    private URL documentBase;

    private final Stack<URIMapping> uriMappingStack = new Stack<URIMapping>();

    private final List<IncompleteTriple> listOfIncompleteTriples = new ArrayList<IncompleteTriple>();

    private final Stack<EvaluationContext> evaluationContextStack = new Stack<EvaluationContext>();

    protected static URL getDocumentBase(URL documentURL, Document document) throws MalformedURLException {
        String base;
        base = DomUtils.find(document, "/HTML/HEAD/BASE/@href");  // Non XHTML documents.
        if( ! "".equals(base) ) return new URL(base);
        base = DomUtils.find(document, "//*/h:head/h:base[position()=1]/@href");  // XHTML documents.
        if( ! "".equals(base) ) return new URL(base);
        return documentURL;
    }

    protected static boolean isCURIE(String curie) {
        if(curie == null) {
            throw new NullPointerException("curie string cannot be null.");
        }
        if(curie.trim().length() == 0) return false;

        // '[' PREFIX ':' VALUE ']'
        if( curie.charAt(0) != '[' || curie.charAt(curie.length() -1) != ']') return false;
        int separatorIndex = curie.indexOf(CURIE_SEPARATOR);
        return separatorIndex > 0 && curie.indexOf(CURIE_SEPARATOR, separatorIndex + 1) == -1;
    }

    protected static boolean isCURIEBNode(String curie) {
        return isCURIE(curie) && curie.substring(1, curie.length() -1).split(CURIE_SEPARATOR)[0].equals("_");
    }

    protected static boolean isRelativeNode(Node node) {
        return DomUtils.hasAttribute(node, REL_ATTRIBUTE) || DomUtils.hasAttribute(node, REV_ATTRIBUTE);
    }

    // 5.5.9.2
    protected static Literal getAsPlainLiteral(Node node, String currentLanguage) {
        final String content = DomUtils.readAttribute(node, CONTENT_ATTRIBUTE, null);
        if(content != null) return RDFUtils.literal(content, currentLanguage);

        if(! node.hasChildNodes() ) return RDFUtils.literal("", currentLanguage);

        final String nodeTextContent = node.getTextContent();
        return nodeTextContent == null ? null : RDFUtils.literal(nodeTextContent.trim(), currentLanguage);
    }

    protected static Literal getAsXMLLiteral(Node node) throws IOException, TransformerException {
        final String datatype = DomUtils.readAttribute(node, DATATYPE_ATTRIBUTE, null);
        if(! XML_LITERAL_DATATYPE.equals(datatype)) return null;

        final String xmlSerializedNode = DomUtils.serializeToXML(node, false);
        return RDFUtils.literal(xmlSerializedNode, RDF.XMLLITERAL);
    }

    protected static boolean isXMLNSDeclared(Document document) {
        final String attributeValue = document.getDocumentElement().getAttribute(XMLNS_ATTRIBUTE);
        if(attributeValue.length() == 0) return false;
        return XMLNS_DEFAULT.equals(attributeValue);
    }

    /**
     * <a href="http://www.w3.org/TR/rdfa-syntax/#s_model">RDFa Syntax - Processing Model</a>.
     *
     * @param documentURL
     * @param extractionResult
     * @param document
     */
    public void processDocument(URL documentURL, Document document, ExtractionResult extractionResult)
    throws RDFa11ParserException {
        reset();

        this.errorReporter = extractionResult;

        // Check 4.1.3 : default XMLNS declaration.
        if( ! isXMLNSDeclared(document)) {
            reportError(
                    document.getDocumentElement(),
                    String.format(
                            "The default %s namespace is expected to be declared and equal to '%s' .",
                            XMLNS_ATTRIBUTE, XMLNS_DEFAULT
                    )
            );
        }

        try {
            documentBase = getDocumentBase(documentURL, document);
        } catch (MalformedURLException murle) {
            throw new RDFa11ParserException("Invalid document base URL.", murle);
        }

        // 5.5.1
        pushContext(document, new EvaluationContext(documentBase));

        depthFirstNode(document, extractionResult);
    }

    protected void updateURIMapping(Node node) {
        final NamedNodeMap attributes = node.getAttributes();
        if (null == attributes) return;

        Node attribute;
        final List<PrefixMap> prefixMapList = new ArrayList<PrefixMap>();
        final String namespacePrefix = XMLNS_ATTRIBUTE + URI_PREFIX_SEPARATOR;
        for (int a = 0; a < attributes.getLength(); a++) {
            attribute = attributes.item(a);
            if (attribute.getNodeName().startsWith(namespacePrefix)) {
                prefixMapList.add(
                        new PrefixMap(
                            attribute.getNodeName().substring(namespacePrefix.length()),
                            RDFUtils.uri(attribute.getNodeValue())
                        )
                );
            }
        }

        extractPrefixes(node, prefixMapList);

        if(prefixMapList.size() == 0) return;
        pushMappings(
                node,
                prefixMapList
        );
    }

    protected URI getMapping(String prefix) {
        for (URIMapping uriMapping : uriMappingStack) {
            final URI mapping = uriMapping.map.get(prefix);
            if (mapping != null) {
                return mapping;
            }
        }
        return null;
    }

    private void reset() {
        errorReporter = null;
        documentBase  = null;
        uriMappingStack.clear();
        listOfIncompleteTriples.clear();
        evaluationContextStack.clear();
    }


    private void pushContext(Node node, EvaluationContext ec) {
        ec.node = node;
        evaluationContextStack.push(ec);
    }

    private EvaluationContext getContext() {
        return evaluationContextStack.peek();
    }

    private void popEvaluationContext(Node n) {
        //if(evaluationContextStack.isEmpty()) return;
        Node peekNode = evaluationContextStack.peek().node;
        if(DomUtils.isAncestorOf(peekNode, n)) {
            evaluationContextStack.pop();
        }
    }

    private void reportError(Node n, String msg) {
        final String errorMsg = String.format(
                "Error while processing node [%s] : '%s'",
                DomUtils.getXPathForNode(n), msg
        );
        final int[] errorLocation = DomUtils.getNodeLocation(n);
        this.errorReporter.notifyError(
                ErrorReporter.ErrorLevel.WARN,
                errorMsg,
                errorLocation == null ? -1 : errorLocation[0],
                errorLocation == null ? -1 : errorLocation[1]
        );
    }

    private void depthFirstNode(Node node, ExtractionResult extractionResult) {
        try {
            processNode(node, extractionResult);
        } catch (Exception e) {
            // e.printStackTrace();
            reportError(node, e.getMessage());
        }
        depthFirstChildren(node.getChildNodes(), extractionResult);
    }

    private void depthFirstChildren(NodeList nodeList, ExtractionResult extractionResult) {
        for(int i = 0; i < nodeList.getLength(); i++) {
            final Node child = nodeList.item(i);
            depthFirstNode(child, extractionResult);
            popMappings(child);
            popEvaluationContext(child);
        }
    }

    private void writeTriple(Resource s, URI p, Value o, ExtractionResult extractionResult) {
        if(logger.isDebugEnabled()) logger.debug(String.format("writeTriple(%s %s %s)" , s, p, o));
        extractionResult.writeTriple(s, p, o);
    }

    private void processNode(Node node, ExtractionResult extractionResult) throws Exception {
        if(logger.isDebugEnabled()) logger.debug("processNode: " + DomUtils.getXPathForNode(node));
        final EvaluationContext currentEvaluationContext = getContext();
        try {
            if(node.getNodeType() != Node.DOCUMENT_NODE && node.getNodeType() != Node.ELEMENT_NODE) {
                return;
            }

            // 5.5.2
            Node currentElement = node;
            updateURIMapping(currentElement);

            // 5.2.3
            updateLanguage(currentElement, currentEvaluationContext);

            // 5.2.4
            if(! isRelativeNode(currentElement)) {
                currentEvaluationContext.newSubject = getNewSubject(currentElement, currentEvaluationContext);
            } else { // 5.2.5
                currentEvaluationContext.newSubject = getNewSubjectCurrentObjectResource(
                        currentElement,
                        currentEvaluationContext
                );
            }
            if(currentEvaluationContext.newSubject == null)
                currentEvaluationContext.newSubject = RDFUtils.uri(documentBase.toExternalForm());

            assert currentEvaluationContext.newSubject != null : "newSubject must be not null.";
            if(logger.isDebugEnabled()) logger.debug("newSubject: " + currentEvaluationContext.newSubject);

            // 5.5.6
            final URI[] types = getTypes(currentElement);
            for(URI type : types) {
                writeTriple(currentEvaluationContext.newSubject, RDF.TYPE, type, extractionResult);
            }

            // 5.5.7
            final URI[] rels = getRels(currentElement);
            final URI[] revs = getRevs(currentElement);
            if(currentEvaluationContext.currentObjectResource != null) {
                for (URI rel : rels) {
                    writeTriple(
                            currentEvaluationContext.newSubject,
                            rel,
                            currentEvaluationContext.currentObjectResource,
                            extractionResult
                    );
                }
                for (URI rev : revs) {
                    writeTriple(
                            currentEvaluationContext.currentObjectResource,
                            rev,
                            currentEvaluationContext.newSubject, extractionResult
                    );
                }
            } else { // 5.5.8
                for(URI rel : rels) {
                    listOfIncompleteTriples.add(
                            new IncompleteTriple(
                                    currentElement,
                                    currentEvaluationContext.newSubject,
                                    rel,
                                    IncompleteTripleDirection.Forward
                            )
                    );
                }
                for(URI rev : revs) {
                    listOfIncompleteTriples.add(
                            new IncompleteTriple(
                                    currentElement,
                                    currentEvaluationContext.newSubject,
                                    rev,
                                    IncompleteTripleDirection.Reverse
                            )
                    );
                }
            }

            // 5.5.9
            final Value currentObject = getCurrentObject(currentElement);
            final URI[] predicates = getPredicate(currentElement);
            if (currentObject != null && predicates != null) {
                for (URI predicate : predicates) {
                    writeTriple(currentEvaluationContext.newSubject, predicate, currentObject, extractionResult);
                }
            }

            // 5.5.10
            if(!currentEvaluationContext.skipElem && currentEvaluationContext.newSubject != null) {
                for (IncompleteTriple incompleteTriple : listOfIncompleteTriples) {
                    incompleteTriple.produceTriple(
                            currentElement,
                            currentEvaluationContext.newSubject,
                            extractionResult
                    );
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if(currentEvaluationContext.recourse) {
                EvaluationContext newEvaluationContext = new EvaluationContext(currentEvaluationContext.base);
                if(currentEvaluationContext.skipElem) {
                    newEvaluationContext.language = currentEvaluationContext.language;
                } else {
                    newEvaluationContext.base = currentEvaluationContext.base;

                    if(currentEvaluationContext.newSubject != null) {
                        newEvaluationContext.parentSubject = currentEvaluationContext.newSubject;
                    } else {
                        newEvaluationContext.parentSubject = currentEvaluationContext.parentSubject;
                    }

                    if(currentEvaluationContext.currentObjectResource != null) {
                        newEvaluationContext.parentObject = currentEvaluationContext.currentObjectResource;
                    } else if(currentEvaluationContext.newSubject != null) {
                        newEvaluationContext.parentObject = currentEvaluationContext.newSubject;
                    } else {
                        newEvaluationContext.parentObject = currentEvaluationContext.parentSubject;
                    }

                    newEvaluationContext.language = currentEvaluationContext.language;
                }
                pushContext(node, newEvaluationContext);
            }
        }
    }

    private void extractPrefixes(Node node, List<PrefixMap> prefixMapList) {
        final String prefixAttribute = DomUtils.readAttribute(node, PREFIX_ATTRIBUTE, null);
        if(prefixAttribute == null) return;
        final String[] prefixParts = prefixAttribute.split("\\s");
        for(String prefixPart : prefixParts) {
            int splitPoint = prefixPart.indexOf(URI_PREFIX_SEPARATOR);
            final String prefix = prefixPart.substring(0, splitPoint);
            if(prefix.length() == 0) {
                reportError(node, String.format("Invalid prefix length in prefix attribute '%s'", prefixAttribute));
                continue;
            }
            final URI uri;
            final String uriStr = prefixPart.substring(splitPoint + 1);
            try {
                uri = RDFUtils.uri(uriStr);
            } catch (Exception e) {
                reportError(
                        node,
                        String.format(
                                "Resolution of prefix '%s' defines an invalid URI: '%s'",
                                prefixAttribute, uriStr
                        )
                );
                continue;
            }
            prefixMapList.add( new PrefixMap(prefix, uri) );
        }
    }

    private void updateLanguage(Node node, EvaluationContext currentEvaluationContext) {
        final String candidateLanguage = DomUtils.readAttribute(node, XML_LANG_ATTRIBUTE, null);
        if(candidateLanguage != null) currentEvaluationContext.language = candidateLanguage;
    }

    // 5.5.4
    private Resource getNewSubject(Node node, EvaluationContext currentEvaluationContext)
    throws URISyntaxException {
        String candidateURIOrCURIE;
        for(String subjectAttribute : SUBJECT_ATTRIBUTES) {
            candidateURIOrCURIE = DomUtils.readAttribute(node, subjectAttribute, null);
            if(candidateURIOrCURIE != null) {
                return resolveCURIEOrURI(candidateURIOrCURIE);
            }
        }

        if(node.getNodeName().equalsIgnoreCase(HEAD_TAG) || node.getNodeName().equalsIgnoreCase(BODY_TAG)) {
            return RDFUtils.uri(currentEvaluationContext.base.toString());
        }

        if(DomUtils.hasAttribute(node, TYPEOF_ATTRIBUTE)) {
            return RDFUtils.bnode();
        }

        if(DomUtils.hasAttribute(node, PROPERTY_ATTRIBUTE)) {
            currentEvaluationContext.skipElem = true;
        }
        if(currentEvaluationContext.parentObject != null) {
            return (Resource) currentEvaluationContext.parentObject;
        }

        return null;
    }

    // 5.5.5
    private Resource getNewSubjectCurrentObjectResource(Node node, EvaluationContext currentEvaluationContext)
    throws URISyntaxException {
        String candidateURIOrCURIE;
        candidateURIOrCURIE = DomUtils.readAttribute(node, ABOUT_ATTRIBUTE, null);
        if(candidateURIOrCURIE != null) {
            return resolveCURIEOrURI(candidateURIOrCURIE);
        }
        candidateURIOrCURIE = DomUtils.readAttribute(node, SRC_ATTRIBUTE, null);
        if(candidateURIOrCURIE != null) {
            return resolveCURIEOrURI(candidateURIOrCURIE);
        }

        if(node.getNodeName().equalsIgnoreCase(HEAD_TAG) || node.getNodeName().equalsIgnoreCase(BODY_TAG)) {
            return RDFUtils.uri(currentEvaluationContext.base.toString());
        }
        if(DomUtils.hasAttribute(node, TYPEOF_ATTRIBUTE)) {
            return RDFUtils.bnode();
        }

        // Then the [current object resource] is set to the URI obtained from the first match from the following rules:
        candidateURIOrCURIE = DomUtils.readAttribute(node, RESOURCE_ATTRIBUTE, null);
        if(candidateURIOrCURIE != null) {
            currentEvaluationContext.currentObjectResource = resolveCURIEOrURI(candidateURIOrCURIE);
        }
        candidateURIOrCURIE = DomUtils.readAttribute(node, HREF_ATTRIBUTE, null);
        if(candidateURIOrCURIE != null) {
            currentEvaluationContext.currentObjectResource = resolveCURIEOrURI(candidateURIOrCURIE);
        }

        if(currentEvaluationContext.parentObject != null) {
            if (!DomUtils.hasAttribute(node, PROPERTY_ATTRIBUTE)) {
                currentEvaluationContext.skipElem = true;
            }
            return (Resource) currentEvaluationContext.parentObject;
        }

        return null;
    }

    private URI[] getTypes(Node node) throws URISyntaxException {
        final String typeOf = DomUtils.readAttribute(node, TYPEOF_ATTRIBUTE, null);
        return resolveCurieOrURIList(node, typeOf);
    }

    private URI[] getRels(Node node) throws URISyntaxException {
        final String rel = DomUtils.readAttribute(node, REL_ATTRIBUTE, null);
        return resolveCurieOrURIList(node, rel);
    }

    private URI[] getRevs(Node node) throws URISyntaxException {
        final String rev = DomUtils.readAttribute(node, REV_ATTRIBUTE, null);
        return resolveCurieOrURIList(node, rev);
    }

    private URI[] getPredicate(Node node) throws URISyntaxException {
        final String candidateURI = DomUtils.readAttribute(node, PROPERTY_ATTRIBUTE, null);
        if(candidateURI == null) return null;
        return resolveCurieOrURIList(node, candidateURI);
    }

    // 5.5.9
    private Value getCurrentObject(Node node) throws URISyntaxException, IOException, TransformerException {
        final String candidateObject = DomUtils.readAttribute(node, HREF_ATTRIBUTE, null);
        if(candidateObject != null) {
            return resolveCURIEOrURI(candidateObject);
        } else {
            return gerCurrentObjectLiteral(node);
        }
    }

    private Literal gerCurrentObjectLiteral(Node node) throws URISyntaxException, IOException, TransformerException {
        final EvaluationContext currentEvaluationContext = getContext();
        Literal literal;

        literal = getAsTypedLiteral(node);
        if(literal != null) return literal;

        literal = getAsXMLLiteral(node);
        if(literal != null) {
            currentEvaluationContext.recourse = false;
            return literal;
        }

        literal = getAsPlainLiteral(node, currentEvaluationContext.language);
        if(literal != null) return literal;

        return null;
    }

    private static String getNodeContent(Node node) {
        final String candidateContent = DomUtils.readAttribute(node, CONTENT_ATTRIBUTE, null);
        if(candidateContent != null) return candidateContent;
        return node.getTextContent();
    }

    // 5.5.9.1
    private Literal getAsTypedLiteral(Node node) throws URISyntaxException {
        final String datatype = DomUtils.readAttribute(node, DATATYPE_ATTRIBUTE, null);
        if (datatype == null || datatype.trim().length() == 0 || XML_LITERAL_DATATYPE.equals(datatype.trim()) ) {
            return null;
        }
        final Resource curieOrURI = resolveCURIEOrURI(datatype);
        return RDFUtils.literal(getNodeContent(node), curieOrURI instanceof URI ? (URI) curieOrURI : null);
    }

    private void pushMappings(Node sourceNode, List<PrefixMap> prefixMapList) {
        logger.debug("pushMappings");

        final Map<String, URI> mapping = new HashMap<String, URI>();
        for (PrefixMap prefixMap : prefixMapList) {
            mapping.put(prefixMap.prefix, prefixMap.uri);
        }
        uriMappingStack.push( new URIMapping(sourceNode, mapping) );
    }

    private void popMappings(Node node) {
        if(uriMappingStack.isEmpty()) return;
        final URIMapping peek = uriMappingStack.peek();
        if( ! DomUtils.isAncestorOf(peek.sourceNode, node) ) {
            logger.debug("popMappings");
            uriMappingStack.pop();
        }
    }

    // 5.4.2
    private Resource resolveCURIE(String curie, boolean verify) {
        if(verify && ! isCURIE(curie) ) {
            throw new IllegalArgumentException(
                String.format("The given string is not a valid CURIE: '%s'", curie)
            );
        }

        final String curieContent = curie.substring(1, curie.length() - 1);

        // 5.4.5
        if(isCURIEBNode(curie)) {
            return RDFUtils.bnode(curieContent);
        }

        return resolveURI(curieContent);
    }

    private boolean isAbsoluteURI(String uri) {
        return uri.indexOf(URI_SCHEMA_SEPARATOR) != -1;
    }

    private Resource resolveURI(String uri) {
        if(isAbsoluteURI(uri)) return RDFUtils.uri(uri);

        if(uri.indexOf(URI_PATH_SEPARATOR) == 0) { // Begins with '/'
            uri = uri.substring(1);
        }

        final String[] parts = uri.split(":");
        if(parts.length != 2) { // there is no prefix separator.
            return RDFUtils.uri( documentBase.toString() + uri );
        }

        final URI curieMapping = getMapping(parts[0]);
        if(curieMapping == null) {
            throw new IllegalArgumentException( String.format("Cannot map prefix '%s'", parts[0]) );
        }
        final String candidateCURIEStr = curieMapping.toString() + parts[1];
        final java.net.URI candidateCURIE;
        try {
            candidateCURIE = new java.net.URI(candidateCURIEStr);
        } catch (URISyntaxException urise) {
            throw new IllegalArgumentException(String.format("Invalid CURIE '%s'", candidateCURIEStr) );
        }
        return RDFUtils.uri(
                candidateCURIE.isAbsolute()
                        ?
                        candidateCURIE.toString()
                        :
                        documentBase.toString() + candidateCURIE.toString()
        );
    }

    private Resource resolveCURIEOrURI(String curieOrURI) throws URISyntaxException {
        if( isCURIE(curieOrURI) ) {
            return resolveCURIE(curieOrURI, false);
        }
        return resolveURI(curieOrURI);
    }

    private URI[] resolveCurieOrURIList(Node n, String curieOrURIList) throws URISyntaxException {
        if(curieOrURIList == null || curieOrURIList.trim().length() == 0) return new URI[0];

        final String[] curieOrURIListParts = curieOrURIList.split("\\s");
        final List<URI> result = new ArrayList<URI>();
        Resource curieOrURI;
        for(String curieORURIListPart : curieOrURIListParts) {
            curieOrURI = resolveCURIEOrURI(curieORURIListPart);
            if(curieOrURI != null && curieOrURI instanceof URI) {
                result.add((URI) curieOrURI);
            } else {
                reportError(n, String.format("Invalid CURIE '%s' : expected URI, found BNode.", curieORURIListPart));
            }
        }
        return result.toArray(new URI[result.size()]);
    }

    class EvaluationContext {
        private Node node;
        private URL base;
        private Resource parentSubject;
        private Value parentObject;
        private String language;
        private boolean recourse = true;
        private boolean skipElem;
        private Resource newSubject;
        private Resource currentObjectResource;

        EvaluationContext(URL base) {
            this.base = base;
        }
    }

    class PrefixMap {
        final String prefix;
        final URI    uri;
        public PrefixMap(String prefix, URI uri) {
            this.prefix = prefix;
            this.uri = uri;
        }
    }

    class URIMapping {
        final Node sourceNode;
        final Map<String, URI> map;

        public URIMapping(Node sourceNode, Map<String, URI> map) {
            this.sourceNode = sourceNode;
            this.map        = map;
        }
    }

    enum IncompleteTripleDirection {
        Forward,
        Reverse
    }

    private class IncompleteTriple {
        final Node     originatingNode;
        final Resource subject;
        final URI      predicate;
        final IncompleteTripleDirection direction;

        public IncompleteTriple(
                Node originatingNode,
                Resource subject,
                URI predicate,
                IncompleteTripleDirection direction
        ) {
            if(originatingNode == null || subject == null || predicate == null || direction == null)
                throw new IllegalArgumentException();

            this.originatingNode = originatingNode;
            this.subject         = subject;
            this.predicate       = predicate;
            this.direction       = direction;
        }

        public boolean produceTriple(Node resourceNode, Resource r, ExtractionResult extractionResult) {
            if( ! DomUtils.isAncestorOf(originatingNode, resourceNode, true) ) return false;

            if(r == null) throw new IllegalArgumentException();
            switch (direction) {
                case Forward:
                    extractionResult.writeTriple(subject, predicate, r);
                    break;
                case Reverse:
                    extractionResult.writeTriple(r, predicate, subject);
                    break;
                default:
                    throw new IllegalStateException();
            }
            return true;
        }
    }

}
