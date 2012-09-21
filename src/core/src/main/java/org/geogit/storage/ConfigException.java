package org.geogit.storage;

@SuppressWarnings("serial")
public class ConfigException extends RuntimeException {
    public enum StatusCode {
        INVALID_LOCATION, CANNOT_WRITE, SECTION_OR_NAME_NOT_PROVIDED, SECTION_OR_KEY_INVALID, OPTION_DOES_N0T_EXIST, MULTIPLE_OPTIONS_MATCH, INVALID_REGEXP, USERHOME_NOT_SET
    }

    public ConfigException.StatusCode statusCode;

    public ConfigException(ConfigException.StatusCode statusCode) {
        this(null, statusCode);
    }

    public ConfigException(Exception e, ConfigException.StatusCode statusCode) {
        super(e);
        this.statusCode = statusCode;
    }
}
