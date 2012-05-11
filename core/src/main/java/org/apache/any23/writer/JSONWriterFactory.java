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
public class JSONWriterFactory implements WriterFactory
{
    
    public static final String MIME_TYPE = "text/json";
    public static final String IDENTIFIER = "json";

    /**
     * 
     */
    public JSONWriterFactory()
    {
    }
    
    @Override
    public RDFFormat getRdfFormat()
    {
        throw new RuntimeException("TODO: Implement an RDFFormat for this RDF JSON serialisation format");
    }

    @Override
    public String getIdentifier()
    {
        return JSONWriterFactory.IDENTIFIER;
    }

    @Override
    public String getMimeType()
    {
        return JSONWriterFactory.MIME_TYPE;
    }
    
    @Override
    public FormatWriter getRdfWriter(OutputStream os)
    {
        return new JSONWriter(os);
    }
    
}
