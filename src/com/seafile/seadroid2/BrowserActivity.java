package com.seafile.seadroid2;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

import org.apache.commons.io.monitor.FileAlterationMonitor;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.ActionProvider;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.SubMenu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.data.DataManager;
import com.seafile.seadroid2.data.DatabaseHelper;
import com.seafile.seadroid2.data.SeafCachedFile;
import com.seafile.seadroid2.data.SeafDirent;
import com.seafile.seadroid2.data.SeafRepo;
import com.seafile.seadroid2.data.SeafStarredFile;
import com.seafile.seadroid2.data.DataManager.ProgressMonitor;
import com.seafile.seadroid2.fileschooser.MultiFileChooserActivity;
import com.seafile.seadroid2.gallery.MultipleImageSelectionActivity;
import com.seafile.seadroid2.monitor.AutoUpdateManager;
import com.seafile.seadroid2.monitor.FileMonitorService;
import com.seafile.seadroid2.monitor.SeafileObserver;
import com.seafile.seadroid2.monitor.SeafileObserver.CachedFileChangedListener;
import com.seafile.seadroid2.monitor.SeafileObserver.seafile_TransReceiver;
import com.seafile.seadroid2.transfer.TransferManager.DownloadTaskInfo;
import com.seafile.seadroid2.transfer.TransferManager.UploadTaskInfo;
import com.seafile.seadroid2.transfer.TransferService;
import com.seafile.seadroid2.transfer.TransferService.TransferBinder;
import com.seafile.seadroid2.ui.AppChoiceDialog;
import com.seafile.seadroid2.ui.AppChoiceDialog.CustomAction;
import com.seafile.seadroid2.ui.FetchFileDialog;
import com.seafile.seadroid2.ui.GetShareLinkDialog;
import com.seafile.seadroid2.ui.NewDirDialog;
import com.seafile.seadroid2.ui.NewFileDialog;
import com.seafile.seadroid2.ui.OpenAsDialog;
import com.seafile.seadroid2.ui.PasswordDialog;
import com.seafile.seadroid2.ui.RenameFileDialog;
import com.seafile.seadroid2.ui.ReposFragment;
import com.seafile.seadroid2.ui.SslConfirmDialog;
import com.seafile.seadroid2.ui.StarredFragment;
import com.seafile.seadroid2.ui.TabsFragment;
import com.seafile.seadroid2.ui.TaskDialog;
import com.seafile.seadroid2.ui.TaskDialog.TaskDialogListener;
import com.seafile.seadroid2.ui.UploadTasksFragment;
import com.seafile.sharing.FileSharing;
import com.seafile.sharing.requestSyncFunction;
import com.seafile.sharing.test;
public class BrowserActivity extends SherlockFragmentActivity
        implements ReposFragment.OnFileSelectedListener, StarredFragment.OnStarredFileSelectedListener, OnBackStackChangedListener {
	
    private static final String DEBUG_TAG = "BrowserActivity";
    public static final String PKG_NAME = "com.seafile.seadroid2";
    public static final String EXTRA_REPO_NAME = PKG_NAME + ".repoName";
    public static final String EXTRA_REPO_ID = PKG_NAME + ".repoID";
    public static final String EXTRA_FILE_PATH = PKG_NAME + ".filePath";
    public static final String EXTRA_ACCOUT = PKG_NAME + ".filePath";
	public static String sharedPath="/mnt/sdcard/Seafile";
	public static ArrayList<String>exitsFiles=new ArrayList<String>();        //当前目录已经存在的文件列表
	public static ArrayList<String>ServerDownloadFiles=new ArrayList<String>();
	public static ArrayList<String>upLoadFiles=new ArrayList<String>(); //通过上传按钮上传的文件列表
	//public SDCardFileObserver fileobserver=null;
	public static ArrayList<String>sendServerfiles=new ArrayList<String>(); //已经上传到服务器的文件
	public FileSharing fi=new FileSharing();
	public test tt=new test();  //加载动态库
    public static Account account;
    NavContext navContext = null;
    public static DataManager dataManager = null;
    public static TransferService txService = null;
    TransferReceiver mTransferReceiver;
    // private boolean twoPaneMode = false;
    UploadTasksFragment uploadTasksFragment = null;
    TabsFragment tabsFragment = null;
    public static Map<String,String> reposInfo=new HashMap<String,String>();
    private String currentSelectedItem = FILES_VIEW;

    FetchFileDialog fetchFileDialog = null;

    AppChoiceDialog appChoiceDialog = null;
    
    public static Map<String,SeafDirent> serverFile=new HashMap<String,SeafDirent>();//li,用来记录服务器文件的信息
    public static boolean downIsLast=false;//li,这俩个标志还没修正
    
    
    private static final String UPLOAD_TASKS_VIEW = "UploadTasks";
    private static final String FILES_VIEW = "Files";

    private static final String LIBRARY_TAB = "Libraries";
    private static final String ACTIVITY_TAB = "Activities";
    private static final String STARRED_TAB = "Starred";

    public static final String REPOS_FRAGMENT_TAG = "repos_fragment";
    public static final String UPLOAD_TASKS_FRAGMENT_TAG = "upload_tasks_fragment";
    public static final String TABS_FRAGMENT_TAG = "tabs_main";
    public static final String ACTIVITIES_FRAGMENT_TAG = "activities_fragment";
    public static final String OPEN_FILE_DIALOG_FRAGMENT_TAG = "openfile_fragment";
    public static final String PASSWORD_DIALOG_FRAGMENT_TAG = "password_fragment";
    public static final String CHOOSE_APP_DIALOG_FRAGMENT_TAG = "choose_app_fragment";
    public static final String PICK_FILE_DIALOG_FRAGMENT_TAG = "pick_file_fragment";

    public DataManager getDataManager() 
    {
        return dataManager;
    }
    public static void  showInfo(String filename)
    {
     Toast.makeText(SeadroidApplication.context,"生成文件: "+filename,Toast.LENGTH_LONG ).show();
    }
    
    public static void  showSynInfo(String infos)
    {
      Toast.makeText(SeadroidApplication.context,infos,Toast.LENGTH_LONG ).show();
    }
    
/**
 * 等待上传的信息
 * @author PC
 *
 */
    private class PendingUploadInfo 
    {
        String repoID;
        String repoName;
        String targetDir;
        String localFilePath;
        boolean isUpdate;

        public PendingUploadInfo(String repoID, String repoName,
                                 String targetDir, String localFilePath,
                                 boolean isUpdate) 
        {
            this.repoID = repoID;
            this.repoName = repoName;
            this.targetDir = targetDir;
            this.localFilePath = localFilePath;
            this.isUpdate = isUpdate;
        }
    }
  /**
   * 若txService为Null,则添加到等待上传信息列表，否则直接更新文件
   * 点击文件的“更新按钮”会调用此函数
   * @param repoID
   * @param repoName
   * @param targetDir
   * @param localFilePath
   */
    public void addUpdateTask(String repoID, String repoName, String targetDir, String localFilePath)
    {
        if (txService != null) 
        {
            txService.addUploadTask(account, repoID, repoName, targetDir, localFilePath, true);
        } 
        else 
        {
            PendingUploadInfo info = new PendingUploadInfo(repoID, repoName, targetDir, localFilePath, true);
            pendingUploads.add(info);
        }
    }
    /**
     * 若txService为Null,则添加到等待上传信息列表，否则直接上传文件
     * @param repoID
     * @param repoName
     * @param targetDir
     * @param localFilePath
     */
    public void addUploadTask(String repoID, String repoName, String targetDir, String localFilePath) 
    {
        if (txService != null) 
        {
            txService.addUploadTask(account, repoID, repoName, targetDir, localFilePath, false);
        } 
        else {
            PendingUploadInfo info = new PendingUploadInfo(repoID, repoName, targetDir, localFilePath, false);
            pendingUploads.add(info);
        }
    }
    
    public static String getIp()
  	{ 
      	//Adhoc模式下获取IP
   // /*  
      	String networkIp = "";  
      	try {  
      	    List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());  
      	    for(NetworkInterface iface : interfaces)
      	    {  
      	        if(iface.getDisplayName().equals("adhoc0"))
      	        {  
      	            List<InetAddress> addresses = Collections.list(iface.getInetAddresses());  
      	            for(InetAddress address : addresses)
      	            {  
      	                if(address instanceof InetAddress)
      	                {  
      	                    networkIp = address.getHostAddress();
      	                } 
      	            }  
      	        }  
      	     }
      	    } catch (SocketException e)
      	    {  
      	    e.printStackTrace();  
      	    }  
      	
  		return networkIp;
    	//*/
  		
     //Wifi获取IP方式
     /*	
      	WifiManager wm=(WifiManager)SeadroidApplication.getAppContext().getSystemService(Context.WIFI_SERVICE);  
      	if(!wm.isWifiEnabled())                     //检查Wifi状态,判断其是否开启     
  		     wm.setWifiEnabled(true);  
  		 WifiInfo wi=wm.getConnectionInfo();        //获取32位整型IP地址     
  		 int IpAdd=wi.getIpAddress();
  		 String Ip=intToIp(IpAdd);                 //把整型地址转换成“*.*.*.*”地址  
  		 return Ip;    
      */
  	}  
  	public static String intToIp(int IpAdd) 
  	{  
  	    return (IpAdd & 0xFF ) + "." +  
  	    ((IpAdd >> 8 ) & 0xFF) + "." +  
  	    ((IpAdd >> 16 ) & 0xFF) + "." +  
  	    ( IpAdd >> 24 & 0xFF) ;  
  	} 

    
    
    private ArrayList<PendingUploadInfo> pendingUploads = new ArrayList<PendingUploadInfo>();

    public TransferService getTransferService() 
    {
        return txService;
    }

    public Account getAccount() {
        return account;
    }

    public NavContext getNavContext() {
        return navContext;
    }

    public void disableActionBarTitle() 
    {
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mNavTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        //当一个Activity在PAUSE时，被kill之前，它可以调用onSaveInstanceState()来保存当前activity的状态信息
        //用来保存状态信息的Bundle会同时传给两个method,即onRestoreInstanceState() and onCreate().
        // Get the message from the intent
        Intent intent = getIntent();
        String server = intent.getStringExtra("server");
        String email = intent.getStringExtra("email");
        String token = intent.getStringExtra("token");
        account = new Account(server, email, null, token);
        Log.d(DEBUG_TAG, "browser activity onCreate " + server + " " + email);
        if (server == null) 
        {
        	  
            SharedPreferences sharedPref = getSharedPreferences(AccountsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            String latest_server = sharedPref.getString(AccountsActivity.SHARED_PREF_SERVER_KEY, null);
            String latest_email = sharedPref.getString(AccountsActivity.SHARED_PREF_EMAIL_KEY, null);
            String latest_token = sharedPref.getString(AccountsActivity.SHARED_PREF_TOKEN_KEY, null);

            if (latest_server != null) 
            {
                account = new Account(latest_server, latest_email, null, latest_token);
            } 
            else 
            {
                Intent newIntent = new Intent(this, AccountsActivity.class);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(newIntent);
                finish();
                return;
            }
         
        }
        /*
        if(fileobserver==null)
        {
        fileobserver=new SDCardFileObserver(this);
        fileobserver.watchDire();  //对Seafile目录进行监听
        }
        */
        int name=0;
        if(!Utils.isNetworkOn()&&account!=null)
        {
        	//有俩个监控程序，如果可以上网的话就不启用自己写的那个监控程序了。
        	fi.MonitorDirentsAndfiles();
        }
        dataManager = new DataManager(account);
       
        navContext = new NavContext();
        fi.running();
        getSupportFragmentManager().addOnBackStackChangedListener(this);
      
        ActionBar actionBar = getSupportActionBar();
       
        actionBar.setDisplayShowTitleEnabled(false);
        unsetRefreshing();
        System.out.println("savedInstanceState!!!!:  "+savedInstanceState);
        if (savedInstanceState != null)
        {
            Log.d(DEBUG_TAG, "savedInstanceState is not null");
            tabsFragment = (TabsFragment)
                    getSupportFragmentManager().findFragmentByTag(TABS_FRAGMENT_TAG);
            uploadTasksFragment = (UploadTasksFragment)
                    getSupportFragmentManager().findFragmentByTag(UPLOAD_TASKS_FRAGMENT_TAG);

            fetchFileDialog = (FetchFileDialog)
                    getSupportFragmentManager().findFragmentByTag(OPEN_FILE_DIALOG_FRAGMENT_TAG);

            appChoiceDialog = (AppChoiceDialog)
                getSupportFragmentManager().findFragmentByTag(CHOOSE_APP_DIALOG_FRAGMENT_TAG);

            if (appChoiceDialog != null) 
            {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(appChoiceDialog);
                ft.commit();
            }

            SslConfirmDialog sslConfirmDlg = (SslConfirmDialog)
                getSupportFragmentManager().findFragmentByTag(SslConfirmDialog.FRAGMENT_TAG);

            if (sslConfirmDlg != null) 
            {
                Log.d(DEBUG_TAG, "sslConfirmDlg is not null");
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.detach(sslConfirmDlg);
                ft.commit();
            } 
            else
            {
                Log.d(DEBUG_TAG, "sslConfirmDlg is null");
            }

            String repoID = savedInstanceState.getString("repoID");
            String repoName = savedInstanceState.getString("repoName");
            String path = savedInstanceState.getString("path");
            String dirID = savedInstanceState.getString("dirID");
            if (repoID != null)
            {
                navContext.setRepoID(repoID);
                navContext.setRepoName(repoName);
                navContext.setDir(path, dirID);
            }
        } 
        else
        {
            Log.d(DEBUG_TAG, "savedInstanceState is null");
            tabsFragment = new TabsFragment();
            uploadTasksFragment = new UploadTasksFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, tabsFragment, TABS_FRAGMENT_TAG).commit();
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, uploadTasksFragment, UPLOAD_TASKS_FRAGMENT_TAG).commit();
            getSupportFragmentManager().beginTransaction().detach(uploadTasksFragment).commit();

        }

        setContentView(R.layout.seadroid_main);

        mTitle = mDrawerTitle = getTitle();
        mNavTitles = getResources().getStringArray(R.array.nav_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mNavTitles));

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle
        		(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                )
        {
            public void onDrawerClosed(View view) 
            {
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) 
            {
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerList.setItemChecked(0, true);

        Intent txIntent = new Intent(this, TransferService.class);
        startService(txIntent);
        Log.d(DEBUG_TAG, "start TransferService");

        // bind transfer service，启动传输服务
        Intent bIntent = new Intent(this, TransferService.class);
        bindService(bIntent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(DEBUG_TAG, "try bind TransferService");
        // start service,启动监控缓存文件的服务
        if(Utils.isNetworkOn())
        {
        Intent monitorIntent = new Intent(this, FileMonitorService.class);
        startService(monitorIntent);
        }
    }

    /**
     * 获取当前窗口停在的位置，资料库，星标文件，最近修改
     * @return
     */
    private String getCurrentTabName() 
    {
        int index = tabsFragment.getCurrentTabIndex();
        switch (index)
        {
        case 0 :
            return LIBRARY_TAB;
        case 1 :
            return STARRED_TAB;
        case 2 :
            return ACTIVITY_TAB;
        default:
            return "";
        }
    }

    ServiceConnection mConnection = new ServiceConnection() 
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) 
        {
            TransferBinder binder = (TransferBinder) service;
            txService = binder.getService();
            Log.d(DEBUG_TAG, "bind TransferService");

            for (PendingUploadInfo info : pendingUploads)
            {
            	System.out.println("待上传的信息开始上传");
                txService.addUploadTask(account, info.repoID,
                                        info.repoName, info.targetDir,
                                        info.localFilePath, info.isUpdate);
            }
            pendingUploads.clear();

            if (currentSelectedItem.equals(UPLOAD_TASKS_VIEW)
                && uploadTasksFragment != null && uploadTasksFragment.isReady())
            {
            	System.out.println("刚开始进入当前目录");
                uploadTasksFragment.refreshView();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) 
        {
            txService = null;
        }
    };
/**
 * 此时注册广播接收器
 */
    @Override
    public void onStart() 
    {
        Log.d(DEBUG_TAG, "onStart");
        super.onStart();

        if (mTransferReceiver == null) 
        {
            mTransferReceiver = new TransferReceiver();
        }

        IntentFilter filter = new IntentFilter(TransferService.BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mTransferReceiver, filter);
        
        IntentFilter s_filter = new IntentFilter(TransferService.BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(SeafileObserver.sTransferReceiver, s_filter);
    }

    @Override
    protected void onPause() 
    {
        Log.d(DEBUG_TAG, "onPause");
      super.onPause();
    }

    @Override
    public void onRestart()
    {
        Log.d(DEBUG_TAG, "onRestart");
        super.onStart();
    }
/**
 * 若intent传来的account与当前登陆的账号不等，则以传来的account重新登录
 */
    @Override
    protected void onNewIntent(Intent intent) 
    {
        Log.d(DEBUG_TAG, "onNewIntent");
        String server = intent.getStringExtra("server");
        String email = intent.getStringExtra("email");

        Account selectedAccount = new Account(server, email);
        if (!account.equals(selectedAccount)) 
        {
            finish();
            startActivity(intent);
        }
    }

    @Override
    protected void onStop()
    {
        Log.d(DEBUG_TAG, "onStop");
        super.onStop();

        if (mTransferReceiver != null) 
        {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mTransferReceiver);
        }

    }
    @Override
    protected void onDestroy() 
    {
        Log.d(DEBUG_TAG, "onDestroy is called");
        fi.destroy();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        //outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
        if (navContext.getRepoID() != null) 
        {
            outState.putString("repoID", navContext.getRepoID());
            outState.putString("repoName", navContext.getRepoName());
            outState.putString("path", navContext.getDirPath());
            outState.putString("dirID", navContext.getDirID());
        }
    }
/**
 * 加载“设置”总的信息，上传，刷新按钮，新建文件/目录功能
 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.browser_menu, menu);
        return true;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);

        }
    }
//界面中设置部分：文件，上传任务，账户
  //当选择文件时，又分为：资料库，星标文件，最近修改
    /**
     * 根据选择的不同（文件，上传任务，账户），采取不同的操作
     * @param position
     */
    private void selectItem(int position) 
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        switch (position) {
        case 0 :
            ft.detach(uploadTasksFragment);
            ft.attach(tabsFragment);
            ft.commit();
            currentSelectedItem = FILES_VIEW;
            break;
        case 1 :
            ft.detach(tabsFragment);
            ft.attach(uploadTasksFragment);
            ft.commit();
            currentSelectedItem = UPLOAD_TASKS_VIEW;
            break;
        case 2 :
            ft.detach(uploadTasksFragment);
            ft.attach(tabsFragment);
            ft.commit();

            Intent newIntent = new Intent(this, AccountsActivity.class);
            newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            startActivity(newIntent);
            break;
        default:
            break;

        }

        mDrawerList.setItemChecked(position, true);
        setTitle(mNavTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);

    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }
