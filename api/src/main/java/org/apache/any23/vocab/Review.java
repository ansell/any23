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

package org.apache.any23.vocab;

import org.openrdf.model.IRI;

/**
 * Vocabulary definitions from vocabularies/review.rdf
 */
public class Review extends Vocabulary {

    private static Review instance;

    public static Review getInstance() {
        if(instance == null) {
            instance = new Review();
        }
        return instance;
    }

    /**
     * The namespace of the vocabulary as a string.
     */
    public static final String NS = "http://purl.org/stuff/rev#";

    /**
     * The namespace of the vocabulary as a URI.
     */
    public final IRI NAMESPACE = createIRI(NS);

    /**
     * The commenter on the review.
     */
    public final IRI commenter =  createProperty("commenter");

    /**
     * Used to associate a review with a comment on the review.
     */
    public final IRI hasComment = createProperty("hasComment");

    /**
     * Associates a review with a feedback on the review.
     */
    public final IRI hasFeedback = createProperty("hasFeedback");

    /**
     * Associates a work with a a review.
     */
    public final IRI hasReview = createProperty("hasReview");

    /**
     * A numeric value.
     */
    public final IRI maxRating = createProperty("maxRating");

    /**
     * A numeric value.
     */
    public final IRI minRating = createProperty("minRating");

    /**
     * Number of positive usefulness votes (integer).
     */
    public final IRI positiveVotes = createProperty("positiveVotes");

    /**
     * A numeric value.
     */
    public final IRI rating = createProperty("rating");

    /**
     * The person that has written the review.
     */
    public final IRI reviewer = createProperty("reviewer");

    /**
     * The text of the review.
     */
    public final IRI text = createProperty("text");

    /**
     * The title of the review.
     */
    public final IRI title = createProperty("title");

    /**
     * Number of usefulness votes (integer).
     */
    public final IRI totalVotes = createProperty("totalVotes");

    /**
     * The type of media of a work under review.
     */
    public final IRI type = createProperty("type");

    /**
     * A comment on a review.
     */
    public final IRI Comment = createProperty("Comment");

    /**
     * Feedback on the review. Expresses whether the review was useful or not.
     */
    public final IRI Feedback = createProperty("Feedback");

    /**
     * A review of an work.
     */
    public final IRI Review = createProperty("Review");

    private IRI createProperty(String localName) {
        return createProperty(NS, localName);
    }

    private Review(){
        super(NS);
    }

}
