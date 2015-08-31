package com.seafile.seadroid2.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;

import com.seafile.seadroid2.BrowserActivity;
import com.seafile.seadroid2.SeadroidApplication;
import com.seafile.seadroid2.SeafConnection;
import com.seafile.seadroid2.SeafException;
import com.seafile.seadroid2.Utils;
import com.seafile.seadroid2.account.Account;
/**
 * ʵ�������ļ������أ��ڱ����½��ļ���һЩ����
 * @author PC
 *
 */
public class DataManager {

    private static final long SET_PASSWORD_INTERVAL = 59 * 60 * 1000; // 59 min
    // private static final long SET_PASSWORD_INTERVAL = 5 * 1000; // 5s
/**
 * �趨�ƶ��ն˵Ĺ���Ŀ¼
 * @return
 */
    public static String getExternalRootDirectory() 
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) 
        {
        	
            File extDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Seafile/");
            if (extDir.mkdirs() || extDir.exists())
            {
                return extDir.getAbsolutePath();
            } else {
                throw new RuntimeException("Couldn't create external directory");
            }
        } else {
            throw new RuntimeException("External Storage is currently not available");
        }
    }
/**
 * ��/mnt/sdcard/Seafile���½��ļ���temp
 * @return
 */
    public static String getExternalTempDirectory()
    {
        String root = getExternalRootDirectory();
        File tmpDir = new File(root + "/" + "temp");
        if (tmpDir.exists())
            return tmpDir.getAbsolutePath();
        else {
            if (tmpDir.mkdirs() == false)
                throw new RuntimeException("Couldn't create external temp directory");
            else
                return tmpDir.getAbsolutePath();
        }
    }
/**
 * ��/data/data/com.seafile.seadroid2/files�����½��ļ���thumb
 * @return String --����·��
 */
    public static String getThumbDirectory() 
    {
        String root = SeadroidApplication.getAppContext().getFilesDir().getAbsolutePath();
        File tmpDir = new File(root + "/" + "thumb");
        if (tmpDir.exists())
            return tmpDir.getAbsolutePath();
        else {
            if (tmpDir.mkdirs() == false)
                throw new RuntimeException("Couldn't create thumb directory");
            else
                return tmpDir.getAbsolutePath();
        }
    }
/**
 * ��/mnt/sdcard/Seafile���½��ļ���cache
 * @return
 */
    public static String getExternalCacheDirectory() 
    {
        String root = getExternalRootDirectory();
        File tmpDir = new File(root + "/" + "cache");
        if (tmpDir.exists())
            return tmpDir.getAbsolutePath();
        else {
            if (tmpDir.mkdirs() == false)
                throw new RuntimeException("Couldn't create external temp directory");
            else
                return tmpDir.getAbsolutePath();
        }
    }
  /**
   * �������ļ� 
   * @param path
   * @param oid
   * @return
   */
    static public String constructFileName(String path, String oid)
    {
        String filename = path.substring(path.lastIndexOf("/") + 1);
        if (filename.contains("."))
        {
            String purename = filename.substring(0, filename.lastIndexOf('.'));
            String suffix = filename.substring(filename.lastIndexOf('.') + 1);
            return purename + "-" + oid.substring(0, 8) + "." + suffix;
        }
        else
        {
            return filename + "-" + oid.substring(0, 8);
        }

    }
/**
 * pathת��Ϊ/Seafile�µ�·��
 * @param path
 * @param oid
 * @return
 */
    static public File getFileForFileCache(String path, String oid)
    {
        String p = getExternalRootDirectory() + "/" + constructFileName(path, oid);
        return new File(p);
    }

    static public File getTempFile(String path, String oid) {
        String p = getExternalTempDirectory() + "/" + constructFileName(path, oid);
        return new File(p);
    }