/**
 * 设定，当用户选择某个窗口时，其它按钮的可见性
 */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem menuUpload = menu.findItem(R.id.upload);
        MenuItem menuRefresh = menu.findItem(R.id.refresh);
        MenuItem menuNewDir = menu.findItem(R.id.newdir);
        MenuItem menuNewFile = menu.findItem(R.id.newfile);

        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        if (getCurrentTabName().equals(LIBRARY_TAB) && !drawerOpen)
        {
            menuUpload.setVisible(true);
            if (navContext.inRepo() && hasRepoWritePermission()) {
                menuUpload.setEnabled(true);
            }
            else
                menuUpload.setEnabled(false);
        } else {
            menuUpload.setVisible(false);
        }

        if (getCurrentTabName().equals(LIBRARY_TAB) && !drawerOpen) {
            menuRefresh.setVisible(true);
        } else if (getCurrentTabName().equals(ACTIVITY_TAB) && !drawerOpen) {
            menuRefresh.setVisible(true);
        } else {
            menuRefresh.setVisible(false);
        }

        if (getCurrentTabName().equals(LIBRARY_TAB) && !drawerOpen) {
            if (navContext.inRepo() && hasRepoWritePermission()) {
                menuNewDir.setVisible(true);
                menuNewFile.setVisible(true);
            } else {
                menuNewDir.setVisible(false);
                menuNewFile.setVisible(false);
            }
        } else {
            menuNewDir.setVisible(false);
            menuNewFile.setVisible(false);
        }

        if (currentSelectedItem.equals(UPLOAD_TASKS_VIEW)) 
        {
            menuUpload.setVisible(false);
            menuRefresh.setVisible(false);
            menuNewDir.setVisible(false);
            menuNewFile.setVisible(false);
        }

        if (getCurrentTabName().equals(STARRED_TAB)) 
        {
            menuUpload.setVisible(false);
            menuNewDir.setVisible(false);
            menuNewFile.setVisible(false);
            if (drawerOpen) {
                menuRefresh.setVisible(false);
            } else {
                menuRefresh.setVisible(true);
            }
        }

        return true;
    }
   /** 
    * 获取仓库的最新信息
    * 第一个参数是指doInbackground接受的参数类型
    * 第二个参数定义onprogressupdate的参数
    * 第三个参数定义doinbackground返回值类型和onpostexecute的参数类型
    * @author PC
    *
    */
    public class LoadTask extends AsyncTask<String, Void, List<SeafRepo> > 
    {
        public SeafException err = null;
        public DataManager dataManager;
        public List<SeafRepo> Repos=null ;
        String isok="";
        public LoadTask(DataManager dataManager) 
        {
            this.dataManager = dataManager;
        }
        @Override
        protected List<SeafRepo> doInBackground(String... params) 
        {
            try {
            	Repos=dataManager.getReposFromServer();
            	isok="ok";
                return 	Repos;	
            } catch (SeafException e) 
            {
                err = e;
                return null;
            }
        }

        private void displaySSLError() 
        {
         
        }

        private void resend() 
        {
            ConcurrentAsyncTask.execute(new LoadTask(dataManager));
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<SeafRepo> rs)
        {
            if (err == SeafException.sslException)
            {
                SslConfirmDialog dialog = new SslConfirmDialog(dataManager.getAccount(),
                new SslConfirmDialog.Listener() 
                {
                    @Override
                    public void onAccepted(boolean rememberChoice) 
                    {
                        Account account = dataManager.getAccount();
                        CertsManager.instance().saveCertForAccount(account, rememberChoice);
                        resend();
                    }

                    @Override
                    public void onRejected() 
                    {
                        displaySSLError();
                    }
                });
                return;
            }
        }
    }
    /**
     * 获取目录的最新内容,下载某个仓库对应的目录
     * @author PC
     *
     */
    private class LoadDirTask extends AsyncTask<String, Void, List<SeafDirent> > 
    {
        SeafException err = null;
        String myRepoName;
        String myRepoID;
        String myPath;
        List<SeafDirent> dirs=null;
        String isok="";
        DataManager dataManager;

        public LoadDirTask(DataManager dataManager) 
        {
            this.dataManager = dataManager;
        }

        @Override
        protected List<SeafDirent> doInBackground(String... params)
        {
            if (params.length != 3)
            {
                Log.d(DEBUG_TAG, "Wrong params to LoadDirTask");
                return null;
            }

            myRepoName = params[0];
            myRepoID = params[1];
            myPath = params[2];
            try {
            	dirs=dataManager.getDirentsFromServer(myRepoID, myPath);
            	isok="ok";
                return dirs;
            } catch (SeafException e)
            {
                err = e;
                return null;
            }
        }

        private void resend()
        {
            NavContext nav =getNavContext();
            if (!myRepoID.equals(nav.getRepoID()) || !myPath.equals(nav.getDirPath())) 
            {
                return;
            }
            ConcurrentAsyncTask.execute(new LoadDirTask(dataManager), myRepoName, myRepoID, myPath);
        }

        private void displaySSLError()
        {     
            NavContext nav =getNavContext();
        }
        
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(List<SeafDirent> dirents)  
        {      
            dirs=dirents;
            if (err == SeafException.sslException)
            {
                SslConfirmDialog dialog = new SslConfirmDialog(dataManager.getAccount(),
                new SslConfirmDialog.Listener()
                {
                    @Override
                    public void onAccepted(boolean rememberChoice)
                    {
                        Account account = dataManager.getAccount();
                        CertsManager.instance().saveCertForAccount(account, rememberChoice);
                        resend();
                    }

                    @Override
                    public void onRejected()
                    {
                        displaySSLError();
                    }
                });
                return;
            }

            if (err != null)
            {
                if (err.getCode() == 440) 
                {
                	 Log.d(DEBUG_TAG, "err.getCode() == 440: " + err.getMessage());
                } 
                else if (err.getCode() == 404)
                {
                   showToast(String.format("The folder \"%s\" was deleted", myPath));
                } 
                else
                {
                    Log.d(DEBUG_TAG, "failed to load dirents: " + err.getMessage());
                    err.printStackTrace();
                }
                return;
            }
            if (dirents == null)
            {
                Log.i(DEBUG_TAG, "failed to load dir");
                return;
            }
          
        }
    }
    /**
     * 本地程序与服务器同步文件，具体做法是：1、获取所有的仓库信息。2、根据每个仓库的信息采取深度遍历，刷新
     * 每个目录下的信息，如果碰到的是文件直接下载，若是目录再向下遍历，刷新。
     * 由于此过程中涉及到文件的下载过程吗，文件下载后会自动广播到Adhoc中。
     * *****由于存储在本地的文件即使没网也会存储在AutoUpdateInfo表中，待有网时会上传。
     * 所以本地会将自己的文件全部上传到服务器
     * 一个文件下载完成后，才会再刷新其它目录，即主线程会等待下载线程，因为在同步时就是要与服务器同步完成后才会再与节点同步。
     * boolean waiting:等待下载线程
     */
    
    //得到服务器端目前的文件和上网结点的服务器时间表进行比较
    public Map getTableFromServer()
    {
    	DatabaseHelper dbHelper;
    	dbHelper = DatabaseHelper.getDatabaseHelper();
    	List<SeafRepo> Repos=null;
    	long time=0;
    	Map<String,Long> map=new HashMap<String,Long>();
    	//Map<String,ArrayList<String[]>> map=new HashMap<String,ArrayList<String[]>>();
    	ArrayList<String[]> list=new ArrayList<String[]>();
    	Stack <stack_data>  stack= new Stack <stack_data >();  //其中放的只是目录
        LoadTask lt=new LoadTask(dataManager);//获取到所有的仓库信息
        ConcurrentAsyncTask.execute(lt);
    	while(true)
    	{
    		if(lt.isok.equals("ok"))
    		{
    			break;
    		}
    	}
    	Repos=lt.Repos;
    	for(int i=0;i<Repos.size();i++)
    	{//将仓库信息压入栈中
    		String dir_path="/";
			stack_data sd=new stack_data(Repos.get(i).id,Repos.get(i).name,Repos.get(i).id,dir_path);
			stack.push(sd);
			System.out.println("Repos.get(i).name: "+Repos.get(i).name);
    	}
    	
    	while(!stack.isEmpty())
    	{
    		
    		stack_data parent = stack.pop(); 
    	     System.out.println("$$当前刷新的目录：  "+parent.repo_name+parent.dir_path);	
    		List<SeafDirent> dirs=null;     //获取parent包含的内容，若是dir放入stack,若是file,则直接处理
			LoadDirTask ltk=new LoadDirTask(dataManager);//下载某个仓库对应的目录
			ConcurrentAsyncTask.execute(ltk,parent.repo_name,parent.repo_id,parent.dir_path);
		    while(true)
		    {
		    	if(ltk.isok.equals("ok"))
	    		{
	    			break;
	    		}
		    }
		    dirs=ltk.dirs;
    		System.out.println("服务器——该目录下包含的文件和目录数量：  "+dirs.size());	
    		for(int j=0;j<dirs.size();j++)
			{		
				if(dirs.get(j).isDir())
				{//是目录的话
					
					String dir_path=null;
					if(parent.dir_path.equals("/"))
						dir_path=parent.dir_path+dirs.get(j).name;
					else
					    dir_path=parent.dir_path+"/"+dirs.get(j).name;
					System.out.println("目录路径  ："+ dir_path);
					stack_data sd=new stack_data(parent.repo_id,parent.repo_name,dirs.get(j).id,dir_path);
					stack.push(sd);
				}
				else 
				{//是文件的话
					String filepath=null;
					String filepath1=null;
					if(parent.dir_path.equals("/"))
					{
						filepath=dataManager.getAccountDir()+"/"+parent.repo_name+parent.dir_path+dirs.get(j).name;
						//filepath1=parent.repo_id+"/"+parent.repo_name+"/"+parent.dir_path+dirs.get(j).name;
					}
					else
					{
					    filepath=dataManager.getAccountDir()+"/"+parent.repo_name+parent.dir_path+"/"+dirs.get(j).name;
					   // filepath1=parent.repo_id+"/"+parent.repo_name+"/"+parent.dir_path+"/"+dirs.get(j).name;
					}
					
					time=dirs.get(j).mtime;//得到文件的服务器时间
					System.out.println("服务器——文件路径："+filepath+"   修改时间："+time);
					//找到文件并记录每个文件的SeafDirent
					serverFile.put(filepath, dirs.get(j));
					
				//	System.out.println("parent.dir_path  SSSSSSSSSSS"+parent.dir_path);
					if(parent.dir_path=="/")
					{
						reposInfo.put(dataManager.getAccountDir()+"/"+parent.repo_name+parent.dir_path+dirs.get(j).name, parent.repo_id);
					}
					else	
					    reposInfo.put(dataManager.getAccountDir()+"/"+parent.repo_name+parent.dir_path+"/"+dirs.get(j).name, parent.repo_id);
					//将得到的服务器文件放进列表里
					
					map.put(filepath, time*1000+150000);
					//String[] dir=new String[2];
					//dir[0]=filepath;
					//dir[1]=Long.toString(time);//将从服务器得到的时间Long类型转换为字符串类型
					//list.add(dir);
					
					
					//System.out.println("txService  "+txService);
					
					//将从服务器获得的文件放进里
			
					//if(txService!=null)
					   //txService.addDownloadTask(account, parent.repo_name,parent.repo_id, filepath);		
					//SeafCachedFile scs;
				
					//while(waiting)
				   // {	
						//scs=dbHelper.getFileCacheItem(parent.repo_id,filepath,dataManager);	
				    	//if(scs!=null&&scs.id!=-1)
				    	//{
				    	   // break;
				    	//}
				    
				    //}
				}
			}
			//System.out.println("stack中的个数：stack.size(): "+stack.size());	
    		
    	}
    	//map.put(getIp(), list);
    	return map;
    }
    /**
     * 当用户点击了“sync”按钮后，采取的操作，若有网，说明该节点是可连接服务器的节点，让其和服务器同步；如果是不联网的节点，也是先让联网节点
     * 和服务器先同步，
     * 
     */
    public void Synchronized()
    {
    	if(Utils.isNetworkOn())
    	{  
    		 System.out.println("***联网节点:"+getIp()+"按了同步按钮发起了同步***");
    		 Map<String,Long> map=new HashMap<String,Long>();
    		 map=syncWithServer();//联网节点首先和服务器进行表同步
    		 show(map);
    		 System.out.println("三表联合后的结果复制进sync表");
    		//然后将得到表复制到sync表里
    		 copyToSynTable(map,getIp());
    		 System.out.println("显示发起同步的联网节点复制后的sync表");
    		 FileSharing.showSync();
    		 String infos="IP地址为："+getIp()+"的主机主动发起同步";
    		 FileSharing.noteType=true;//发起节点将自己的节点类型置为true
    		 requestSyncFunction rsf=new  requestSyncFunction(0,null,infos);
      	     rsf.start();
      	     
    	}
    	else
    	{
    		 System.out.println("***普通节点:"+getIp()+"按了同步按钮发起了同步***");
    		//非联网节点首先将local表复制到sync表里
    		 Map<String,Long> map=new HashMap<String,Long>();
    		 map=FileSharing.getFileModified();
    		 System.out.println("显示普通节点local表中的内容");
    		 show(map);
    		 copyToSynTable(map,getIp());
    		 System.out.println("显示发起同步普通节点复制后的sync表");
    		 FileSharing.showSync();
    		 FileSharing.noteType=true;
    		 String infos="IP地址为："+getIp()+"的主机主动发起同步";
    		 requestSyncFunction rsf=new  requestSyncFunction(0,null,infos);
      	     rsf.start();
    		
    	}
    	/*试整理表
    	String[] t1=new String[]{"a","a","c","a","b","b","a"};
    	Long[] t2=new Long[]{(long) 6,(long) 6,(long) 3,(long) 6,(long) 5,(long) 3,(long) 6};
    	String[] t3=new String[]{"12","13","12","10","10","12","11"};
    	*/
    	//为了调试，在这里显示
    	//showsyncWithServer();
	 	
    }
    
    
      //用于显示表的内容
      public static void show(Map<String,Long> map)
      {
    	  Iterator it = map.keySet().iterator();
  	 	  while (it.hasNext())
  	 	  {
  	 		 String key; 
  	 		 Long value;
  	    	 key=(String)it.next(); 
  	    	 value=map.get(key);
  	    	 System.out.println("表的内容："+key+";对应的时间"+value);
  	    	
  	 	  }
    	  
      }
      
    
    /**
     * 通过settleSynTable1得到了文件的种类；通过settleSynTable2得到了时间最大文件
     * 统计每种文件的最大时间的个数，如果个数等于 FileSharing.count就是收到表的个数，说明每个节点都有这个文件且时间相同则这个文件不需要发送
     * 如果这个文件的最大时间的个数小于 FileSharing.count说明不是每个节点都有，要发送，选择找到的第一个，因为是升序，就是IP最小的那个
     */
    public static ArrayList<String[]> settleSynTable3()
    {
    	ArrayList<String> infos1=new ArrayList<String>();
    	ArrayList<String[]> infos2=new ArrayList<String[]>();
    	ArrayList<String[]> infos3=new ArrayList<String[]>();
    	DatabaseHelper dbHelper;
	 	dbHelper = DatabaseHelper.getDatabaseHelper();
	 	infos1=dbHelper.settleSynTable1();//得到文件的种类
	 	infos2=dbHelper.settleSynTable2();//得到时间最大的文件
	 	if(infos1!=null&&infos2!=null)
	 	{
	    	int[] location =new int[infos1.size()];//存放每种文件的数量
	 	    int[] fileNumber=new int[infos1.size()];
	 	    int k=0;
	    	int j=0;
	    	for(int i=0;i<infos1.size();i++)//每种文件的数量
	    	{
	 		int count=0;
	 		
	 		for(j=k;j<infos2.size();j++)
	 		{
	 			
	 			if(infos1.get(i).equals(infos2.get(j)[0]))//如果文件名相等的话
	 			{
	 				count++;
	 			}
	 			else//遇到第一个文件名不相等的，统计结束，因为是按文件名升序的
	 			{
	 				break;
	 			}
	 		}
	 	    k=j;
	 		fileNumber[i]=count;
	 	}
	 	for(int m=0;m<fileNumber.length;m++)//找出每种文件的起始位置，如果这种文件的数量不等于发送IP的数量则这种文件需要发送
 		{
 			if(m==0)
 			{
 				location[m]=0;
 			}
 			else
 			{
 				location[m]=fileNumber[m-1]+location[m-1];
 			}
 			if(fileNumber[m]!=FileSharing.ipCount)
 			{
 				infos3.add(infos2.get(location[m]));
 			}
 		}
	 	return infos3;
	 	}
	  else
	 		return null;
    }
    
    /**
     * 从settleSynTable3整理出的要发送的文件名，时间，IP中找到每个IP对要发送的文件
     * 
     */
    public static Map<String,ArrayList<String[]>> settleSynTable4()
    {
    	ArrayList<String[]> infos4=new ArrayList<String[]>();
    	infos4=settleSynTable3();
    	ArrayList<String> infos5=new ArrayList<String>();
    	Set<String> set = new HashSet<String>();
    	
    	Map<String,ArrayList<String[]>> ipFile=new LinkedHashMap<String,ArrayList<String[]>>();
    	if(infos4!=null)
    	{
    	for(int i=0;i<infos4.size();i++)
    	{
    		infos5.add(infos4.get(i)[2]);
    	}
    	set.addAll(infos5);//将所有的IP值放进set中为的是去掉重复的
    	Iterator it = set.iterator();
    	while(it.hasNext())//找出每个IP对应的要发送的文件，将IP和对应的文件放进map中
    	{
    		String key=(String)it.next();
    		ArrayList<String[]> infos6=new ArrayList<String[]>();
    		for(int j=0;j<infos4.size();j++)
    	    {
    	    	if(key.equals(infos4.get(j)[2]))
    	    	{
    	    		String[] dir=new String[2];
    	    		dir[0]=infos4.get(j)[0];
    	    		dir[1]=infos4.get(j)[1];
    	    		infos6.add(dir);
    	    	}	
    	    }
    	    ipFile.put(key,infos6);	
    	}
    	return ipFile;
    	}
    	else 
    		return null;
    }
    public static synchronized void showSettle4(Map<String,ArrayList<String[]>> savedMap)
    {
    	 Iterator it = savedMap.keySet().iterator();
    	while(it.hasNext())//找出每个IP对应的要发送的文件，将IP和对应的文件放进map中
    	{
    		String key=(String)it.next();
    		ArrayList<String[]> infos7=new ArrayList<String[]>();
    		infos7=savedMap.get(key);
    		System.out.println("settle4--IP:  "+key);
    		for(int j=0;j<infos7.size();j++)
    	    {
    	    	System.out.println("settle4文件名："+infos7.get(j)[0]+"时间："+infos7.get(j)[1]);
    	    }
    	    
    	}
    	
    }
    
    	   	
    /**
     * 
     * 将local表、server表和服务器端表三张表比较，得到最终的要发送的表
     *
     */
    public  Map syncWithServer()
    {
    	Map<String,Long> mapserver=new HashMap<String,Long>();//从服务器获得的最新的文件表
    	Map<String,Long> mapservtable=new HashMap<String,Long>();//从server表获得的文件表
    	Map<String,Long> mapmodifytable=new HashMap<String,Long>();//从local表获得的文件表
    	Map<String,Long> finaltable=new HashMap<String,Long>();//能上网结点最终要发送的表
    	mapserver = getTableFromServer(); 
    	System.out.println("下面显示服务器的文件");
    	show(mapserver);
    	mapservtable=getServerTable();
    	System.out.println("下面显示*成功表*的文件");
    	show(mapservtable);
    	mapmodifytable=FileSharing.getFileModified();
    	System.out.println("下面显示local表的文件");
    	show(mapmodifytable);
    	Iterator it = mapserver.keySet().iterator(); 
    	while (it.hasNext()){ 
    	    String key; 
    	    key=(String)it.next(); 
    	    Long value,svalue,mvalue;
    	    value=mapserver.get(key);//服务器端的键值
    	    //System.out.println("服务器端的键值为："+key);
    	    //System.out.println("服务器端的键值为："+value);
    	    System.out.println("将local表、server表和服务器端表三张表比较，得到最终的要发送的表的文件");
    	       boolean contains=mapservtable.containsKey(key);//如果服务器有的文件server表里也有
    	       if(contains)
    	       {
    	    	   //System.out.println("！！！！在server表中包含键：" + key);
    	    	   svalue=mapservtable.get(key);//server的键值
    	    	   //System.out.println("server表中的键为    "+key+"   值为："+svalue);
    	    	   if(value>svalue)//如果服务器端的时间大于server表的时间，采用服务器端的时间
    	    	   {
    	    		   FileSharing.onlyInServer.add(key);//这个列表里放的是服务器比本地新的文件，包括本地没有的
    	    		   finaltable.put(key,value);
    	    	   }
    	    	   else
    	    	   {
    	    		   if(value.equals(svalue))//如果服务器端的时间等于server表的时间，采用local表中的时间
    	    		   {
    	    			   //从local表中找出值放进finaltable里
    	    			   boolean localcons=mapmodifytable.containsKey(key);
    	    			   if(localcons)//如果local表中存在该键，放进最终表中
    	    			   {
    	    				   mvalue=mapmodifytable.get(key);
    	    				   finaltable.put(key,mvalue);
    	    				
    	    			   }
    	    			   else
    	    			   {
    	    				   System.out.println("local表出错了");
    	    			   }
    	    			
    	    		   }
    	    		  else
    	    		  {
    	    			   System.out.println("表出错了");
    	    		  }
    	    	}
    	    }
    	    else
    	    {
    	    	//System.out.println("server表中不包含键：" + key);
    	    	//server表中没有该键说明server表没有服务器端的这个文件，说明是服务端有的本地没有的，要把这种文件放进一个静态列表中
    	    	//System.out.println("local--server表中没有该键说明server表没有服务器端的这个文件");
    	    	FileSharing.onlyInServer.add(key);
    	    	finaltable.put(key,value);
    	    }
    	   	
    	}
 	    return finaltable;
    }
    /**
     * 
     * 
     * 将server表中的内容读出
     */
    public static Map getServerTable()
    {
       ArrayList<String[]> filelist=new ArrayList<String[]>();
  	   DatabaseHelper dbHelper;
  	   dbHelper = DatabaseHelper.getDatabaseHelper();
  	   filelist=dbHelper.getFileFromServerTable();
  	   //System.out.println("从local——server表中读出数据成功");
  	   Map<String,Long> map=new HashMap<String,Long>(); 
  	   if(filelist==null)
  	   {
  		   return map;
  	   }
  	   for(int i=0;i<filelist.size();i++)
  	   {  
  		   
		   String filename=filelist.get(i)[0];
		   String time=filelist.get(i)[1];
         map.put(filename, Long.parseLong(time));
  	   }
  	   return map;
  	   
    }
    
    /**
     * 复制到sync表里
     * 
     */
    public static void copyToSynTable(Map<String,Long> map,String ip)
    {
    	Iterator it = map.keySet().iterator();
    	DatabaseHelper dbHelper;
	 	dbHelper = DatabaseHelper.getDatabaseHelper();
    	while (it.hasNext())
    	{ 
    		String key; 
    		long value;
    	    key=(String)it.next();
    	    value=map.get(key);
    	    dbHelper.saveSynTable(key,value,ip);//每一条数据插入到syn表中
    	    
    	}
    }
    
    
    
    
