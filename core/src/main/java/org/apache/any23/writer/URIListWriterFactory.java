/**
 * 
 */
package org.apache.any23.writer;

import java.io.OutputStream;

import org.kohsuke.MetaInfServices;
import org.openrdf.rio.RDFFormat;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
@MetaInfServices
public class URIListWriterFactory implements WriterFactory
{
    
    public static final String MIME_TYPE = "text/plain";
    public static final String IDENTIFIER = "uri";

    /**
     * 
     */
    public URIListWriterFactory()
    {
    }
    
    @Override
    public RDFFormat getRdfFormat()
    {
        throw new RuntimeException("This writer does not print RDF triples");
    }

    @Override
    public String getIdentifier()
    {
        return URIListWriterFactory.IDENTIFIER;
    }

    @Override
    public String getMimeType()
    {
        return URIListWriterFactory.MIME_TYPE;
    }
    
    @Override
    public FormatWriter getRdfWriter(OutputStream os)
    {
        return new URIListWriter(os);
    }
    
}
