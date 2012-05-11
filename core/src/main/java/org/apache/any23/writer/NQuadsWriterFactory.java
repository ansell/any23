/**
 * 
 */
package org.apache.any23.writer;

import java.io.OutputStream;

import net.fortytwo.sesametools.nquads.NQuadsFormat;

import org.kohsuke.MetaInfServices;
import org.openrdf.rio.RDFFormat;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
@MetaInfServices
public class NQuadsWriterFactory implements WriterFactory
{
    
    public static final String MIME_TYPE = "text/plain";
    public static final String IDENTIFIER = "nquads";

    /**
     * 
     */
    public NQuadsWriterFactory()
    {
    }
    
    @Override
    public RDFFormat getRdfFormat()
    {
        return NQuadsFormat.NQUADS;
    }

    @Override
    public String getIdentifier()
    {
        return NQuadsWriterFactory.IDENTIFIER;
    }

    @Override
    public String getMimeType()
    {
        return NQuadsWriterFactory.MIME_TYPE;
    }
    
    @Override
    public FormatWriter getRdfWriter(OutputStream os)
    {
        return new NQuadsWriter(os);
    }
    
}
