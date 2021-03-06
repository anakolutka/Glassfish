/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.java.net/public/CDDL+GPL_1_1.html
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

package org.glassfish.nexus.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.nexus.client.beans.ContentItem;
import org.glassfish.nexus.client.beans.ContentItems;
import org.glassfish.nexus.client.beans.Failures;
import org.glassfish.nexus.client.beans.MavenArtifactInfo;
import org.glassfish.nexus.client.beans.MavenInfo;
import org.glassfish.nexus.client.beans.Repo;
import org.glassfish.nexus.client.beans.RepoDetail;
import org.glassfish.nexus.client.beans.RepoDetails;
import org.glassfish.nexus.client.beans.Repos;
import org.glassfish.nexus.client.beans.StagingOperationRequest;
import org.glassfish.nexus.client.beans.StagingOperationRequestData;
import org.glassfish.nexus.client.beans.StagingProfile;
import org.glassfish.nexus.client.beans.StagingProfileRepo;
import org.glassfish.nexus.client.beans.StagingProfileRepos;
import org.glassfish.nexus.client.beans.StagingProfiles;
import org.glassfish.nexus.client.logging.CustomHandler;
import org.glassfish.nexus.client.logging.CustomPrinter;
import org.glassfish.nexus.client.logging.DefaultNexusClientPrinter;
import org.glassfish.nexus.client.logging.DefaultRestClientPrinter;

/**
 *
 * @author Romain Grecourt
 */
public final class NexusClientImpl implements NexusClient {

    private final RestClient restClient;
    private final String nexusUrl;

    private static final String REPOSITORY_GROUP_PATH = "service/local/repo_groups";
    private static final String REPOSITORIES_PATH = "service/local/repositories";
    private static final String STAGING_REPO_BULK_PATH = "service/local/staging/bulk";
    private static final String PROFILES_PATH = "/service/local/staging/profiles";
    private static final String PROFILES_REPOS_PATH = "/service/local/staging/profile_repositories";
    private static final String SEARCH_PATH = "service/local/lucene/search";

    private static final Logger LOGGER;
    private static final CustomHandler LOGGER_HANDLER;
    private final SyncedClose syncedClose = new SyncedClose();
    private final SyncedDrop syncedDrop = new SyncedDrop();
    private final SyncedPromote syncedPromote = new SyncedPromote();

    private static NexusClientImpl instance;
    private static List<StagingProfileRepo> stagingProfileRepositories = null;
    private static HashMap<String,StagingProfileRepo> stagingProfileRepositoriesMap;

