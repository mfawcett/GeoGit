package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geogit.api.Platform;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.storage.fs.IniConfigDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

// TODO: Not sure if this belongs in porcelain or integration

public class ConfigOpTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public final void setUp() {
    }

    @After
    public final void tearDown() {
    }

    private void test(Platform platform, boolean global) {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));
        config.setGlobal(global);

        config.setGet(false).setNameValuePair(Arrays.asList("section.string", "1")).call();

        final String one = config.setGet(true).setNameValuePair(Arrays.asList("section.string"))
                .call().or("-1");
        assertEquals(one, "1");

        // Get using alternate syntax
        final String alt = config.setGet(false).setNameValuePair(Arrays.asList("section.string"))
                .call().or("-1");
        assertEquals(alt, "1");

        // Test overwriting a value that already exists
        config.setGet(false).setNameValuePair(Arrays.asList("section.string", "2")).call();

        final String two = config.setGet(true).setNameValuePair(Arrays.asList("section.string"))
                .call().or("-1");
        assertEquals(two, "2");
    }

    @Test
    public void testLocal() {
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);

        test(platform, false);
    }

    @Test
    public void testGlobal() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        test(platform, true);
    }

    @Test
    public void testNullNameValuePair() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setGet(false).setNameValuePair(null).call();
    }

    @Test
    public void testInvalidSectionKey() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));
        Optional<String> str = config.setGlobal(true).setGet(true)
                .setNameValuePair(Arrays.asList("doesnt.exist")).call();
        assertFalse(str.isPresent());
    }

    @Test
    public void testEmptyList() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setGet(true).setNameValuePair(new ArrayList<String>()).call();
    }

    @Test
    public void testTooManyArguments() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setGet(false)
                .setNameValuePair(Arrays.asList("more.than", "two", "arguments")).call();
    }

    @Test
    public void testAccessors() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));
        config.setGlobal(true);
        assertTrue(config.getGlobal());

        config.setGlobal(false);
        assertFalse(config.getGlobal());

        config.setGet(true);
        assertTrue(config.getGet());

        config.setGet(false);
        assertFalse(config.getGet());

        List<String> list = Arrays.asList("section.string", "value");
        config.setNameValuePair(list);
        assertEquals(config.getNameValuePair(), list);
    }
}
