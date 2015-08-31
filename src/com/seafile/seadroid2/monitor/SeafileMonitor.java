package com.seafile.seadroid2.monitor;

import java.util.List;
import java.util.Map;

import org.apache.commons.io.monitor.FileAlterationMonitor;

import android.util.Log;

import com.google.common.collect.Maps;
import com.seafile.seadroid2.SeadroidApplication;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.account.AccountManager;
import com.seafile.seadroid2.monitor.SeafileObserver.CachedFileChangedListener;
/**
 * 为账号的共享目录/sdcard/Seafile建立监控， 停止监控等
 * @author PC
 *
 */
public class SeafileMonitor {

    private static final String DEBUG_TAG = "SeafileMonitor";
    private Map<Account, SeafileObserver> observers = Maps.newHashMap();
    private FileAlterationMonitor alterationMonitor = new FileAlterationMonitor(1000);
   // private FileAlterationMonitor alterationMonitor1 = new FileAlterationMonitor(1000);
    private CachedFileChangedListener listener;
    private boolean started;

    public SeafileMonitor(CachedFileChangedListener listener)
    {
        this.listener = listener;
    }

    public boolean isStarted() {
        return started;
    }
/**
 * 为account设置监控对象，并将其保存在observers中
 * @param account
 */
    private synchronized void monitorFilesForAccount(Account account)
    {
        if (observers.containsKey(account)) 
        {
            return;
        }
        SeafileObserver fileObserver = new SeafileObserver(account, listener);
        addObserver(fileObserver);
        observers.put(account, fileObserver);
    }
/**
 * 停止监听
 * @param account
 */
    public synchronized void stopMonitorFilesForAccount(Account account)
    {
        SeafileObserver fileObserver = observers.get(account);
        removeObserver(fileObserver);
        observers.remove(account);
    }
/**
 * 将fileObserver加入到alterationMonitor中
 * @param fileObserver
 */
    private void addObserver(SeafileObserver fileObserver) 
    {
        alterationMonitor.addObserver(fileObserver.getAlterationObserver());
    }
    /**
     * 从alterationMonitor中删除fileObserver
     * @param fileObserver
     */
    private void removeObserver(SeafileObserver fileObserver)
    {
        alterationMonitor.removeObserver(fileObserver.getAlterationObserver());
    }
/**
 * 文件下载完毕时，并将其加入到监控map中
 * @param account
 * @param repoID
 * @param repoName
 * @param pathInRepo
 * @param localPath
 */
    public synchronized void onFileDownloaded(Account account, String repoID, String repoName,
            String pathInRepo, String localPath) {
        SeafileObserver observer = observers.get(account);
        System.out.println("李丹霞当文件下载成功时，加入watch");
        observer.watchDownloadedFile(repoID, repoName, pathInRepo, localPath);
    }
    /**
     * 文件上传完毕时，并将其加入到监控map中
     * @param account
     * @param repoID
     * @param repoName
     * @param pathInRepo
     * @param localPath
     */
        public synchronized void onFileuploaded(Account account, String repoID, String repoName,
                String pathInRepo, String localPath)
        {
        	System.out.println("seafile-Monitor:文件上传完毕时，并将其加入到监控map中");
            SeafileObserver observer = observers.get(account);
            if(!observer.watchedFiles.containsKey(localPath))
               observer.watchUploadedFile(repoID, repoName, pathInRepo, localPath);
        }
    private void start() throws Exception {
        if (!started) {
            alterationMonitor.start();
            started = true;
        }
    }

    public void stop() throws Exception {
        alterationMonitor.stop();
    }

    /**
     * Watch cached files for all accounts
     */
    public synchronized void monitorAllAccounts() 
    {
        List<Account> accounts =
                new AccountManager(SeadroidApplication.getAppContext()).getAccountList();

        for (Account account : accounts) 
        {
            monitorFilesForAccount(account);
           
        }

        try {
            start();
            Log.d(DEBUG_TAG, "monitor started");
        } catch (Exception e) {
            Log.w(DEBUG_TAG, "failed to start file monitor");
            throw new RuntimeException("failed to start file monitor");
        }
    }
}