    static {
        LOGGER = Logger.getLogger(NexusClientImpl.class.getSimpleName());
        LOGGER_HANDLER = new CustomHandler();
        LOGGER.addHandler(LOGGER_HANDLER);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);
    }

    public static NexusClient init(
            RestClient restClient,
            String nexusUrl,
            CustomPrinter p){

        LOGGER_HANDLER.setPrinter(p);
        instance = new NexusClientImpl(restClient, nexusUrl);
        LOGGER.log(Level.FINE, instance.toString());
        return instance;
    }

    private interface SyncedOperation {
      void doOperation(String desc, String[] repoIds, String[] params);
      Object doReturn(StagingProfileRepo repo);
      boolean isOperationComplete(StagingProfileRepo repo);
    }

    private class SyncedClose implements SyncedOperation {
      public void doOperation(String desc, String[] repoIds, String[] params) {
        _closeStagingRepo(desc, repoIds);
      }
      public Object doReturn(StagingProfileRepo repo) {
        return null;
      }
      public boolean isOperationComplete(StagingProfileRepo repo) {
        return repo!=null && !repo.isOpen();
      }
    }

    private class SyncedPromote implements SyncedOperation {
      public void doOperation(String desc, String[] repoIds, String[] params) {
        if(params == null || params.length != 1){
          throw new IllegalArgumentException(
                  "params must contain promotion profile");
        }
        _promoteStagingRepo(params[0], desc, repoIds);
      }
      public Object doReturn(StagingProfileRepo repo) {
        StagingProfileRepo srepo = 
                getStagingProfileRepo(repo.getParentGroupId());
        if(srepo != null){
          return new Repo(srepo);
        }
        return null;
      }
      public boolean isOperationComplete(StagingProfileRepo repo) {
        return repo != null && repo.getParentGroupId() != null;
      }
    }

    private class SyncedDrop implements SyncedOperation {
      public void doOperation(String desc, String[] repoIds, String[] params) {
        _dropStagingRepo(desc, repoIds);
      }
      public Object doReturn(StagingProfileRepo repo) {
        return null;
      }
      public boolean isOperationComplete(StagingProfileRepo repo) {
        return repo == null;
      }
    }

    public String getNexusURL() {
        return nexusUrl;
    }

    private static enum Operation {
        close, promote, drop
    }

    public static NexusClient getInstance(){
        return instance;
    }

    private NexusClientImpl(RestClient restClient, String nexusUrl) {
        this.restClient = restClient;
        this.nexusUrl = nexusUrl;
    }

    WebTarget target(String path){
        return restClient.getClient().target(nexusUrl).path(path);
    }

    Builder request(String path){
        return target(path).request(MediaType.APPLICATION_JSON);
    }

    private Response get(String path) throws NexusClientException {
        
        try {
            return request(path).get();
        } catch (ClientException ex) {
            throw new NexusClientException(ex);
        }
    }

    private void refreshStagingRepos() throws NexusClientException {

        StagingProfileRepos retVal = 
                (StagingProfileRepos) handleResponse(
                    get(PROFILES_REPOS_PATH),
                    StagingProfileRepos.class);
        stagingProfileRepositories = Arrays.asList(retVal.getData());
        stagingProfileRepositoriesMap = new HashMap<String, StagingProfileRepo>();
        for (StagingProfileRepo profileRepo : stagingProfileRepositories) {
            stagingProfileRepositoriesMap.put(profileRepo.getRepositoryId(), profileRepo);
        }
    }

    private static String checksum(File datafile) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            FileInputStream fis = new FileInputStream(datafile);
            byte[] dataBytes = new byte[1024];

            int nread;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }

            byte[] mdbytes = md.digest();

            //convert the byte to hex format
            StringBuilder sb = new StringBuilder("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new NexusClientException(ex);
        } catch (IOException ex) {
            throw new NexusClientException(ex);
        }
    }

    private Response stagingOperation(
            Operation op,
            String[] repoIds,
            String profileGroup,
            String desc)
            throws NexusClientException {

        if (repoIds == null || repoIds.length == 0) {
            throw new NexusClientException(
                    "repoId list is null or empty, can't perform a staging operation");
        }

        Response response = null;
        try {
            response = request(STAGING_REPO_BULK_PATH + '/' + op).post(
                        Entity.entity(new StagingOperationRequestData(
                            new StagingOperationRequest(repoIds, profileGroup, op + " - " +String.valueOf(desc)))
                            ,MediaType.APPLICATION_JSON));
        } catch (ClientException ex) {
            throw new NexusClientException(ex);
        }
        refreshStagingRepos();
        return response;
    }

    private Object retry(
            SyncedOperation r,
            String desc,
            String[] repoIds,
            String extraParams[],
            int retryCount,
            long timeout)
            throws NexusClientException {

      int currentRetryCount = 0;
      long startTime = System.currentTimeMillis(), currentTimeout = 0;
      r.doOperation(desc, repoIds, extraParams);

      while(true){
        if(currentRetryCount > retryCount){
          throw new NexusClientException("retryCount reached: "+currentRetryCount);
        }
        if(currentTimeout > timeout){
          throw new NexusClientException("timeout reached: "+timeout);
        }

        refreshStagingRepos();
        StagingProfileRepo repo = null;
        boolean operationComplete = true;
        for(String repoId : repoIds){
          repo = getStagingProfileRepo(repoId);
          if(!r.isOperationComplete(repo)){
            operationComplete = false;
            break;
          }
        }
        if(operationComplete){
          return r.doReturn(repo);
        } else {
          long currentTime = System.currentTimeMillis();
          currentTimeout = currentTime - startTime;
          currentRetryCount ++;
          LOGGER.log(Level.INFO, 
                  "State of operation ({0}) isn''t verified yet... retrying({1})", 
                  new Object[]{
                    r.getClass().getSimpleName(),
                    currentRetryCount
                  });
        }
      }
    }

    private static Object handleResponse(
            Response r,
            Class c) throws NexusClientException {

        // 400 means failure entity
        if (r.getStatus() == 400) {
            try {
                throw new NexusFailureException(r.readEntity(Failures.class));
            } catch (MessageProcessingException ex) {
                throw new NexusClientException(ex);
            }
        }

        // something is wrong
        if (r.getStatus() >= 299) {
            throw new NexusResponseException(r.getStatus());
        }

        // TODO throw exception if content type isn't application/json

        if (c != null) {
            try {
                return r.readEntity(c);
            } catch (MessageProcessingException ex) {
                throw new NexusClientException(ex);
            }
        }
        return null;
    }

    public Repo getStagingRepo(String repoName) throws NexusClientException {
        if (stagingProfileRepositories == null) {
            refreshStagingRepos();
        }
        return ((Repos) handleResponse(get(
                REPOSITORIES_PATH + "/" + repoName), Repos.class)).getData();
    }

    public Set<Repo> getGroupTree(String repoId){
        Set<Repo> repos = new HashSet<Repo>();
        for(ContentItem item : ((ContentItems) handleResponse(
                get(PROFILES_REPOS_PATH+"/tree/"+repoId),ContentItems.class)).getData()){
            String[] tokens = item.getResourceURI().split("/");
            repos.add(new Repo(stagingProfileRepositoriesMap.get(tokens[tokens.length-1])));
        }
        return repos;
    }

    public StagingProfileRepo getStagingProfileRepo(String repoId){
        return stagingProfileRepositoriesMap.get(repoId);
    }

    public void closeStagingRepo(
            final String desc,
            final String[] repoIds,
            final int retryCount,
            final long timeout) 
            throws NexusClientException {

      retry(syncedClose, desc, repoIds, repoIds, retryCount, timeout);
    }

    public void closeStagingRepo(
            final String desc,
            final String[] repoIds) 
            throws NexusClientException {

      retry(
              syncedClose,
              desc, 
              repoIds,
              null,
              StagingOperation.DEFAULT_RETRY_COUNT,
              StagingOperation.DEFAULT_TIMEOUT);
    }

    public void dropStagingRepo(
            final String desc,
            final String[] repoIds,
            final int retryCount,
            final long timeout) 
            throws NexusClientException {

      retry(syncedDrop,desc, repoIds, null, retryCount, timeout);
    }

    public void dropStagingRepo(
            final String desc,
            final String[] repoIds) 
            throws NexusClientException {

      retry(
              syncedDrop,
              desc, 
              repoIds,
              null,
              StagingOperation.DEFAULT_RETRY_COUNT,
              StagingOperation.DEFAULT_TIMEOUT);
    }

    public Repo promoteStagingRepo(
            final String promotionProfile,
            final String desc,
            final String[] repoIds,
            final int retryCount,
            final long timeout) 
            throws NexusClientException {

     return (Repo) retry(
              syncedPromote,
              desc,
              repoIds,
              new String[]{promotionProfile},
              retryCount,
              timeout);
    }
 
    public Repo promoteStagingRepo(
            final String promotionProfile,
            final String desc,
            final String[] repoIds) 
            throws NexusClientException {

     return (Repo) retry(
              syncedPromote,
              desc,
              repoIds,
              new String[]{promotionProfile},
              StagingOperation.DEFAULT_RETRY_COUNT,
              StagingOperation.DEFAULT_TIMEOUT);
    }

    private void _closeStagingRepo(
            String desc,
            String[] repoIds) 
            throws NexusClientException {

        LOGGER.info(" ");
        LOGGER.log(Level.INFO,
                "-- closing {0} --",
                Arrays.toString(repoIds));
        handleResponse(stagingOperation(Operation.close, repoIds, null,desc), null);
    }

    private void _dropStagingRepo(String desc, String[] repoIds) throws NexusClientException {
        LOGGER.info(" ");
        LOGGER.log(Level.INFO,
                "-- droping {0} --",
                Arrays.toString(repoIds));
        handleResponse(stagingOperation(Operation.drop, repoIds, null, desc), null);
    }

    private Repo _promoteStagingRepo(
            String promotionProfile,
            String desc,
            String[] repoIds)
            throws NexusClientException {

        LOGGER.info(" ");
        LOGGER.log(Level.INFO,
                "-- searching for the promotion profile id of \"{0}\" --",
                promotionProfile);

        for (StagingProfile profile : ((StagingProfiles) handleResponse(
                get(PROFILES_PATH),
                StagingProfiles.class))
                .getData()) {

            if (profile.getMode().equals("GROUP")) {
                if (profile.getName().equals(promotionProfile)) {

                    LOGGER.info(" ");
                    LOGGER.log(Level.INFO,
                            "-- promoting {0} with promotion profile \"{1}\" --",
                            new Object[]{Arrays.toString(repoIds), promotionProfile});

                    StagingProfileRepo repo = getStagingProfileRepo(repoIds[0]);

                    if(repo == null){
                        throw new NexusClientException(
                            "unable to find the staging repository for id "+Arrays.toString(repoIds));
                    }
                    if(repo.isOpen()){
                        throw new NexusClientException(
                           "can't promote, repository is still in 'open' state "+Arrays.toString(repoIds));
                    }

                    if(repo.getParentGroupId() == null){
                        // promote if not already promoted
                        handleResponse(
                                stagingOperation(
                                Operation.promote,
                                repoIds,
                                profile.getId(),
                                desc), null);
                    }

                    // try to return the repo or null if not found.
                    repo = getStagingProfileRepo(repoIds[0]);
                    if(repo != null && repo.getParentGroupId() != null){
                        return new Repo(
                                stagingProfileRepositoriesMap.get(
                                        repo.getParentGroupId()));
                    } else {
                      return null;
                    }
                }
            }
        }

        throw new NexusClientException(
                "unable to find the following promotion profile\""
                + promotionProfile
                + "\"");
    }

    private static StringBuilder getRepoContentURL(String repoId){
        StringBuilder sb = new StringBuilder(REPOSITORIES_PATH);
        sb.append('/');
        sb.append(repoId);
        sb.append('/');
        sb.append("content");
        sb.append('/');
        return sb;
    }

    private ContentItems getRepoContent(StringBuilder contentPath, String path){
        ContentItems content =
                (ContentItems) handleResponse(
                    request(contentPath.append(path).toString()).get()
                    , ContentItems.class);
        return content;
    }

    private void scrubRepo(
            String repoId,
            String path,
            Set<MavenArtifactInfo> artifacts)
            throws NexusClientException {

        StringBuilder repoContentURL = getRepoContentURL(repoId);
        String root = repoContentURL.toString();
        ContentItems content = getRepoContent(repoContentURL, path);

        for(ContentItem item : content.getData()){
            // if file
            if(item.getLeaf()){
                if(item.isValidArtifactFile()){
                    MavenArtifactInfo artifact =
                            ((MavenInfo) handleResponse(
                                target(root+"/"+item.getRelativePath())
                                    .queryParam("describe", "maven2")
                                    .request(MediaType.APPLICATION_JSON).get(),
                                MavenInfo.class)).getData()[0];

                    LOGGER.log(Level.INFO, "found {0}", artifact);
                    artifacts.add(artifact);
                }
            } else {
                // if directory
                if(item.getSizeOnDisk() == -1){
                    scrubRepo(repoId, item.getRelativePath(), artifacts);
                }
            }
        }
    }

    public boolean existsInRepoGroup(String repoGroup, MavenArtifactInfo artifact)
            throws NexusClientException {

        ContentItem[] items = ((ContentItems) handleResponse(
                get(REPOSITORY_GROUP_PATH+'/'+repoGroup+"/"+artifact.getRepositoryRelativePath()),
                ContentItems.class)).getData();
        return (items!= null && items.length > 0);
    }

    public Set<MavenArtifactInfo> getArtifactsInRepo(
            String repoId) 
            throws NexusClientException {

        LOGGER.info(" ");
        LOGGER.log(Level.INFO
                , "-- retrieving full content of repository [{0}] --"
                , repoId);
        
        Set<MavenArtifactInfo> artifacts = new HashSet<MavenArtifactInfo>();
        scrubRepo(repoId, "", artifacts);
        return artifacts;
    }

    public Set<MavenArtifactInfo> getArtifactsInRepo(String repoId, String path) 
            throws NexusClientException {
        
        LOGGER.info(" ");
        LOGGER.log(Level.INFO
                , "-- retrieving content of [{0}] in repository [{1}] --"
                , new Object[]{repoId});
        
        Set<MavenArtifactInfo> artifacts = new HashSet<MavenArtifactInfo>();
        scrubRepo(repoId, "", artifacts);
        return artifacts;
    }

    public void deleteContent(
            String repoId,
            String path) 
            throws NexusClientException {

        LOGGER.info(" ");
        LOGGER.log(Level.INFO
                ,"-- deleting content of [{0}] in repository [{1}]] --"
                , new Object[]{path, nexusUrl});

        StringBuilder repoContentURL = getRepoContentURL(repoId);

        ContentItems repoContent = getRepoContent(repoContentURL, path);
        if (repoContent != null && repoContent.getData() != null) {
            for (ContentItem item : repoContent.getData()) {
                if (item.getLeaf()) {

                    String itemPath = item.getResourceURI().replace(nexusUrl, "");
                    handleResponse(target(itemPath).request().delete(), null);

                    LOGGER.log(Level.INFO, "deleted {0}", item.getRelativePath());
                }
            }
        }
    }

    public Repo getStagingRepo(
            String stagingProfile,
            MavenArtifactInfo refArtifact)
            throws NexusClientException {

        String refChecksum = checksum(refArtifact.getFile());

        LOGGER.info(" ");
        LOGGER.info("-- searching for the staging repository --");

        if (stagingProfileRepositories == null)
            refreshStagingRepos();

        for (StagingProfileRepo repo : stagingProfileRepositories) {
            if (repo.getProfileName().equals(stagingProfile)
                    && repo.getUserId().equals(restClient.getUsername())) {

                // get checksum from repo
                StringBuilder sb = new StringBuilder(REPOSITORIES_PATH);
                sb.append('/');
                sb.append(repo.getRepositoryId());
                sb.append('/');
                sb.append("content");
                sb.append('/');
                sb.append(refArtifact.getRepositoryRelativePath());
                sb.append(".sha1");

                String checksum = null;
                try{
                    checksum = (String) handleResponse(request(sb.toString()).get(),String.class);
                } catch (NexusResponseException ex){
                    LOGGER.log(Level.INFO,
                        "[{0}] does not contain the ref artifact",
                        new Object[]{repo.getRepositoryId()});
                }

                // if the checksum is not null and not empty, the coordinates exist
                // we always return the staging repo, even if the checksum does not match
                // since there can be only one representation of a coordinate
                if (checksum != null) {
                    if (refChecksum.equals(checksum)) {
                        LOGGER.log(Level.INFO,
                                "found staging repository: [{0}]",
                                new Object[]{repo.getRepositoryId()});
                    } else {
                        LOGGER.log(Level.WARNING,
                                "[{0}] contains a different version of {1}",
                                new Object[]{repo.getRepositoryId(), refArtifact});
                    }
                    return getStagingRepo(repo.getRepositoryId());
                }
            }
        }

        throw new NexusClientException(
                "unable to find an open staging repository for staging profile \""
                + stagingProfile
                + "\" and ref artifact \""
                + refArtifact
                + "\"");
    }

    public Repo getStagingRepo(File f) throws NexusClientException {

        RepoDetail[] results = ((RepoDetails) handleResponse(
                target(SEARCH_PATH)
                .queryParam("sha1", checksum(f))
                .request(MediaType.APPLICATION_JSON).get(),
                RepoDetails.class)).getRepoDetails();

        if (results != null) {
            for (RepoDetail detail : results) {
                if (!detail.isGroup()) {
                    return getStagingRepo(detail.getRepositoryId());
                }
            }
        }
        return null;
    }
    
    @Override
    public String toString(){
      StringBuilder sb = new StringBuilder("NexusClient{");
      sb.append("URL=");
      sb.append(nexusUrl);
      sb.append(", ");
      sb.append(restClient.toString());
      sb.append("}");
      return sb.toString();
    }

    public static void main(String[] args) {

        NexusClient nexusClient = NexusClientImpl.init(
                new RestClient(
                    null, 0,
                    "user", "password",
                    true,
                    new DefaultRestClientPrinter())
                ,"https://maven.java.net"
                , new DefaultNexusClientPrinter());

        Repo repo = nexusClient.getStagingRepo(
                "org-glassfish",
                new MavenArtifactInfo("groupId", "artifactId", "version", "classifier", "extension", null));
        repo.close("Add some message here");
    }
}