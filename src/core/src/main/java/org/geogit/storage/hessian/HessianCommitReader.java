/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.storage.hessian;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.repository.CommitBuilder;
import org.geogit.storage.ObjectReader;

import com.caucho.hessian.io.Hessian2Input;
import com.google.common.base.Throwables;

class HessianCommitReader extends HessianRevReader implements ObjectReader<RevCommit> {

    @Override
    public RevCommit read(ObjectId id, InputStream rawData) throws IllegalArgumentException {
        Hessian2Input hin = new Hessian2Input(rawData);
        CommitBuilder builder = new CommitBuilder();

        try {
            hin.startMessage();
            BlobType type = BlobType.fromValue(hin.readInt());
            if (type != BlobType.COMMIT)
                throw new IllegalArgumentException("Could not parse blob of type " + type
                        + " as a commit.");

            builder.setTreeId(readObjectId(hin));
            int parentCount = hin.readInt();
            List<ObjectId> pIds = new ArrayList<ObjectId>(parentCount);
            for (int i = 0; i < parentCount; i++) {
                pIds.add(readObjectId(hin));
            }
            builder.setParentIds(pIds);
            builder.setAuthor(hin.readString());
            builder.setAuthorEmail(hin.readString());
            builder.setCommitter(hin.readString());
            builder.setCommitterEmail(hin.readString());
            builder.setMessage(hin.readString());
            builder.setTimestamp(hin.readLong());

            hin.completeMessage();

            return builder.build(id);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
