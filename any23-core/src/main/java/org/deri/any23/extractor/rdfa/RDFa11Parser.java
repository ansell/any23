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

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.deri.any23.extractor.ExtractionResult;
import org.deri.any23.extractor.html.DomUtils;
import org.deri.any23.rdf.RDFUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
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

    public static final String CURIE_SEPARATOR    = ":";

    public static final String XMLNS_ATTRIBUTE    = "xmlns";
    public static final String XML_LANG_ATTRIBUTE = "xml:lang";

    public static final String REL_ATTRIBUTE = "rel";
    public static final String REV_ATTRIBUTE = "rev";

    public static final String ABOUT_ATTRIBUTE    = "about";
    public static final String SRC_ATTRIBUTE      = "src";
    public static final String RESOURCE_ATTRIBUTE = "resource";
    public static final String HREF_ATTRIBUTE     = "href";
    public static final String[] SUBJECT_ATTRIBUTES =
            {ABOUT_ATTRIBUTE, SRC_ATTRIBUTE, RESOURCE_ATTRIBUTE, HREF_ATTRIBUTE};

    public static final String TYPEOF_ATTRIBUTE   = "typeof";

    public static final String PROPERTY_ATTRIBUTE = "property";

    public static final String DATATYPE_ATTRIBUTE = "datatype";

    public static final String CONTENT_ATTRIBUTE  = "content";

    public static final String XML_LITERAL_DATATYPE = "rdf:XMLLiteral";

    private Stack<URIMapping> uriMappingStack = new Stack<URIMapping>();
    private List<IncompleteTriple> listOfIncompleteTriples = new ArrayList<IncompleteTriple>();

    private URL documentBase;
    private EvaluationContext currentEvaluationContext;

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

    /**
     * <a href="http://www.w3.org/TR/rdfa-syntax/#s_model">RDFa Syntax - Processing Model</a>.
     *
     * @param documentURL
     * @param extractionResult
     * @param document
     */
    public void processDocument(URL documentURL, Document document, ExtractionResult extractionResult) {
        try {
            documentBase = getDocumentBase(documentURL, document);
        } catch (MalformedURLException murle) {
            throw new RuntimeException(murle); // TODO
        }

        // 5.5.1
        currentEvaluationContext = new EvaluationContext(documentBase);

        processChildList(document.getChildNodes(), extractionResult);
    }

    private void processChildList(NodeList nodelist, ExtractionResult extractionResult) {
        // Depth first.
        Node current;
        for(int i = 0; i < nodelist.getLength(); i++) {
            current = nodelist.item(i);
            processNode(current, extractionResult);
            processChildList(current.getChildNodes(), extractionResult);
            popMappings(current);
        }

        /* breath first.
        for(int i = 0; i < nodelist.getLength(); i++) {
            processNode(nodelist.item(i), extractionResult);
        }
        for(int i = 0; i < nodelist.getLength(); i++) {
            processChildList(nodelist.item(i).getChildNodes(), extractionResult);
        }
        */
    }

    private void processNode(Node node, ExtractionResult extractionResult) {
        System.out.println("node: " + node);
        try {

            // 5.5.2
            Node currentElement = node;
            updateURIMapping(currentElement);

            // 5.2.3
            updateLanguage(currentElement, currentEvaluationContext);

            // 5.2.4
            final Resource newSubject;
            if(! isRelativeNode(currentElement)) {
                newSubject = getNewSubject(currentElement, currentEvaluationContext);
            } else { // 5.2.5
                newSubject = getNewSubjectCurrentObjectResource(currentElement, currentEvaluationContext);
            }

            assert newSubject != null : "newSubject must be not null.";

            // 5.5.6
            final URI[] types = getTypes(currentElement);
            for(URI type : types) {
                extractionResult.writeTriple(newSubject, RDF.TYPE, type);
            }

            // 5.5.7
            final URI[] rels = getRels(currentElement);
            final URI[] revs = getRevs(currentElement);
            if(currentEvaluationContext.currentObjectResource != null) {
                for (URI rel : rels) {
                    extractionResult.writeTriple(newSubject, rel, currentEvaluationContext.currentObjectResource);
                }
                for (URI rev : revs) {
                    extractionResult.writeTriple(currentEvaluationContext.currentObjectResource, rev, newSubject);
                }
            } else { // 5.5.8
                for(URI rel : rels) {
                    listOfIncompleteTriples.add( new IncompleteTriple(rel, IncompleteTripleDirection.Forward) );
                }
                for(URI rev : revs) {
                    listOfIncompleteTriples.add( new IncompleteTriple(rev, IncompleteTripleDirection.Reverse) );
                }
            }

            // 5.5.9
            final Literal currentObjectLiteral = getCurrentObjectLiteral(currentElement);
            final URI[] predicates = getPredicate(currentElement);
            if (currentObjectLiteral != null && predicates != null) {
                for (URI predicate : predicates) {
                    extractionResult.writeTriple(newSubject, predicate, currentObjectLiteral);
                }
            }

            // 5.5.10
            if(!currentEvaluationContext.skipElem && newSubject != null) {
                for(IncompleteTriple incompleteTriple : listOfIncompleteTriples) {
                    incompleteTriple.produceTriple(
                            currentEvaluationContext.parentSubject,
                            currentEvaluationContext.newSubject,
                            extractionResult
                    );
                }
            }

            // 5.5.11
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
                        newEvaluationContext.parentObject = (URI) currentEvaluationContext.currentObjectResource;
                    } else if(currentEvaluationContext.newSubject != null) {
                        newEvaluationContext.parentObject = currentEvaluationContext.newSubject;
                    } else {
                        newEvaluationContext.parentObject = currentEvaluationContext.parentSubject;
                    }

                    newEvaluationContext.language = currentEvaluationContext.language;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e); // TODO
        }

    }

    protected void updateURIMapping(Node node) {
        final NamedNodeMap attributes = node.getAttributes();
        if (null == attributes) return;

        Node attribute;
        final List<String> prefixes = new ArrayList<String>();
        final List<URI> uris     = new ArrayList<URI>();
        final String attributePrefix = XMLNS_ATTRIBUTE + ":";
        for (int a = 0; a < attributes.getLength(); a++) {
            attribute = attributes.item(a);
            if (attribute.getNodeName().startsWith(attributePrefix)) {
                prefixes.add( attribute.getNodeName().substring(attributePrefix.length()) );
                uris.add( RDFUtils.uri(attribute.getNodeValue()) );
            }
        }
        if(prefixes.size() == 0) return;
        pushMappings(
                node,
                prefixes.toArray( new String[prefixes.size()]),
                uris.toArray( new URI[uris.size()] )
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

    // 5.5.9.2
    protected static Literal getAsPlainLiteral(Node node, String currentLanguage) {
        final String content = DomUtils.readAttribute(node, CONTENT_ATTRIBUTE, null);
        if(content != null) return RDFUtils.literal(content, currentLanguage);

        if(! node.hasChildNodes() ) return RDFUtils.literal("", currentLanguage);

        final NodeList children = node.getChildNodes();
        boolean allTextNodes = true;
        for(int c = 0; c < children.getLength(); c++) {
            if( children.item(c).getNodeType() != Node.TEXT_NODE ) {
                allTextNodes = false;
                break;
            }
        }
        if(allTextNodes) return RDFUtils.literal(node.getTextContent(), currentLanguage);

        final String datatype = DomUtils.readAttribute(node, DATATYPE_ATTRIBUTE, null);
        if(datatype != null && datatype.trim().length() == 0) return RDFUtils.literal("", currentLanguage);

        return null;
    }

    protected static Literal getAsXMLLiteral(Node node) throws IOException {
        final String datatype = DomUtils.readAttribute(node, DATATYPE_ATTRIBUTE, null);
        if(! XML_LITERAL_DATATYPE.equals(datatype)) return null;

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // TODO: remove this dependency.
        final XMLSerializer serializer = new XMLSerializer(baos, null);
        serializer.serialize(node);
        final String content = baos.toString();
        return RDFUtils.literal(content, RDF.XMLLITERAL);
    }

    private void updateLanguage(Node node, EvaluationContext currentEvaluationContext) {
        final String candidateLanguage = DomUtils.readAttribute(node, XML_LANG_ATTRIBUTE, null);
        if(candidateLanguage != null) currentEvaluationContext.currentLanguage = candidateLanguage;
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

        if(node.getNodeName().equalsIgnoreCase("HEAD") || node.getNodeName().equalsIgnoreCase("BODY")) {
            return RDFUtils.uri(currentEvaluationContext.base.toString());
        }

        if(DomUtils.hasAttribute(node, TYPEOF_ATTRIBUTE)) {
            return RDFUtils.bnode();
        }

        if(DomUtils.hasAttribute(node, PROPERTY_ATTRIBUTE)) {
            currentEvaluationContext.skipElem = true;
        }
        if(currentEvaluationContext.parentObject != null) {
            return currentEvaluationContext.parentObject;
        }

        //throw new IllegalStateException();
        //return null;
        // TODO: verify
        return RDFUtils.uri(currentEvaluationContext.base.toString());
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

        if(node.getNodeName().equalsIgnoreCase("HEAD") || node.getNodeName().equalsIgnoreCase("BODY")) {
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
            return currentEvaluationContext.parentObject;
        }

        return null;
    }

    private URI[] getTypes(Node node) throws URISyntaxException {
        final String typeOf = DomUtils.readAttribute(node, TYPEOF_ATTRIBUTE, null);
        return resolveCurieOrURIList(typeOf);
    }

    private URI[] getRels(Node node) throws URISyntaxException {
        final String rel = DomUtils.readAttribute(node, REL_ATTRIBUTE, null);
        return resolveCurieOrURIList(rel);
    }

    private URI[] getRevs(Node node) throws URISyntaxException {
        final String rev = DomUtils.readAttribute(node, REV_ATTRIBUTE, null);
        return resolveCurieOrURIList(rev);
    }

    private URI[] getPredicate(Node node) throws URISyntaxException {
        final String candidateURI = DomUtils.readAttribute(node, PROPERTY_ATTRIBUTE, null);
        if(candidateURI == null) return null;
        return resolveCurieOrURIList(candidateURI);
    }

    // 5.5.9
    private Literal getCurrentObjectLiteral(Node node) throws URISyntaxException, IOException {
        Literal literal;
        literal = getAsTypedLiteral(node);
        if(literal != null) return literal;
        literal = getAsPlainLiteral(node, this.currentEvaluationContext.currentLanguage);
        if(literal != null) return literal;
        literal = getAsXMLLiteral(node);
        if(literal != null) {
            currentEvaluationContext.recourse = false;
            return literal;
        }
        return null;
    }

    // 5.5.9.1
    private Literal getAsTypedLiteral(Node node) throws URISyntaxException {
        final String datatype = DomUtils.readAttribute(node, DATATYPE_ATTRIBUTE, null);
        if (datatype == null || XML_LITERAL_DATATYPE.equals(datatype.trim()) ) return null;
        final Resource curieOrURI = resolveCURIEOrURI(datatype);
        return RDFUtils.literal( node.getTextContent(), curieOrURI instanceof URI ? (URI) curieOrURI : null);
    }

    private void pushMappings(Node sourceNode, String[] prefixes, URI[] uris) {
        if (prefixes.length != uris.length) {
            throw new IllegalArgumentException();
        }

        final Map<String, URI> mapping = new HashMap<String, URI>();
        for (int i = 0; i < prefixes.length; i++) {
            mapping.put(prefixes[i], uris[i]);
        }
        uriMappingStack.push( new URIMapping(sourceNode, mapping) );
    }

    private void pushMappings(Node sourceNode, String[] mappings) {
        List<String> prefixes = new ArrayList<String>();
        List<URI> uris        = new ArrayList<URI>();
        for(String mapping : mappings) {
            final String[] mappingParts = mapping.split(":");
            if(mappingParts[0].length() > 0 && mappingParts[1].length() > 0) {
                prefixes.add(mappingParts[0]);
                uris    .add(RDFUtils.uri(mappingParts[1]));
            }
        }
        pushMappings(
                sourceNode,
                prefixes.toArray( new String[prefixes.size()] ),
                uris.toArray    ( new URI[uris.size()]        )
        );
    }

    private void popMappings(Node node) {
        if(uriMappingStack.isEmpty()) return;
        final URIMapping peek = uriMappingStack.peek();
        if(peek.sourceNode.equals(node)) uriMappingStack.pop();
    }

    // 5.4.2
    private Resource resolveCURIE(String curie, boolean verify) {
        if(verify && ! isCURIE(curie) ) {
            throw new IllegalArgumentException(
                String.format("The given string is not a valid CURIE: '%s'", curie)
            );
        }

        final String curieContent = curie.substring(1, curie.length() - 2);

        // 5.4.5
        if(isCURIEBNode(curie)) {
            return RDFUtils.bnode(curieContent);
        }

        return resolveURI(curie);
    }

    private Resource resolveURI(String uri) {
        final String[] parts = uri.split(":");
        final URI curieMapping = getMapping(parts[0]);
        if(curieMapping == null) {
            throw new IllegalArgumentException( String.format("Cannot map prefix '%s'", parts[0]) );
        }
        final String candidateCURIEStr = curieMapping.toString() + parts[1];
        final java.net.URI candidateCURIE;
        try {
            candidateCURIE = new java.net.URI(candidateCURIEStr);
        } catch (URISyntaxException urise) {
            throw new IllegalArgumentException(String.format("Invalid CURIE URI '%s'", candidateCURIEStr) );
        }
        return RDFUtils.uri(
                candidateCURIE.isAbsolute()
                        ?
                candidateCURIE.toString()
                        :
                documentBase.toString() + candidateCURIE.toString()
        );
    }

//    private Resource resolveURI(String uri) throws URISyntaxException {
//        if( new java.net.URI(uri).getSchemeSpecificPart().startsWith("//") ) {
//            return RDFUtils.uri(uri);
//        }
//        return RDFUtils.uri(documentBase.toString() + uri);
//    }

    private Resource resolveCURIEOrURI(String curieOrURI) throws URISyntaxException {
        if( isCURIE(curieOrURI) ) {
            return resolveCURIE(curieOrURI, false);
        }
        return resolveURI(curieOrURI);
    }

    private URI[] resolveCurieOrURIList(String curieOrURIList) throws URISyntaxException {
        if(curieOrURIList == null || curieOrURIList.trim().length() == 0) return new URI[0];

        final String[] curieOrURIListParts = curieOrURIList.split("\\s");
        final List<URI> result = new ArrayList<URI>();
        Resource curieOrURI;
        for(String curieORURIListPart : curieOrURIListParts) {
            curieOrURI = resolveCURIEOrURI(curieORURIListPart);
            if(curieOrURI != null && curieOrURI instanceof URI) {
                result.add((URI) curieOrURI);
            } else {
                // TODO: report error.
            }
        }
        return result.toArray( new URI[result.size()] );
    }

    class EvaluationContext {
        private URL base;
        private URI parentSubject;
        private URI parentObject;
        private String language;
        private boolean recourse = true;
        private boolean skipElem;
        private URI newSubject;
        private Resource currentObjectResource;
        private String currentLanguage;
        private final List<IncompleteTriple> localListOfIncompleteTriples = new ArrayList<IncompleteTriple>();

        EvaluationContext(URL base) {
            this.base = base;
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
        final URI predicate;
        final IncompleteTripleDirection direction;

        public IncompleteTriple(URI predicate, IncompleteTripleDirection direction) {
            this.predicate = predicate;
            this.direction = direction;
        }

        public void produceTriple(Resource s, Resource o, ExtractionResult extractionResult) {
            switch (direction) {
                case Forward:
                    extractionResult.writeTriple(s, predicate, o);
                    break;
                case Reverse:
                    extractionResult.writeTriple(o, predicate, s);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

}
