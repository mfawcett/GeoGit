/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;

/**
 * @author mfawcett
 * 
 * CLI proxy for {@link ConfigOp}
 */
@Parameters(commandNames = "config", commandDescription = "Get and set repository or global options")
public class Config extends AbstractCommand implements CLICommand {

    @Parameter(names = "--global", description = "For writing options: write to global ~/.geogitconfig file rather than the repository .geogit/config."
            + "For reading options: read only from global ~/.geogitconfig rather than from all available files.")
    private boolean global = false;

    @Parameter(names = "--get", description = "Get the value for a given key.")
    private boolean get = false;

    @Parameter(description = "name value (name is section.key format, value is only required when setting)")
    private List<String> nameValuePair;

    @Override
    public void runInternal(GeogitCLI cli) throws Exception {

        GeoGIT geogit = cli.getGeogit();
        if (null == geogit) {
            // we're not in a repository, need a geogit anyways to run the global commands
            geogit = cli.newGeoGIT();
        }

        try {
            final Optional<String> value = geogit.command(ConfigOp.class).setGet(get)
                    .setGlobal(global).setNameValuePair(nameValuePair).call();

            if (value.isPresent()) {
                cli.getConsole().println(value.get());
            }
        } catch (ConfigException e) {
            // These mirror 'git config' status codes. Some of these are unused,
            // since we don't have regex support yet.
            switch (e.statusCode) {
            case INVALID_LOCATION:
                // TODO: This could probably be more descriptive.
                cli.getConsole().println("The config location is invalid");
                break;
            case CANNOT_WRITE:
                cli.getConsole().println("Cannot write to the config");
                break;
            case SECTION_OR_NAME_NOT_PROVIDED:
                cli.getConsole().println("No section or name was provided");
                break;
            case SECTION_OR_KEY_INVALID:
                cli.getConsole().println("The section or key is invalid");
                break;
            case OPTION_DOES_N0T_EXIST:
                cli.getConsole().println("Tried to unset an option that does not exist");
                break;
            case MULTIPLE_OPTIONS_MATCH:
                cli.getConsole().println(
                        "Tried to unset/set an option for which multiple lines match");
                break;
            case INVALID_REGEXP:
                cli.getConsole().println("Tried to use an invalid regexp");
                break;
            case USERHOME_NOT_SET:
                cli.getConsole().println("Used --global option without $HOME being properly set");
                break;
            }
        }
    }

}

