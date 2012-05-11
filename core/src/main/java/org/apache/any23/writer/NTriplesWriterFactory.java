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
public class NTriplesWriterFactory implements WriterFactory
{
    
    public static final String MIME_TYPE = "text/plain";
    public static final String IDENTIFIER = "ntriples";

    /**
     * 
     */
    public NTriplesWriterFactory()
    {
    }
    
    @Override
    public RDFFormat getRdfFormat()
    {
        return RDFFormat.NTRIPLES;
    }

    @Override
    public String getIdentifier()
    {
        return NTriplesWriterFactory.IDENTIFIER;
    }

    @Override
    public String getMimeType()
    {
        return NTriplesWriterFactory.MIME_TYPE;
    }
    
    @Override
    public FormatWriter getRdfWriter(OutputStream os)
    {
        return new NTriplesWriter(os);
    }
    
}
