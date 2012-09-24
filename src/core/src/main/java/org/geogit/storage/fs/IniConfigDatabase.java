package org.geogit.storage.fs;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigException.StatusCode;
import org.geogit.storage.ConfigDatabase;
import org.ini4j.Wini;

import com.google.common.base.Optional;
import com.google.inject.Inject;

public class IniConfigDatabase implements ConfigDatabase {

    final private Platform platform;

    @Inject
    public IniConfigDatabase(Platform platform) {
        this.platform = platform;
    }

    private class SectionOptionPair {
        String section;

        String option;

        public SectionOptionPair(String key) {
            final int index = key.indexOf('.');

            if (index == -1) {
                throw new ConfigException(StatusCode.SECTION_OR_KEY_INVALID);
            }

            section = key.substring(0, index);
            option = key.substring(index + 1);

            if (section.length() == 0 || option.length() == 0) {
                throw new ConfigException(StatusCode.SECTION_OR_KEY_INVALID);
            }
        }
    }

    private File config() {
        final URL url = new ResolveGeogitDir(platform).call();

        if (url == null) {
            throw new ConfigException(StatusCode.INVALID_LOCATION);
        }

        /*
         * See http://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html for explanation on this idiom.
         */
        File f;
        try {
            f = new File(new File(url.toURI()), "config");
        } catch (URISyntaxException e) {
            f = new File(url.getPath(), "config");
        }

        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new ConfigException(e, StatusCode.CANNOT_WRITE);
        }

        return f;
    }

    private File globalConfig() {
        File home = platform.getUserHome();

        if (home == null) {
            throw new ConfigException(StatusCode.USERHOME_NOT_SET);
        }

        File f = new File(home.getPath(), ".geogitconfig");
        try {
            f.createNewFile();
        } catch (IOException e) {
            throw new ConfigException(e, StatusCode.CANNOT_WRITE);
        }
        return f;
    }

    private <T> Optional<T> get(String key, File file, Class<T> c) {
        if (key == null) {
            throw new ConfigException(StatusCode.SECTION_OR_NAME_NOT_PROVIDED);
        }

        final SectionOptionPair pair = new SectionOptionPair(key);
        try {
            final Wini ini = new Wini(file);
            T value = ini.get(pair.section, pair.option, c);

            if (value == null)
                return Optional.absent();
            
            if (c == String.class && ((String) value).length() == 0)
                return Optional.absent();

            return Optional.of(value);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }
    }

    private void put(String key, Object value, File file) {
        final SectionOptionPair pair = new SectionOptionPair(key);
        try {
            final Wini ini = new Wini(file);
            ini.put(pair.section, pair.option, value);
            ini.store();
        } catch (Exception e) {
            throw new ConfigException(e, StatusCode.INVALID_LOCATION);
        }
    }

    @Override
    public Optional<String> get(String key) {
        return get(key, config(), String.class);
    }

    @Override
    public Optional<String> getGlobal(String key) {
        return get(key, globalConfig(), String.class);
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> c) {
        return get(key, config(), c);
    }

    @Override
    public <T> Optional<T> getGlobal(String key, Class<T> c) {
        return get(key, globalConfig(), c);
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, config());
    }

    @Override
    public void putGlobal(String key, Object value) {
        put(key, value, globalConfig());
    }

}
