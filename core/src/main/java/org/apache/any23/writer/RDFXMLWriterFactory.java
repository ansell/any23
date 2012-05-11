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
public class RDFXMLWriterFactory implements WriterFactory
{
    
    public static final String MIME_TYPE = "application/rdf+xml";
    public static final String IDENTIFIER = "rdfxml";

    /**
     * 
     */
    public RDFXMLWriterFactory()
    {
    }
    
    @Override
    public RDFFormat getRdfFormat()
    {
        return RDFFormat.RDFXML;
    }

    @Override
    public String getIdentifier()
    {
        return RDFXMLWriterFactory.IDENTIFIER;
    }

    @Override
    public String getMimeType()
    {
        return RDFXMLWriterFactory.MIME_TYPE;
    }
    
    @Override
    public FormatWriter getRdfWriter(OutputStream os)
    {
        return new RDFXMLWriter(os);
    }
    
}
