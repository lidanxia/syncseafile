package com.seafile.seadroid2;

import java.io.File;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.data.DataManager;
import com.seafile.seadroid2.transfer.TransferService;
import com.seafile.seadroid2.transfer.TransferManager.DownloadTaskInfo;
import com.seafile.seadroid2.transfer.TransferService.TransferBinder;
import com.seafile.seadroid2.ui.OpenAsDialog;
import com.seafile.seadroid2.ui.PasswordDialog;
import com.seafile.seadroid2.ui.TaskDialog;

/**
 * Display a file
 */
public class FileActivity extends SherlockFragmentActivity 
{
    private static final String DEBUG_TAG = "FileActivity";

    private TextView mFileNameText;
    private ImageView mFileIcon;
    private Button mButtonCancel;

    private TextView mProgressText;
    private ProgressBar mProgressBar;

    private String mRepoName, mRepoID, mFilePath;
    private DataManager mDataManager;
    private Account mAccount;

    private int mTaskID = -1;
    private TransferService mTransferService;
    private TransferReceiver mTransferReceiver;
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.d(DEBUG_TAG, "TransferService connected");

            TransferBinder binder = (TransferBinder) service;
            mTransferService = binder.getService();
            onTransferSericeConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        mAccount  = (Account)intent.getParcelableExtra("account");
        mRepoName = intent.getStringExtra("repoName");
        mRepoID = intent.getStringExtra("repoID");
        mFilePath = intent.getStringExtra("filePath");

        mDataManager = new DataManager(mAccount);

        setContentView(R.layout.file_activity);
        initWidgets();
        bindTransferService();  //绑定服务TransferService
    }

    @Override
    protected void onDestroy() {
        if (mTransferService != null) 
        {
            unbindService(mConnection);
            mTransferService = null;
        }

        if (mTransferReceiver != null)
        {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mTransferReceiver);
        }

        super.onDestroy();
    }

    private void initWidgets() 
    {
        mFileNameText = (TextView)findViewById(R.id.file_name);
        mFileIcon = (ImageView)findViewById(R.id.file_icon);
        mButtonCancel = (Button)findViewById(R.id.op_cancel);
        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        mProgressText = (TextView)findViewById(R.id.progress_text);

        String fileName = Utils.fileNameFromPath(mFilePath);
        mFileNameText.setText(fileName);

        // icon
        mFileIcon.setImageResource(Utils.getFileIcon(fileName));

        mButtonCancel.setOnClickListener(new View.OnClickListener() 
        {
            @Override
            public void onClick(View view)
            {
                if (mTaskID > 0) {
                    mTransferService.cancelDownloadTask(mTaskID);
                }
                finish();
            }
        });
    }

    private void startMarkdownActivity(String path) 
    {
        Intent intent = new Intent(this, MarkdownActivity.class);
        intent.putExtra("path", path);
        startActivity(intent);
    }
/**
 * 根据mRepoName, mRepoID, mFilePath在本地新建已经下载好的文件，并识别出文件的类型，然后打开文件。显示在新的activity中
 */
    private void showFile()
    {
        File file = mDataManager.getLocalRepoFile(mRepoName, mRepoID, mFilePath);
        String name = file.getName();
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase();

        if (suffix.length() == 0)
        {
            showToast(R.string.unknown_file_type);
            return;
        }

        // Open markdown files in MarkdownActivity
        if (suffix.endsWith("md") || suffix.endsWith("markdown"))
        {
            startMarkdownActivity(file.getPath());
            finish();
            overridePendingTransition(0, 0);
            return;
        }
       // mimeType  它的作用是告诉Android系统本Activity可以处理的文件的类型。
       // MimeTypeMap类是专门处理mimeType的类。 下面语句的含义：根据文件的后缀名判断文件的类型
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        Intent open = new Intent(Intent.ACTION_VIEW);
       //android打开文件，第一个参数是数据，第二个参数类型
        open.setDataAndType((Uri.fromFile(file)), mime);

        try {
            startActivity(open);
            finish();
            return;
        } catch (ActivityNotFoundException e)
        {
            new OpenAsDialog(file) {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            }.show(getSupportFragmentManager(), "OpenAsDialog");
            return;
        }

/*      String chooser_title = getString(R.string.open_with);
        Intent chooser = Intent.createChooser(open, chooser_title);

        if (open.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
            finish();
            overridePendingTransition(0, 0);
            return;
        } else {
            showToast(R.string.activity_not_found);
            return;
        }*/

    }
