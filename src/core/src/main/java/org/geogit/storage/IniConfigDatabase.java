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
        	throw new ConfigException(1);
        }
        
		File f;
		try {
			f = new File(url.toURI() + "/.geogitconfig");
		}
		catch(URISyntaxException e) {
			f = new File(url.getPath() + "/.geogitconfig");
		}
		
		try {
			f.createNewFile();
		}
		catch (IOException e) {
			throw new ConfigException(2);
		}
		
		return f;
	}
	
	private File globalConfig() throws ConfigException {
		File f = new File(platform.getUserHome().getPath() + "/.geogitconfig");
		try {
			f.createNewFile();
		}
		catch (IOException e) {
			throw new ConfigException(2);
		}
		return f;
	}
	
	private String get(String key, File file) throws ConfigException {
		try {
			final SectionOptionPair pair = new SectionOptionPair(key);
			final Wini ini = new Wini(file);
			return ini.get(pair.section, pair.option);
		}
		catch (Exception e) {
			throw new ConfigException(1);
		}
	}
	
	private void put(String key, Object value, File file) throws ConfigException {
		try {
			final SectionOptionPair pair = new SectionOptionPair(key);
			final Wini ini = new Wini(file);
			ini.put(pair.section, pair.option, value);
			ini.store();
		} catch (Exception e) {
			throw new ConfigException(1);
		}
	}
	
	@Override
	public String get(String key) throws ConfigException {
		return get(key, config());
	}

	@Override
	public String getGlobal(String key) throws ConfigException {
		return get(key, globalConfig());
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
