/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.virtualization.libvirt;

import org.glassfish.virtualization.libvirt.jna.Domain;
import org.glassfish.virtualization.libvirt.jna.DomainInfo;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Representation of a virtual machine in the libvirt world.
 *
 * @author Jerome Dochez
 */
public class LibVirtVirtualMachine implements VirtualMachine {

    final private Machine owner;
    final Domain domain;
    final String name;
    private String address;
    private CountDownLatch latch;

    protected LibVirtVirtualMachine(Machine owner, Domain domain, CountDownLatch latch) throws VirtException {
        this.domain = domain;
        this.owner = owner;
        this.latch = latch;
        this.name = domain.getName();
    }

    public void setAddress(String address) {
        if (latch!=null) {
            latch.countDown();
            latch=null;
        }
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void start() throws VirtException {
        domain.create();
    }

    public void stop() throws VirtException {

        if (DomainInfo.DomainState.VIR_DOMAIN_RUNNING.equals(domain.getInfo().getState()))
            domain.destroy();

    }

    public void resume() throws VirtException {

        domain.resume();
    }

    public void suspend() throws VirtException {
        domain.suspend();
    }

    public String getName() {
       return name;
    }

    public void delete() throws VirtException {

        try {
            stop();
        } catch(VirtException e) {
            e.printStackTrace();
            // ignore any shutdown failure
        }
        for (StorageVol volume : volumes()) {
            volume.delete();
        }
        domain.undefine();
    }

    public Iterable<StorageVol> volumes() throws VirtException {

        List<StorageVol> volumes = new ArrayList<StorageVol>();
        for (StoragePool pool : owner.getStoragePools().values()) {
            for (StorageVol volume : pool.volumes()) {
                if (volume.getName().startsWith(getName())) {
                    volumes.add(volume);
                }
            }
        }
        return volumes;
    }

    @Override
    public VirtualMachineInfo getInfo() {
        return new VirtualMachineInfo() {

            @Override
            public int nbVirtCpu() throws VirtException {
                return domain.getInfo().nrVirtCpu;
            }

            @Override
            public long memory() throws VirtException {
                return domain.getInfo().memory.longValue();
            }

            @Override
            public long cpuTime() throws VirtException {
                return domain.getInfo().cpuTime;
            }

            @Override
            public long maxMemory() throws VirtException {
                return domain.getInfo().maxMem.longValue();
            }

            @Override
            public Machine.State getState() throws VirtException {
                try {
                    DomainInfo.DomainState state = DomainInfo.DomainState.values()[domain.getInfo().state];
                    if (DomainInfo.DomainState.VIR_DOMAIN_RUNNING.equals(state)
                            || DomainInfo.DomainState.VIR_DOMAIN_BLOCKED.equals(state)) {
                        return Machine.State.READY;
                    } else {
                        if (DomainInfo.DomainState.VIR_DOMAIN_SHUTDOWN.equals(state)) {
                            return Machine.State.SUSPENDING;
                        } else {
                            return Machine.State.SUSPENDED;
                        }
                    }
                } catch(VirtException e) {
                    throw new VirtException(e);
                }

            }

            private final Map<MemoryListener, ScheduledFuture> listeners =
                    new HashMap<MemoryListener, ScheduledFuture>();

            @Override
            public void registerMemoryListener(final MemoryListener ml, long period, TimeUnit unit) {
                final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                final VirtualMachine owner = LibVirtVirtualMachine.this;

                listeners.put(ml,
                        executor.schedule(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ml.notified(owner, memory(), cpuTime());
                                } catch (VirtException e) {
                                    RuntimeContext.logger.log(Level.FINE, "Exception while notifying of vm load ", e);
                                }
                            }
                        }, period, unit)
                );
            }

            @Override
            public void unregisterMemoryListener(final MemoryListener ml) {

                listeners.get(ml).cancel(false);
            }
        };
    }
}
