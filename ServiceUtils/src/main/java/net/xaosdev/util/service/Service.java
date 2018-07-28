package net.xaosdev.util.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A Service is a set of automatically managed java.util.ServiceLoader instances.
 *
 * A ServiceLoader instance will be created for each source allowing new sources to be added and removed on the fly
 * by the user.  This will allow users to use the ServiceLoader indirectly, and only need to be concerned with where
 * the service classes are being loaded from.
 *
 * java.util.ServiceLoader best practices should still be enforced!  This means that, ideally, your SPI classes should
 * be interfaces, and the implementations should have no-argument or default constructors to ensure no unexpected
 * exceptions are fired.
 *
 * The Service class will catch all throwables and throw a ServiceException with the original throwable as parent.
 * This allows error handling to be pipelined appropriately.
 * @param <T> the SPI to find implementations for.
 */
public final class Service<T> {

    //region Fields (Private)

    /**
     * The SPI Class.
     */
    private final Class<T> clazz;

    /**
     * A mapping of UUIDs to Sources, for source management.
     */
    private final Map<UUID, Source> sourceMap = new HashMap<>();

    /**
     * A mapping of UUIDs to ServiceLoaders, for loader management.
     */
    private final Map<UUID, ServiceLoader<T>> loaderMap = new HashMap<>();

    //endregion

    //region Constructors (Public)

    /**
     * Creates a new Service.
     * @param clazz the Class object used to identify service implementations.
     */
    public Service(final Class<T> clazz) {
        this.clazz = clazz;
    }

    //endregion

    //region Interface (Public)

    /**
     * Adds a source to this Service.
     * @param source the Source to add.
     */
    public void addSource(final Source source) {
        if (sourceMap.containsKey(source.getUUID())) {
            throw new IllegalArgumentException("Source with UUID already added to this Service.");
        }

        sourceMap.put(source.getUUID(), source);
        loaderMap.put(source.getUUID(), ServiceLoader.load(clazz, source.getClassLoader()));
    }

    /**
     * Gets an unmodifiable view of all the sources within this Service.
     * @return an unmodifiable view of all the sources added to this Service.
     */
    public Collection<Source> getSources() {
        return Collections.unmodifiableCollection(sourceMap.values());
    }

    /**
     * Removes a Source from this Service.
     * @param source the Source to remove.
     * @return a boolean indicating if the source was removed.
     */
    public boolean removeSource(final Source source) {
        return removeSource(source.getUUID()) != null;
    }

    /**
     * Removes a Source from this Service.
     * @param uuid the UUID of the Source to remove.
     * @return the Source removed from this Service or null if none present.
     */
    public Source removeSource(final UUID uuid) {
        if (!sourceMap.containsKey(uuid)) {
            return null;
        }

        final Source source = sourceMap.remove(uuid);
        loaderMap.remove(uuid);
        return source;
    }

    /**
     * Gets a stream of all the service implementations within this Service.
     * @return a Stream to the implementations found by this Service.
     */
    public Stream<T> getServiceStream() {
        Stream<T> stream = Stream.empty();
        for (ServiceLoader<T> loader : loaderMap.values()) {
            stream = Stream.concat(stream, StreamSupport.stream(loader.spliterator(), false));
        }
        return stream;
    }

    //endregion
}
