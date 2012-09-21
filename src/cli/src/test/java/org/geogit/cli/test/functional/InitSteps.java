/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.test.functional;

import static org.geogit.cli.test.functional.GlobalState.currentDirectory;
import static org.geogit.cli.test.functional.GlobalState.stdOut;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.apache.commons.io.FileUtils;

import cucumber.annotation.en.Given;
import cucumber.annotation.en.Then;
import cucumber.annotation.en.When;

/**
 *
 */
public class InitSteps extends AbstractGeogitFunctionalTest {

    @Given("^I am in an empty directory$")
    public void I_am_in_an_empty_directory() throws Throwable {
        currentDirectory = new File("target", "testrepo");
        FileUtils.deleteDirectory(currentDirectory);
        assertFalse(currentDirectory.exists());
        assertTrue(currentDirectory.mkdirs());
        assertEquals(0, currentDirectory.list().length);
    }

    @When("^I run the command \"([^\"]*)\"$")
    public void I_run_the_command_X(String commandSpec) throws Throwable {
        String[] args = commandSpec.split(" ");
        runCommand(args);
    }

    @Then("^it should answer \"([^\"]*)\"$")
    public void it_should_answer_exactly(String expected) throws Throwable {
        expected = expected.replace("${currentdir}", currentDirectory.getAbsolutePath());
        String actual = stdOut.toString().replaceAll("\n", "");
        assertEquals(expected, actual);
    }

    @Then("^the response should start with \"([^\"]*)\"$")
    public void the_response_should_start_with(String expected) throws Throwable {
        String actual = stdOut.toString().replaceAll("\n", "");
        assertTrue(actual.startsWith(expected));
    }

    @Then("^the repository directory shall exist$")
    public void the_repository_directory_shall_exist() throws Throwable {
        List<String> output = runAndParseCommand("rev-parse", "--resolve-geogit-dir");
        assertEquals(output.toString(), 1, output.size());
        String location = output.get(0);
        assertNotNull(location);
        if (location.startsWith("Error:")) {
            fail(location);
        }
        File repoDir = new File(new URI(location));
        assertTrue("Repository directory not found: " + repoDir.getAbsolutePath(), repoDir.exists());
    }

    @Given("^I have a repository$")
    public void I_have_a_repository() throws Throwable {
        FileUtils.deleteDirectory(currentDirectory);
        assertFalse(currentDirectory.exists());
        assertTrue(currentDirectory.mkdirs());

        List<String> output = runAndParseCommand("init");
        assertEquals(output.toString(), 1, output.size());
        assertNotNull(output.get(0));
        assertTrue(output.get(0), output.get(0).startsWith("Initialized"));
    }

    @Then("^if I change to the respository subdirectory \"([^\"]*)\"$")
    public void if_I_change_to_the_respository_subdirectory(String subdirSpec) throws Throwable {
        String[] subdirs = subdirSpec.split("/");
        File dir = currentDirectory;
        for (String subdir : subdirs) {
            dir = new File(dir, subdir);
        }
        assertTrue(dir.exists());
        currentDirectory = dir;
    }

    @Given("^I am inside a repository subdirectory \"([^\"]*)\"$")
    public void I_am_inside_a_repository_subdirectory(String subdirSpec) throws Throwable {
        String[] subdirs = subdirSpec.split("/");
        File dir = currentDirectory;
        for (String subdir : subdirs) {
            dir = new File(dir, subdir);
        }
        assertTrue(dir.mkdirs());
        currentDirectory = dir;
    }
}
