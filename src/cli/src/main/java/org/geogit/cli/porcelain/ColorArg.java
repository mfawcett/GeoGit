/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import com.beust.jcommander.IStringConverter;

public enum ColorArg {
    auto, never, always;

    public static class Converter implements IStringConverter<ColorArg> {

        @Override
        public ColorArg convert(String value) {
            // TODO Auto-generated method stub
            return ColorArg.valueOf(value);
        }

    }
}
