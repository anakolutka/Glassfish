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
package org.glassfish.virtualization.runtime;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.grizzly.config.dom.NetworkListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.virtualization.config.Emulator;
import org.glassfish.virtualization.config.MachineConfig;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.VirtUser;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.os.Disk;
import org.glassfish.virtualization.os.FileOperations;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.PhysicalGroup;
import org.glassfish.virtualization.spi.StoragePool;
import org.glassfish.virtualization.spi.StorageVol;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.util.Host;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;


/**
 * Local machine capabilities include creating a virtual machine
 *
 * @author Jerome Dochez
 */


public abstract class LocalMachine implements Machine, FileOperations {

    final protected MachineConfig config;
    final protected PhysicalGroup group;

    final protected Map<String, VMTemplate> installedTemplates = new HashMap<String, VMTemplate>();

    // Sa far, IP addresses are static within a single run. we could support changing the IP address eventually.
    volatile Machine.State state;
    VirtUser myself=null;

    @Inject
    Virtualizations virtualizations;

    @Inject
    Habitat habitat;

    @Inject
    Host host;

    final Injector injector;

    protected  LocalMachine(Injector injector, PhysicalGroup group, MachineConfig config) {
        this.group = group;
        this.config = config;
        this.injector = injector;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public MachineConfig getConfig() {
        return config;
    }

    @Override
    public String getIpAddress() {
        return "";
    }

    @Override
    public PhysicalGroup getGroup() {
        return group;
    }

    public void setState(Machine.State state) {
        this.state = state;
    }

    @Override
    public Machine.State getState() {
        return state;
    }

    @Override
    public FileOperations getFileOperations() {
        return this;
    }

    @Override
    public String toString() {
        return "Machine " + getName();
    }

    /**
     * Returns the VMtemplate for this template configuration
     * @param template the template configuration
     * @return the virtual machine template instance
     */
    public VMTemplate getVMTemplateFor(Template template) {
        return installedTemplates.get(template.getName());

    }

    @Override
    public void sleep() throws IOException, InterruptedException  {
        throw new IOException("Impossible to put myself to sleep");
    }

    @Override
    public boolean isUp() {
        // we are up by definition of  having this code running
        return true;
    }

    @Override
    public synchronized VirtUser getUser() {
        if (myself==null) {
            myself = LocalUser.myself(habitat);
        }
        return myself;
    }

    protected String getUserHome() {
        return System.getProperty("user.home");
    }

    protected Emulator getEmulator() {
        if (config.getEmulator()==null) {
            return group.getConfig().getVirtualization().getDefaultEmulator();
        }
        return config.getEmulator();
    }

    protected File absolutize(File source) {
        return RuntimeContext.absolutize(source);
    }

    @Override
    public boolean mkdir(String destPath) throws IOException {
        File target = absolutize(new File(destPath));
        return !target.exists() && target.mkdirs();
    }

    @Override
    public boolean delete(String path) throws IOException {
        File target = absolutize(new File(path));
        return target.exists() && target.delete();

    }

    @Override
    public boolean mv(String source, String dest) throws IOException {
        File sourceFile = absolutize(new File(source));
        if (sourceFile.exists()) {
            return sourceFile.renameTo(absolutize(new File(dest)));
        }
        return false;
    }

    @Override
    public long length(String path) throws IOException {
        File sourceFile = absolutize(new File(path));
        if (sourceFile.exists()) return sourceFile.length();
        else throw new FileNotFoundException("Cannot find file " + path);
    }

    @Override
    public void copy(File source, File destDir) throws IOException {
        FileUtils.copy(absolutize(source), new File(absolutize(destDir), source.getName()));
    }

    @Override
    public void localCopy(String source, String destDir) throws IOException {
        copy(new File(source), new File(destDir));
    }

    @Override
    public boolean exists(String path) throws IOException {
        return (absolutize(new File(path))).exists();
    }

    protected List<StorageVol> prepare(final Template template, final String name, final VirtualCluster cluster)

            throws VirtException, IOException {

        // 1. copy the template to the destination machine.
        mkdir(config.getDisksLocation());

        final File sourceFile = new File(virtualizations.getTemplatesLocation(), template.getName() + ".img");

        delete(config.getDisksLocation() + "/" + name + "-cust.img");
        delete(config.getDisksLocation() + "/" + name + ".img");

        final StoragePool pool = (getStoragePools().get("glassfishInstances")!=null?
                getStoragePools().get("glassfishInstances"):
                    addStoragePool("glassfishInstances", 136112211968L));

        List<StorageVol> volumes = new ArrayList<StorageVol>();
        File custFile = null;
        Future<StorageVol> diskFuture=null;
        final String diskLocation = config.getDisksLocation();
        final Machine target = this;
        try {
            diskFuture = habitat.getComponent(ExecutorService.class).submit(
                    new Callable<StorageVol>() {
                        @Override
                        public StorageVol call() throws Exception {
                            VMTemplate vmTemplate = getVMTemplateFor(template);
                            vmTemplate.copyTo(target, diskLocation);
                            StorageVol volume = pool.byName(name);
                            if (volume!=null) {
                                volume.delete();
                            }
                            if (pool.byName(name)!=null) {
                                pool.byName(name).delete();
                            }
                            volume = pool.allocate(name, vmTemplate.getSize());
                            delete(diskLocation + "/" + name + ".img");
                            mv(diskLocation+ "/" + sourceFile.getName(),
                               diskLocation+ "/" + name + ".img");
                            return volume;
                        }
                    }

            );


        } catch(Exception e) {
            e.printStackTrace();
            for (StorageVol vol : volumes) {
                try {
                    vol.delete();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (custFile.exists())
                assert custFile.delete();

            throw new VirtException(e);
        }

        // wait for the disk creation to be finished.
        try {
            volumes.add(0, diskFuture.get());
        } catch(Exception e) {
            e.printStackTrace();
            try {
                List<StorageVol> copy =  new ArrayList<StorageVol>();
                for (StorageVol volume : pool.volumes()) {
                    copy.add(volume);
                }
                for (StorageVol volume : copy) {
                    volume.delete();
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            throw new VirtException(e);
        }
        return volumes;
    }

    protected File prepareCustDirectory(String name, VirtualCluster cluster, Template template ) throws IOException {

        File machineDisks = absolutize(new File(virtualizations.getDisksLocation(), group.getName()));
        machineDisks = new File(machineDisks, getName());
        File custDir = new File(machineDisks, name + "cust");
        if (!custDir.exists()) {
            if (!custDir.mkdirs()) {
                throw new IOException("cannot create disk cache on local machine");
            }
        }
        createCustomizationFile(custDir, cluster, name, template);
        // copy the ssh public key
        File home = new File(System.getProperty("user.home"));
        File keyFile = new File(home, ".ssh/id_dsa.pub");
        FileUtils.copy(keyFile, new File(custDir, keyFile.getName()));
        keyFile = new File(home, ".ssh/id_rsa.pub");
        if (keyFile.exists()) {
            FileUtils.copy(keyFile, new File(custDir, keyFile.getName()));
        }
        return custDir;

    }
    
    /*
     * create or update the "customization" file with correct tokens in the customizationDir directory
     * return a File pointing to this entry.
     */
    
    protected File createCustomizationFile (File customizationDir, VirtualCluster cluster, String name , Template template) throws IOException{
         // create the customized properties.
        Properties customizedProperties = new Properties();
        Config configuration = habitat.getComponent(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
        NetworkListener nl = configuration.getNetworkConfig().getNetworkListener("admin-listener");
        // soon enough, I need to replace this with a token and keep this information from being stored in the
        // customization disk.
        customizedProperties.put("Group", group.getName());
        customizedProperties.put("Cluster", cluster.getConfig().getName());
        customizedProperties.put("Machine", getName());
        customizedProperties.put("MachineAlias", name);
        customizedProperties.put("GroupMasterPort", nl.getPort() );
        customizedProperties.put("GroupMasterMachine", host.getHostAddress(group.getConfig().getPortName()));
        customizedProperties.put("DAS", System.getProperty("com.sun.aas.hostName"));
        customizedProperties.put("DASAddress", host.getHostAddress(group.getConfig().getPortName()));
        VirtUser vmUser = getUser();
        if (template.getUser()!=null) {
            vmUser = template.getUser();
        }
        customizedProperties.put("UserName", vmUser.getName());
        customizedProperties.put("UserId", vmUser.getUserId());
        customizedProperties.put("GroupId", vmUser.getGroupId());

        AuthTokenManager tokenMgr = habitat.getComponent(AuthTokenManager.class);
        customizedProperties.put("AuthToken", tokenMgr.createToken(30L*60L*1000L));
        customizedProperties.put("StartToken", tokenMgr.createToken(30L*60L*1000L));
        File customization =new File(customizationDir, "customization");
        // write them out
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(customization);
            customizedProperties.store(fileWriter, "Customization properties for virtual machine" + name);
        } finally {
            if (fileWriter!=null)
                fileWriter.close();
            return customization;
        }       
    }

    protected Disk prepareCustomization(File custDir, File custFile,  String name) throws IOException {
        Disk custDisk = habitat.getComponent(Disk.class);
        // create the iso file.
        custDisk.createISOFromDirectory(custDir, custFile);
        return custDisk;
    }
}
