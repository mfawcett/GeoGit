package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.geogit.api.ConfigOp;
import org.geogit.api.Platform;
import org.geogit.storage.ConfigDatabase.ConfigException;
import org.geogit.storage.IniConfigDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

// TODO: Not sure if this belongs in porcelain or integration

public class ConfigOpTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public final void setUp() {
    }
    
    @After
    public final void tearDown() {
    }
    
    private void test(Platform platform, boolean global) throws ConfigException {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));
        config.setGlobal(global);
        
        config.setGet(false).setNameValuePair(Arrays.asList("section.string", "1")).call();

        final String one = config.setGet(true).setNameValuePair(Arrays.asList("section.string")).call();
        assertEquals(one, "1");
        
        // Test overwriting a value that already exists
        config.setGet(false).setNameValuePair(Arrays.asList("section.string", "2")).call();

        final String two = config.setGet(true).setNameValuePair(Arrays.asList("section.string")).call();
        assertEquals(two, "2");
    }
    
    @Test
    public void testLocal() throws ConfigException {
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        
        test(platform, false);
    }

    @Test
    public void testGlobal() throws ConfigException {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);
        
        test(platform, true);
    }
}
