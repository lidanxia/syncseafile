package com.seafile.seadroid2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.util.Log;
import android.util.Pair;

import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.data.DataManager;
import com.seafile.seadroid2.data.DataManager.ProgressMonitor;
import com.seafile.sharing.FileSharing;

/**
 * SeafConnection encapsulates Seafile Web API
 * 与服务器连接，
 * @author plt
 */
public class SeafConnection {

    private static final String DEBUG_TAG = "SeafConnection";

    private Account account;

    public SeafConnection(Account act)
    {
        account = act;
    }

    public Account getAccount() {
        return account;
    }

    private HttpRequest prepareApiGetRequest(String apiPath, Map<String, ?> params) throws IOException {
        HttpRequest req = HttpRequest.get(account.server + apiPath, params, false);
        setRequestCommon(req);
        return req;
    }

    private HttpRequest prepareApiPutRequest(String apiPath, Map<String, ?> params) throws IOException {
        HttpRequest req = HttpRequest.put(account.server + apiPath, params, false);
        setRequestCommon(req);
        return req;
    }

    private void setRequestCommon(HttpRequest req) 
    {
        req.readTimeout(30000)
            .connectTimeout(15000)
            .followRedirects(true)
            .header("Authorization", "Token " + account.token);
        prepareHttpsCheck(req);
    }
    private void getServer_time(HttpRequest req)
    {
    	HttpURLConnection conn = req.getConnection();
    	long server_time=conn.getDate();
    	System.out.println("服务器--时间戳：  "+server_time);
    //	FileSharing.writeLog("服务器--时间戳：  "+server_time);
    	Date date=new Date(server_time);
    	SimpleDateFormat formatter1 = new SimpleDateFormat("HH-mm-ss-SSS");
    	String m_time1 = formatter1.format(date);
    	System.out.println("服务器--转换后的时间  ：  "+m_time1 );
    //	FileSharing.writeLog("服务器--转换后的时间  ：  "+m_time1);
    	long node_time=System.currentTimeMillis();
    	Date curr_date=new Date(node_time);
    	String curr_time=formatter1.format(curr_date);
    	System.out.println("本地节点--时间戳：  "+node_time);
    	System.out.println("本地节点--转换后的时间  ：  "+curr_time );
    //	FileSharing.writeLog("本地节点--转换后的时间  ：  "+curr_time);

    }
    private HttpRequest prepareHttpsCheck(HttpRequest req) {
        HttpURLConnection conn = req.getConnection();
        if (conn instanceof HttpsURLConnection) 
        {
            req.trustAllHosts();
            HttpsURLConnection sconn = (HttpsURLConnection)conn;
            sconn.setSSLSocketFactory(SSLTrustManager.instance().getSSLSocketFactory(account));
        }

        return req;
    }

    private HttpRequest prepareApiGetRequest(String apiPath) throws IOException {
        return prepareApiGetRequest(apiPath, null);
    }

    private HttpRequest prepareApiFileGetRequest(String url) throws HttpRequestException {
        HttpRequest req =  HttpRequest.get(url).connectTimeout(15000).followRedirects(true);

        return prepareHttpsCheck(req);
    }

    /** Prepare a post request.
     *  @param apiPath The path of the http request
     *  @param withToken
     *  @param params The query param to be appended to the request url
     *  @throws IOException
     */
    private HttpRequest prepareApiPostRequest(String apiPath, boolean withToken, Map<String, ?> params)
                                            throws HttpRequestException {
        HttpRequest req = HttpRequest.post(account.server + apiPath, params, true)
            .followRedirects(true)
            .connectTimeout(15000);

        if (withToken) {
            req.header("Authorization", "Token " + account.token);
        }

        return prepareHttpsCheck(req);
    }

