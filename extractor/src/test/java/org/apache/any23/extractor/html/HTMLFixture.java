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

package org.apache.any23.extractor.html;

import org.apache.any23.source.DocumentSource;
import org.apache.any23.source.StringDocumentSource;
import org.apache.tika.io.IOUtils;
import org.junit.Assert;
import org.w3c.dom.Node;

import java.io.IOException;

/**
 * This class is a wrapper around an HTML document providing a simply facade.
 */
public class HTMLFixture {

    private final String filename;

    public HTMLFixture(String filename) {
        this.filename = filename;
    }

    public DocumentSource getOpener(String baseURI) {
        try
        {
            return new StringDocumentSource(IOUtils.toString(this.getClass().getResourceAsStream(filename)), baseURI);
        }
        catch(IOException e)
        {
            Assert.fail("Could not process document");
            throw new AssertionError(e);
        }
    }

    /**
     * @return the DOM root {@link org.w3c.dom.Node} of the whole document.
     */
    public Node getDOM() {
        try {
            return new TagSoupParser(this.getClass().getResourceAsStream(filename), "http://example.org/").getDOM();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return an {@link HTMLDocument} object of the whole HTML document.
     */
    public HTMLDocument getHTMLDocument() {
        return new HTMLDocument(getDOM());
    }
}
