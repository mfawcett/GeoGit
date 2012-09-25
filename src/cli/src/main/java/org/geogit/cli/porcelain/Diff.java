/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.cli.porcelain;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.ADDED;
import static org.geogit.api.plumbing.diff.DiffEntry.ChangeType.MODIFIED;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.porcelain.DiffOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.AnsiDecorator;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.StagingDatabase;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 */
@Parameters(commandNames = "diff", commandDescription = "Show changes between commits, commit and working tree, etc")
public class Diff extends AbstractCommand implements CLICommand {

    @Parameter(description = "[<commit> [<commit>]] [-- <path>...]", arity = 2)
    private List<String> refSpec;

    @Parameter(names = "--", hidden = true, variableArity = true)
    private List<String> paths;

    @Parameter(names = "--cached", description = "compares the specified tree (commit, branch, etc) and the staging area")
    private boolean cached;

    @Parameter(names = "--raw", description = "List only summary changes for each feature")
    private boolean raw;

    /**
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {

        GeoGIT geogit = cli.getGeogit();

        DiffOp diff = geogit.command(DiffOp.class);

        ObjectId oldVersion = resolveOldVersion(geogit);
        ObjectId newVersion = resolveNewVersion(geogit);

        diff.setOldVersion(oldVersion).setNewVersion(newVersion);

        Iterator<DiffEntry> entries = diff.setProgressListener(cli.getProgressListener()).call();

        DiffPrinter printer;
        if (raw) {
            printer = new RawPrinter();
        } else {
            printer = new FullPrinter();
        }
        DiffEntry entry;
        while (entries.hasNext()) {
            entry = entries.next();
            printer.print(geogit, cli.getConsole(), entry);
        }
    }

    /**
     * @param geogit
     * @return
     */
    private ObjectId resolveNewVersion(GeoGIT geogit) {
        return geogit.getRepository().getHead().get().getObjectId();
    }

    /**
     * @param geogit
     * @return
     */
    private ObjectId resolveOldVersion(GeoGIT geogit) {
        Repository repository = geogit.getRepository();
        ObjectId objectId = repository.getHead().get().getObjectId();
        RevCommit commit = repository.getCommit(objectId);
        ObjectId parentId = commit.getParentIds().get(0);
        return parentId;
    }

    private static interface DiffPrinter {

        /**
         * @param geogit
         * @param console
         * @param entry
         * @throws IOException
         */
        void print(GeoGIT geogit, ConsoleReader console, DiffEntry entry) throws IOException;

    }

    private static class RawPrinter implements DiffPrinter {

        @Override
        public void print(GeoGIT geogit, ConsoleReader console, DiffEntry entry) throws IOException {

            Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());

            final NodeRef newObject = entry.getNewObject();
            final NodeRef oldObject = entry.getOldObject();

            String oldMode = oldObject == null ? shortOid(ObjectId.NULL) : shortOid(oldObject
                    .getMetadataId());
            String newMode = newObject == null ? shortOid(ObjectId.NULL) : shortOid(newObject
                    .getMetadataId());

            String oldId = oldObject == null ? shortOid(ObjectId.NULL) : shortOid(oldObject
                    .getObjectId());
            String newId = newObject == null ? shortOid(ObjectId.NULL) : shortOid(newObject
                    .getObjectId());

            ansi.a(oldMode).a(" ");
            ansi.a(newMode).a(" ");

            ansi.a(oldId).a(" ");
            ansi.a(newId).a(" ");

            ansi.fg(entry.changeType() == ADDED ? GREEN : (entry.changeType() == MODIFIED ? YELLOW
                    : RED));
            char type = entry.changeType().toString().charAt(0);
            ansi.a("  ").a(type).reset();
            ansi.a("  ").a(formatPath(entry));

            console.println(ansi.toString());
        }

        private String shortOid(ObjectId oid) {
            return new StringBuilder(oid.toString().substring(0, 6)).append("...").toString();
        }
    }

    private static class FullPrinter implements DiffPrinter {
        @Override
        public void print(GeoGIT geogit, ConsoleReader console, DiffEntry entry) throws IOException {

            Repository repository = geogit.getRepository();
            StagingDatabase index = repository.getIndex().getDatabase();

            final String oldPath = entry.oldPath();
            final String newPath = entry.newPath();

            ObjectId id = null;
            ObjectReader<Object> reader = null;
            index.get(id, reader);
        }
    }

    private static String formatPath(DiffEntry entry) {
        String path;
        NodeRef oldObject = entry.getOldObject();
        NodeRef newObject = entry.getNewObject();
        if (oldObject == null) {
            path = newObject.getPath();
        } else if (newObject == null) {
            path = oldObject.getPath();
        } else {
            if (oldObject.getPath().equals(newObject.getPath())) {
                path = oldObject.getPath();
            } else {
                path = oldObject.getPath() + " -> " + newObject.getPath();
            }
        }
        return path;
    }
}
