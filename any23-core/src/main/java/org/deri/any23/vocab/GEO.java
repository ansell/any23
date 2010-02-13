/*
 * Copyright 2008-2010 Digital Enterprise Research Institute (DERI)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deri.any23.vocab;

import org.openrdf.model.URI;

public class GEO extends Vocabulary {

    public static final String NS = "http://www.w3.org/2003/01/geo/wgs84_pos#";

    // Classes
    public static final URI Point = createURI(NS, "Point");

    // Properties
    public static final URI lat = createURI(NS, "lat" );
    public static final URI lon = createURI(NS, "long");

    private GEO(){}

}