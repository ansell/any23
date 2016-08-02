/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.any23.rdf;

import java.util.Date;

import org.apache.any23.extractor.IssueReport;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Any23 specialization of the {@link org.openrdf.model.ValueFactory}.
 * It provides a wrapper to instantiate RDF objects.
 */
// TODO: Merge with RDFUtils.java
public class Any23ValueFactoryWrapper implements ValueFactory {

    private static final Logger logger = LoggerFactory.getLogger(Any23ValueFactoryWrapper.class);

    private final ValueFactory wrappedFactory;

    private IssueReport issueReport;

    private String defaultLiteralLanguage;

    /**
     * Constructor with error reporter.
     *
     * @param factory the wrapped value factory, cannot be <code>null</code>.
     * @param er the error reporter.
     * @param defaultLitLanguage the default literal language.
     */
    public Any23ValueFactoryWrapper(
            final ValueFactory factory,
            IssueReport er,
            String defaultLitLanguage
    ) {
        if(factory == null) {
            throw new NullPointerException("factory cannot be null.");
        }
        wrappedFactory = factory;
        issueReport = er;
        defaultLiteralLanguage = defaultLitLanguage;
    }

    public Any23ValueFactoryWrapper(final ValueFactory vFactory, IssueReport er) {
        this(vFactory, er, null);
    }

    public Any23ValueFactoryWrapper(final ValueFactory vFactory) {
        this(vFactory, null, null);
    }

    public IssueReport getIssueReport() {
        return issueReport;
    }

    public void setIssueReport(IssueReport er) {
        issueReport = er;
    }

    public String getDefaultLiteralLanguage() {
        return defaultLiteralLanguage;
    }

    public BNode createBNode() {
        return wrappedFactory.createBNode();
    }

    public BNode createBNode(String id) {
        if (id == null) return null;
        return wrappedFactory.createBNode(id);
    }

    public Literal createLiteral(String content) {
        if (content == null) return null;
        return wrappedFactory.createLiteral(content, defaultLiteralLanguage);
    }

    public Literal createLiteral(boolean b) {
        return wrappedFactory.createLiteral(b);
    }

    public Literal createLiteral(byte b) {
        return wrappedFactory.createLiteral(b);
    }

    public Literal createLiteral(short i) {
        return wrappedFactory.createLiteral(i);
    }

    public Literal createLiteral(int i) {
        return wrappedFactory.createLiteral(i);
    }

    public Literal createLiteral(long l) {
        return wrappedFactory.createLiteral(l);
    }

    public Literal createLiteral(float v) {
        return wrappedFactory.createLiteral(v);
    }

    public Literal createLiteral(double v) {
        return wrappedFactory.createLiteral(v);
    }

    public Literal createLiteral(XMLGregorianCalendar calendar) {
        return wrappedFactory.createLiteral(calendar);
    }

    public Literal createLiteral(String label, String language) {
        if (label == null) return null;
        return wrappedFactory.createLiteral(label, language);
    }

    public Literal createLiteral(String pref, IRI value) {
        if (pref == null) return null;
        return wrappedFactory.createLiteral(pref, value);
    }

    @Override
    public Literal createLiteral(Date date) {
        return wrappedFactory.createLiteral(date);
    }

    public Statement createStatement(Resource sub, IRI pre, Value obj) {
        if (sub == null || pre == null || obj == null) {
            return null;
        }
        return wrappedFactory.createStatement(sub, pre, obj);
    }

    public Statement createStatement(Resource sub, IRI pre, Value obj, Resource context) {
        if (sub == null || pre == null || obj == null) return null;
        return wrappedFactory.createStatement(sub, pre, obj, context);
    }

    /**
     * @param uriStr
     * @return a valid sesame IRI or null if any exception occurred
     */
    public IRI createIRI(String uriStr) {
        if (uriStr == null) return null;
        try {
            return wrappedFactory.createIRI(RDFUtils.fixIRIWithException(uriStr));
        } catch (Exception e) {
            reportError(e);
            return null;
        }
    }

    /**
     * @return a valid sesame IRI or null if any exception occurred
     */
    public IRI createIRI(String namespace, String localName) {
        if (namespace == null || localName == null) return null;
        return wrappedFactory.createIRI(RDFUtils.fixIRIWithException(namespace), localName);
    }

    /**
     * Fixes typical errors in IRIs, and resolves relative IRIs against a base IRI.
     *
     * @param uri     A IRI, relative or absolute, can have typical syntax errors
     * @param baseIRI A base IRI to use for resolving relative IRIs
     * @return An absolute IRI, sytnactically valid, or null if not fixable
     */
    public IRI resolveIRI(String uri, java.net.URI baseIRI) {
        try {
            return wrappedFactory.createIRI(baseIRI.resolve(RDFUtils.fixIRIWithException(uri)).toString());
        } catch (IllegalArgumentException iae) {
            reportError(iae);
            return null;
        }
    }

    /**
     * @param uri
     * @return a valid sesame IRI or null if any exception occurred
     */
    public IRI fixIRI(String uri) {
        try {
            return wrappedFactory.createIRI(RDFUtils.fixIRIWithException(uri));
        } catch (Exception e) {
            reportError(e);
            return null;
        }
    }

    /**
     * Helper method to conditionally add a schema to a IRI unless it's there, or null if link is empty.
     */
    public IRI fixLink(String link, String defaultSchema) {
        if (link == null) return null;
        link = fixWhiteSpace(link);
        if ("".equals(link)) return null;
        if (defaultSchema != null && !link.startsWith(defaultSchema + ":")) {
            link = defaultSchema + ":" + link;
        }
        return fixIRI(link);
    }

    public String fixWhiteSpace(String name) {
        return name.replaceAll("\\s+", " ").trim();
    }

    /**
     * Reports an error in the most appropriate way.
     * 
     * @param e error to be reported.
     */
    private void reportError(Exception e) {
        if(issueReport == null) {
            logger.warn(e.getMessage());
        } else {
            issueReport.notifyIssue(IssueReport.IssueLevel.Warning, e.getMessage(), -1, -1);
        }
    }

}
