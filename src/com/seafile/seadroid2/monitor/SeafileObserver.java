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
 * FileAlterationObserver,����Ŀ¼�ڵ��ļ����ļ��仯�ļ�����ӿ�FileAlterationListener
 * @author PC
 * ��ش�account�����л����ļ���CachedFileChangedListenerΪ�����
 * account
 * CachedFileChangedListener
 */

public class SeafileObserver implements FileAlterationListener
{
    private final String DEBUG_TAG = "SeafileObserver";
    private Account account;
    public static DataManager dataManager;
    //�ϴ�������ʱ��Ϊȫ�ֱ�������ʼֵΪ0��
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
         //���ü�����Ŀ¼��/sdcard/Seafile
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
 * ��dataManager���д�����л����ļ�������watchedFiles��
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
 * �����ص��ļ����뵽���MAP��
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
     * ���ϴ����ļ����뵽���MAP��
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
 * ��ص��ļ��ı��� ���ͻᴥ���ú��������Ǹú���û�в�ȡ�κβ���
 */
    @Override
    public void onFileChange(File file) 
    {
        String path = file.getPath();
        SeafCachedFile cachedFile = watchedFiles.get(path);
        FileSharing.netupdate=true;
        long curr_time=System.currentTimeMillis();
       /* if (recentDownloadedFiles.isRecentDownloadedFiles(path)) 
        {   //�ӷ��������صĸ��£����ڷ��͸������������ӷ��������ظ��³ɹ����д��local���server����adhoc�㲥
            Log.d(DEBUG_TAG, "ignore change signal for recent downloaded file " + path);
            System.out.println("���µĵ�һ��������ӷ��������ظ���");
            
            //return;
        }*/
        
        //else
        //{
        	if(cachedFile != null&&Utils.isNetworkOn())
        	{//!FileSharing.recvFiles.containsKey(path)
        		if(FileSharing.adhocreceive.contains(path))//˵���Ǵ�adhoc�յ��ĸ���
        		{
        			//�����ڵ��adhoc���յ������ļ���Ҫ�ϴ����������ϴ��ɹ����д��server��
        			System.out.println("���µĵ������������adhoc�յ������ļ�");
        			//�ж��Ǵ�adhoc���յ��ļ��󣬴�adhocreceive��ɾ��
        			FileSharing.adhocreceive.clear();
        			listener.onCachedFiledChanged(account, cachedFile, file,true);
        		}
        		else 
        		{
        			 if (TransferManager.isreqdown) 
        	        {   //�ӷ��������صĸ��£����ڷ��͸������������ӷ��������ظ��³ɹ����д��local���server����adhoc�㲥
        	            Log.d(DEBUG_TAG, "ignore change signal for recent downloaded file " + path);
        	            System.out.println("���µĵ�һ��������ӷ��������ظ���");
        	            TransferManager.isreqdown=false;
        	            //return;
        	        }
        			
        			//�����ڵ��Լ��������ļ���Ҫ������ļ�д��local�����㲥��ȥ�����ϴ����������ϴ��ɹ���д��server��
        			else
        			{
        			System.out.println("���µĵڶ�������������ڵ��Լ��������ļ�");
        			//���ڲ���
        	     	   Map<String,Long> map=new HashMap<String,Long>();
        	   		   map=FileSharing.getFileModified();
        	   		   System.out.println("��ʾlocal���е�����@@@@");
        	   		   BrowserActivity.show(map);
        	   		 //���ڲ���
        	   		FileSharing.addToQueue(path, curr_time, false,false);
        	   	    FileSharing.netupdate=false;
        	   		listener.onCachedFiledChanged(account, cachedFile, file,true);
        			}
        	   		
        		}
        	//}
        }
       
    }
    /**
     * �ж������󣬷��͵�������
     * @param filename���ļ���
     */
    public  void sendToserver(String filename,Account account)
    {
    	 SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
    	 Date curDate = new Date(System.currentTimeMillis());
    	 String send_serverTime = formatter.format(curDate); 	
    	 FileSharing. writeLog(""+"\r\n");
    	 FileSharing.writeLog("%%%�����ڵ��3g����"+filename+",	");
    	 FileSharing.writeLog(System.currentTimeMillis()+",	");
    	 FileSharing.writeLog(send_serverTime+",	"+"\r\n");
    	  
    	 if(Utils.isNetworkOn()&&account!=null)
		 {
    		 boolean isdownload=SeafileObserver.watchedFiles.containsKey(filename);  //�ļ��Ƿ������صõ���
    		 System.out.println("�Ƿ������صģ�"+isdownload);
    		 if(isdownload==false&&!BrowserActivity.upLoadFiles.contains(filename)) //��������Ҫ���ϴ����ļ�
		        {
		        DatabaseHelper dbHelper;
		        dbHelper = DatabaseHelper.getDatabaseHelper();
		        ArrayList<String[]> infos= new ArrayList<String[]>();
		        infos= dbHelper.getAllRepoDirInfo(account);
		        if(infos!=null)
		        {
		          System.out.println("�г����еĲֿ���Ϣ!!!!!!!!!!!!!!");
		          for(int i=0;i<infos.size();i++)
		          {
		     	    System.out.println(infos.get(i)[0]+"    "+infos.get(i)[1]+"    "+infos.get(i)[2]);
		          }
		          System.out.println("!!!!!!!!!!!!!!");
		        }
		        else 
		        {
		        	 System.out.println("���ݵ�ǰ��account�Ҳ������ػ���Ĳֿ���Ϣ");
		        }
		          String[]filepath=filename.split("/");
		          String repodir=filepath[0]+"/"+filepath[1]+"/"+filepath[2]+"/"+filepath[3]+"/"+filepath[4]+"/"+filepath[5];
		          System.out.println("repodirfrom path�� "+repodir);
		          String[] dir=new String[2];   
		          dir=dbHelper.getRepoDirInfo(account, repodir); 
		          if(dir!=null)
		          {
		          String tarPath="/";
		     	  for(int j=6;j<filepath.length-1;j++)
		     	           tarPath+=filepath[j]+"/";	  
		        
		           System.out.println("repoID��  "+dir[0]);
				   System.out.println("repoName�� "+dir[1]);
				   System.out.println("targetdir��"+tarPath);    
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
     * �ж��ļ��Ǵ��ĸ��ӿ����ģ�Ȼ���ȡ��Ӧ�Ĳ���
     * @param filename���ļ���
     */
	public void Judefiles(String filename,Account account)
	{
		
		if(BrowserActivity.ServerDownloadFiles.contains(filename)&&!FileSharing.recvFiles.containsKey(filename))
		{
			System.out.println("�Ǵ�3G�ӿڽ��յ���,��Adhoc����  "+filename);
		}	
		else if(FileSharing.recvFiles.containsKey(filename)&&!BrowserActivity.ServerDownloadFiles.contains(filename))
		{
			System.out.println("��adhoc���յ��ģ������������3G����");
			FileSharing.writeLog("%%%����Judefiles��ʱ�䣺"+filename+",	");
	        FileSharing.writeLog(System.currentTimeMillis()+",	");
	        FileSharing.writeLog("\r\n"); 
	        
			sendToserver(filename,account);  //�ϴ��ɹ����ͷ����ʱ�伴��
		}
		else if(!FileSharing.recvFiles.containsKey(filename)&&!BrowserActivity.ServerDownloadFiles.contains(filename))
	   {
		    System.out.println("���ǽ��յ��ģ��Լ����صĲ��������͵������������ھ������й㲥");
		    long curr_time=System.currentTimeMillis();
		    //�����ڵ㽫�Լ������ļ������ƺʹ���ʱ��д���ļ��޸ı��ļ��ϴ����������γ�һ���������汾��ʱ�䣬�����ʱ��Ž���server����
		    FileSharing.addFileModified(filename, curr_time);//�����ʱ��Ž�local��
		  //���ڲ���
     	   Map<String,Long> map=new HashMap<String,Long>();
   		   map=FileSharing.getFileModified();
   		   System.out.println("��ʾlocal���е�����@@@@");
   		   BrowserActivity.show(map);
   		 //���ڲ���
			FileSharing.addToQueue(filename,curr_time,false,false);  
			sendToserver(filename,account);           //�ϴ��ɹ����ͷ�����ʱ�伴��
	   }
	}
    @Override
    public void onFileCreate(File file)
    {	
        Log.d(DEBUG_TAG, file.getPath() + " was created!");	
        System.out.println("�����ڵ㴴�����ļ�");
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
        //����Ӧ���ļ������б���ɾ��
          
         if(watchedFiles.containsKey(file.getPath()))
         {
        	 SeafCachedFile sce= watchedFiles.get(file.getPath()); 
        	 dbHelper=DatabaseHelper.getDatabaseHelper();
        	 dbHelper.deleteFileCacheItem(sce);
         }
        //ֹͣ�������ļ�
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
     * be triggered��������, which we should not treat it as a modification. This class provides a work aroud
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
            System.out.println("�ϼ2���سɹ�����recentdown:"+Utils.now());
            
        }
    }
    /**
     * �ļ����سɹ��󣬻�ȡ�ļ��ڷ������ϵ�ʱ��server_time�������ļ�+server_time�㲥����������
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
     	   System.out.println("downInfo�� "+downInfo);	
     	   System.out.println("downInfo.pathInRepo:  "+downInfo.pathInRepo);	  
     	   filename=downInfo.pathInRepo.substring(downInfo.pathInRepo.lastIndexOf("/")+1);
     	   System.out.println("filename:  "+filename);
           String filepath=downInfo.pathInRepo.substring(0,downInfo.pathInRepo.lastIndexOf("/")+1);
           System.out.println("filepath:  "+filepath);
           
           SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
			Date curDate = new Date(System.currentTimeMillis());
			String m_time = formatter.format(curDate);
           FileSharing.writeLog(""+"\r\n");
           FileSharing.writeLog("%%%�����ڵ�recving from server-"+filename+",	");
           FileSharing.writeLog(System.currentTimeMillis()+",	");
           FileSharing.writeLog(m_time+",	"+"\r\n");
           System.out.println("%%%�����ڵ�recving from server-"+filename+",	"+m_time);
            
           
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
            System.out.println("�������ļ�File.time:  "+zhuan_time);
            //�ļ���3G���سɹ��󣬽��ļ�������ʱ��Ž�local���server��
            FileSharing.addFileModified(AbsoluteFilePath,time*1000+150000); 
            //itemtime=time*1000+150000;
            dbHelper.saveFileServerTable(AbsoluteFilePath,time*1000+150000);
          //������ʾlocal�������
     	   Map<String,Long> map=new HashMap<String,Long>();
   		   map=FileSharing.getFileModified();
   		   System.out.println("��ʾlocal���е�����@@@@");
   		   BrowserActivity.show(map);
   		  //������ʾlocal�������
   		   //������ʾserver�������
   		   Map<String,Long> maps=new HashMap<String,Long>();
   		   maps=BrowserActivity.getServerTable();
   		   System.out.println("��ʾserver���е�����@@@@");
   		   BrowserActivity.show(maps);
   		  //������ʾserver�������
            System.out.println("downloadFromServer---" +AbsoluteFilePath);
            //if (recentDownloadedFiles.isRecentDownloadedFiles(AbsoluteFilePath)) 
            //{
            	//System.out.println("###ֻҪ�Ǹ��¾ͻ��������");
            	//FileSharing.netupdate=true;
            //}
             if(BrowserActivity.downIsLast==true)
             {
            	 FileSharing.addToQueue(AbsoluteFilePath, time*1000+150000, false,true); 
            	 //�����꽫�����־�Ļ������Ա��´���,Ҳ����˵һ���ڵ��ҵ����һ���ļ������Ž�addtoqueue��downislast��־
            	 //�Ϳ��Ըû����ˣ���Ϊһ���ڵ�ֻ��һ�����һ���ļ�
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
     * �ļ��ϴ��ɹ��󣬻�ȡ�������ϵ�ʱ�䣬���ڿ������ڵ�ı������ݱ��д洢���ļ���Ӧ��server_time
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
    		System.out.println("upInfo��  "+upInfo);
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
   //      System.out.println(" AbsoluteFilePath: �Ƿ�Ϊ�ļ��ľ���·���� "+AbsoluteFilePath);     
         long time=0;
         if(seafDirents!=null)
         {
         for(int i=0;i<seafDirents.size();i++)
         {
       	  if(seafDirents.get(i).name.equals(filename)&&seafDirents.get(i).type==SeafDirent.DirentType.FILE)
       	  {
       		 time=seafDirents.get(i).mtime;
       		 System.out.println("������File.time-ʱ���:  "+time);
       		  break;
       	  } 
         } 
         }
         Date d = new Date(time*1000);
         SimpleDateFormat fmt = new SimpleDateFormat("HH-mm-ss-SSS");
        FileSharing.writeLog(""+"\r\n");
        FileSharing.writeLog("%%%�ϴ��ɹ���server recving-time "+filename+",	"); 
        FileSharing.writeLog(time+",	"+", "); 
        FileSharing.writeLog(fmt.format(d)+"\r\n");
     
       // FileSharing.addFileModified(filename,localTime,time);
        System.out.println("������File.time-ת�����ʱ��:  "+fmt.format(d));
        
        
        System.out.println("�ϴ������������ļ���·��  "+upInfo.localFilePath);  
      //�ϴ��ɹ��󣬽��ļ��ڷ�������ʱ��д����������
        dbHelper.saveFileServerTable(upInfo.localFilePath, time*1000+150000);
        //������ʾserver�������
		   Map<String,Long> maps=new HashMap<String,Long>();
		   maps=BrowserActivity.getServerTable();
		   System.out.println("��ʾserver���е�����@@@@");
		   BrowserActivity.show(maps);
		  //������ʾserver�������
        
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
        	   System.out.println("!!!!!!!SeafileObserve---�յ��ļ��Ѿ��ɹ����صĹ㲥��Ϣ"); 
        	   int taskID = intent.getIntExtra("taskID", 0);
        	   ds=new downloadFromServer(taskID);
        	   ds.start();
            } 
           else if (type.equals(TransferService.BROADCAST_FILE_UPLOAD_SUCCESS))
            {
            	System.out.println("SeafileObserve----�յ��ļ��ѳɹ��ϴ��Ĺ㲥��Ϣ");
                int taskID = intent.getIntExtra("taskID", 0);
         	    us=new uploadFromServer(taskID);
         	    us.start();
             } 
        }
    } // TransferReceiver
    
}