    /**
     * Login into the server
     * @return true if login success, false otherwise
     * @throws SeafException
     */
    private boolean realLogin() throws SeafException 
    {
        HttpRequest req = null;
        try {
            req = prepareApiPostRequest("api2/auth-token/", false, null);
            Log.d(DEBUG_TAG, "Login to " + account.server + "api2/auth-token/");

            req.form("username", account.email);
            req.form("password", account.passwd);
            String appVersion = "";    
            Context context = SeadroidApplication.getAppContext();
            try {
                PackageInfo pInfo = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0);
                appVersion = pInfo.versionName;
            } catch (NameNotFoundException e) {
                // ignore
            }

            String deviceId = Secure.getString(context.getContentResolver(),
                                               Secure.ANDROID_ID);

            req.form("platform", "android");
            req.form("device_id", deviceId);
            req.form("device_name", Build.MODEL);
            req.form("client_version", appVersion);
            req.form("platform_version", Build.VERSION.RELEASE);

            if (req.code() != 200) 
            {
                if (req.message() == null) 
                {
                    throw SeafException.networkException;
                } 
                else 
                {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String contentAsString = new String(req.bytes(), "UTF-8");
            JSONObject obj = Utils.parseJsonObject(contentAsString);
            if (obj == null)
                return false;
            account.token = obj.getString("token");
            
            getServer_time(req);
            
            return true;
        } catch (SeafException e) {
            throw e;
        } catch (HttpRequestException e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                throw SeafException.sslException;
            } else {
                throw SeafException.networkException;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw SeafException.networkException;
        } catch (JSONException e) {
            throw SeafException.illFormatException;
        }
    }

    public boolean doLogin() throws SeafException {
        try {
            return realLogin();
        } catch (Exception e) {
            // do again
            return realLogin();
        }
    }
/**
 * 从服务器上获取所有的仓库信息，以String形式保存
 * @return
 * @throws SeafException
 */
    public String getRepos() throws SeafException 
    {
        HttpRequest req = null;
        try {		
            req = prepareApiGetRequest("api2/repos/");
            if (req.code() != 200) 
            {
                if (req.message() == null) 
                {	
                    throw SeafException.networkException;
                } 
                else 
                {	
                    throw new SeafException(req.code(), req.message());
                }
            }
            String result = new String(req.bytes(), "UTF-8");	
            return result;
        } catch (SeafException e) {
            throw e;
        } catch (HttpRequestException e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                throw SeafException.sslException;
            } else {
                throw SeafException.networkException;
            }
        } catch (IOException e) {
            throw SeafException.networkException;
        }
    }
/**
 * 从服务器上下载星标文件
 * @return
 * @throws SeafException
 */
    public String getStarredFiles() throws SeafException 
    {
        try {
            HttpRequest req = prepareApiGetRequest("api2/starredfiles/");
            if (req.code() != 200) {
                if (req.message() == null) {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String result = new String(req.bytes(), "UTF-8");
            return result;
        } catch (SeafException e) {
            throw e;
        } catch (HttpRequestException e) {
            throw SeafException.networkException;
        } catch (IOException e) {
            throw SeafException.networkException;
        }
    }

    private static String encodeUriComponent(String src) throws UnsupportedEncodingException {
        return URLEncoder.encode(src, "UTF-8");
    }

    /**
     * Get the contents of a directory.
     * 获取指定目录的全部文件信息
     * @param repoID
     * @param path
     * @param cachedDirID 在本地缓存的目录id.
     * @return A non-null Pair of (dirID, content). If the local cache is up to date, the "content" is null.
     * @throws SeafException
     */
    public Pair<String, String> getDirents(String repoID, String path, String cachedDirID)
                                        throws SeafException
       {
        try {
            String apiPath = String.format("api2/repos/%s/dir/", repoID);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("p", encodeUriComponent(path));
            if (cachedDirID != null) 
            {
                params.put("oid", cachedDirID);
            }
            HttpRequest req = prepareApiGetRequest(apiPath, params);
            if (req.code() != 200)
                if (req.message() == null)
                    throw SeafException.networkException;
                else
                    throw new SeafException(req.code(), req.message());

            String dirID = req.header("oid");
            String content;
            if (dirID == null) {
                throw SeafException.unknownException;
            }

            if (dirID.equals(cachedDirID))
            {
                // local cache is valid
                Log.d(DEBUG_TAG, String.format("dir %s is cached", path));
                content = null;
            }
            else {
                Log.d(DEBUG_TAG,
                      String.format("dir %s will be downloaded from server, latest %s, local cache %s",
                                    path, dirID, cachedDirID != null ? cachedDirID : "null"));
                byte[] rawBytes = req.bytes();
                if (rawBytes == null) 
                {
                    throw SeafException.unknownException;
                }
                content = new String(rawBytes, "UTF-8");
            }

            return new Pair<String, String>(dirID, content);

        } catch (SeafException e) {
            throw e;
        } catch (UnsupportedEncodingException e) {
            throw SeafException.encodingException;
        } catch (HttpRequestException e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                throw SeafException.sslException;
            } else {
                throw SeafException.networkException;
            }
        } catch (IOException e) {
            throw SeafException.networkException;
        }
    }

    private Pair<String, String> getDownloadLink(String repoID, String path) throws SeafException {
        try {
            String apiPath = String.format("api2/repos/%s/file/", repoID);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("p", encodeUriComponent(path));
            params.put("op", "download");
            HttpRequest req = prepareApiGetRequest(apiPath, params);
            if (req.code() != 200) {
                if (req.message() == null) {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String result = new String(req.bytes(), "UTF-8");
            String fileID = req.header("oid");

            // should return "\"http://gonggeng.org:8082/...\"" or "\"https://gonggeng.org:8082/...\"
            if (result.startsWith("\"http") && fileID != null) 
            {
                String url = result.substring(1, result.length() - 1);
                return new Pair<String, String>(url, fileID);
            } else {
                throw SeafException.illFormatException;
            }
        } catch (SeafException e) {
            throw e;
        } catch (UnsupportedEncodingException e) {
            throw SeafException.encodingException;
        } catch (IOException e) {
            throw SeafException.networkException;
        } catch (HttpRequestException e) {
            throw SeafException.networkException;
        }
    }
/**
 * 下载文件，返回值即为下载的文件
 * @param dlink
 * @param path
 * @param localPath
 * @param oid
 * @param monitor
 * @return
 * @throws SeafException
 */
    private File getFileFromLink(String dlink, String path, String localPath,
                                 String oid, ProgressMonitor monitor)
                                    throws SeafException {
        if (dlink == null)
            return null;

        File file = new File(localPath);

        try {
            int i = dlink.lastIndexOf('/');
            String quoted = dlink.substring(0, i) + "/" +
                    URLEncoder.encode(dlink.substring(i+1), "UTF-8");

            HttpRequest req = prepareApiFileGetRequest(quoted);
            if (req.code() != 200) 
            {
                if (req.message() == null) 
                {
                    throw SeafException.networkException;
                } 
                else 
                {
                    throw new SeafException(req.code(), req.message());
                }
            }

            if (monitor != null) 
            {
                /*if (req.header(HttpRequest.HEADER_CONTENT_LENGTH) == null) {
                    throw SeafException.illFormatException;
                }
                Long size = Long.parseLong(req.header(HttpRequest.HEADER_CONTENT_LENGTH));*/
                if (req.contentLength() > 0)
                {
                    Long size =  Long.valueOf(req.contentLength());
                    monitor.onProgressNotify(size);
                };
            }

            File tmp = DataManager.getTempFile(path, oid);
            // Log.d(DEBUG_TAG, "write to " + tmp.getAbsolutePath());
            if (monitor == null)
            {
                req.receive(tmp);
            } 
            else
            {
                req.bufferSize(MonitoredFileOutputStream.BUFFER_SIZE);
                req.receive(new MonitoredFileOutputStream(tmp, monitor));
            }

            if (tmp.renameTo(file) == false) {
                Log.w(DEBUG_TAG, "Rename file error");
                return null;
            }
            //下载的文件放在列表中
            BrowserActivity.ServerDownloadFiles.add(file.getAbsolutePath());
            System.out.println("列表中放的文件的路径   "+file.getAbsolutePath());
            return file;

        } catch (SeafException e) {
            throw e;
        } catch (UnsupportedEncodingException e) {
            throw SeafException.encodingException;
        } catch (IOException e) {
            e.printStackTrace();
            throw SeafException.networkException;
        } catch (HttpRequestException e) {
            if (e.getCause() instanceof MonitorCancelledException) {
                Log.d(DEBUG_TAG, "download is cancelled");
                throw SeafException.userCancelledException;
            } else {
                throw SeafException.networkException;
            }
        }
    }

    /**
     * 下载文件
     * Get the latest version of the file from server
     * @param repoID
     * @param path
     * @param localPath
     * @param cachedDirID The file id of the local cached version
     * @param monitor
     * @return A two tuple of (fileID, file). If the local cached version is up to date, the returned file is null.
     */
    public Pair<String, File> getFile(String repoID,
                                          String path,
                                          String localPath,
                                          String cachedFileID,
                                          ProgressMonitor monitor) throws SeafException {
        
    	 SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
    	 Date curDate = new Date(System.currentTimeMillis());
    	 String m = formatter.format(curDate); 	
         FileSharing. writeLog(""+"\r\n");
    	 FileSharing.writeLog("%%%服务器sending(=联网节点发送“下载”请求 ) "+path+",	");
    	 FileSharing.writeLog(System.currentTimeMillis()+",	");
    	 FileSharing.writeLog(m+",	"+"\r\n");
    	
    	Pair<String, String> ret = getDownloadLink(repoID, path);
        String dlink = ret.first;
        String fileID = ret.second;

        if (fileID.equals(cachedFileID))
        {
            // cache is valid
            Log.d(DEBUG_TAG, String.format("file %s is cached ,fileid: "+fileID, path)); 
            return new Pair<String, File>(fileID, null);
          
        }
        else {
            Log.d(DEBUG_TAG,
                  String.format("file %s will be downloaded from server, latest %s, local cache %s",
                                path, fileID, cachedFileID != null ? cachedFileID : "null"));

            File file = getFileFromLink(dlink, path, localPath, fileID, monitor);
            System.out.println("fileDownload success ，文件id:  "+fileID);
            if (file != null)
            {
                return new Pair<String, File>(fileID, file);
            } 
            else 
            {
                throw SeafException.unknownException;
            }
        }
    }

   
    /**
     * set password for an encrypted repo
     * @param repoID
     * @param passwd
     * @throws SeafException
     */
    public void setPassword(String repoID, String passwd) throws SeafException 
    {
        try {
            HttpRequest req = prepareApiPostRequest("api2/repos/" + repoID + "/", true, null);

            req.form("password", passwd);

            if (req.code() != 200) {
                if (req.message() == null) {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }
        } catch (SeafException e) {
            Log.d(DEBUG_TAG, "Set Password err: " + e.getCode());
            throw e;
        } catch (Exception e) {
            Log.d(DEBUG_TAG, "Exception in setPassword ");
            e.printStackTrace();
            return;
        }
    }
/**
 * 获取上传的具体仓库的地址
 * @param repoID
 * @param update
 * @return
 * @throws SeafException
 */
    private String getUploadLink(String repoID, boolean update) throws SeafException {
        try {
            String apiPath;
            if (update) 
            {
                apiPath = "api2/repos/" + repoID + "/update-link/";
            } 
            else
            {
                apiPath = "api2/repos/" + repoID + "/upload-link/";
            }

            HttpRequest req = prepareApiGetRequest(apiPath);
            if (req.code() != 200) {
                Log.d("Upload", "Failed to get upload link " + req.code());
                if (req.message() == null)
                {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String result = new String(req.bytes(), "UTF-8");
            // should return "\"http://gonggeng.org:8082/...\"" or "\"https://gonggeng.org:8082/...\"
            if (result.startsWith("\"http"))
            {
                // remove the starting and trailing quote
                return result.substring(1, result.length()-1);
            } else
                throw SeafException.unknownException;
        } catch (SeafException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null)
                Log.d(DEBUG_TAG, msg);
            else
                Log.d(DEBUG_TAG, "get upload link error");
            throw SeafException.unknownException;
        }
    }

    /**
     * 将本地文件，更新到服务器
     * Upload a file to update an existing file
     * return newfile_id
     */
    public String updateFile(String repoID, String dir, String filePath, ProgressMonitor monitor)
                                throws SeafException 
      {
        try {
            String url = getUploadLink(repoID, true);
            return uploadFileCommon(url, repoID, dir, filePath, monitor, true);
        } catch (SeafException e) {
            // do again
            String url = getUploadLink(repoID, true);
            return uploadFileCommon(url, repoID, dir, filePath, monitor, true);
        }
    }

    /**
     * Upload a new file
     */
    public String uploadFile(String repoID, String dir, String filePath, ProgressMonitor monitor)
                            throws SeafException {
        try {
            String url = getUploadLink(repoID, false);
            return uploadFileCommon(url, repoID, dir, filePath, monitor, false);
        } catch (SeafException e) {
            // do again
            String url = getUploadLink(repoID, false);
            return uploadFileCommon(url, repoID, dir, filePath, monitor, false);
        }
    }

    private static final String CRLF = "\r\n";
    private static final String TWO_HYPENS = "--";
    private static final String BOUNDARY = "----SeafileAndroidBound$_$";

    
    
     /** Upload a file to seafile httpserver
      * update若为true,则是更新本地文件到服务器
      * @param link
      * @param repoID
      * @param dir
      * @param filePath
      * @param monitor
      * @param update
      * @return return newfile_id
      * @throws SeafException
      */
    private String uploadFileCommon(String link, String repoID, String dir,
                                     String filePath, ProgressMonitor monitor, boolean update)
                                        throws SeafException
    {

        try {
            File file = new File(filePath);
            if (!file.exists())
            {
            	System.out.println("上传线程错误:  File not exists");
                throw new SeafException(SeafException.OTHER_EXCEPTION, "File not exists");
            }
            System.out.println("SeafileConnection:与服务器连接的上传线程开始执行");
            HttpRequest req = HttpRequest.post(link).followRedirects(true).connectTimeout(15000);
            prepareHttpsCheck(req);

            /**
             * We have to set the content-length header, otherwise the whole
             * request would be buffered by android. So we have to format the
             * multipart form-data request ourselves in order to calculate the
             * content length.
             */
            int totalLen = 0;
            byte[] dirParam = {};
            byte[] targetFileParam = {};
            StringBuilder builder;

            if (update)
            {
                // the "target_file" param is for update file api
                builder = new StringBuilder();
                // line 1, ------SeafileAndroidBound$_$
                builder.append(TWO_HYPENS + BOUNDARY + CRLF);
                // line 2
                builder.append("Content-Disposition: form-data; name=\"target_file\"" + CRLF);
                // line 3, an empty line
                builder.append(CRLF);
                String targetFilePath = Utils.pathJoin(dir, file.getName());
                // line 4
                builder.append(targetFilePath + CRLF);
                targetFileParam = builder.toString().getBytes("UTF-8");
                totalLen += targetFileParam.length;
            } 
            else {
                // the "parent_dir" param is for upload file api
                builder = new StringBuilder();
                // line 1, ------SeafileAndroidBound$_$
                builder.append(TWO_HYPENS + BOUNDARY + CRLF);
                // line 2
                builder.append("Content-Disposition: form-data; name=\"parent_dir\"" + CRLF);
                // line 3, an empty line
                builder.append(CRLF);
                // line 4
                builder.append(dir + CRLF);
                dirParam = builder.toString().getBytes("UTF-8");
                totalLen += dirParam.length;
            }

            // line 1
            String l1 = TWO_HYPENS + BOUNDARY + CRLF;
            // line 2,
            byte[] l2 = new String("Content-Disposition: form-data; name=\"file\";filename=\""
                    + file.getName() + "\"" + CRLF).getBytes("UTF-8");
            // line 3
            String l3 = "Content-Type: text/plain" + CRLF;
            // line 4
            String l4 = CRLF;
            totalLen += l1.length() + l2.length + l3.length() + l4.length() + file.length() + 2;

            String end = TWO_HYPENS + BOUNDARY + TWO_HYPENS + CRLF;
            totalLen += end.length();

            req.contentLength(totalLen);
            req.header("Connection", "close");
            req.header("Cache-Control", "no-cache");
            req.header("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);

            if (update) 
            {
                req.send(targetFileParam);
            } 
            else {
                req.send(dirParam);
            }
            req.send(l1);
            req.send(l2);
            req.send(l3);
            req.send(l4);

            if (monitor != null)
            {
                req.bufferSize(MonitoredFileInputStream.BUFFER_SIZE);
                req.send(new MonitoredFileInputStream(file, monitor));
                System.out.println("SeafileConnection:上传文件长度（monitor！=null）： "+file.length());
            } 
            else {
                req.send(new FileInputStream(file));
                System.out.println("SeafileConnection:上传文件长度： "+file.length());
            }

            req.send(CRLF);
            req.send(end);

            if (req.code() != 200)
                if (req.message() == null) 
                {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            
            return new String(req.bytes(), "UTF-8");
        } catch (IOException e) 
        {
            throw SeafException.networkException;

        } catch (HttpRequestException e)
        {
            if (e.getCause() instanceof MonitorCancelledException) 
            {
                Log.d(DEBUG_TAG, "upload is cancelled");
                throw SeafException.userCancelledException;
            } 
            else
            {
                throw SeafException.networkException;
            }
        }
    }
/**
 * 在服务器上新建目录
 * @param repoID
 * @param parentDir
 * @param dirName
 * @return newDirID, content
 * @throws SeafException
 */
    public Pair<String, String> createNewDir(String repoID,
                                                 String parentDir,
                                                 String dirName) throws SeafException 
       {

        HttpRequest req = null;
        try {
            String fullPath = Utils.pathJoin(parentDir, dirName);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("p", fullPath);
            params.put("reloaddir", "true");

            req = prepareApiPostRequest("api2/repos/" + repoID + "/dir/", true, params);

            req.form("operation", "mkdir");

            if (req.code() != 200) {
                if (req.message() == null) {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String newDirID = req.header("oid");
            if (newDirID == null) {
                return null;
            }

            String content = new String(req.bytes(), "UTF-8");
            if (content.length() == 0) {
                return null;
            }

            return new Pair<String, String>(newDirID, content);

        } catch (SeafException e) {
            throw e;
        } catch (UnsupportedEncodingException e) {
            throw SeafException.encodingException;
        } catch (HttpRequestException e) {
            throw SeafException.networkException;
        }
    }
/**
 * 在服务器上新建文件
 * @param repoID
 * @param parentDir
 * @param fileName
 * @return
 * @throws SeafException
 */
    public Pair<String, String> createNewFile(String repoID,
                                                  String parentDir,
                                                  String fileName) throws SeafException {

        try {
            String fullPath = Utils.pathJoin(parentDir, fileName);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("p", fullPath);
            params.put("reloaddir", "true");

            HttpRequest req = prepareApiPostRequest("api2/repos/" + repoID + "/file/", true, params);

            req.form("operation", "create");

            if (req.code() != 200) {
                if (req.message() == null) {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String newDirID = req.header("oid");
            if (newDirID == null) {
                return null;
            }

            String content = new String(req.bytes(), "UTF-8");
            if (content.length() == 0) {
                return null;
            }

            return new Pair<String, String>(newDirID, content);

        } catch (SeafException e) {
            throw e;
        } catch (UnsupportedEncodingException e) {
            throw SeafException.encodingException;
        } catch (HttpRequestException e) {
            throw SeafException.networkException;
        }
    }


    /**
     * Wrap a FileInputStream in a upload task. We publish the progress of the upload during the process,
     *  and if we detect the task has been cancelled by the user, 
     *  we throw a {@link MonitorCancelledException} to indicate such a situation.
     */
    private class MonitoredFileInputStream extends InputStream {
        public static final int BUFFER_SIZE = 1024;


        private static final long PROGRESS_UPDATE_INTERVAL = 1000;
        private ProgressMonitor monitor;
        private InputStream src;
        private long bytesRead = 0;
        private long nextUpdate = System.currentTimeMillis() + PROGRESS_UPDATE_INTERVAL;

        public MonitoredFileInputStream(File file, ProgressMonitor monitor) throws IOException {
            this.src = new FileInputStream(file);
            this.monitor = monitor;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            int read = src.read(buffer);
            if (read != -1) {
                bytesRead += read;
            }

            checkMonitor();

            return read;
        }

        @Override
        public int read() throws IOException {
            int ret = src.read();
            if (ret != -1) {
                ++bytesRead;
                if (bytesRead % BUFFER_SIZE == 0) {
                    checkMonitor();
                }
            }

            return ret;
        }

        @Override
        public void close() throws IOException {
            src.close();
        }

        private void checkMonitor() throws MonitorCancelledException {
            if (monitor.isCancelled() ||
                Thread.currentThread().isInterrupted()) {
                throw new MonitorCancelledException();
            }

            if (System.currentTimeMillis() > nextUpdate) {
                monitor.onProgressNotify(bytesRead);
                nextUpdate = System.currentTimeMillis() + PROGRESS_UPDATE_INTERVAL;
            }
        }
    }

    /**
     * Wrap a FileOutputStream in a download task. We publish the upload progress during the process, and if we detect the task has been cancelled by the user, we throw a {@link MonitorCancelledException} to indicate such a situation.
     */
    private class MonitoredFileOutputStream extends OutputStream {
        public static final int BUFFER_SIZE = 4096;

        private static final long PROGRESS_UPDATE_INTERVAL = 500;
        private ProgressMonitor monitor;
        private OutputStream dst;
        private long bytesWritten = 0;
        private long nextUpdate = System.currentTimeMillis() + PROGRESS_UPDATE_INTERVAL;

        public MonitoredFileOutputStream(File file, ProgressMonitor monitor) throws IOException {
            this.dst = new FileOutputStream(file);
            this.monitor = monitor;
        }

        @Override
        public void write(byte[] buffer, int off, int len) throws IOException {
            dst.write(buffer, off, len);
            bytesWritten += len;
            checkMonitor();
        }

        @Override
        public void write(byte[] buffer) throws IOException {
            dst.write(buffer);
            bytesWritten += buffer.length;
            checkMonitor();
        }

        @Override
        public void write(int b) throws IOException {
            dst.write(b);
            ++bytesWritten;
            if (bytesWritten % BUFFER_SIZE == 0) {
                checkMonitor();
            }
        }

        @Override
        public void close() throws IOException {
            dst.close();
        }

        private void checkMonitor() throws MonitorCancelledException {
            if (monitor.isCancelled() ||
                Thread.currentThread().isInterrupted()) {
                throw new MonitorCancelledException();
            }

            if (System.currentTimeMillis() > nextUpdate) {
                monitor.onProgressNotify(bytesWritten);
                nextUpdate = System.currentTimeMillis() + PROGRESS_UPDATE_INTERVAL;
            }
        }
    }

    private class MonitorCancelledException extends IOException {
        private static final long serialVersionUID = -1170466989781746232L;

        @Override
        public String toString() {
            return "the upload/download task has been cancelled";
        }
    }

    public String getShareLink(String repoID, String path, boolean isdir) throws SeafException {
        try {
            String apiPath = String.format("api2/repos/%s/file/shared-link/", repoID);
            HttpRequest req = prepareApiPutRequest(apiPath, null);
            req.form("p", path);
            req.form("type", isdir ? "d": "f");
            if (req.code() != 201) {
                if (req.message() == null) {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String result = req.header("Location");
            if (result == null) {
                throw SeafException.illFormatException;
            }
            return result;
        } catch (UnsupportedEncodingException e) {
            throw SeafException.encodingException;
        } catch (IOException e) {
            throw SeafException.networkException;

        } catch (SeafException e) {
            throw e;
        } catch (HttpRequestException e) {
            throw SeafException.networkException;
        }
    }
/**
 * 重命名文件，并将结果上传到服务器
 * @param repoID
 * @param path
 * @param newName
 * @param isdir
 * @return newDirID, content
 * @throws SeafException
 */
    public Pair<String, String> rename(String repoID, String path,
                                       String newName, boolean isdir) throws SeafException {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("p", path);
            params.put("reloaddir", "true");
            String suffix = isdir ? "/dir/" : "/file/";
            HttpRequest req = prepareApiPostRequest("api2/repos/" + repoID + suffix, true, params);

            req.form("operation", "rename");
            req.form("newname", newName);

            if (req.code() != 200) {
                if (req.message() == null) {
                    throw SeafException.networkException;
                } else {
                    throw new SeafException(req.code(), req.message());
                }
            }

            String newDirID = req.header("oid");
            if (newDirID == null) {
                return null;
            }
            String content = new String(req.bytes(), "UTF-8");
            if (content.length() == 0) {
                return null;
            }

            return new Pair<String, String>(newDirID, content);
        } catch (SeafException e) {
            throw e;
        } catch (UnsupportedEncodingException e) {
            throw SeafException.encodingException;
        } catch (HttpRequestException e) {
            throw SeafException.networkException;
        }
    }
}