/**
 * ���ݲ����½�.png�ļ�
 * @param oid
 * @return file
 */
    static public File getThumbFile(String oid)
    {
        String p = Utils.pathJoin(getThumbDirectory(), oid + ".png");
        return new File(p);
    }

    // Obtain a cache file for storing a directory with oid
    static public File getFileForDirentsCache(String oid) {
        return new File(getExternalCacheDirectory() + "/" + oid);
    }

    static public final int MAX_GEN_CACHE_THUMB = 1000000;  // Only generate thumb cache for files less than 1MB
    static public final int MAX_DIRECT_SHOW_THUMB = 100000;  // directly show thumb
    /**
     * ���ļ������bitmap,����bitmap�޸ĳɺ��ʵĴ�С��Ȼ����ѹ��д���ļ���
     * �ļ��洢��/data/data/com.seafile.seadroid2/files/thumb,�ļ�������Ϊoid+".png"
     * @param repoName
     * @param repoID
     * @param path--�ļ��ڲֿ�Ŀ¼�µ����·��
     * @param oid--�ļ�id
     */
    public void calculateThumbnail(String repoName, String repoID, String path, String oid) 
    {
        try {
            final int THUMBNAIL_SIZE = 72;

            File file = getLocalRepoFile(repoName, repoID, path);
            if (!file.exists())
                return;
            if (file.length() > MAX_GEN_CACHE_THUMB)
                return;

            Bitmap imageBitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, THUMBNAIL_SIZE,
                    THUMBNAIL_SIZE, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //��imageBitmapѹ���Ժ�д��baos
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] byteArray = baos.toByteArray();
            File thumb = getThumbFile(oid);  //��ѹ��������д���ļ�thumb
            FileOutputStream out = new FileOutputStream(thumb);
            out.write(byteArray);
            out.close();
            System.out.println("DataManager--calculateThumbnail--Image--"+thumb);
        } catch (Exception ex) 
        {

        }
    }

    /**
     * ��fileת���ɺ��ʴ�С��bitmap
     * return bitmap
     * Caculate the thumbnail��С��������ͼ�� of an image directly when its size is less that
     * {@link #MAX_DIRECT_SHOW_THUMB}
     */
    public Bitmap getThumbnail(File file) 
    {
        try {
            final int THUMBNAIL_SIZE = caculateThumbnailSizeOfDevice();
            
            if (!file.exists())
                return null;
             //�������������bitmap��
            Bitmap imageBitmap = BitmapFactory.decodeStream(new FileInputStream(file));
            //���¹������ʴ�С��bitmap
            imageBitmap = Bitmap.createScaledBitmap(imageBitmap, THUMBNAIL_SIZE,
                    THUMBNAIL_SIZE, false);
            return imageBitmap;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * ���ݲ�ͬ���豸���趨��ͬ��bitmap�Ĵ�С
     * @return
     */
    public static int caculateThumbnailSizeOfDevice() 
    {
        
        DisplayMetrics metrics = SeadroidApplication.getAppContext().getResources().getDisplayMetrics();
        
        switch(metrics.densityDpi) {
        case DisplayMetrics.DENSITY_LOW: 
            return 36;
        case DisplayMetrics.DENSITY_MEDIUM:
            return 48;
        case DisplayMetrics.DENSITY_HIGH:
            return 72;
        case DisplayMetrics.DENSITY_XHIGH:
            return 96;
        default:
            return 36;
        }
        
    }
    

    private static final String DEBUG_TAG = "DataManager";

    public SeafConnection sc;
    private Account account;
    private DatabaseHelper dbHelper;

    List<SeafRepo> reposCache = null;

    public DataManager(Account act)
    {
        account = act;
        sc = new SeafConnection(act);
        dbHelper = DatabaseHelper.getDatabaseHelper();
    }

    public Account getAccount()
    {
        return account;
    }
/**
 * �γ�.dat�ļ�������/Seafile/cache��
 * @return
 */
    private File getFileForReposCache() 
    {
        String filename = "repos-" + (account.server + account.email).hashCode() + ".dat";
        return new File(getExternalCacheDirectory() + "/" +
                filename);
    }

    /**
     * The directory structure of Seafile on external storage is like this:
     *
     * /sdcard/Seafile
     *            |__ cache
     *            |__ temp
     *            |__ foo@gmail.com (cloud.seafile.com)
     *                      |__ Photos
     *                      |__ Musics
     *                      |__ ...
     *            |__ foo@mycompany.com (seafile.mycompany.com)
     *                      |__ Documents
     *                      |__ Manuals
     *                      |__ ...
     *            |__ ...
     *
     * In the above directory, the user has used two accounts.
     *
     * 1. One account has email "foo@gmail.com" and server
     * "cloud.seafile.com". Two repos, "Photos" and "Musics", has been
     * viewed.
     *
     * 2. Another account has email "foo@mycompany.com", and server
     * "seafile.mycompany.com". Two repos, "Documents" and "Manuals", has
     * been viewed.
     */
    public String getAccountDir() 
    {

        String username = account.getEmail();
        String server = Utils.stripSlashes(account.getServerHost());
        // strip port, like :8000 in 192.168.1.116:8000
        if (server.indexOf(":") != -1)
            server = server.substring(0, server.indexOf(':'));
        String p = String.format("%s (%s)", username, server);
        p = p.replaceAll("[^\\w\\d\\.@\\(\\) ]", "_");
        String accountDir = Utils.pathJoin(getExternalRootDirectory(), p);
        return accountDir;
    }

    /**
     * ����repoID,repoName,��ȡ�ֿ�Ŀ¼,����û�У��ڱ��ؽ���Ŀ¼
     * ���زֿ��ڱ��ص�Ŀ¼��·��
     * Get the top dir of a �ֿ�. If there are multiple repos with same name,
     * say "ABC", their top dir would be "ABC", "ABC (1)", "ABC (2)", etc. The
     * mapping (repoName, repoID, dir) is stored in a database table.
     */
    private String getRepoDir(String repoName, String repoID)
    {
        File repoDir;
       
        // Check if there is a record in databse
        String path = dbHelper.getRepoDir(account, repoName, repoID);
        if (path != null) 
        {
            // Has record in databse
            repoDir = new File(path);
            if (!repoDir.exists()) 
            {
                if (repoDir.mkdirs() == false) {
                    throw new RuntimeException("Could not create library directory " + path);
                }
            }
            return path;
        }
       //�������ݿ���û�д˲ֿ�ļ�¼
        int i = 0;
        while (true) 
        {
            String uniqueRepoName;
            if (i == 0) 
            {
                uniqueRepoName = repoName;
            } 
            else 
            {
                uniqueRepoName = repoName + " (" + i + ")";
            }
            path = Utils.pathJoin(getAccountDir(), uniqueRepoName);
            repoDir = new File(path);
            if (!repoDir.exists() &&!dbHelper.repoDirExists(account, uniqueRepoName)) 
            {
                // This repo dir does not exist yet, we can use it
                break;
            }
            i++;
        }

        if (repoDir.mkdirs() == false) 
        {
            throw new RuntimeException("Could not create repo directory " + path);
        }

        // Save the new mapping in database
         dbHelper.saveRepoDirMapping(account, repoName, repoID, path);
        return repoDir.getPath();
    }

    /**
     * �½������ļ����ڱ��ؽ��������ڵĲֿ�Ŀ¼���Ҵ���RepoDir����
     * Each repo is places under [account-dir]/[repo-name]. When a
     * file is downloaded, it's placed in its repo, with it full path.
     * @param repoName
     * @param repoID
     * @param path--�ļ��ڲֿ�Ŀ¼�µ����·��
     * return--file
     */
    public File getLocalRepoFile(String repoName, String repoID, String path)
    {
        String localPath = Utils.pathJoin(getRepoDir(repoName, repoID), path);
        File parentDir = new File(Utils.getParentPath(localPath));
        if (!parentDir.exists()) 
        {
            parentDir.mkdirs();
        }
        File f=new File(localPath);
        return new File(localPath);
     
    }
/**
 * ��jsonת��Ϊrepo���������б���
 * @param json
 * @return �洢��repo�б�
 */
    private List<SeafRepo> parseRepos(String json)
    {
        try {
            // may throw ClassCastException
            JSONArray array = Utils.parseJsonArray(json);
            if (array.length() == 0)
                return null;
            ArrayList<SeafRepo> repos = new ArrayList<SeafRepo>();
            for (int i = 0; i < array.length(); i++) 
            {
                JSONObject obj = array.getJSONObject(i);
                SeafRepo repo = SeafRepo.fromJson(obj);
                if (repo != null)
                    repos.add(repo);
            }
            return repos;
        } catch (JSONException e)
        {
            Log.w(DEBUG_TAG, "repos: parse json error");
            return null;
        } catch (Exception e) 
        {
            // other exception, for example ClassCastException
            return null;
        }
    }

    public List<SeafRepo> getCachedRepos() 
    {
        return reposCache;
    }

    public SeafRepo getCachedRepo(int position) 
    {
        return reposCache.get(position);
    }
/**
 * ����id�ҵ��洢�Ĳֿ���ص���Ϣ
 * @param id
 * @return repo
 */
    public SeafRepo getCachedRepoByID(String id)
    {
        List<SeafRepo> cachedRepos = getReposFromCache();
        if (cachedRepos == null)
        {
            return null;
        }

        for (SeafRepo repo: cachedRepos)
        {
            if (repo.getID().equals(id)) 
            {
                return repo;
            }
        }

        return null;
    }
/**
 * ����cacheĿ¼�µ�.dat�ļ����ҵ��洢�Ĳֿ�
 * @return
 */
    public List<SeafRepo> getReposFromCache() 
    {
        if (reposCache != null)
            return reposCache;

        File cache = getFileForReposCache();
        if (cache.exists())
        {
            String json = Utils.readFile(cache);
            reposCache = parseRepos(json);
            return reposCache;
        }
        return null;
    }
/**
 * �ӷ������ϻ�ȡ���µĲֿ���Ϣ�������浽����cache/
 * @return
 * @throws SeafException
 */
    public List<SeafRepo> getReposFromServer() throws SeafException
    {
        // First decide if use cache
        if (!Utils.isNetworkOn()) 
        {
            throw SeafException.networkException;
        }
        String json = sc.getRepos();
        if (json == null)
            return null;
        reposCache = parseRepos(json);
        
        try {
            File cache = getFileForReposCache();
            Utils.writeFile(cache, json);
        } catch (IOException e) {
            // ignore
        }

        return reposCache;
    }

    public interface ProgressMonitor
    {
        public void onProgressNotify(long total);
        boolean isCancelled();
    }
/**
 * �����ļ������浽���أ�������Ӧ�����ݱ�FileCache�м�¼
 * seafConnection�����أ��ϴ��ļ�
 * @param repoName
 * @param repoID
 * @param path
 * @param monitor
 * @return
 * @throws SeafException
 */
    public File getFile(String repoName, String repoID, String path,
                        ProgressMonitor monitor) throws SeafException
       {

        String cachedFileID = null;
        SeafCachedFile cf = getCachedFile(repoName, repoID, path);
        File localFile = getLocalRepoFile(repoName, repoID, path);
        // If local file is up to date, show it
        if (cf != null) 
        {
            if (localFile.exists()) 
            {
                cachedFileID = cf.fileID;
            }
        }

        Pair<String, File> ret = sc.getFile(repoID, path, localFile.getPath(), cachedFileID, monitor);

        String fileID = ret.first;
        if (fileID.equals(cachedFileID)) 
        {
            // cache is valid
            return localFile;
        } 
        else {
            File file = ret.second;
            addCachedFile(repoName, repoID, path, fileID, file);
            return file;
        }
    }
/**
 * ��String���͵�ת��ΪSeafDirent���͵Ķ���
 * @param json
 * @return List<SeafDirent>
 */
    private List<SeafDirent> parseDirents(String json)
    {
        try {
            JSONArray array = Utils.parseJsonArray(json);
            if (array == null)
                return null;

            ArrayList<SeafDirent> dirents = new ArrayList<SeafDirent>();
            for (int i = 0; i < array.length(); i++) 
            {
                JSONObject obj = array.getJSONObject(i);
                SeafDirent de = SeafDirent.fromJson(obj);
                if (de != null)
                    dirents.add(de);
            }
            return dirents;
        } catch (JSONException e)
        {
            return null;
        }
    }
/**
 * ��String���͵�ת��ΪSeafStarredFile���͵Ķ���
 * @param json
 * @return List<SeafStarredFile>
 */
    private List<SeafStarredFile> parseStarredFiles(String json)
    {
        try {
            JSONArray array = Utils.parseJsonArray(json);
            if (array == null)
                return null;

            ArrayList<SeafStarredFile> starredFiles = new ArrayList<SeafStarredFile>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                SeafStarredFile sf = SeafStarredFile.fromJson(obj);
                if (sf != null)
                    starredFiles.add(sf);
            }
            return starredFiles;
        } catch (JSONException e) {
            return null;
        }
    }
    /**
     * ���ݲֿ�id,path����ȡ�洢�ڱ��صı��ֿ��е�Ŀ¼��Ϣ
     * @param repoID
     * @param path
     * @return List<SeafDirent>-dirid�� context
     */
    public List<SeafDirent> getCachedDirents(String repoID, String path) 
    {
        String json = null;
        Pair<String, String> ret = dbHelper.getCachedDirents(repoID, path);
        if (ret == null) 
        {
            return null;
        }

        json = ret.second;
        if (json == null) 
        {
            return null;
        }

        return parseDirents(json);
    }

    /**
     * �ӷ������������µ�Ŀ¼��Ϣ��ת��Ϊ SeafDirent���󣬲��������ݿ�DirentsCache�У�ԭ�����е�Ŀ¼��Ϣ��ɾ�������ݣ�repoid��path��
     * return  List<SeafDirent>
     * 
     * In two cases we need to visit the server for dirents
     * 1. No cached dirents
     * 2. User clicks "refresh" button.
     *
     * In the second case, the local cache may still be valid.
     */
    public List<SeafDirent> getDirentsFromServer(String repoID, String path) throws SeafException 
    {
        Pair<String, String> cache = dbHelper.getCachedDirents(repoID, path);
        String cachedDirID = null;
        if (cache != null) 
        {
            cachedDirID = cache.first;
        }
        Pair<String, String> ret = sc.getDirents(repoID, path, cachedDirID);
        if (ret == null)
        {
            return null;
        }

        String dirID = ret.first;
        String content;

        if (cache != null && dirID.equals(cachedDirID)) 
        {
            // local cache still valid
            content = cache.second;
        } 
        else {
            content = ret.second;
            dbHelper.saveDirents(repoID, path, dirID, content);
        }

        return parseDirents(content);
    }
  /**
   * �ӷ����������Ǳ��ļ�����ת��ΪSeafStarredFile����
   * @return List<SeafStarredFile> 
   * @throws SeafException
   */
    public List<SeafStarredFile> getStarredFiles() throws SeafException {
        
        String starredFiles = sc.getStarredFiles();
        Log.i("GET STARRED FILES", starredFiles);
        return parseStarredFiles(starredFiles);
    }
  /**  
   * �������repoID,repoName,path,�ҵ����ݱ��еĶ�Ӧ��Ŀ
   * @param repoName
   * @param repoID
   * @param path--����ڲֿ�Ŀ¼��·��
   * @return
   */
    public SeafCachedFile getCachedFile(String repoName, String repoID, String path) 
    {
        SeafCachedFile cf = dbHelper.getFileCacheItem(repoID, path, this);
        return cf;
    }
  /**
   * ���ظ�dataManager�������˺ŵ�ȫ���ļ�������Ŀ���ļ������
   * @return
   */
    public List<SeafCachedFile> getCachedFiles() 
    {
        return dbHelper.getFileCacheItems(this);
    }
/**
 * ���ļ���ص���Ϣ���浽�ļ��������
 * @param repoName
 * @param repoID
 * @param path
 * @param fileID
 * @param file
 */
    public void addCachedFile(String repoName, String repoID, String path, String fileID, File file)
    {
    	 System.out.println("����ص��ļ���Ϣ���浽�ļ��������FileCache " ); 
    	SeafCachedFile item = new SeafCachedFile();
        item.repoName = repoName;
        item.repoID = repoID;
        item.path = path;
        item.fileID = fileID;
        item.accountSignature = account.getSignature();
        dbHelper.saveFileCacheItem(item, this);
    }
/**
 * ɾ���ļ����������ݿ���ɾ����¼
 * @param cf
 */
    public void removeCachedFile(SeafCachedFile cf) 
    {
        cf.file.delete();
        dbHelper.deleteFileCacheItem(cf);
    }
/**
 * Ϊ���ܵĲֿ���������
 * @param repoID
 * @param passwd
 * @throws SeafException
 */
    public void setPassword(String repoID, String passwd) throws SeafException
    {
        sc.setPassword(repoID, passwd);
    }
/**
 * �ϴ��ļ���������
 * @param repoName
 * @param repoID
 * @param dir
 * @param filePath
 * @param monitor
 * @throws SeafException
 */
    
    public void uploadFile(String repoName, String repoID, String dir, String filePath,
            ProgressMonitor monitor) throws SeafException 
    {
        uploadFileCommon(repoName, repoID, dir, filePath, monitor, false);
    }
/**
 * ��������ϸ����ļ�
 * @param repoName
 * @param repoID
 * @param dir
 * @param filePath
 * @param monitor
 * @throws SeafException
 */
    public void updateFile(String repoName, String repoID, String dir, String filePath,
            ProgressMonitor monitor) throws SeafException
            {
        uploadFileCommon(repoName, repoID, dir, filePath, monitor, true);
    }
/**
 * ��isUpdateΪtrue,��Ϊ�����ļ�������Ϊ�����ļ�,�����ļ��洢���ļ��������
 * @param repoName
 * @param repoID
 * @param dir
 * @param filePath
 * @param monitor
 * @param isUpdate ����true,������ļ��������ϴ��ļ�
 * @throws SeafException
 */
    private void uploadFileCommon(String repoName, String repoID, String dir,
                                  String filePath, ProgressMonitor monitor,
                                  boolean isUpdate) throws SeafException 
    {
        String newFileID = null;
        if (isUpdate)
        {
            newFileID  = sc.updateFile(repoID, dir, filePath, monitor);
            System.out.println("�������ļ� ,���ļ�ID:  "+ newFileID );
        } 
        else 
        {
            newFileID  = sc.uploadFile(repoID, dir, filePath, monitor);
            System.out.println("�ϴ����ļ������ļ�ID :  "+ newFileID );
        }

        if (newFileID == null || newFileID.length() == 0)  //���ز��ɹ�
        {
            return;
        }

        File srcFile = new File(filePath);
        String path = Utils.pathJoin(dir, srcFile.getName());
        //�½������ļ�
        File fileInRepo = getLocalRepoFile(repoName, repoID, path);
        System.out.println("׼�����Ƶ�Դ�ļ�  "+ srcFile.getAbsolutePath() );
        System.out.println("׼�����Ƶ���Ŀ���ļ�  "+fileInRepo.getAbsolutePath()  );
        String srcPath=srcFile.getAbsolutePath();
        String dstPath=fileInRepo.getAbsolutePath();
        if (!isUpdate&&!srcPath.equals(dstPath)) 
        {
           	BrowserActivity.upLoadFiles.add(dstPath);
           	//ֻ��ͨ���ϴ����ܣ����ҴӷǼ��Ŀ¼��Seafile���ϴ����ļ��ŷ�����б���
            // Copy the uploaded file to local repo cache
            try {
                Utils.copyFile(srcFile, fileInRepo);
                System.out.println("���������������������ļ��� " ); 
            } catch (IOException e) 
            {
            	 System.out.println("e Exception:  "+e.getMessage() );
                return;
            }
        }

        // Update file cache entry
        addCachedFile(repoName, repoID, path, newFileID, fileInRepo);
    }
/**
 * ������Ŀ¼
 * @param repoID
 * @param parentDir
 * @param dirName
 * @throws SeafException
 */
    public void createNewDir(String repoID, String parentDir, String dirName) throws SeafException {
        Pair<String, String> ret = sc.createNewDir(repoID, parentDir, dirName);
        if (ret == null) 
        {
            return;
        }

        String newDirID = ret.first;
        String response = ret.second;

        // The response is the dirents of the parentDir after creating
        // the new dir. We save it to avoid request it again
        dbHelper.saveDirents(repoID, parentDir, newDirID, response);
        System.out.println("dataManager---createNewDir");
        System.out.println("repoID:  "+repoID);
        System.out.println("parentDir:  "+parentDir);
        System.out.println("newDirID:  "+newDirID);
        System.out.println("response/context:  "+response);
    }
/**
 * �������ļ�
 * @param repoID
 * @param parentDir
 * @param fileName
 * @throws SeafException
 */
    public void createNewFile(String repoID, String parentDir, String fileName) throws SeafException {
        Pair<String, String> ret = sc.createNewFile(repoID, parentDir, fileName);
        if (ret == null) {
            return;
        }
        String newDirID = ret.first;
        String response = ret.second;

        // The response is the dirents of the parentDir after creating
        // the new file. We save it to avoid request it again
        dbHelper.saveDirents(repoID, parentDir, newDirID, response);
        System.out.println("dataManager---createNewFile");
        System.out.println("repoID:  "+repoID);
        System.out.println("parentDir:  "+parentDir);
        System.out.println("newDirID:  "+newDirID);
        System.out.println("response/context:   "+response);
    }

    public File getLocalCachedFile(String repoName, String repoID, String filePath, String fileID) {
        File localFile = getLocalRepoFile(repoName, repoID, filePath);
        if (!localFile.exists()) 
        {
            return null;
        }

        if (!Utils.isNetworkOn()) 
        {
            return localFile;
        }
        
        SeafCachedFile cf = getCachedFile(repoName, repoID, filePath);
        if (cf != null && cf.fileID != null && cf.fileID.equals(fileID)) {
            return localFile;
        } else {
            return null;
        }
    }
/**
 * �û�ѡ���������󣬲�ȡ�Ĳ�����������ص��ļ���Ϣ��Ŀ¼�������
 * @param repoID
 * @param path
 * @param newName
 * @param isdir
 * @throws SeafException
 */
    public void rename(String repoID, String path, String newName, boolean isdir) throws SeafException {
        Pair<String, String> ret = sc.rename(repoID, path, newName, isdir);
        if (ret == null) {
            return;
        }

        String newDirID = ret.first;
        String response = ret.second;

        // The response is the dirents of the parentDir after creating
        // the new file. We save it to avoid request it again
        dbHelper.saveDirents(repoID, Utils.getParentPath(path), newDirID, response);
        System.out.println("dataManager---rename����");
        System.out.println("repoID:  "+repoID);
        System.out.println("path  "+Utils.getParentPath(path));
        System.out.println("newDirID:  "+newDirID);
        System.out.println("response/context:  "+response);
    }

    private static class PasswordInfo 
    {
        String password;
        long timestamp;

        public PasswordInfo(String password, long timestamp) 
        {
            this.password = password;
            this.timestamp = timestamp;
        }
    }

    private static Map<String, PasswordInfo> passwords = new HashMap<String, PasswordInfo>();
/**
 * �ж�����������Ƿ��Ѿ����ڣ�
 * @param repoID
 * @return true--����������������
 */
    public static boolean getRepoPasswordSet(String repoID) 
    {
        PasswordInfo info = passwords.get(repoID);
        if (info == null) 
        {
            return false;
        }

        if (Utils.now() - info.timestamp > SET_PASSWORD_INTERVAL)
        {
            return false;
        }

        return true;
    }

    public static void setRepoPasswordSet(String repoID, String password) {
        passwords.put(repoID, new PasswordInfo(password, Utils.now()));
    }

    public static String getRepoPassword(String repoID) 
    {
        PasswordInfo info = passwords.get(repoID);
        if (info == null) {
            return null;
        }

        return info.password;
    }
}
