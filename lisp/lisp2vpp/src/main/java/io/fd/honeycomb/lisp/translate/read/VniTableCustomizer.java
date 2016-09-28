/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.lisp.translate.read;

import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.base.Optional;
import io.fd.honeycomb.lisp.translate.read.dump.executor.VniTableDumpExecutor;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.exceptions.execution.DumpExecutionFailedException;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetails;
import io.fd.vpp.jvpp.core.dto.LispEidTableMapDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the reads of {@link VniTable} nodes
 */
public class VniTableCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<VniTable, VniTableKey, VniTableBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(VniTableCustomizer.class);

    private static final String LISP_TABLE_ID_DUMP = VniTableCustomizer.class.getName();
    private final DumpCacheManager<LispEidTableMapDetailsReplyDump, Void> dumpManager;

    public VniTableCustomizer(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<LispEidTableMapDetailsReplyDump, Void>()
                .withExecutor(new VniTableDumpExecutor(futureJvpp))
                .build();
    }

    private static VniTableKey detailsToKey(final LispEidTableMapDetails lispEidTableMapDetails) {
        return new VniTableKey(Integer.valueOf(lispEidTableMapDetails.vni).longValue());

    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<VniTable> readData) {
        ((EidTableBuilder) builder).setVniTable(readData);
    }

    @Nonnull
    @Override
    public VniTableBuilder getBuilder(@Nonnull final InstanceIdentifier<VniTable> id) {
        return new VniTableBuilder();
    }

    @Nonnull
    @Override
    public List<VniTableKey> getAllIds(@Nonnull final InstanceIdentifier<VniTable> id,
                                       @Nonnull final ReadContext context)
            throws ReadFailedException {
        LOG.trace("Reading all IDS...");

        Optional<LispEidTableMapDetailsReplyDump> optionalReply;
        try {
            optionalReply = dumpManager.getDump(LISP_TABLE_ID_DUMP, context.getModificationCache(), NO_PARAMS);
        } catch (DumpExecutionFailedException e) {
            throw new ReadFailedException(id, e);
        }

        if (!optionalReply.isPresent() || optionalReply.get().lispEidTableMapDetails.isEmpty()) {
            return Collections.emptyList();
        }

        return optionalReply.get().lispEidTableMapDetails.stream().map(VniTableCustomizer::detailsToKey)
                .collect(Collectors.toList());
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VniTable> id,
                                      @Nonnull final VniTableBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {

        checkState(id.firstKeyOf(VniTable.class) != null, "No VNI present");
        VniTableKey key = new VniTableKey(id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier());

        Optional<LispEidTableMapDetailsReplyDump> optionalReply;
        try {
            optionalReply = dumpManager.getDump(LISP_TABLE_ID_DUMP, ctx.getModificationCache(), NO_PARAMS);
        } catch (DumpExecutionFailedException e) {
            throw new ReadFailedException(id, e);
        }

        if (!optionalReply.isPresent() || optionalReply.get().lispEidTableMapDetails.isEmpty()) {
            return;
        }

        //transforming right away to single detail(specific request should do the magic)
        final LispEidTableMapDetails details =
                optionalReply.get().lispEidTableMapDetails.stream().filter(a -> detailsToKey(a).equals(key))
                        .collect(RWUtils.singleItemCollector());

        builder.setVirtualNetworkIdentifier((long) details.vni);
        builder.setTableId((long) details.dpTable);
    }
}
