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
public class TriXWriterFactory implements WriterFactory
{
    
    public static final String MIME_TYPE = "application/trix";
    public static final String IDENTIFIER = "trix";

    /**
     * 
     */
    public TriXWriterFactory()
    {
    }
    
    @Override
    public RDFFormat getRdfFormat()
    {
        return RDFFormat.TRIX;
    }

    @Override
    public String getIdentifier()
    {
        return TriXWriterFactory.IDENTIFIER;
    }

    @Override
    public String getMimeType()
    {
        return TriXWriterFactory.MIME_TYPE;
    }
    
    @Override
    public FormatWriter getRdfWriter(OutputStream os)
    {
        return new TriXWriter(os);
    }
    
}
