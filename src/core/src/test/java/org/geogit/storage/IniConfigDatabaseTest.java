package org.geogit.storage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase.ConfigException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

// TODO: Not sure if this belongs in porcelain or integration

public class IniConfigDatabaseTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Before
    public final void setUp() {
    }
    
    @After
    public final void tearDown() {
    }
    
    @Test
    public void testLocal() throws ConfigException {
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);

        final ConfigDatabase ini = new IniConfigDatabase(platform);        
        
        // Test get when config file doesn't even exist yet
        final String none = ini.get("section.int");
        assertEquals(none, null);
        
        // Test integer and string
        ini.put("section.int", "1");
        ini.put("section.string", "2");

        final int one = ini.get("section.int", int.class);
        assertEquals(one, 1);
        
        final String two = ini.get("section.string");
        assertEquals(two, "2");
        
        // Test overwriting a value that already exists
        ini.put("section.string", "3");

        final String three = ini.get("section.string");
        assertEquals(three, "3");
    }
    
    @Test
    public void testGlobal() throws ConfigException {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);
        
        final ConfigDatabase ini = new IniConfigDatabase(platform);
        
        // Test get when config file doesn't even exist yet
        final String none = ini.getGlobal("section.int");
        assertEquals(none, null);
        
        // Test integer and string
        ini.putGlobal("section.int", 1);
        ini.putGlobal("section.string", "2");

        final int one = ini.getGlobal("section.int", int.class);
        assertEquals(one, 1);
        
        final String two = ini.getGlobal("section.string");
        assertEquals(two, "2");
        
        // Test overwriting a value that already exists
        ini.putGlobal("section.string", "3");

        final String three = ini.getGlobal("section.string");
        assertEquals(three, "3");
    }
    
}
