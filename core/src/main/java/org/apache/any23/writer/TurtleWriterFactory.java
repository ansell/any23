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
public class TurtleWriterFactory implements WriterFactory
{
    
    public static final String MIME_TYPE = "text/turtle";
    public static final String IDENTIFIER = "turtle";

    /**
     * 
     */
    public TurtleWriterFactory()
    {
    }
    
    @Override
    public RDFFormat getRdfFormat()
    {
        return RDFFormat.TURTLE;
    }

    @Override
    public String getIdentifier()
    {
        return TurtleWriterFactory.IDENTIFIER;
    }

    @Override
    public String getMimeType()
    {
        return TurtleWriterFactory.MIME_TYPE;
    }
    
    @Override
    public FormatWriter getRdfWriter(OutputStream os)
    {
        return new TurtleWriter(os);
    }
    
}
