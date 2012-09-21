package org.geogit.storage;

import com.google.common.base.Optional;

public interface ConfigDatabase {

    public Optional<String> get(String key);

    public Optional<String> getGlobal(String key);

    public <T> Optional<T> get(String key, Class<T> c);

    public <T> Optional<T> getGlobal(String key, Class<T> c);

    public void put(String key, Object value);

    public void putGlobal(String key, Object value);

}
