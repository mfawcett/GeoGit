package org.geogit.storage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.geogit.api.Platform;
import org.geogit.command.plumbing.ResolveGeogitDir;
import org.ini4j.Wini;

import com.google.inject.Inject;

public class IniConfigDatabase implements ConfigDatabase {

    @Inject
    private Platform platform;

    private class SectionOptionPair {
        String section;

        String option;

        public SectionOptionPair(String key) {
            final int index = key.indexOf('.');
            section = key.substring(0, index);
            option = key.substring(index + 1);
        }
    }

    private File config() throws ConfigException {
        final URL url = new ResolveGeogitDir(platform).call();

        if (url == null) {
            throw new ConfigException(StatusCode.INVALID_LOCATION);
        }

        File f;
        try {
            f = new File(new File(url.toURI()), "config");
        } catch (URISyntaxException e) {
            f = new File(url.getPath(), "config");
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new ConfigException(StatusCode.CANNOT_WRITE);
        }

        return f;
    }

    private File globalConfig() throws ConfigException {
        File home = platform.getUserHome();

        if (home == null) {
            throw new ConfigException(StatusCode.USERHOME_NOT_SET);
        }

        File f = new File(home.getPath(), ".geogitconfig");
        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new ConfigException(StatusCode.CANNOT_WRITE);
        }
        return f;
    }

    private <T> T get(String key, File file, Class<T> c) throws ConfigException {
        try {
            final SectionOptionPair pair = new SectionOptionPair(key);
            final Wini ini = new Wini(file);
            return ini.get(pair.section, pair.option, c);
        } catch (Exception e) {
            throw new ConfigException(StatusCode.INVALID_LOCATION);
        }
    }

    private void put(String key, Object value, File file) throws ConfigException {
        try {
            final SectionOptionPair pair = new SectionOptionPair(key);
            final Wini ini = new Wini(file);
            ini.put(pair.section, pair.option, value);
            ini.store();
        } catch (Exception e) {
            throw new ConfigException(StatusCode.INVALID_LOCATION);
        }
    }

    @Override
    public String get(String key) throws ConfigException {
        return get(key, config(), String.class);
    }

    @Override
    public String getGlobal(String key) throws ConfigException {
        return get(key, globalConfig(), String.class);
    }

    @Override
    public <T> T get(String key, Class<T> c) throws ConfigException {
        return get(key, config(), c);
    }

    @Override
    public <T> T getGlobal(String key, Class<T> c) throws ConfigException {
        return get(key, globalConfig(), c);
    }

    @Override
    public void put(String key, Object value) throws ConfigException {
        put(key, value, config());
    }

    @Override
    public void putGlobal(String key, Object value) throws ConfigException {
        put(key, value, globalConfig());
    }

}
