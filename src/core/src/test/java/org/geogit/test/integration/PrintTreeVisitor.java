/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration;

import java.io.PrintWriter;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectDatabase;

public class PrintTreeVisitor implements TreeVisitor {
    private final ObjectDatabase odb;

    private final PrintWriter writer;

    private int depth;

    private int printlimit;

    private int unprinted;

    private int subtreeEntries;

    public int visitedEntries;

    private boolean print = false;

    private Repository repo;

    public PrintTreeVisitor(final PrintWriter writer, final Repository repo) {
        this.writer = writer;
        this.repo = repo;
        this.odb = repo.getObjectDatabase();
    }

    /**
     * @see org.geogit.api.TreeVisitor#visitEntry(NodeRef)
     */
    @Override
    public boolean visitEntry(final NodeRef ref) {
        visitedEntries++;
        subtreeEntries++;
        printlimit++;
        if (printlimit <= 1) {
            indent();
            println(ref.getPath());
            writer.flush();
        } else {
            unprinted++;
        }
        return true;
    }

    @Override
    public boolean visitSubTree(final int bucket, final ObjectId treeId) {

        // if (unprinted > 0) {
        // indent();
        // writer.println("...and " + unprinted + " more.");
        // unprinted = 0;
        // }

        if (subtreeEntries > 0) {
            println(" (" + subtreeEntries + " entries)");
        } else {
            println('\n');
        }
        subtreeEntries = 0;
        depth++;
        indent();
        print("order/bucket: " + depth + "/" + bucket);
        printlimit = 0;
        RevTree tree = odb.get(treeId, repo.newRevTreeReader(odb, depth));
        tree.accept(this);
        depth--;

        return false;
    }

    private void println(char c) {
        if (print)
            writer.println(c);
    }

    private void println(String string) {
        if (print)
            writer.println(string);
    }

    private void print(String string) {
        if (print)
            writer.print(string);
    }

    private void indent() {
        if (print)
            for (int i = 0; i < depth; i++) {
                print('\t');
            }
    }

    private void print(char c) {
        if (print)
            writer.print(c);
    }

}
