/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rmp553
 */
public class TwoRavensHelper {
    
    private final SettingsServiceBean settingsService;
    private final PermissionServiceBean permissionService;
    private final AuthenticationServiceBean authService;
    
    private final DataverseSession session;
            
    private final Map<Long, Boolean> fileMetadataTwoRavensExploreMap = new HashMap<>(); // { FileMetadata.id : Boolean } 

    public TwoRavensHelper(SettingsServiceBean settingsService, PermissionServiceBean permissionService, DataverseSession session,
                        AuthenticationServiceBean authService){
        if (settingsService == null){
            throw new NullPointerException("settingsService cannot be null");
        }
        if (permissionService == null){
            throw new NullPointerException("permissionService cannot be null");
        }
        this.permissionService = permissionService;
        this.settingsService = settingsService;
        this.session = session;
        this.authService = authService;
        
    }
   
    
     /**
     * Call this from a Dataset or File page
     *   - calls private method canSeeTwoRavensExploreButton
     * 
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     *   
     * @param fm
     * @return 
     */
    public boolean canSeeTwoRavensExploreButtonFromAPI(FileMetadata fm, User user){
        
        if (fm == null){
            return false;
        }
        
        if (user == null){
            return false;
        }
                       
        if (!this.permissionService.userOn(user, fm.getDataFile()).has(Permission.DownloadFile)){
            return false;
        }
        
        return this.canSeeTwoRavensExploreButton(fm, true);
    }
    
    /**
     * Call this from a Dataset or File page
     *   - calls private method canSeeTwoRavensExploreButton
     * 
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     *   
     * @param fm
     * @return 
     */
    public boolean canSeeTwoRavensExploreButtonFromPage(FileMetadata fm){
        
        if (fm == null){
            return false;
        }
        
        return this.canSeeTwoRavensExploreButton(fm, true);
    }
    
    /**
     * Used to check whether a tabular file 
     * may be viewed via TwoRavens
     * 
     * @param fm
     * @return 
     */
    public boolean canSeeTwoRavensExploreButton(FileMetadata fm, boolean permissionsChecked){
       
        if (fm == null){
            return false;
        }
        
        // This is only here as a reminder to the public method users 
        if (!permissionsChecked){
            return false;
        }
        
        // Has this already been checked?
        if (this.fileMetadataTwoRavensExploreMap.containsKey(fm.getId())){
            // Yes, return previous answer
            //logger.info("using cached result for candownloadfile on filemetadata "+fid);
            return this.fileMetadataTwoRavensExploreMap.get(fm.getId());
        }
        
        
        // (1) Is TwoRavens active via the "setting" table?
        //      Nope: get out
        //      
        if (!settingsService.isTrueForKey(SettingsServiceBean.Key.TwoRavensTabularView, false)){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
            return false;
        }
        

        // (2) Is the DataFile object there and persisted?
        //      Nope: scat
        //
        if ((fm.getDataFile() == null)||(fm.getDataFile().getId()==null)){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
            return false;
        }
        
        // (3) Is there tabular data or is the ingest in progress?
        //      Yes: great
        //
        if ((fm.getDataFile().isTabularData())||(fm.getDataFile().isIngestInProgress())){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), true);
            return true;
        }
        
        // Nope
        this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);            
        return false;
        
        //       (empty fileMetadata.dataFile.id) and (fileMetadata.dataFile.tabularData or fileMetadata.dataFile.ingestInProgress)
        //                                        and DatasetPage.canDownloadFile(fileMetadata) 
    }
    
   
    /**
     * Copied over from the dataset page - 9/21/2016
     * 
     * @return 
     */
    public String getDataExploreURL() {
        String TwoRavensUrl = settingsService.getValueForKey(SettingsServiceBean.Key.TwoRavensUrl);

        if (TwoRavensUrl != null && !TwoRavensUrl.equals("")) {
            return TwoRavensUrl;
        }

        return "";
    }


    /**
     * Copied over from the dataset page - 9/21/2016
     * 
     * @param fileid
     * @param apiTokenKey
     * @return 
     */
    public String getDataExploreURLComplete(Long fileid) {
        System.out.print("in Two ravens helper...");
        if (fileid == null){
            throw new NullPointerException("fileid cannot be null");
        }
            
        
        String TwoRavensUrl = settingsService.getValueForKey(SettingsServiceBean.Key.TwoRavensUrl);
        String TwoRavensDefaultLocal = "/dataexplore/gui.html?dfId=";

        if (TwoRavensUrl != null && !TwoRavensUrl.equals("")) {
            // If we have TwoRavensUrl set up as, as an optional 
            // configuration service, it must mean that TwoRavens is sitting 
            // on some remote server. And that in turn means that we must use 
            // full URLs to pass data and metadata to it. 
            // update: actually, no we don't want to use this "dataurl" notation.
            // switching back to the dfId=:
            // -- L.A. 4.1
            /*
            String tabularDataURL = getTabularDataFileURL(fileid);
            String tabularMetaURL = getVariableMetadataURL(fileid);
            return TwoRavensUrl + "?ddiurl=" + tabularMetaURL + "&dataurl=" + tabularDataURL + "&" + getApiTokenKey();
            */
            System.out.print("TwoRavensUrl Set up " + TwoRavensUrl + "?dfId=" + fileid + "&" + getApiTokenKey());
            
            return TwoRavensUrl + "?dfId=" + fileid + "&" + getApiTokenKey();
        }

        // For a local TwoRavens setup it's enough to call it with just 
        // the file id:
                    System.out.print("TwoRavensUrl " + TwoRavensUrl);
           System.out.print("TwoRavensUrlDefault " + TwoRavensDefaultLocal + fileid + "&" + getApiTokenKey());         
        return TwoRavensDefaultLocal + fileid + "&" + getApiTokenKey();
    }
    
    private String getApiTokenKey() {
        ApiToken apiToken;
            System.out.print("In getApiTokenKey ");
        if (session.getUser() == null) {
            System.out.print("Session User null ");
            return null;
        }
        if (isSessionUserAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authService.findApiTokenByUser(au);
            System.out.print("apiToken " + apiToken);
            if (apiToken != null) {
                return "key=" + apiToken.getTokenString();
            }
            // Generate if not available?
            // Or should it just be generated inside the authService
            // automatically? 
            apiToken = authService.generateApiTokenForUser(au);
            if (apiToken != null) {
                return "key=" + apiToken.getTokenString();
            }
        }
        return "";

    }
    
    public boolean isSessionUserAuthenticated() {

        if (session == null) {
            return false;
        }
        
        if (session.getUser() == null) {
            return false;
        }
        
        return session.getUser().isAuthenticated();

    }
}