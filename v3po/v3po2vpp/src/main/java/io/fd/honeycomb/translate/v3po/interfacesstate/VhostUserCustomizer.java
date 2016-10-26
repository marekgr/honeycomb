/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.dto.SwInterfaceVhostUserDetails;
import io.fd.vpp.jvpp.core.dto.SwInterfaceVhostUserDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.SwInterfaceVhostUserDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.VhostUserBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VhostUserCustomizer extends FutureJVppCustomizer
        implements InitializingReaderCustomizer<VhostUser, VhostUserBuilder>, InterfaceDataTranslator, JvppReplyConsumer {

    public static final String DUMPED_VHOST_USERS_CONTEXT_KEY =
            VhostUserCustomizer.class.getName() + "dumpedVhostUsersDuringGetAllIds";
    private static final Logger LOG = LoggerFactory.getLogger(VhostUserCustomizer.class);
    private NamingContext interfaceContext;

    public VhostUserCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull VhostUser readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setVhostUser(readValue);
    }

    @Nonnull
    @Override
    public VhostUserBuilder getBuilder(@Nonnull InstanceIdentifier<VhostUser> id) {
        return new VhostUserBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VhostUser> id,
                                      @Nonnull final VhostUserBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        if (!isInterfaceOfType(getFutureJVpp(), ctx.getModificationCache(), id, index,
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VhostUser.class,
                LOG)) {
            return;
        }

        LOG.debug("Reading attributes for vhpost user interface: {}", key.getName());

        @SuppressWarnings("unchecked")
        Map<Integer, SwInterfaceVhostUserDetails> mappedVhostUsers =
                (Map<Integer, SwInterfaceVhostUserDetails>) ctx.getModificationCache()
                        .get(DUMPED_VHOST_USERS_CONTEXT_KEY);

        if (mappedVhostUsers == null) {
            // Full VhostUser dump has to be performed here, no filter or anything is here to help so at least we cache it
            final SwInterfaceVhostUserDump request = new SwInterfaceVhostUserDump();
            final CompletionStage<SwInterfaceVhostUserDetailsReplyDump>
                    swInterfaceVhostUserDetailsReplyDumpCompletionStage =
                    getFutureJVpp().swInterfaceVhostUserDump(request);
            final SwInterfaceVhostUserDetailsReplyDump reply =
                    getReplyForRead(swInterfaceVhostUserDetailsReplyDumpCompletionStage.toCompletableFuture(), id);

            if (null == reply || null == reply.swInterfaceVhostUserDetails) {
                mappedVhostUsers = Collections.emptyMap();
            } else {
                final List<SwInterfaceVhostUserDetails> swInterfaceVhostUserDetails =
                        reply.swInterfaceVhostUserDetails;
                // Cache interfaces dump in per-tx context to later be used in readCurrentAttributes
                mappedVhostUsers = swInterfaceVhostUserDetails.stream()
                        .collect(Collectors.toMap(t -> t.swIfIndex, swInterfaceDetails -> swInterfaceDetails));
            }

            ctx.getModificationCache().put(DUMPED_VHOST_USERS_CONTEXT_KEY, mappedVhostUsers);
        }

        // Relying here that parent InterfaceCustomizer was invoked first to fill in the context with initial ifc mapping
        final SwInterfaceVhostUserDetails swInterfaceVhostUserDetails = mappedVhostUsers.get(index);
        LOG.trace("Vhost user interface: {} attributes returned from VPP: {}", key.getName(),
                swInterfaceVhostUserDetails);

        builder.setRole(swInterfaceVhostUserDetails.isServer == 1
                ? VhostUserRole.Server
                : VhostUserRole.Client);
        builder.setFeatures(BigInteger.valueOf(swInterfaceVhostUserDetails.features));
        builder.setNumMemoryRegions((long) swInterfaceVhostUserDetails.numRegions);
        builder.setSocket(toString(swInterfaceVhostUserDetails.sockFilename));
        builder.setVirtioNetHdrSize((long) swInterfaceVhostUserDetails.virtioNetHdrSz);
        // TODO: map error code to meaningful message after VPP-436 is done
        builder.setConnectError(Integer.toString(swInterfaceVhostUserDetails.sockErrno));

        LOG.debug("Vhost user interface: {}, id: {} attributes read as: {}", key.getName(), index, builder);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VhostUser> init(
            @Nonnull final InstanceIdentifier<VhostUser> id,
            @Nonnull final VhostUser readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VhostUserBuilder()
                        .setRole(readValue.getRole())
                        .setSocket(readValue.getSocket())
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VhostUser> getCfgId(
            final InstanceIdentifier<VhostUser> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.VhostUser.class);
    }
}