/**
 * 刷新，上传，同步，新建文件，新建文件夹
 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {

        if (mDrawerToggle.onOptionsItemSelected(getMenuItem(item))) 
        {
            return true;
        }
        switch (item.getItemId()) {
        case android.R.id.home:
            return true;
        case R.id.upload:
            pickFile();
            return true;
        case R.id.sync:
        	Synchronized();
            return true;
        case R.id.refresh:
       	    System.out.println("刷新是否有网？："+Utils.isNetworkOn());
       	   // System.out.println("AAAAAAAAAAAAAA"+ dataManager.getAccountDir() );
            if (!Utils.isNetworkOn())
            {
                showToast(R.string.network_down);
                return true;
            }
            if (getCurrentTabName().equals(LIBRARY_TAB))
            {
            	 System.out.println("111111111："+navContext.inRepo());
            	if (navContext.inRepo()) 
                {
                    SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());
                    if (repo.encrypted && !DataManager.getRepoPasswordSet(repo.id))
                    {
                        String password = DataManager.getRepoPassword(repo.id);
                        showPasswordDialog(repo.name, repo.id,
                            new TaskDialog.TaskDialogListener()
                           {
                                @Override
                                public void onTaskSuccess() 
                                {
                                    tabsFragment.getReposFragment().refreshView(true);
                                }
                            } , password);
                   
                        return true;
                    }
                }
                tabsFragment.getReposFragment().refreshView(true);
            } 
            else if (getCurrentTabName().equals(ACTIVITY_TAB)) 
            {
                tabsFragment.getActivitiesFragment().refreshView();
            } 
            else if (getCurrentTabName().equals(STARRED_TAB)) 
            {
                tabsFragment.getStarredFragment().refreshView();
            }
            return true;
        case R.id.newdir:
            showNewDirDialog();
            return true;
        case R.id.newfile:
            showNewFileDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private android.view.MenuItem getMenuItem(final MenuItem item) {
        return new android.view.MenuItem() {
           @Override
           public int getItemId() {
              return item.getItemId();
           }

           public boolean isEnabled() {
              return true;
           }

           @Override
           public boolean collapseActionView() {
              // TODO Auto-generated method stub
              return false;
           }

           @Override
           public boolean expandActionView() {
              // TODO Auto-generated method stub
              return false;
           }

           @Override
           public ActionProvider getActionProvider() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public View getActionView() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public char getAlphabeticShortcut() {
              // TODO Auto-generated method stub
              return 0;
           }

           @Override
           public int getGroupId() {
              // TODO Auto-generated method stub
              return 0;
           }

           @Override
           public Drawable getIcon() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public Intent getIntent() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public ContextMenuInfo getMenuInfo() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public char getNumericShortcut() {
              // TODO Auto-generated method stub
              return 0;
           }

           @Override
           public int getOrder() {
              // TODO Auto-generated method stub
              return 0;
           }

           @Override
           public SubMenu getSubMenu() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public CharSequence getTitle() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public CharSequence getTitleCondensed() {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public boolean hasSubMenu() {
              // TODO Auto-generated method stub
              return false;
           }

           @Override
           public boolean isActionViewExpanded() {
              // TODO Auto-generated method stub
              return false;
           }

           @Override
           public boolean isCheckable() {
              // TODO Auto-generated method stub
              return false;
           }

           @Override
           public boolean isChecked() {
              // TODO Auto-generated method stub
              return false;
           }

           @Override
           public boolean isVisible() {
              // TODO Auto-generated method stub
              return false;
           }

           @Override
           public android.view.MenuItem setActionProvider(ActionProvider actionProvider) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setActionView(View view) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setActionView(int resId) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setAlphabeticShortcut(char alphaChar) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setCheckable(boolean checkable) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setChecked(boolean checked) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setEnabled(boolean enabled) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setIcon(Drawable icon) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setIcon(int iconRes) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setIntent(Intent intent) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setNumericShortcut(char numericChar) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setShortcut(char numericChar, char alphaChar) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public void setShowAsAction(int actionEnum) {
              // TODO Auto-generated method stub

           }

           @Override
           public android.view.MenuItem setShowAsActionFlags(int actionEnum) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setTitle(CharSequence title) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setTitle(int title) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setTitleCondensed(CharSequence title) {
              // TODO Auto-generated method stub
              return null;
           }

           @Override
           public android.view.MenuItem setVisible(boolean visible) {
              // TODO Auto-generated method stub
              return null;
           }
        };
     }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void showNewDirDialog() 
    {
        if (!hasRepoWritePermission()) 
        {
            showToast(R.string.library_read_only);
            return;
        }

        final NewDirDialog dialog = new NewDirDialog();
        dialog.init(navContext.getRepoID(), navContext.getDirPath(), account);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() 
        {
            @Override
            public void onTaskSuccess()
            {
                showToast("Sucessfully created folder " + dialog.getNewDirName());
                ReposFragment reposFragment = tabsFragment.getReposFragment();
                if (getCurrentTabName().equals(LIBRARY_TAB) && reposFragment != null) {
                    reposFragment.refreshView();
                }
            }
        });
        dialog.show(getSupportFragmentManager(), "DialogFragment");
    }

    private void showNewFileDialog() {
        if (!hasRepoWritePermission()) {
            showToast(R.string.library_read_only);
            return;
        }

        final NewFileDialog dialog = new NewFileDialog();
        dialog.init(navContext.getRepoID(), navContext.getDirPath(), account);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() {
            @Override
            public void onTaskSuccess() {
                showToast("Sucessfully created file " + dialog.getNewFileName());
                ReposFragment reposFragment = tabsFragment.getReposFragment();
                if (getCurrentTabName().equals(LIBRARY_TAB) && reposFragment != null) {
                    reposFragment.refreshView();
                }
            }
        });
        dialog.show(getSupportFragmentManager(), "DialogFragment");
    }

    public void setRefreshing() {
        setSupportProgressBarIndeterminateVisibility(Boolean.TRUE);
    }

    public void unsetRefreshing() {
        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
    }

    public void showToast(CharSequence msg) {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void showToast(int id) {
        showToast(getString(id));
    }

    public void enableUpButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void disableUpButton() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    /***********  Start other activity  ***************/

    public static final int PICK_FILES_REQUEST = 1;
    public static final int PICK_PHOTOS_VIDEOS_REQUEST = 2;
    public static final int PICK_FILE_REQUEST = 3;