/**
 * 注册应用程序广播接收器
 * 广播 Intent 的发送是通过调用 Context.sendBroadcast() 、 Context.sendOrderedBroadcast() 来实现的。
 * 通常一个广播 Intent 可以被订阅了此 Intent 的多个广播接收者所接收。 
 */
    private void onTransferSericeConnected() 
    {
        // Register broadcast receiver
        IntentFilter filter = new IntentFilter(TransferService.BROADCAST_ACTION);
        mTransferReceiver = new TransferReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mTransferReceiver, filter);
        //Android应用程序是通过调用ContextWrapper类的registerReceiver函数来把广播接收器BroadcastReceiver
        //注册到ActivityManagerService中去的，而ContextWrapper类本身又借助ContextImpl类来注册广播接收器。
        mTaskID = mTransferService.addDownloadTask(mAccount, mRepoName, mRepoID, mFilePath);
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.setIndeterminate(true);
        mProgressText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void bindTransferService() 
    {
        Intent txIntent = new Intent(this, TransferService.class);
        startService(txIntent);
        Log.d(DEBUG_TAG, "start TransferService");

        // bind transfer service
        Intent bIntent = new Intent(this, TransferService.class);
        bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(DEBUG_TAG, "try bind TransferService");
    }
/**
 * 显示文件的下载进度，文件打开就会自动下载
 * @param info
 */
    private void onFileDownloadProgress(DownloadTaskInfo info)
    {
        long fileSize = info.fileSize;
        long finished = info.finished;

        mProgressBar.setIndeterminate(false);
        int percent;
        if (fileSize == 0) {
            percent = 100;
        } else {
            percent = (int)(finished * 100 / fileSize);
        }
        mProgressBar.setProgress(percent);

        String txt = Utils.readableFileSize(finished) + " / " + Utils.readableFileSize(fileSize);

        mProgressText.setText(txt);
    }
/**
 * 文件下载成功，并显示新文件
 * @param info
 */
    private void onFileDownloaded(DownloadTaskInfo info) 
    {
        mProgressBar.setVisibility(View.GONE);
        mProgressText.setVisibility(View.GONE);
        mButtonCancel.setVisibility(View.GONE);

        showFile();
    }

    private void onFileDownloadFailed(DownloadTaskInfo info) {
        mProgressBar.setVisibility(View.GONE);
        mProgressText.setVisibility(View.GONE);
        mButtonCancel.setVisibility(View.GONE);

        SeafException err = info.err;
        String fileName = Utils.fileNameFromPath(info.pathInRepo);
        if (err.getCode() == 404) {
            // file deleted
            showToast("The file \"" + fileName + "\" has been deleted");
        } else if (err.getCode() == 440) {
            handlePassword();
        } else {
            showToast("Failed to download file \"" + fileName);
        }
    }

    private void handlePassword() 
    {
        PasswordDialog passwordDialog = new PasswordDialog();
        passwordDialog.setRepo(mRepoName, mRepoID, mAccount);
        passwordDialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener()
        {
            @Override
            public void onTaskSuccess() {
                mTaskID = mTransferService.addDownloadTask(mAccount,
                                                           mRepoName,
                                                           mRepoID,
                                                           mFilePath);
            }

            @Override
            public void onTaskCancelled() 
            {
                finish();
            }
        });
        passwordDialog.show(getSupportFragmentManager(), "DialogFragment");
    }

    public void showToast(CharSequence msg) {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void showToast(int id) {
        showToast(getString(id));
    }

    private class TransferReceiver extends BroadcastReceiver
    {

        private TransferReceiver() {}

        public void onReceive(Context context, Intent intent)
        {
            if (mTaskID < 0) 
            {
                return;
            }

            String type = intent.getStringExtra("type");
            if (type == null)
            {
                return;
            }

            int taskID = intent.getIntExtra("taskID", 0);
            if (taskID != mTaskID)  //mTaskID:当前文件下载任务的id
            {
                return;
            }

            DownloadTaskInfo info = mTransferService.getDownloadTaskInfo(taskID);
            if (info == null) {
                Log.w(DEBUG_TAG, "download info is null");
            }
            if (type.equals(TransferService.BROADCAST_FILE_DOWNLOAD_PROGRESS)) {
                onFileDownloadProgress(info);
            } else if (type.equals(TransferService.BROADCAST_FILE_DOWNLOAD_SUCCESS))
            {
                onFileDownloaded(info);
            } else if (type.equals(TransferService.BROADCAST_FILE_DOWNLOAD_FAILED)) {
                onFileDownloadFailed(info);
            }
        }

    } // TransferReceiver

}
