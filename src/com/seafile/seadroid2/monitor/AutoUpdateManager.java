package com.seafile.seadroid2.monitor;

import java.io.File;
import java.util.List;
import java.util.Set;

import android.os.Handler;
import android.util.Log;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.seafile.seadroid2.ConcurrentAsyncTask;
import com.seafile.seadroid2.SeafException;
import com.seafile.seadroid2.Utils;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.data.SeafCachedFile;
import com.seafile.seadroid2.monitor.SeafileObserver.CachedFileChangedListener;
import com.seafile.seadroid2.transfer.TransferService;

/**
 * Update modified files, retry until success����һ���̣߳����ڸ����ϴ����񣬲��ϴ�
 *
 */
public class AutoUpdateManager implements Runnable, CachedFileChangedListener 
{

    private static final String DEBUG_TAG = "AutoUpdateManager";

    private TransferService txService;
    private Thread thread;
    private volatile boolean running;
    private static final int CHECK_INTERVAL_MILLI = 3000;
    private final Handler mHandler = new Handler();

    private Set<AutoUpdateInfo> infos = Sets.newHashSet();

    private MonitorDBHelper db = MonitorDBHelper.getMonitorDBHelper();

    public void onTransferServiceConnected(TransferService txService) {
        this.txService = txService;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() 
    {
        running = false;
    }
 
    /**
     * This method is called by file monitor, so it would be executed in the file monitor thread
     */
    @Override
    public void onCachedFiledChanged(final Account account, final SeafCachedFile cachedFile,
            final File localFile,boolean isUpdate) 
    {
    	Log.d(DEBUG_TAG, "CachedFileChangedListener ::onCachedFiledChanged " + localFile.getAbsolutePath());
        addTask(account, cachedFile, localFile,isUpdate);
    }

 /**
  * ��cachedFile��ӵ�AutoUpdateInfo infos��,���浽AutoUpdateInfo���в��ϴ�
  * @param account
  * @param cachedFile
  * @param localFile
  */  
    public void addTask(Account account, SeafCachedFile cachedFile, File localFile,boolean isUpdate) 
    {

        AutoUpdateInfo info =
                new AutoUpdateInfo(account, cachedFile.repoID, cachedFile.repoName,
                        Utils.getParentPath(cachedFile.path), localFile.getPath());
       if(isUpdate==true)
       {
        synchronized (infos) 
        {
            if (infos.contains(info))
            {
                return;
            }
            infos.add(info);
        }

        db.saveAutoUpdateInfo(info);
       }

        if (!Utils.isNetworkOn() || txService == null)
        {
            return;
        }

        addUpdateTask(info,isUpdate);
    }
/**
 * ��info��Ϣ�ϴ�
 * @param info
 */
    private void addUpdateTask(final AutoUpdateInfo info,final boolean isUpdate) 
    {
        mHandler.post(new Runnable() 
        {
            @Override
            public void run() 
            {  
                txService.addUploadTask(info.account, info.repoID, info.repoName, info.parentDir,
                        info.localPath,isUpdate);
            }
        });
      
    }
    /**
     * ��infos��Ϣ�ϴ�
     * @param infos
     */
    private void addAllUploadTasks(final List<AutoUpdateInfo> infos)
    {
        mHandler.post(new Runnable() 
        {
            @Override
            public void run() {
                for (AutoUpdateInfo info : infos) 
                {
                    txService.addUploadTask(info.account, info.repoID, info.repoName,
                            info.parentDir, info.localPath, true);
                }
            }
        });
    }

    /**
     * This callback in called in the main thread when the transfer service broadcast is received
     * 
     * ��AutoUpdateInfo info��AutoUpdateInfo����ɾ��
     */
    public void onFileUpdateSuccess(Account account, String repoID, String repoName,
            String parentDir, String localPath) 
    {
        final AutoUpdateInfo info =
                new AutoUpdateInfo(account, repoID, repoName, parentDir, localPath);
        boolean exist = false;

        synchronized (infos) 
        {
            exist = infos.remove(info);
        }

        if (exist) {
            Log.d(DEBUG_TAG, "auto updated " + localPath);
            ConcurrentAsyncTask.execute(new Runnable()
            {
                @Override
                public void run() {
                    db.removeAutoUpdateInfo(info);
                }
            });
        }
    }

    public void onFileUpdateFailure(Account account, String repoID, String repoName,
            String parentDir, String localPath, SeafException e) {

        if (e.getCode() / 100 != 4) {
            return;
        }

        // This file has already been removed on server, so we abort the auto update task.
        final AutoUpdateInfo info =
                new AutoUpdateInfo(account, repoID, repoName, parentDir, localPath);

        boolean exist = false;
        synchronized (infos) {
            exist = infos.remove(info);
        }

        if (exist) {
            Log.d(DEBUG_TAG, String.format("failed to auto update %s, error %s", localPath, e));
            ConcurrentAsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    db.removeAutoUpdateInfo(info);
                }
            });
        }
    }

    /**
     * Periodically �����ڣ�checks the upload tasks and schedule them to run
     **/
    private void scheduleUpdateTasks()
    {
        int size = infos.size();
        if (!Utils.isNetworkOn()) 
        {
            Log.d(DEBUG_TAG, "network is not available, " + size + " in queue");
            return;
        }

        if (txService == null)
        {
            return;
        }

        Log.v(DEBUG_TAG, String.format("check auto upload tasks, %d in queue", size));

        List<AutoUpdateInfo> infosList;
        synchronized (infos) {
            if (infos.size() == 0) {
                return;
            }
            infosList = ImmutableList.copyOf(infos);
        }

        addAllUploadTasks(infosList);
    }
   /**
    * �߳����
    */
    public void run()
    {
        synchronized (infos) 
        {
            infos.addAll(db.getAutoUploadInfos());
        }

        while (running) 
        {
            scheduleUpdateTasks();
            if (!running) 
            {
                break;
            }
            try {
                Thread.sleep(CHECK_INTERVAL_MILLI);
            } catch (final InterruptedException ignored) {
                break;
            }
        }
    }

}


class AutoUpdateInfo {
    final Account account;
    final String repoID;
    final String repoName;
    final String parentDir;
    final String localPath;

    public AutoUpdateInfo(Account account, String repoID, String repoName, String parentDir,
            String localPath) {

        this.account = account;
        this.repoID = repoID;
        this.repoName = repoName;
        this.parentDir = parentDir;
        this.localPath = localPath;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || (obj.getClass() != this.getClass()))
            return false;

        AutoUpdateInfo that = (AutoUpdateInfo) obj;

        return this.account == that.account && this.repoID == that.repoID
                && this.repoName == that.repoName && this.parentDir == that.parentDir
                && this.localPath == that.localPath;
    }

    private volatile int hashCode = 0;

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hashCode(account, repoID, repoName, parentDir, localPath);
        }

        return hashCode;
    }
}
