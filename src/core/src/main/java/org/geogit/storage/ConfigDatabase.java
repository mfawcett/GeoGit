package org.geogit.storage;

public interface ConfigDatabase {
    public enum StatusCode {
        INVALID_LOCATION, CANNOT_WRITE, SECTION_OR_NAME_NOT_PROVIDED, SECTION_OR_KEY_INVALID, OPTION_DOES_N0T_EXIST, MULTIPLE_OPTIONS_MATCH, INVALID_REGEXP, USERHOME_NOT_SET
    }

    @SuppressWarnings("serial")
    public class ConfigException extends Exception {
        public StatusCode statusCode;

        public ConfigException(StatusCode statusCode) {
            this.statusCode = statusCode;
        }
    }

    public String get(String key) throws ConfigException;

    public String getGlobal(String key) throws ConfigException;

    public <T> T get(String key, Class<T> c) throws ConfigException;

    public <T> T getGlobal(String key, Class<T> c) throws ConfigException;

    public void put(String key, Object value) throws ConfigException;

    public void putGlobal(String key, Object value) throws ConfigException;

}
