package com.seafile.seadroid2.monitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.seafile.seadroid2.SeadroidApplication;
import com.seafile.seadroid2.account.Account;
import com.seafile.seadroid2.account.AccountManager;

public class MonitorDBHelper extends SQLiteOpenHelper {
	private static final String DEBUG_TAG = "MonitorDBHelper";

	// If you change the database schema, you must increment the database
	// version.
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "monitor.db";

	// FileCache table
	private static final String AUTO_UPDATE_INFO_TABLE_NAME = "AutoUpdateInfo";

	private static final String AUTO_UPDATE_INFO_COLUMN_ID = "id";
	private static final String AUTO_UPDATE_INFO_COLUMN_ACCOUNT = "account";
	private static final String AUTO_UPDATE_INFO_COLUMN_REPO_ID = "repo_id";
	private static final String AUTO_UPDATE_INFO_COLUMN_REPO_NAME = "repo_name";
	private static final String AUTO_UPDATE_INFO_COLUMN_PARENT_DIR = "parent_dir";
	private static final String AUTO_UPDATE_INFO_COLUMN_LOCAL_PATH = "local_path";

	private static final String SQL_CREATE_AUTO_UPDATE_INFO_TABLE = "CREATE TABLE "
			+ AUTO_UPDATE_INFO_TABLE_NAME
			+ " ("
			+ AUTO_UPDATE_INFO_COLUMN_ID
			+ " INTEGER PRIMARY KEY, "
			+ AUTO_UPDATE_INFO_COLUMN_ACCOUNT
			+ " TEXT NOT NULL, "
			+ AUTO_UPDATE_INFO_COLUMN_REPO_ID
			+ " TEXT NOT NULL, "
			+ AUTO_UPDATE_INFO_COLUMN_REPO_NAME
			+ " TEXT NOT NULL, "
			+ AUTO_UPDATE_INFO_COLUMN_PARENT_DIR
			+ " TEXT NOT NULL, "
			+ AUTO_UPDATE_INFO_COLUMN_LOCAL_PATH
			+ " TEXT NOT NULL);";

	private static final String[] FULL_PROJECTION = {
			AUTO_UPDATE_INFO_COLUMN_ACCOUNT, AUTO_UPDATE_INFO_COLUMN_REPO_ID,
			AUTO_UPDATE_INFO_COLUMN_REPO_NAME,
			AUTO_UPDATE_INFO_COLUMN_PARENT_DIR,
			AUTO_UPDATE_INFO_COLUMN_LOCAL_PATH, };

	// Use only single dbHelper to prevent multi-thread issue and db is closed
	// exception
	// Reference
	// http://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android
	private static MonitorDBHelper dbHelper = null;
	private SQLiteDatabase database = null;

	public static MonitorDBHelper getMonitorDBHelper() 
	{
		if (dbHelper != null)
			return dbHelper;
		dbHelper = new MonitorDBHelper(SeadroidApplication.getAppContext());
		dbHelper.database = dbHelper.getWritableDatabase();
		return dbHelper;
	}

	private MonitorDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createFileCacheTable(db);
	}

	private void createFileCacheTable(SQLiteDatabase db)
	{
		db.execSQL(SQL_CREATE_AUTO_UPDATE_INFO_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		db.execSQL("DROP TABLE IF EXISTS " + AUTO_UPDATE_INFO_TABLE_NAME + ";");
		onCreate(db);
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}
/**
 * 将info的相关信息插入到AutoUpdateInfo表中
 * @param info
 */
	public void saveAutoUpdateInfo(AutoUpdateInfo info) 
	{
		removeAutoUpdateInfo(info);

		ContentValues values = new ContentValues();
		values.put(AUTO_UPDATE_INFO_COLUMN_ACCOUNT, info.account.getSignature());
		values.put(AUTO_UPDATE_INFO_COLUMN_REPO_ID, info.repoID);
		values.put(AUTO_UPDATE_INFO_COLUMN_REPO_NAME, info.repoName);
		values.put(AUTO_UPDATE_INFO_COLUMN_PARENT_DIR, info.parentDir);
		values.put(AUTO_UPDATE_INFO_COLUMN_LOCAL_PATH, info.localPath);

		database.insert(AUTO_UPDATE_INFO_TABLE_NAME, null, values);
	}
	/**
	 * 将info的相关信息从表中删除
	 * @param info
	 */
	public void removeAutoUpdateInfo(AutoUpdateInfo info) {
		String whereClause = String.format(
				"%s = ? and %s = ? and %s = ? and %s = ? and %s = ?",
				AUTO_UPDATE_INFO_COLUMN_ACCOUNT,
				AUTO_UPDATE_INFO_COLUMN_REPO_ID,
				AUTO_UPDATE_INFO_COLUMN_REPO_NAME,
				AUTO_UPDATE_INFO_COLUMN_PARENT_DIR,
				AUTO_UPDATE_INFO_COLUMN_LOCAL_PATH);
		String[] params = { info.account.getSignature(), info.repoID,
				info.repoName, info.parentDir, info.localPath, };
		database.delete(AUTO_UPDATE_INFO_TABLE_NAME, whereClause, params);
	}
/**
 * 将AutoUpdateInfo表中有效地信息取出放到List<AutoUpdateInfo> infos中
 * @return
 */
	public List<AutoUpdateInfo> getAutoUploadInfos() 
	{
		List<AutoUpdateInfo> infos = new ArrayList<AutoUpdateInfo>();
		List<AutoUpdateInfo> invalidInfos = new LinkedList<AutoUpdateInfo>();

		Cursor c = database.query(AUTO_UPDATE_INFO_TABLE_NAME, FULL_PROJECTION,
				null, null, null, // don't group the rows
				null, // don't filter by row groups
				null // The sort order
				);

		c.moveToFirst();

		Map<String, Account> accounts = getAllAccouts();
		while (!c.isAfterLast()) 
		{
			AutoUpdateInfo item = cursorToAutoUpdateInfo(c, accounts);
			c.moveToNext();

			if (item != null) {
				if (item.account == null || item.localPath == null) 
				{
					invalidInfos.add(item);
				} 
				else {
					infos.add(item);
				}
			}
		}

        for (AutoUpdateInfo info : invalidInfos)
        {
            removeAutoUpdateInfo(info);
        }

		c.close();

		Log.d(DEBUG_TAG, String.format("loaded %d auto update info", infos.size()));
		return infos;
	}

	private Map<String, Account> getAllAccouts()
	{
		AccountManager accountMgr = new AccountManager(
				SeadroidApplication.getAppContext());
		Map<String, Account> accounts = new HashMap<String, Account>();
		for (Account account : accountMgr.getAccountList()) 
		{
			accounts.put(account.getSignature(), account);
		}

		return accounts;
	}
/**
 * 获取指定游标c中的AutoUpdateInfo 
 * @param c
 * @param accounts
 * @return
 */
	private AutoUpdateInfo cursorToAutoUpdateInfo(Cursor c,
			Map<String, Account> accounts) 
	{
		String accountSignature = c.getString(0);
		String repoID = c.getString(1);
		String repoName = c.getString(2);
		String parentDir = c.getString(3);
		String localPath = c.getString(4);

		// infos whose account or file has been deleted would be removed in the
		// while loop
		if (!new File(localPath).exists()) 
		{
			localPath = null;
		}

		Account account = accounts.get(accountSignature);
		AutoUpdateInfo info = new AutoUpdateInfo(account, repoID, repoName,
				parentDir, localPath);
		return info;
	}

}
