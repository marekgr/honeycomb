/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.northbound.bgp.extension;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.opendaylight.protocol.bgp.l3vpn.ipv4.BgpIpv4Activator;
import org.opendaylight.protocol.bgp.l3vpn.ipv4.RibIpv4Activator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static io.fd.honeycomb.northbound.bgp.extension.AbstractBgpExtensionModule.TableTypeRegistration.tableType;

public class L3VpnV4Module extends AbstractBgpExtensionModule {

    private static final Logger LOG = LoggerFactory.getLogger(L3VpnV4Module.class);

    @Override
    public Set<Class<? extends RIBExtensionProviderActivator>> getRibActivators() {
        return ImmutableSet.of(RibIpv4Activator.class);
    }

    @Override
    public Set<Class<? extends BGPExtensionProviderActivator>> getExtensionActivators() {
        return ImmutableSet.of(BgpIpv4Activator.class);
    }

    @Override
    public Set<TableTypeRegistration> getTableTypes() {
        return ImmutableSet.of(
                tableType(
                        Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class, L3VPNIPV4UNICAST.class));
    }

    @Override
    public Set<Class<? extends Provider<AfiSafi>>> getAfiSafiTypeProviders() {
        return Collections.emptySet();
    }

    @Override
    public Set<Class<? extends WriterFactory>> getApplicationRibWriters() {
        return ImmutableSet.of(L3VpnV4WriterFactory.class);
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}
