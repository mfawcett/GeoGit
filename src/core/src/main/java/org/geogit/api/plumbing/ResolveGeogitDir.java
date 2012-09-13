/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Platform;

import com.google.common.base.Throwables;
import com.google.inject.Inject;

/**
 * Resolves the location of the {@code .geogit} repository directory relative to the
 * {@link Platform#pwd() current directory}.
 * <p>
 * The location can be a either the current directory, a parent of it, or {@code null} if no
 * {@code .geogit} directory is found.
 * 
 */
public class ResolveGeogitDir extends AbstractGeoGitOp<URL> {

    private Platform platform;

    @Inject
    public ResolveGeogitDir(Platform platform) {
        this.platform = platform;
    }

    /**
     * @return the location of the {@code .geogit} repository environment directory or {@code null}
     *         if not inside a working directory
     * @see org.geogit.api.AbstractGeoGitOp#call()
     */
    @Override
    public URL call() {
        File pwd = platform.pwd();
        URL lookup;
        try {
            lookup = lookupGeogitDirectory(pwd);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return lookup;
    }

    /**
     * @param pwd the current working directory
     * @return the location of the {@code .geogit} repository environment directory or {@code null}
     *         if not inside a working directory
     */
    private URL lookupGeogitDirectory(File file) throws IOException {
        if (file == null) {
            return null;
        }
        if (file.isDirectory()) {
            if (file.getName().equals(".geogit")) {
                return file.toURI().toURL();
            }
            File[] contents = file.listFiles();
            for (File dir : contents) {
                if (dir.isDirectory() && dir.getName().equals(".geogit")) {
                    return lookupGeogitDirectory(dir);
                }
            }
        }
        return lookupGeogitDirectory(file.getParentFile());
    }

}
