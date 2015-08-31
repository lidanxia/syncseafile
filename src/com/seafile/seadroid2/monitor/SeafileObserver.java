package com.seafile.seadroid2.monitor;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.common.collect.Maps;
import com.seafile.seadroid2.BrowserActivity;
import com.seafile.seadroid2.SeafException;
import com.seafile.seadroid2.Utils;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.data.DataManager;
import com.seafile.seadroid2.data.DatabaseHelper;
import com.seafile.seadroid2.data.SeafCachedFile;
import com.seafile.seadroid2.data.SeafDirent;
import com.seafile.seadroid2.data.SeafDirent.DirentType;
import com.seafile.seadroid2.transfer.TransferManager;
import com.seafile.seadroid2.transfer.TransferManager.DownloadTaskInfo;
import com.seafile.seadroid2.transfer.TransferManager.UploadTaskInfo;
import com.seafile.seadroid2.transfer.TransferService;
import com.seafile.sharing.FileMtimeData;
import com.seafile.sharing.FileSharing;
import com.seafile.sharing.SendServerTime;


/**
 * FileAlterationObserver,监听目录内的文件，文件变化的监控器接口FileAlterationListener
 * @author PC
 * 监控此account的所有缓存文件，CachedFileChangedListener为监控器
 * account
 * CachedFileChangedListener
 */

public class SeafileObserver implements FileAlterationListener
{
    private final String DEBUG_TAG = "SeafileObserver";
    private Account account;
    public static DataManager dataManager;
    //上传服务器时间为全局变量，初始值为0；
    public static long localTime=0;
    //public static long itemtime=0;
   // public static int flag=0;
   
    
    private FileAlterationObserver alterationObserver;
    public static final Map<String, SeafCachedFile> watchedFiles = Maps.newHashMap();
    public static CachedFileChangedListener listener;
    private final  RecentDownloadedFilesWorkAround recentDownloadedFiles =
            new RecentDownloadedFilesWorkAround();
    public FileSharing fileShare=new FileSharing();
    public static DatabaseHelper dbHelper;
	public static TransferManager txManager= new TransferManager();
    public static seafile_TransReceiver sTransferReceiver = new seafile_TransReceiver();
    public interface CachedFileChangedListener 
    {
        void onCachedFiledChanged(Account account, SeafCachedFile cf, File file,boolean isUpdate);
    }

    public SeafileObserver(Account account, CachedFileChangedListener listener)
    {

        this.account = account;
        this.dataManager = new DataManager(account);
        this.listener = listener;
         //设置监听的目录，/sdcard/Seafile
        alterationObserver = new FileAlterationObserver(getAccountDir());
        alterationObserver.addListener(this);
        watchAllCachedFiles();   
       
  
    }

    private String getAccountDir() {
        return dataManager.getAccountDir();
    }

    public FileAlterationObserver getAlterationObserver() {
        return alterationObserver;
    }
/**
 * 将dataManager表中存的所有缓存文件，加入watchedFiles中
 */
    private void watchAllCachedFiles() 
    {
        List<SeafCachedFile> cachedfiles = dataManager.getCachedFiles();
        for (SeafCachedFile cached : cachedfiles)
        {
            File file = dataManager.getLocalRepoFile(cached.repoName, cached.repoID, cached.path);
            if (file.exists()) 
            {
                watchedFiles.put(file.getPath(), cached);
            }
        }
    }
/**
 * 将下载的文件加入到监控MAP中
 * @param repoID
 * @param repoName
 * @param pathInRepo
 * @param localpath
 */
    public void watchDownloadedFile(String repoID, String repoName, String pathInRepo,
            String localpath) {
        recentDownloadedFiles.addRecentDownloadedFile(localpath);

        SeafCachedFile cacheInfo = new SeafCachedFile();
        cacheInfo.repoID = repoID;
        cacheInfo.repoName = repoName;
        cacheInfo.path = pathInRepo;
        watchedFiles.put(localpath, cacheInfo);

        Log.d(DEBUG_TAG, "start watch downloaded file " + localpath);
    }
   
