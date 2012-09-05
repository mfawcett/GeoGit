/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.List;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * @author mfawcett
 * 
 */
@Parameters(commandNames = "config", commandDescription = "Get and set repository or global options")
public class Config extends AbstractCommand implements CLICommand {

    @Parameter(names = "--global", description = "For writing options: write to global ~/.geogitconfig file rather than the repository .geogit/config.\n" +
    "For reading options: read only from global ~/.geogitconfig rather than from all available files.")
    private boolean global = false;
	
    @Parameter(names = "--get", description = "Get the value for a given key. Returns error code 1 if the key was not found and error code 2 if multiple key values were found.")
    private boolean get = false;
    
    @Parameter(description = "name value")
    private List<String> nameValuePair;
    
    @Override
    public void runInternal(GeogitCLI cli) {
    	
    	System.out.println("global: " + global);
    	System.out.println("get: " + get);
    	System.out.println("name: " + nameValuePair.toString());
    }

}