/**
 * 选择“上传”后，要打开的对话框
 * @author PC
 *
 */
    public class UploadChoiceDialog extends DialogFragment 
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) 
        {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.pick_upload_type);
            builder.setItems(R.array.pick_upload_array,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                            case 0:
                            	//files
                                Intent intent = new Intent(BrowserActivity.this, MultiFileChooserActivity.class);
                                getActivity().startActivityForResult(intent, PICK_FILES_REQUEST);
                                break;
                            case 1:
                                // photos
                                intent = new Intent(BrowserActivity.this, MultipleImageSelectionActivity.class);
                                getActivity().startActivityForResult(intent, PICK_PHOTOS_VIDEOS_REQUEST);
                                break;
                            case 2:
                                // thirdparty file chooser
                                Intent target = Utils.createGetContentIntent();
                                intent = Intent.createChooser(target, getString(R.string.choose_file));
                                getActivity().startActivityForResult(intent, PICK_FILE_REQUEST);
                                break;
                            default:
                                return;
                            }
                        }
                    });

            return builder.create();
        }
    }

    public boolean hasRepoWritePermission() {
        SeafRepo repo = dataManager.getCachedRepoByID(navContext.getRepoID());
        if (repo == null) {
            return false;
        }

        if (repo.permission.indexOf('w') == -1) {
            return false;
        }
        return true;
    }
