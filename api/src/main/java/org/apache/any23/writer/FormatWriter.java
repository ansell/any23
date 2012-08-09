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

package org.apache.any23.writer;

import org.openrdf.rio.RDFFormat;

/**
 * Base interface used for the definition of <i>RDF format writers</i>.
 */
public interface FormatWriter extends TripleHandler {

    /**
     * If <code>true</code> then the produced <b>RDF</b> is annotated with
     * the extractors used to generate the specific statements.
     *
     * @return the annotation flag value.
     */
     boolean isAnnotated();

    /**
     * Sets the <i>annotation</i> flag.
     *
     * @param f If <code>true</code> then the produced <b>RDF</b> is annotated with
     *          the extractors used to generate the specific statements.
     */
     void setAnnotated(boolean f);
     
     /**
      * The mnemonic identifier for the format.
      *
      * @return a not <code>null</code> identifier.
      */
     String getIdentifier();

     String getMimeType();
     
     /**
      * 
      * @return The Rio RDFFormat object matching the format for this writer.
      */
     RDFFormat getRdfFormat();

}
