package org.geogit.api.porcelain;

import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.porcelain.ConfigException.StatusCode;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class ConfigOp extends AbstractGeoGitOp<Optional<String>> {

    private boolean global;

    private boolean get;

    private List<String> nameValuePair;

    final private ConfigDatabase config;

    @Inject
    public ConfigOp(ConfigDatabase config) {
        this.config = config;
    }

    @Override
    public Optional<String> call() {
        if (nameValuePair == null || nameValuePair.isEmpty()) {
            throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);
        }

        // Alternate syntax is to omit '--get' and only provide section.key, no value
        if (get || nameValuePair.size() == 1) {
            String name = nameValuePair.get(0);
            Optional<String> value;
            if (global) {
                value = config.getGlobal(name);
            } else {
                value = config.get(name);
            }

            return value;
        } else {
            if (nameValuePair.size() > 2) {
                throw new ConfigException(StatusCode.SECTION_OR_KEY_INVALID);
            }

            if (global) {
                config.putGlobal(nameValuePair.get(0), nameValuePair.get(1));
            } else {
                config.put(nameValuePair.get(0), nameValuePair.get(1));
            }
        }

        return Optional.absent();
    }

    public boolean getGlobal() {
        return global;
    }

    public ConfigOp setGlobal(boolean global) {
        this.global = global;
        return this;
    }

    public boolean getGet() {
        return get;
    }

    public ConfigOp setGet(boolean get) {
        this.get = get;
        return this;
    }

    public List<String> getNameValuePair() {
        return nameValuePair;
    }

    public ConfigOp setNameValuePair(List<String> nameValuePair) {
        this.nameValuePair = nameValuePair;
        return this;
    }

}
