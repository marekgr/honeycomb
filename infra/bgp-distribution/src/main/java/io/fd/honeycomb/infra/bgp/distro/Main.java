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

package io.fd.honeycomb.infra.bgp.distro;

import static io.fd.honeycomb.infra.distro.ActiveModuleProvider.STANDARD_MODULES_RELATIVE_PATH;
import static io.fd.honeycomb.infra.distro.ActiveModuleProvider.aggregateResources;
import static io.fd.honeycomb.infra.distro.ActiveModuleProvider.loadActiveModules;

import com.google.inject.ConfigurationException;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import io.fd.honeycomb.infra.bgp.BgpConfiguration;
import io.fd.honeycomb.infra.bgp.BgpServerProvider;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main() {
    }

    public static void main(String[] args) {
        final ClassLoader classLoader = Main.class.getClassLoader();
        init(loadActiveModules(aggregateResources(STANDARD_MODULES_RELATIVE_PATH, classLoader)));
    }

    /**
     * Initialize the Honeycomb with provided modules
     */
    public static Injector init(final Set<? extends Module> modules) {
        try {
            Injector injector = io.fd.honeycomb.infra.distro.Main.init(modules);
            final BgpConfiguration bgpAttributes = injector.getInstance(BgpConfiguration.class);

            if (bgpAttributes.isBgpEnabled()) {
                LOG.info("Starting BGP");
                injector.getInstance(BgpServerProvider.BgpServer.class);
                LOG.info("BGP started successfully!");
            }

            return injector;
        } catch (CreationException | ProvisionException | ConfigurationException e) {
            LOG.error("Failed to initialize Honeycomb components", e);
            throw e;
        } catch (RuntimeException e) {
            LOG.error("Unexpected initialization failure", e);
            throw e;
        } finally {
            // Trigger gc to force collect initial garbage + dedicated classloader
            System.gc();
        }
    }

}
