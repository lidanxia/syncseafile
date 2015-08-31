package com.seafile.seadroid2;
/**
 * 该类成员包括仓库Id,仓库名，目录dirPath:相对与当前仓库的路径，目录id:dirID
 * @author PC
 *
 */
public class NavContext 
{
    
    String repoID = null;
    String repoName = null;     // for display
    String dirPath = null;
    String dirID = null;

    public NavContext()
    {
    }
 
    public void setRepoID(String repoID) {
        this.repoID = repoID;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }
    
    public void setDir(String path, String dirID) {
        this.dirPath = path;
        this.dirID = dirID;
    }

    public void setDirID(String dirID) {
        this.dirID = dirID;
    }
/**
 * 此类中记载的repoid不为空
 * @return
 */
    public boolean inRepo() 
    {
        return repoID != null;
    }
    
    public String getRepoID() {
        return repoID;
    }

    public String getRepoName() {
        return repoName;
    }
    
    public boolean isRepoRoot() {
        return "/".equals(dirPath);
    }
    
    public String getDirPath() {
        return dirPath;
    }
    
    public String getDirID() {
        return dirID;
    }
}