    /**
     * 将上传的文件加入到监控MAP中
     * @param repoID
     * @param repoName
     * @param pathInRepo
     * @param localpath
     */
        public void  watchUploadedFile(String repoID, String repoName, String pathInRepo,
                String localpath)
        {
            recentDownloadedFiles.addRecentDownloadedFile(localpath);

            SeafCachedFile cacheInfo = new SeafCachedFile();
            cacheInfo.repoID = repoID;
            cacheInfo.repoName = repoName;
            cacheInfo.path = pathInRepo;
            watchedFiles.put(localpath, cacheInfo);

            Log.d(DEBUG_TAG, "start watch uploaded file " + localpath);
        }
    public void setAccount(Account account) {
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    public void startWatching() 
    {
        try {
            alterationObserver.initialize();
        } catch (Exception e) {

        }
        alterationObserver.checkAndNotify();
    }

    public void stopWatching()
    {
        try {
            alterationObserver.destroy();
        } catch (Exception e) {

        }
    }

    @Override
    public void onDirectoryChange(File directory) 
    {
        Log.v(DEBUG_TAG, directory.getPath() + " was modified!");
    }

    @Override
    public void onDirectoryCreate(File directory) {
        Log.v(DEBUG_TAG, directory.getPath() + " was created!");
    }

    @Override
    public void onDirectoryDelete(File directory) {
        Log.v(DEBUG_TAG, directory.getPath() + " was deleted!");
    }
/**
 * 监控的文件改变了 ，就会触发该函数，但是该函数没有采取任何操作
 */
    @Override
    public void onFileChange(File file) 
    {
        String path = file.getPath();
        SeafCachedFile cachedFile = watchedFiles.get(path);
        FileSharing.netupdate=true;
        long curr_time=System.currentTimeMillis();
       /* if (recentDownloadedFiles.isRecentDownloadedFiles(path)) 
        {   //从服务器下载的更新，不在发送给服务器，当从服务器下载更新成功后会写进local表和server表，并adhoc广播
            Log.d(DEBUG_TAG, "ignore change signal for recent downloaded file " + path);
            System.out.println("更新的第一种情况，从服务器下载更新");
            
            //return;
        }*/
        
        //else
        //{
        	if(cachedFile != null&&Utils.isNetworkOn())
        	{//!FileSharing.recvFiles.containsKey(path)
        		if(FileSharing.adhocreceive.contains(path))//说明是从adhoc收到的更新
        		{
        			//联网节点从adhoc接收到更新文件，要上传服务器，上传成功后会写进server表
        			System.out.println("更新的第三种情况，从adhoc收到更新文件");
        			//判断是从adhoc接收的文件后，从adhocreceive中删除
        			FileSharing.adhocreceive.clear();
        			listener.onCachedFiledChanged(account, cachedFile, file,true);
        		}
        		else 
        		{
        			 if (TransferManager.isreqdown) 
        	        {   //从服务器下载的更新，不在发送给服务器，当从服务器下载更新成功后会写进local表和server表，并adhoc广播
        	            Log.d(DEBUG_TAG, "ignore change signal for recent downloaded file " + path);
        	            System.out.println("更新的第一种情况，从服务器下载更新");
        	            TransferManager.isreqdown=false;
        	            //return;
        	        }
        			
        			//联网节点自己更新了文件，要将这个文件写进local表，并广播出去，并上传服务器，上传成功会写进server表
        			else
        			{
        			System.out.println("更新的第二种情况，联网节点自己更新了文件");
        			//用于测试
        	     	   Map<String,Long> map=new HashMap<String,Long>();
        	   		   map=FileSharing.getFileModified();
        	   		   System.out.println("显示local表中的内容@@@@");
        	   		   BrowserActivity.show(map);
        	   		 //用于测试
        	   		FileSharing.addToQueue(path, curr_time, false,false);
        	   	    FileSharing.netupdate=false;
        	   		listener.onCachedFiledChanged(account, cachedFile, file,true);
        			}
        	   		
        		}
        	//}
        }
       
    }
    /**
     * 判断有网后，发送到服务器
     * @param filename：文件名
     */
    public  void sendToserver(String filename,Account account)
    {
    	 SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
    	 Date curDate = new Date(System.currentTimeMillis());
    	 String send_serverTime = formatter.format(curDate); 	
    	 FileSharing. writeLog(""+"\r\n");
    	 FileSharing.writeLog("%%%联网节点从3g发送"+filename+",	");
    	 FileSharing.writeLog(System.currentTimeMillis()+",	");
    	 FileSharing.writeLog(send_serverTime+",	"+"\r\n");
    	  
    	 if(Utils.isNetworkOn()&&account!=null)
		 {
    		 boolean isdownload=SeafileObserver.watchedFiles.containsKey(filename);  //文件是否是下载得到的
    		 System.out.println("是否是下载的："+isdownload);
    		 if(isdownload==false&&!BrowserActivity.upLoadFiles.contains(filename)) //不是主动要求上传的文件
		        {
		        DatabaseHelper dbHelper;
		        dbHelper = DatabaseHelper.getDatabaseHelper();
		        ArrayList<String[]> infos= new ArrayList<String[]>();
		        infos= dbHelper.getAllRepoDirInfo(account);
		        if(infos!=null)
		        {
		          System.out.println("列出所有的仓库信息!!!!!!!!!!!!!!");
		          for(int i=0;i<infos.size();i++)
		          {
		     	    System.out.println(infos.get(i)[0]+"    "+infos.get(i)[1]+"    "+infos.get(i)[2]);
		          }
		          System.out.println("!!!!!!!!!!!!!!");
		        }
		        else 
		        {
		        	 System.out.println("根据当前的account找不到本地缓存的仓库信息");
		        }
		          String[]filepath=filename.split("/");
		          String repodir=filepath[0]+"/"+filepath[1]+"/"+filepath[2]+"/"+filepath[3]+"/"+filepath[4]+"/"+filepath[5];
		          System.out.println("repodirfrom path： "+repodir);
		          String[] dir=new String[2];   
		          dir=dbHelper.getRepoDirInfo(account, repodir); 
		          if(dir!=null)
		          {
		          String tarPath="/";
		     	  for(int j=6;j<filepath.length-1;j++)
		     	           tarPath+=filepath[j]+"/";	  
		        
		           System.out.println("repoID：  "+dir[0]);
				   System.out.println("repoName： "+dir[1]);
				   System.out.println("targetdir："+tarPath);    
				   System.out.println("llocalPath  "+filename);
				    
		        SeafCachedFile cacheInfo1 = new SeafCachedFile();
		        cacheInfo1.repoID = dir[0];
		        cacheInfo1.repoName = dir[1];
		        cacheInfo1.path = tarPath;
		        
		        if(cacheInfo1 != null) 
		        {
		        	File file=new File(filename);
		        	SeafileObserver.listener.onCachedFiledChanged(account,cacheInfo1,file,false);
		        } 
		      }
		   } 
	 }
    }
    /**
     * 判断文件是从哪个接口来的，然后采取相应的操作
     * @param filename：文件名
     */
	public void Judefiles(String filename,Account account)
	{
		
		if(BrowserActivity.ServerDownloadFiles.contains(filename)&&!FileSharing.recvFiles.containsKey(filename))
		{
			System.out.println("是从3G接口接收到的,从Adhoc发送  "+filename);
		}	
		else if(FileSharing.recvFiles.containsKey(filename)&&!BrowserActivity.ServerDownloadFiles.contains(filename))
		{
			System.out.println("从adhoc接收到的，若有网，则从3G发送");
			FileSharing.writeLog("%%%进入Judefiles的时间："+filename+",	");
	        FileSharing.writeLog(System.currentTimeMillis()+",	");
	        FileSharing.writeLog("\r\n"); 
	        
			sendToserver(filename,account);  //上传成功后发送服务端时间即可
		}
		else if(!FileSharing.recvFiles.containsKey(filename)&&!BrowserActivity.ServerDownloadFiles.contains(filename))
	   {
		    System.out.println("不是接收到的，自己本地的操作，发送到服务器，并在局域网中广播");
		    long curr_time=System.currentTimeMillis();
		    //联网节点将自己创建文件的名称和创建时间写进文件修改表，文件上传服务器后形成一个服务器版本的时间，将这个时间放进表server表里
		    FileSharing.addFileModified(filename, curr_time);//将这个时间放进local表
		  //用于测试
     	   Map<String,Long> map=new HashMap<String,Long>();
   		   map=FileSharing.getFileModified();
   		   System.out.println("显示local表中的内容@@@@");
   		   BrowserActivity.show(map);
   		 //用于测试
			FileSharing.addToQueue(filename,curr_time,false,false);  
			sendToserver(filename,account);           //上传成功后发送服务器时间即可
	   }
	}
    @Override
    public void onFileCreate(File file)
    {	
        Log.d(DEBUG_TAG, file.getPath() + " was created!");	
        System.out.println("上网节点创建了文件");
        String path = file.getPath();
        FileSharing.adhocreceive.clear();
        Judefiles(path,account);
    }

    @Override
    public void onFileDelete(File file) 
    {
        Log.d(DEBUG_TAG, file.getPath() + " was deleted!");
        if(BrowserActivity.ServerDownloadFiles.contains(file.getPath()))
		{
        	BrowserActivity.ServerDownloadFiles.remove(file.getPath());
		}
        if(BrowserActivity.upLoadFiles.contains(file.getPath())) 
        {
        	BrowserActivity.upLoadFiles.remove(file.getPath());
        }
        //从相应的文件缓存列表中删除
          
         if(watchedFiles.containsKey(file.getPath()))
         {
        	 SeafCachedFile sce= watchedFiles.get(file.getPath()); 
        	 dbHelper=DatabaseHelper.getDatabaseHelper();
        	 dbHelper.deleteFileCacheItem(sce);
         }
        //停止监听该文件
        SeafileObserver.watchedFiles.remove(file.getPath());
       fileShare.deleteRecord(file.getPath());
//       fileShare.deleteFileModified(file.getPath());
    }

    @Override
    public void onStart(FileAlterationObserver fao) {
        Log.v(DEBUG_TAG, fao.toString() + " start checking event!");
    }

    @Override
    public void onStop(FileAlterationObserver fao) {
        Log.v(DEBUG_TAG, fao.toString() + " finished checking event!");
    }

    /**
     * When user downloads a file, the outdated file is replaced, so the onFileChange signal would
     * be triggered（触发）, which we should not treat it as a modification. This class provides a work aroud
     * for this.
     */
    private static class RecentDownloadedFilesWorkAround 
    {
        private static final Map<String, Long> recentDownloadedFiles = Maps.newConcurrentMap();

        public  boolean isRecentDownloadedFiles(String filePath) 
        {
            Long timestamp = recentDownloadedFiles.get(filePath);
            System.out.println("timestamp:"+timestamp);
            
            if (timestamp != null) {
                long timeWhenDownloaded = timestamp;
                long now = Utils.now();
                System.out.println("now:"+now);
                if (now - timeWhenDownloaded < 10000) 
                {
                    return true;
                }
            }

            return false;
        }

        public void addRecentDownloadedFile(String filePath) 
        {
            recentDownloadedFiles.put(filePath, Utils.now());
            System.out.println("李丹霞2下载成功加入recentdown:"+Utils.now());
            
        }
    }
    /**
     * 文件下载成功后，获取文件在服务器上的时间server_time，并将文件+server_time广播到无线网中
     * @author PC
     *
     */
    public static class downloadFromServer extends Thread
    {
    	public int taskID=0;
    	
    	public  String filename="";
    	public  String AbsoluteFilePath="";
    	public long m_time=0;
    	public  List<SeafDirent> seafDirents=null;
    	downloadFromServer(int taskID)
    	{
    		this.taskID=taskID;
    		dbHelper = DatabaseHelper.getDatabaseHelper();
    	}
    	public void run()
    	{
    		
    		System.out.println("downloadFromServer--taskID:    "+taskID);
     	   DownloadTaskInfo downInfo=BrowserActivity.txService.getDownloadTaskInfo(taskID);  		  
     	   System.out.println("downInfo： "+downInfo);	
     	   System.out.println("downInfo.pathInRepo:  "+downInfo.pathInRepo);	  
     	   filename=downInfo.pathInRepo.substring(downInfo.pathInRepo.lastIndexOf("/")+1);
     	   System.out.println("filename:  "+filename);
           String filepath=downInfo.pathInRepo.substring(0,downInfo.pathInRepo.lastIndexOf("/")+1);
           System.out.println("filepath:  "+filepath);
           
           SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
			Date curDate = new Date(System.currentTimeMillis());
			String m_time = formatter.format(curDate);
           FileSharing.writeLog(""+"\r\n");
           FileSharing.writeLog("%%%联网节点recving from server-"+filename+",	");
           FileSharing.writeLog(System.currentTimeMillis()+",	");
           FileSharing.writeLog(m_time+",	"+"\r\n");
           System.out.println("%%%联网节点recving from server-"+filename+",	"+m_time);
            
           
            try {
    			dataManager.getDirentsFromServer(downInfo.repoID, filepath);
    		  } catch (SeafException e) 
    		  {
    		   System.out.println(e.getMessage());
    			e.printStackTrace();
    		 } 
            seafDirents=dataManager.getCachedDirents(downInfo.repoID, filepath);
            
            String[] front_path=downInfo.localFilePath.split("/");
            String pp="";
            for(int i=0;i<4;i++)
            	pp+="/"+front_path[i];  
            
           // AbsoluteFilePath=pp+"/"+downInfo.repoName+downInfo.pathInRepo;   
           
            AbsoluteFilePath=downInfo.localFilePath;
            System.out.println("@@@@@@ AbsoluteFilePath "+downInfo.localFilePath);       
            long time=0;
            for(int i=0;i<seafDirents.size();i++)
            {
          	  if(seafDirents.get(i).name.equals(filename)&&seafDirents.get(i).type==SeafDirent.DirentType.FILE)
          	  {
          		  time=seafDirents.get(i).mtime;
          		  break;
          	  } 
            } 
            
            Date d = new Date(time*1000);
            SimpleDateFormat fmt = new SimpleDateFormat("HH-mm-ss-SSS");
            String zhuan_time=fmt.format(d);
            System.out.println("服务器文件File.time:  "+zhuan_time);
            //文件从3G下载成功后，将文件服务器时间放进local表和server表
            FileSharing.addFileModified(AbsoluteFilePath,time*1000+150000); 
            //itemtime=time*1000+150000;
            dbHelper.saveFileServerTable(AbsoluteFilePath,time*1000+150000);
          //用于显示local表的内容
     	   Map<String,Long> map=new HashMap<String,Long>();
   		   map=FileSharing.getFileModified();
   		   System.out.println("显示local表中的内容@@@@");
   		   BrowserActivity.show(map);
   		  //用于显示local表的内容
   		   //用于显示server表的内容
   		   Map<String,Long> maps=new HashMap<String,Long>();
   		   maps=BrowserActivity.getServerTable();
   		   System.out.println("显示server表中的内容@@@@");
   		   BrowserActivity.show(maps);
   		  //用于显示server表的内容
            System.out.println("downloadFromServer---" +AbsoluteFilePath);
            //if (recentDownloadedFiles.isRecentDownloadedFiles(AbsoluteFilePath)) 
            //{
            	//System.out.println("###只要是更新就会进入这里");
            	//FileSharing.netupdate=true;
            //}
             if(BrowserActivity.downIsLast==true)
             {
            	 FileSharing.addToQueue(AbsoluteFilePath, time*1000+150000, false,true); 
            	 //发送完将这个标志改回来，以便下次用,也就是说一个节点找到最后一个文件把它放进addtoqueue后，downislast标志
            	 //就可以该回来了，因为一个节点只有一个最后一个文件
            	 BrowserActivity.downIsLast=false;
             }
             else
             {
            	 FileSharing.addToQueue(AbsoluteFilePath, time*1000+150000, false,false);
             }
             FileSharing.netupdate=false;
            
    	}
    	
    }
    /**
     * 文件上传成功后，获取服务器上的时间，并在可联网节点的本部数据表中存储该文件对应的server_time
     * @author PC
     *
     */
    
    public static class uploadFromServer extends Thread
    {
    	public int taskID=0;
    	
    	public String filename="";
    	public String AbsoluteFilePath="";
    	public long m_time=0;
    	public List<SeafDirent> seafDirents=null; 
    	uploadFromServer(int taskID)
    	{
    		this.taskID=taskID;
    		dbHelper = DatabaseHelper.getDatabaseHelper();
    	}
    	public void run()
    	{
    		System.out.println("uploadFromServer--taskID:    "+taskID);
    		UploadTaskInfo upInfo= BrowserActivity.txService.getUploadTaskInfo(taskID);
    		System.out.println("upInfo：  "+upInfo);
            System.out.println("upInfo.parentDir:  "+upInfo.parentDir);
    		System.out.println("upInfo.localFilePath:  "+upInfo.localFilePath);
    	 try {
    		 dataManager.getDirentsFromServer(upInfo.repoID, upInfo.parentDir);
    		 } catch (SeafException e) 
    		 {
    		    System.out.println(e.getMessage());
    		    e.printStackTrace();
    		 }
    	 filename=upInfo.localFilePath.substring(upInfo.localFilePath.lastIndexOf("/")+1); 
         seafDirents=dataManager.getCachedDirents(upInfo.repoID, upInfo.parentDir);
         AbsoluteFilePath=upInfo.localFilePath;
        
   //      System.out.println("filiename:  "+filename);
   //      System.out.println(" AbsoluteFilePath: 是否为文件的绝对路径？ "+AbsoluteFilePath);     
         long time=0;
         if(seafDirents!=null)
         {
         for(int i=0;i<seafDirents.size();i++)
         {
       	  if(seafDirents.get(i).name.equals(filename)&&seafDirents.get(i).type==SeafDirent.DirentType.FILE)
       	  {
       		 time=seafDirents.get(i).mtime;
       		 System.out.println("服务器File.time-时间戳:  "+time);
       		  break;
       	  } 
         } 
         }
         Date d = new Date(time*1000);
         SimpleDateFormat fmt = new SimpleDateFormat("HH-mm-ss-SSS");
        FileSharing.writeLog(""+"\r\n");
        FileSharing.writeLog("%%%上传成功后server recving-time "+filename+",	"); 
        FileSharing.writeLog(time+",	"+", "); 
        FileSharing.writeLog(fmt.format(d)+"\r\n");
     
       // FileSharing.addFileModified(filename,localTime,time);
        System.out.println("服务器File.time-转换后的时间:  "+fmt.format(d));
        
        
        System.out.println("上传到服务器，文件的路径  "+upInfo.localFilePath);  
      //上传成功后，将文件在服务器的时间写进服务器表
        dbHelper.saveFileServerTable(upInfo.localFilePath, time*1000+150000);
        //用于显示server表的内容
		   Map<String,Long> maps=new HashMap<String,Long>();
		   maps=BrowserActivity.getServerTable();
		   System.out.println("显示server表中的内容@@@@");
		   BrowserActivity.show(maps);
		  //用于显示server表的内容
        
    	}
    }
    public static class seafile_TransReceiver extends BroadcastReceiver 
    {

        public seafile_TransReceiver() {}
        public downloadFromServer ds=null;
        public uploadFromServer us=null;
        public void onReceive(Context context, Intent intent)
        {
            String type = intent.getStringExtra("type");
           if(type.equals(TransferService.BROADCAST_FILE_DOWNLOAD_SUCCESS)) 
            {
        	   System.out.println("!!!!!!!SeafileObserve---收到文件已经成功下载的广播消息"); 
        	   int taskID = intent.getIntExtra("taskID", 0);
        	   ds=new downloadFromServer(taskID);
        	   ds.start();
            } 
           else if (type.equals(TransferService.BROADCAST_FILE_UPLOAD_SUCCESS))
            {
            	System.out.println("SeafileObserve----收到文件已成功上传的广播消息");
                int taskID = intent.getIntExtra("taskID", 0);
         	    us=new uploadFromServer(taskID);
         	    us.start();
             } 
        }
    } // TransferReceiver
    
}
