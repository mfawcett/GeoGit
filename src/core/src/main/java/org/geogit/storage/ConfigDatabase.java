package org.geogit.storage;


public interface ConfigDatabase {

	@SuppressWarnings("serial")
	public class ConfigException extends Exception {
		public int statusCode;
		
		public ConfigException(int statusCode) {
			this.statusCode = statusCode;
		}
	}
	
	public String get(String key) throws ConfigException;
	public String getGlobal(String key) throws ConfigException;

	public void put(String key, Object value) throws ConfigException;
	public void putGlobal(String key, Object value) throws ConfigException;
	
}