/**
 * 跳到上传对话框
 */
    void pickFile()
    {
        if (!hasRepoWritePermission()) 
        {
            showToast(R.string.library_read_only);
            return;
        }

        UploadChoiceDialog dialog = new UploadChoiceDialog();
        dialog.show(getSupportFragmentManager(), PICK_FILE_DIALOG_FRAGMENT_TAG);
    }
/**
 * 选择文件爱/图片/视频上传后的处理函数
 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        switch (requestCode) {
        case PICK_FILES_REQUEST:
            if (resultCode == RESULT_OK) 
            {
                String[] paths = data.getStringArrayExtra(MultiFileChooserActivity.MULTI_FILES_PATHS);
                if (paths == null)
                    return;
                showToast(getString(R.string.added_to_upload_tasks));
                for (String path : paths)
                {
                    addUploadTask(navContext.getRepoID(),
                        navContext.getRepoName(), navContext.getDirPath(), path);       
                }
            }
            break;
        case PICK_PHOTOS_VIDEOS_REQUEST:
            if (resultCode == RESULT_OK) {
                ArrayList<String> paths = data.getStringArrayListExtra("photos");
                if (paths == null)
                    return;
                showToast(getString(R.string.added_to_upload_tasks));
                for (String path : paths) {
                    addUploadTask(navContext.getRepoID(),
                        navContext.getRepoName(), navContext.getDirPath(), path);
                }
            }
            break;
        case PICK_FILE_REQUEST:
            if (resultCode == RESULT_OK) {
                if (!Utils.isNetworkOn()) {
                    showToast("Network is not connected");
                    return;
                }

                Uri uri = data.getData();
                String path;
                try {
                    path = Utils.getPath(this, uri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    return;
                }
                showToast(getString(R.string.added_to_upload_tasks));
                //showToast(getString(R.string.upload) + " " + Utils.fileNameFromPath(path));
                addUploadTask(navContext.getRepoID(),
                    navContext.getRepoName(), navContext.getDirPath(), path);
            }
            break;
         default:
             break;
        }

    }

    /***************  Navigation *************/
