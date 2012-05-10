package org.apache.any23.extractor.html;

import org.w3c.dom.Node;

public class TagSoupParserUtils
{
    /**
     * Returns the row/col location of the given node.
     *
     * @param n input node.
     * @return an array of two elements of type
     *         <code>[&lt;begin-row&gt;, &lt;begin-col&gt;, &lt;end-row&gt; &lt;end-col&gt;]</code>
     *         or <code>null</code> if not possible to extract such data.
     */
    public static int[] getNodeLocation(Node n) {
        if(n == null) throw new NullPointerException("node cannot be null.");
        final TagSoupParser.ElementLocation elementLocation =
            (TagSoupParser.ElementLocation) n.getUserData( TagSoupParser.ELEMENT_LOCATION );
        if(elementLocation == null) return null;
        return new int[]{
                elementLocation.getBeginLineNumber(),
                elementLocation.getBeginColumnNumber(),
                elementLocation.getEndLineNumber(),
                elementLocation.getEndColumnNumber()
        };
    }

    
}
