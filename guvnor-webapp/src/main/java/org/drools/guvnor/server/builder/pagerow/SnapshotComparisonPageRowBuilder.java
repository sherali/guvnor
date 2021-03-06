/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.drools.guvnor.server.builder.pagerow;

import java.util.ArrayList;
import java.util.List;

import org.drools.guvnor.client.rpc.PageRequest;
import org.drools.guvnor.client.rpc.SnapshotComparisonPageRow;
import org.drools.guvnor.client.rpc.SnapshotDiffs;

public class SnapshotComparisonPageRowBuilder {
    public List<SnapshotComparisonPageRow> createRows(final PageRequest pageRequest,
                                                      final SnapshotDiffs diffs) {
        List<SnapshotComparisonPageRow> rowList = new ArrayList<SnapshotComparisonPageRow>();

        int pageStart = pageRequest.getStartRowIndex();
        int numRowsToReturn = (pageRequest.getPageSize() == null ? diffs.diffs.length : pageRequest.getPageSize());
        int maxRow = Math.min( numRowsToReturn,
                               diffs.diffs.length - pageRequest.getStartRowIndex() );
        for ( int i = pageStart; i < pageStart + maxRow; i++ ) {
            SnapshotComparisonPageRow pr = new SnapshotComparisonPageRow();
            pr.setDiff( diffs.diffs[i] );
            rowList.add( pr );
        }

        return rowList;
    }
}