/**
 * 在文件上点击“下载”按钮的操作
 */
    @Override
    public void onFileSelected(SeafDirent dirent) 
    {
        String fileName= dirent.name;
        final String repoName = navContext.getRepoName();
        final String repoID = navContext.getRepoID();
        final String filePath = Utils.pathJoin(navContext.getDirPath(), fileName);

        File localFile = dataManager.getLocalCachedFile(repoName, repoID, filePath, dirent.id);
        if (localFile != null) {
            showFile(localFile);
            return;
        }

        Intent intent = new Intent(this, FileActivity.class);
        intent.putExtra("repoName", repoName);
        intent.putExtra("repoID", repoID);
        intent.putExtra("filePath", filePath);
        intent.putExtra("account", account);
        startActivity(intent);
        return;
    }

    @Override
    public void onStarredFileSelected(SeafStarredFile starredFile) {

        final String repoID = starredFile.getRepoID();
        SeafRepo seafRepo = dataManager.getCachedRepoByID(repoID);
        final String repoName = seafRepo.getName();
        final String filePath = starredFile.getPath();

        File localFile = dataManager.getLocalCachedFile(repoName, repoID, filePath, null);
        if (localFile != null) {
            showFile(localFile);
            return;
        }

        Intent intent = new Intent(this, FileActivity.class);
        intent.putExtra("repoName", repoName);
        intent.putExtra("repoID", repoID);
        intent.putExtra("filePath", filePath);
        intent.putExtra("account", account);
        startActivity(intent);
        return;
    }

    @Override
    public void onBackPressed() 
    {
        if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }
        System.out.println(" onBackPressed()   ");
        if (currentSelectedItem == FILES_VIEW && getCurrentTabName().equals(LIBRARY_TAB))
        {
            if (navContext.inRepo()) 
            {
                if (navContext.isRepoRoot()) 
                {
                    navContext.setRepoID(null);
                }
                else
                {
                    String parentPath = Utils.getParentPath(navContext .getDirPath());
                    navContext.setDir(parentPath, null);
                }
                tabsFragment.getReposFragment().refreshView();

            } 
            else
                super.onBackPressed();
        } 
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged()
    {
    }


    /************  Files ************/

    private void startMarkdownActivity(String path) {
        Intent intent = new Intent(this, MarkdownActivity.class);
        intent.putExtra("path", path);
        startActivity(intent);
    }

    public void showFile(File file) {
        String name = file.getName();
        String suffix = name.substring(name.lastIndexOf('.') + 1).toLowerCase();

        if (suffix.length() == 0) {
            showToast(R.string.unknown_file_type);
            return;
        }

        if (suffix.endsWith("md") || suffix.endsWith("markdown")) {
            startMarkdownActivity(file.getPath());
            return;
        }

        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        Intent open = new Intent(Intent.ACTION_VIEW);
        open.setDataAndType((Uri.fromFile(file)), mime);

        try {
            startActivity(open);
            return;
        } catch (ActivityNotFoundException e){
            new OpenAsDialog(file).show(getSupportFragmentManager(), "OpenAsDialog");
            //showToast(R.string.activity_not_found);
            return;
        }

/*      String chooser_title = getString(R.string.open_with);
        Intent chooser = Intent.createChooser(open, chooser_title);

        if (open.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
            return;
        } else {
            showToast(R.string.activity_not_found);
            return;
        }*/

    }

    /**
     * Export a file.
     * 1. first ask the user to choose an app
     * 2. then download the latest version of the file
     * 3. start the choosen app
     *
     * @param fileName The name of the file to share in the current navcontext
     */
    public void exportFile(String fileName) {
        String repoName = navContext.getRepoName();
        String repoID = navContext.getRepoID();
        String dirPath = navContext.getDirPath();
        String fullPath = Utils.pathJoin(dirPath, fileName);
        chooseExportApp(repoName, repoID, fullPath);
    }

    private void chooseExportApp(final String repoName, final String repoID, final String path) {
        final File file = dataManager.getLocalRepoFile(repoName, repoID, path);
        Uri uri = Uri.fromFile(file);

        final Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType(Utils.getFileMimeType(file));
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);

        // Get a list of apps
        List<ResolveInfo> infos = getAppsByIntent(sendIntent);

        if (infos.isEmpty()) {
            showToast(R.string.no_app_available);
            return;
        }

        AppChoiceDialog dialog = new AppChoiceDialog();
        dialog.init(getString(R.string.export_file), infos, new AppChoiceDialog.OnItemSelectedListener() {
            @Override
            public void onCustomActionSelected(CustomAction action) {
            }
            @Override
            public void onAppSelected(ResolveInfo appInfo) {
                String className = appInfo.activityInfo.name;
                String packageName = appInfo.activityInfo.packageName;
                sendIntent.setClassName(packageName, className);

                if (!Utils.isNetworkOn() && file.exists()) {
                    startActivity(sendIntent);
                    return;
                }
                fetchFileAndExport(appInfo, sendIntent, repoName, repoID,
                                   path);
            }

        });
        dialog.show(getSupportFragmentManager(), CHOOSE_APP_DIALOG_FRAGMENT_TAG);
    }

    private void fetchFileAndExport(final ResolveInfo appInfo, final Intent intent,
                                    final String repoName, final String repoID, final String path) {

        fetchFileDialog = new FetchFileDialog();
        fetchFileDialog.init(repoName, repoID, path, new FetchFileDialog.FetchFileListener() {
            @Override
            public void onSuccess() {
                startActivity(intent);
            }

            @Override
            public void onDismiss() {
                fetchFileDialog = null;
            }

            @Override
            public void onFailure(SeafException err) {
            }
        });
        fetchFileDialog.show(getSupportFragmentManager(), OPEN_FILE_DIALOG_FRAGMENT_TAG);
    }

    /**
     * Share a file. Generating a file share link and send the link to someone
     * through some app.
     * @param fileName
     */
    public void shareFile(String repoID, String path) 
    {
        chooseShareApp(repoID, path, false);
    }

    public void shareDir(String repoID, String path)
    {
        chooseShareApp(repoID, path, true);
    }

    private List<ResolveInfo> getAppsByIntent(Intent intent) {
        PackageManager pm = getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);

        // Remove seafile app from the list
        String seadroidPackageName = getPackageName();
        ResolveInfo info;
        Iterator<ResolveInfo> iter = infos.iterator();
        while (iter.hasNext()) {
            info = iter.next();
            if (info.activityInfo.packageName.equals(seadroidPackageName)) {
                iter.remove();
            }
        }

        return infos;
    }
