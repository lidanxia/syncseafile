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
 * �ļ�����ķ��񣬲����ϴ��������ļ��������Ϣ�㲥��Ӧ�ó�����
 * ���д󲿷��ǵ���TransferManager�ķ���
 * ʵ����TransferListener�ӿڵķ�������Ҫ�ǽ��ļ���״̬��Ϣ�㲥��Ӧ�ó�����
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
 *  ΪTransferManager txManager����TransferListener
 */
	@Override
	public void onCreate() {
		txManager = new TransferManager();
		txManager.setListener(this);

	}
	/**
	 *  ȥ��TransferManager txManager��TransferListener
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
 * @return ����TransferManager��addUploadTask�����������÷����л�ִ���ϴ��߳�
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
 * @return ����TransferManager��addDownloadTask����
 */
	public int addDownloadTask(Account account, String repoName, String repoID,
			String path) 
	{
		return txManager.addDownloadTask(account, repoName, repoID, path);
	}
/**
 * 
 * @param taskID
 * @return ����TransferManager��getUploadTaskInfo(taskID)����
 */
	public UploadTaskInfo getUploadTaskInfo(int taskID) 
	{
		return txManager.getUploadTaskInfo(taskID);
	}
/**
 * 
 * @return ����TransferManager��getAllUploadTaskInfos()����
 */
	public List<UploadTaskInfo> getAllUploadTaskInfos() 
	{
		return txManager.getAllUploadTaskInfos();
	}
/**
 * ͨ������TransferManager��removeUploadTask(taskID)����ʵ��ɾ���ϴ�����Ĺ���
 * @param taskID
 */
	public void removeUploadTask(int taskID) 
	{
		txManager.removeUploadTask(taskID);
	}
/**
 * ͨ������TransferManager��removeFinishedUploadTasks()����ʵ�ֹ���
 */
	public void removeFinishedUploadTasks() {
		txManager.removeFinishedUploadTasks();
	}
/**
 * ����TransferManager��cancelUploadTask(taskID);
 * @param taskID
 */
	public void cancelUploadTask(int taskID) {
		txManager.cancelUploadTask(taskID);
	}
/**
 * ����TransferManager��retryUploadTask(taskID);
 * @param taskID
 */
	public void retryUploadTask(int taskID) {
		txManager.retryUploadTask(taskID);
	}
/**
 * 
 * @param taskID
 * @return ����TransferManager��getDownloadTaskInfo(taskID);
 */
	public DownloadTaskInfo getDownloadTaskInfo(int taskID) 
	{
		return txManager.getDownloadTaskInfo(taskID);
	}
/**
 * ��Ӧ�ó����й㲥�ļ��ϴ��Ľ���
 */
	@Override
	public void onFileUploadProgress(int taskID)
	{
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_UPLOAD_PROGRESS).putExtra("taskID", taskID);
	//	localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		//�ڱ�Ӧ�ó����й㲥
	}
	/**
	 * ��Ӧ�ó����й㲥�ļ��Ѿ��ϴ��ɹ�
	 */
	@Override
	public void onFileUploaded(int taskID) 
	{
		Intent localIntent = new Intent(BROADCAST_ACTION).putExtra("type",
				BROADCAST_FILE_UPLOAD_SUCCESS).putExtra("taskID", taskID);
	//	localIntent.setAction("com.seafile");
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		System.out.println("TransferService--�㲥���ļ��Ѿ��ϴ��ɹ���");
	}
	/**
	 * ��Ӧ�ó����й㲥�ļ��Ѿ��ϴ�ȡ��
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
	 * ��Ӧ�ó����й㲥�ļ����صĽ���
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
		System.out.println("TransferService--�㲥���ļ��Ѿ����سɹ���");
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
 * ���ڵĴ�������
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
 * ���������ã������TransferManger��addUploadTask����
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
