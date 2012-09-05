package org.geogit.cli;

public interface ConfigDatabase {

	public String get(String key);
	public String getGlobal(String key);

	public void put(String key, Object value);
	public void putGlobal(String key, Object value);
	
}