/**
 * 共享文件
 * @param repoID
 * @param path
 * @param isdir
 */
    private void chooseShareApp(final String repoID, final String path, final boolean isdir) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        // Get a list of apps
        List<ResolveInfo> infos = getAppsByIntent(shareIntent);

        String title = getString(isdir ? R.string.share_dir_link : R.string.share_file_link);

        AppChoiceDialog dialog = new AppChoiceDialog();
        dialog.addCustomAction(0, getResources().getDrawable(R.drawable.copy_link),
                               getString(R.string.copy_link));
        dialog.init(title, infos, new AppChoiceDialog.OnItemSelectedListener() {
            @Override
            public void onCustomActionSelected(CustomAction action) {
                final GetShareLinkDialog gdialog = new GetShareLinkDialog();
                gdialog.init(repoID, path, isdir, account);
                gdialog.setTaskDialogLisenter(new TaskDialogListener() {
                    @Override
                    public void onTaskSuccess() {
                        // TODO: generate a share link through SeafConnection and copy
                        // it to clipboard
                        ClipboardManager clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(gdialog.getLink());
                        // ClipData clip = ClipData.newPlainText("seafile shared link", gdialog.getLink());
                        // clipboard.setPrimaryClip(clip);
                        showToast(R.string.link_ready_to_be_pasted);
                    }
                });
                gdialog.show(getSupportFragmentManager(), "DialogFragment");
            }

            @Override
            public void onAppSelected(ResolveInfo appInfo) {
                String className = appInfo.activityInfo.name;
                String packageName = appInfo.activityInfo.packageName;
                shareIntent.setClassName(packageName, className);

                final GetShareLinkDialog gdialog = new GetShareLinkDialog();
                gdialog.init(repoID, path, isdir, account);
                gdialog.setTaskDialogLisenter(new TaskDialogListener() {
                    @Override
                    public void onTaskSuccess() {
                        shareIntent.putExtra(Intent.EXTRA_TEXT, gdialog.getLink());
                        startActivity(shareIntent);
                    }
                });
                gdialog.show(getSupportFragmentManager(), "DialogFragment");
            }

        });
        dialog.show(getSupportFragmentManager(), CHOOSE_APP_DIALOG_FRAGMENT_TAG);
    }
