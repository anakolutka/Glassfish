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

import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.virtualization.config.*;
import org.glassfish.virtualization.spi.PhysicalGroup;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Injector;
import org.jvnet.hk2.component.PostConstruct;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of a remote physical machine managed by the libvirt interfaces.
 *
 * @author Jerome Dochez
 */
class LibVirtMachine extends LibVirtLocalMachine {

    // Sa far, IP addresses are static within a single run. we could support changing the IP address eventually.
    final String ipAddress;

    @Inject
    SSHLauncher sshLauncher;

    public static LibVirtMachine from(Injector injector, LibVirtGroup group, MachineConfig config, String ipAddress) {
        return injector.inject(new LibVirtMachine(injector, group, config, ipAddress));
    }

    protected  LibVirtMachine(Injector injector, LibVirtGroup group, MachineConfig config, String ipAddress) {
        super(injector, group, config);
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public boolean mkdir(String destPath) throws IOException {
       getSSH();

        final SFTPClient sftpClient = sshLauncher.getSFTPClient();
        if (!sftpClient.exists(config.getDisksLocation())) {
            sftpClient.mkdirs(config.getDisksLocation(), 0755);
            return true;
        }
        return false;
    }

    public boolean delete(String path) throws IOException {
        final SFTPClient sftpClient = sshLauncher.getSFTPClient();

        if (sftpClient.exists(path)) {
            sftpClient.rm(path);
            return true;
        }
        return false;
    }

    @Override
    public boolean mv(String source, String dest) throws IOException {
        final SFTPClient sftpClient = sshLauncher.getSFTPClient();
        if (exists(dest)) {
            delete(dest);
        }
        sftpClient.mv(source, dest);
        return true;
    }

    public long length(String path) throws IOException {
        final SFTPClient sftpClient = sshLauncher.getSFTPClient();
        try {
            return sftpClient.lstat(path).size;
        } finally {
            sftpClient.close();
        }
    }

    @Override
    public boolean exists(String path) throws IOException {
        final SFTPClient sftpClient = sshLauncher.getSFTPClient();
        try {
            return sftpClient.exists(path);
        } finally {
            sftpClient.close();
        }
    }

    @Override
    public void copy(File source, File destination) throws IOException {

        getSSH();

        final SFTPClient sftpClient = sshLauncher.getSFTPClient();
        try {
            mkdirs(sftpClient, destination);

            String destPath = destination + "/" + source.getName();
            if (sftpClient.exists(destPath)) {
                sftpClient.rm(destPath);
            }
        } finally {
            sftpClient.close();
        }
        sshLauncher.getSCPClient().put(source.getAbsolutePath(), destination.getPath());
    }

    public void mkdirs(SFTPClient client, File path) throws IOException {
        if (path.getParentFile()!=null) {
            mkdirs(client, path.getParentFile());
        }
        if (!client.exists(path.getPath())) {
            client.mkdirs(path.getPath(), 0755);
        }
    }

    @Override
    public void localCopy(String source, String destDir) throws IOException {
        getSSH();

        try {

            SFTPClient client = sshLauncher.getSFTPClient();
            try {
                if (!client.exists(destDir)) {
                    // not installed
                    client.mkdirs(destDir, 0755);
                }
            } finally {
                client.close();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            RuntimeContext.logger.info("Remote cp file " + source + " on " + getName());
            sshLauncher.runCommand("cp " + source + " " + destDir, baos);
        } catch (IOException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot copy file on " + getName(),e);
            throw e;
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public PhysicalGroup getGroup() {
        return group;
    }

    public void ping() throws IOException, InterruptedException  {
        SSHLauncher ssl = getSSH();
        ssl.pingConnection();
    }

    public void sleep() throws IOException, InterruptedException  {
        SSHLauncher ssl = getSSH();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ssl.runCommand("sudo pm-suspend",baos );
        System.out.println(baos.toString());
    }

    public boolean isUp() {

        if (State.READY.equals(getState())) return true;
        if (ipAddress==null) return false;

        try {
            ping();
        } catch(Exception e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception while pinging " + config.getName() + " : "
                   + e.getMessage());
            RuntimeContext.logger.log(Level.FINE, "Exception while pinging " + config.getName(), e);
            return false;
        }
        // the machine is alive, let's connect to it's virtualization implementation
        try {
            connection();
        } catch(VirtException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot connect to machine " + config.getName() +
                " with the user " + group.getConfig().getUser().getName(), e);
            return false;
        }
        return true;
    }

    public VirtUser getUser() {
        if (config.getUser()!=null) {
            return config.getUser();
        } else {
            return group.getConfig().getUser();
        }
    }

    protected String getUserHome() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            getSSH().runCommand("echo $HOME", baos);
        } catch(Exception e) {
            return "/home" + getUser().getName();
        }
        String userHome = baos.toString();
        // horrible hack to remove trailing \n
        return userHome.substring(0, userHome.length()-1);
    }


    private SSHLauncher getSSH() {
        File home = new File(System.getProperty("user.home"));
        String keyFile = new File(home,".ssh/id_dsa").getAbsolutePath();
        sshLauncher.init(getUser().getName(), ipAddress, 22, null, keyFile, null, Logger.getAnonymousLogger());
        return sshLauncher;
    }
}
