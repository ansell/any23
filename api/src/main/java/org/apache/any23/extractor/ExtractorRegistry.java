package org.apache.any23.extractor;

import java.util.List;

public interface ExtractorRegistry
{
    
    /**
     * Registers an {@link ExtractorFactory}.
     *
     * @param factory
     * @throws IllegalArgumentException if trying to register a {@link ExtractorFactory}
     *         with a that already exists in the registry.
     */
    void register(ExtractorFactory<?> factory);
    
    /**
     *
     * Retrieves a {@link ExtractorFactory} given its name
     *
     * @param name of the desired factory
     * @return the {@link ExtractorFactory} associated to the provided name
     * @throws IllegalArgumentException if there is not a
     * {@link ExtractorFactory} associated to the provided name.
     */
    ExtractorFactory<?> getFactory(String name);
    
    /**
     * @return an {@link ExtractorGroup} with all the registered
     * {@link Extractor}.
     */
    ExtractorGroup getExtractorGroup();
    
    /**
     * Returns an {@link ExtractorGroup} containing the
     * {@link ExtractorFactory} mathing the names provided as input.
     * @param names a {@link java.util.List} containing the names of the desired {@link ExtractorFactory}.
     * @return the extraction group.
     */
    ExtractorGroup getExtractorGroup(List<String> names);
    
    /**
     * 
     * @param name of the {@link ExtractorFactory}
     * @return <code>true</code> if is there a {@link ExtractorFactory}
     * associated to the provided name.
     */
    boolean isRegisteredName(String name);
    
    /**
     * Returns the names of all registered extractors, sorted alphabetically.
     */
    List<String> getAllNames();
    
}