/**
 * 点击“重命名”的操作
 * @param repoID
 * @param repoName
 * @param path
 */
    public void renameFile(String repoID, String repoName, String path) 
    {
        doRename(repoID, repoName, path, false);
    }

    public void renameDir(String repoID, String repoName, String path) {
        doRename(repoID, repoName, path, true);
    }
/**
 * 重命名文件
 * @param repoID
 * @param repoName
 * @param path
 * @param isdir
 */
    private void doRename(String repoID, String repoName, String path, boolean isdir)
    {
        final RenameFileDialog dialog = new RenameFileDialog();
        dialog.init(repoID, path, isdir, account);
        dialog.setTaskDialogLisenter(new TaskDialog.TaskDialogListener() 
        {
            @Override
            public void onTaskSuccess() {
                showToast(R.string.rename_successful);
                ReposFragment reposFragment = tabsFragment.getReposFragment();
                if (getCurrentTabName().equals(LIBRARY_TAB) && reposFragment != null) {
                    reposFragment.refreshView();
                }
            }
        });
        dialog.show(getSupportFragmentManager(), "DialogFragment");
    }

    private void onFileUploadProgress(int taskID) 
    {
        if (txService == null) {
            return;
        }
        UploadTaskInfo info = txService.getUploadTaskInfo(taskID);
        if (uploadTasksFragment != null && uploadTasksFragment.isReady())
            uploadTasksFragment.onTaskProgressUpdate(info);
    }
/**
 * 该程序收到文件已经上传的广播信息后，采取的操作，可以获取该文件的相关信息，刷新窗口等。
 * @param taskID
 */
    private void onFileUploaded(int taskID) 
    {
        if (txService == null) 
        {
            return;
        }
    	System.out.println("Browser Activity---onFileUploaded 1931  "+taskID);
        UploadTaskInfo info = txService.getUploadTaskInfo(taskID);

        String repoID = info.repoID;
        String dir = info.parentDir;
        if (getCurrentTabName().equals(LIBRARY_TAB)
                && repoID.equals(navContext.getRepoID())
                && dir.equals(navContext.getDirPath())) 
        {
            tabsFragment.getReposFragment().refreshView(true);
            String verb = getString(info.isUpdate ? R.string.updated : R.string.uploaded);
            showToast(verb + " " + Utils.fileNameFromPath(info.localFilePath));
        }

        if (uploadTasksFragment != null && uploadTasksFragment.isReady())
            uploadTasksFragment.onTaskFinished(info);
    }

    private void onFileUploadCancelled(int taskID)
    {
        if (txService == null) {
            return;
        }
        UploadTaskInfo info = txService.getUploadTaskInfo(taskID);
        if (uploadTasksFragment != null && uploadTasksFragment.isReady())
            uploadTasksFragment.onTaskCancelled(info);
    }

    private void onFileUploadFailed(int taskID) {
        if (txService == null) {
            return;
        }
        UploadTaskInfo info = txService.getUploadTaskInfo(taskID);
        showToast(getString(R.string.upload_failed) + " " + Utils.fileNameFromPath(info.localFilePath));
        if (uploadTasksFragment != null && uploadTasksFragment.isReady())
            uploadTasksFragment.onTaskFailed(info);
    }

    private void onFileDownloadProgress(int taskID) {
        if (txService == null) {
            return;
        }

        DownloadTaskInfo info = txService.getDownloadTaskInfo(taskID);
        if (fetchFileDialog != null && fetchFileDialog.getTaskID() == taskID) {
                fetchFileDialog.handleDownloadTaskInfo(info);
        }
    }
    /**
     * 该程序收到文件已经下载的广播信息后，采取的操作，可以获取该文件的相关信息，刷新窗口等。
     * @param taskID
     */
    private void onFileDownloaded(int taskID) 
    {
        if (txService == null) {
            return;
        }
        System.out.println("Browser Activity---onFileDownloaded 1989  "+taskID);
        DownloadTaskInfo info = txService.getDownloadTaskInfo(taskID);
        if (fetchFileDialog != null && fetchFileDialog.getTaskID() == taskID) {
            fetchFileDialog.handleDownloadTaskInfo(info);
        } else {
            if (getCurrentTabName().equals(LIBRARY_TAB)
                && info.repoID.equals(navContext.getRepoID())
                && Utils.getParentPath(info.pathInRepo).equals(navContext.getDirPath())) {
                tabsFragment.getReposFragment().getAdapter().notifyChanged();
            }
        }
    }

    private void onFileDownloadFailed(int taskID) {
        if (txService == null) {
            return;
        }

        DownloadTaskInfo info = txService.getDownloadTaskInfo(taskID);
        if (fetchFileDialog != null && fetchFileDialog.getTaskID() == taskID) {
            fetchFileDialog.handleDownloadTaskInfo(info);
            return;
        }

        SeafException err = info.err;
        final String repoName = info.repoName;
        final String repoID = info.repoID;
        final String path = info.pathInRepo;

        if (err != null && err.getCode() == 440) {
            if (getCurrentTabName().equals(LIBRARY_TAB)
                && repoID.equals(navContext.getRepoID())
                && Utils.getParentPath(path).equals(navContext.getDirPath())) {
                showPasswordDialog(repoName, repoID, new TaskDialog.TaskDialogListener() {
                    @Override
                    public void onTaskSuccess() {
                        txService.addDownloadTask(account, repoName, repoID, path);
                    }
                });
                return;
            }
        }

        showToast(getString(R.string.download_failed) + " " + Utils.fileNameFromPath(path));
    }

    public PasswordDialog showPasswordDialog(String repoName, String repoID,
                                             TaskDialog.TaskDialogListener listener) {
        return showPasswordDialog(repoName, repoID, listener, null);
    }

    public PasswordDialog showPasswordDialog(String repoName, String repoID,
                                             TaskDialog.TaskDialogListener listener, String password) {
        PasswordDialog passwordDialog = new PasswordDialog();
        passwordDialog.setRepo(repoName, repoID, account);
        if (password != null) {
            passwordDialog.setPassword(password);
        }
        passwordDialog.setTaskDialogLisenter(listener);
        passwordDialog.show(getSupportFragmentManager(), PASSWORD_DIALOG_FRAGMENT_TAG);
        return passwordDialog;
    }

    // for receive broadcast from TransferService
    private class TransferReceiver extends BroadcastReceiver 
    {

        private TransferReceiver() {}

        public void onReceive(Context context, Intent intent)
        {
            String type = intent.getStringExtra("type");
            if (type.equals(TransferService.BROADCAST_FILE_DOWNLOAD_PROGRESS)) 
            {
            	
                int taskID = intent.getIntExtra("taskID", 0);
                onFileDownloadProgress(taskID);

            } else if (type.equals(TransferService.BROADCAST_FILE_DOWNLOAD_SUCCESS)) 
            {
            	System.out.println("BA收到了TransferService--文件下载成功后的消息");
                int taskID = intent.getIntExtra("taskID", 0);
                onFileDownloaded(taskID);

            } else if (type.equals(TransferService.BROADCAST_FILE_DOWNLOAD_FAILED)) 
            {
                int taskID = intent.getIntExtra("taskID", 0);
                onFileDownloadFailed(taskID);

            } else if (type.equals(TransferService.BROADCAST_FILE_UPLOAD_SUCCESS))
            {
            	System.out.println("BA收到了TransferService--收到文件已上传的广播消息");
                int taskID = intent.getIntExtra("taskID", 0);
                onFileUploaded(taskID);

            } else if (type.equals(TransferService.BROADCAST_FILE_UPLOAD_FAILED)) {
                int taskID = intent.getIntExtra("taskID", 0);
                onFileUploadFailed(taskID);

            } else if (type.equals(TransferService.BROADCAST_FILE_UPLOAD_PROGRESS)) {
                int taskID = intent.getIntExtra("taskID", 0);
                onFileUploadProgress(taskID);
            } else if (type.equals(TransferService.BROADCAST_FILE_UPLOAD_CANCELLED)) {
                int taskID = intent.getIntExtra("taskID", 0);
                onFileUploadCancelled(taskID);
            }
        }

    } // TransferReceiver

}
