package org.geogit.storage;

import java.io.File;

import org.geogit.api.Platform;
import org.ini4j.Wini;

public class IniConfigDatabase implements ConfigDatabase {

	private Platform platform;
	
	private class SectionOptionPair {
		String section;
		String option;
		
		public SectionOptionPair(String key) {
			int index = key.indexOf('.');
			section = key.substring(0, index);
			option = key.substring(index + 1);
		}
	}
	
	public IniConfigDatabase(Platform platform)
	{
		this.platform = platform;
	}

	private File config() {
		return null;
	}
	
	private File globalConfig() {
		return platform.getUserHome();
	}
	
	private String get(String key, File file) {
		try {
			SectionOptionPair pair = new SectionOptionPair(key);
			Wini ini = new Wini(file);
			return ini.get(pair.section, pair.option);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void put(String key, Object value, File file) {
		try {
			SectionOptionPair pair = new SectionOptionPair(key);
			Wini ini = new Wini(file);
			ini.put(pair.section, pair.option, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String get(String key) {
		return get(key, config());
	}

	@Override
	public String getGlobal(String key) {
		return get(key, globalConfig());
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
