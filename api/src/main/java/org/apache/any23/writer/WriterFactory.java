/**
 * 
 */
package org.apache.any23.writer;

import java.io.OutputStream;

import org.openrdf.rio.RDFFormat;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
public interface WriterFactory
{
    RDFFormat getRdfFormat();
    
    String getIdentifier();
    
    String getMimeType();
    
    FormatWriter getRdfWriter(OutputStream os);
}
