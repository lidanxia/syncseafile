package com.seafile.seadroid2.transfer;

import java.io.File;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.seafile.seadroid2.Utils;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.transfer.TransferManager.DownloadTaskInfo;
import com.seafile.seadroid2.transfer.TransferManager.TransferListener;
import com.seafile.seadroid2.transfer.TransferManager.UploadTaskInfo;
/**
 * 文件传输的服务，并将上传或下载文件的相关信息广播到应用程序中
 * 其中大部分是调用TransferManager的方法
 * 实现了TransferListener接口的方法：主要是将文件的状态信息广播到应用程序中
 * @author PC
 *
 */
public class TransferService extends Service implements TransferListener {

	@SuppressWarnings("unused")
	private static final String DEBUG_TAG = "TransferService";

	public static final String BROADCAST_ACTION = "com.seafile.seadroid.TX_BROADCAST";

	private final IBinder mBinder = new TransferBinder();
	private TransferManager txManager;

	public static final String BROADCAST_FILE_DOWNLOAD_SUCCESS = "downloaded";
	public static final String BROADCAST_FILE_DOWNLOAD_FAILED = "downloadFailed";
	public static final String BROADCAST_FILE_DOWNLOAD_PROGRESS = "downloadProgress";

	public static final String BROADCAST_FILE_UPLOAD_SUCCESS = "uploaded";
	public static final String BROADCAST_FILE_UPLOAD_FAILED = "uploadFailed";
	public static final String BROADCAST_FILE_UPLOAD_PROGRESS = "uploadProgress";
	public static final String BROADCAST_FILE_UPLOAD_CANCELLED = "uploadCancelled";
/**
 *  为TransferManager txManager加了TransferListener
 */
	@Override
	public void onCreate() {
		txManager = new TransferManager();
		txManager.setListener(this);

	}
	/**
	 *  去掉TransferManager txManager的TransferListener
	 */
	@Override
	public void onDestroy() {
		Log.d(DEBUG_TAG, "onDestroy");
		txManager.unsetListener();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{

		return START_STICKY;
	}

	public class TransferBinder extends Binder
	{
		public TransferService getService() 
		{
			return TransferService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		// Log.d(DEBUG_TAG, "onBind");
		return mBinder;
	}
/**
 * 
 * @param account
 * @param repoID
 * @param repoName
 * @param dir
 * @param filePath
 * @param isUpdate
 * @return 调用TransferManager的addUploadTask（）方法，该方法中会执行上传线程
 */
	public int addUploadTask(Account account, String repoID, String repoName,
			String dir, String filePath, boolean isUpdate) 
	{
		return txManager.addUploadTask(account, repoID, repoName, dir,
				filePath, isUpdate);
	}
/**
 * 
 * @param account
 * @param repoName
 * @param repoID
 * @param path
 * @return 调用TransferManager的addDownloadTask方法
 */
	public int addDownloadTask(Account account, String repoName, String repoID,
			String path) 
	{
		return txManager.addDownloadTask(account, repoName, repoID, path);
	}
/**
 * 
 * @param taskID
 * @return 调用TransferManager的getUploadTaskInfo(taskID)方法
 */
	public UploadTaskInfo getUploadTaskInfo(int taskID) 
	{
		return txManager.getUploadTaskInfo(taskID);
	}
/**
 * 
 * @return 调用TransferManager的getAllUploadTaskInfos()方法
 */
	public List<UploadTaskInfo> getAllUploadTaskInfos() 
	{
		return txManager.getAllUploadTaskInfos();
	}
/**
 * 通过调用TransferManager的removeUploadTask(taskID)方法实现删除上传任务的功能
 * @param taskID
 */
	public void removeUploadTask(int taskID) 
	{
		txManager.removeUploadTask(taskID);
	}
/**
 * 通过调用TransferManager的removeFinishedUploadTasks()方法实现功能
 */
	public void removeFinishedUploadTasks() {
		txManager.removeFinishedUploadTasks();
	}
/**
 * 调用TransferManager的cancelUploadTask(taskID);
 * @param taskID
 */
	public void cancelUploadTask(int taskID) {
		txManager.cancelUploadTask(taskID);
	}
/**
 * 调用TransferManager的retryUploadTask(taskID);
 * @param taskID
 */
	public void retryUploadTask(int taskID) {
		txManager.retryUploadTask(taskID);
	}
/**
 * 
 * @param taskID
 * @return 调用TransferManager的getDownloadTaskInfo(taskID);
 */
	public DownloadTaskInfo getDownloadTaskInfo(int taskID) 
	{
		return txManager.getDownloadTaskInfo(taskID);
	}
/**
 * 在应用程序中广播文件上传的进度
 */
	@Override
	public void onFileUploadProgress(int taskID)
	{
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_UPLOAD_PROGRESS).putExtra("taskID", taskID);
	//	localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		//在本应用程序中广播
	}
	/**
	 * 在应用程序中广播文件已经上传成功
	 */
	@Override
	public void onFileUploaded(int taskID) 
	{
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_UPLOAD_SUCCESS).putExtra("taskID", taskID);
	//	localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		System.out.println("TransferService--广播“文件已经上传成功”");
	}
	/**
	 * 在应用程序中广播文件已经上传取消
	 */
	@Override
	public void onFileUploadCancelled(int taskID) {
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_UPLOAD_CANCELLED).putExtra("taskID", taskID);
	//	localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	@Override
	public void onFileUploadFailed(int taskID) {
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_UPLOAD_FAILED).putExtra("taskID", taskID);
	//	localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}
	/**
	 * 在应用程序中广播文件下载的进度
	 */
	@Override
	public void onFileDownloadProgress(int taskID) {
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_DOWNLOAD_PROGRESS).putExtra("taskID", taskID);
	//	localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	@Override
	public void onFileDownloaded(int taskID) {
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_DOWNLOAD_SUCCESS).putExtra("taskID", taskID);
		//localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		System.out.println("TransferService--广播“文件已经下载成功”");
	}

	@Override
	public void onFileDownloadFailed(int taskID) {
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_DOWNLOAD_FAILED).putExtra("taskID", taskID);
		//localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	public void cancelDownloadTask(int taskID) {
		txManager.cancelDownloadTask(taskID);
	}
}

interface TransferDBHelper
{
	void saveUploadTaskInfo();

	void removeUploadTaskInfo();

	List<UploadTaskInfo> getUploadTaskInfoList();
}

interface UpdateTaskListener 
{
    void onTaskSuccess(UploadTaskInfo info);
    void onTaskFailed(UploadTaskInfo info);
}

/**
 * 定期的传输任务
 * Retries to auto update changed files util the update succeeds.
 */
class PersistentTransferScheduler implements UpdateTaskListener 
{
	TransferDBHelper helper;
	TransferService service;

    public void addPersistentUpdateTask() {
    }

    @Override
    public void onTaskFailed(UploadTaskInfo info) {
    }

    @Override
    public void onTaskSuccess(UploadTaskInfo info) 
    {
    }
/**
 * 如果网络可用，则调用TransferManger的addUploadTask方法
 */
	public void callback()
	{
		if (!Utils.isNetworkOn()) 
		{
			return;
		}

		for (UploadTaskInfo info : helper.getUploadTaskInfoList())
		{
			Account account = null;
            File file = new File(info.localFilePath);
            if (!file.exists())
            {
                continue;
            }

			service.addUploadTask(account, info.repoID, info.repoName,
					info.parentDir, info.localFilePath, true);
		}
	}
}
