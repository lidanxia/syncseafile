package com.seafile.seadroid2.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import com.seafile.seadroid2.SeadroidApplication;
import com.seafile.seadroid2.account.Account;

public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DEBUG_TAG = "DatabaseHelper";
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "data.db";

    // FileCache table
    private static final String FILECACHE_TABLE_NAME = "FileCache";

    private static final String FILECACHE_COLUMN_ID = "id";
    private static final String FILECACHE_COLUMN_FILEID = "fileid";
    private static final String FILECACHE_COLUMN_REPO_NAME = "repo_name";
    private static final String FILECACHE_COLUMN_REPO_ID = "repo_id";
    private static final String FILECACHE_COLUMN_PATH = "path";
    private static final String FILECACHE_COLUMN_ACCOUNT = "account";

    // RepoDir table
    private static final String REPODIR_TABLE_NAME = "RepoDir";

    private static final String REPODIR_COLUMN_ID = "id";
    private static final String REPODIR_COLUMN_ACCOUNT = "account";
    private static final String REPODIR_COLUMN_REPO_NAME = "repo_name";
    private static final String REPODIR_COLUMN_REPO_ID = "repo_id";
    private static final String REPODIR_COLUMN_REPO_DIR = "repo_dir";

    private static final String DIRENTS_CACHE_TABLE_NAME = "DirentsCache";

    private static final String DIRENTS_CACHE_COLUMN_ID = "id";
    private static final String DIRENTS_CACHE_COLUMN_REPO_ID = "repo_id";
    private static final String DIRENTS_CACHE_COLUMN_PATH = "path";
    private static final String DIRENTS_CACHE_COLUMN_DIR_ID = "dir_id";
    private static final String DIRENTS_CACHE_COLUMN_CONTENT = "content";

    private static final String FILES_MODIFIED_TABLE_NAME = "FilesModifiedTime";//local表

    private static final String FILES_MODIFIED_COLUMN_ID = "id";
    private static final String FILES_MODIFIED_COLUMN_FILE_NAME= "filie_name";
    private static final String FILES_MODIFIED_COLUMN_FILE_TIME = "m_time";
    //private static final String FILES_MODIFIED_COLUMN_SERVER_TIME = "s_time";
    //private static final String FILES_MODIFIED_COLUMN_IP = "ip";
    
    private static final String FILES_SERVER_TABLE_NAME = "FileServerTime";//SERVER表
    
    private static final String FILES_SERVER_COLUMN_ID="id";
    private static final String FILES_SERVER_COLUMN__FILE_NAME="sfile_name";
    private static final String FILES_SERVER_COLUMN__FILE_TIME="sm_time";
    
    
    private static final String FILES_SYN_TABLE_NAME = "SynTable";//SYN表
    
    private static final String FILES_SYN_COLUMN_ID="id";
    private static final String FILES_SYN_COLUMN__FILE_NAME="synfile_name";
    private static final String FILES_SYN_COLUMN__FILE_TIME="synfile_time";
    private static final String FILES_SYN_COLUMN__FILE_IP="synfile_ip";
    
    

    
    
    
    private static final String SQL_CREATE_FILES_MODIFIED_TABLE =
    		 "CREATE TABLE " +FILES_MODIFIED_TABLE_NAME + " ("
    		+ FILES_MODIFIED_COLUMN_ID +" INTEGER PRIMARY KEY, " 
            + FILES_MODIFIED_COLUMN_FILE_NAME + " TEXT NOT NULL, "
            + FILES_MODIFIED_COLUMN_FILE_TIME+ " TEXT NOT NULL);";
           
    private static final String SQL_CREATE_FILES_SERVER_TABLE =
   		 "CREATE TABLE " +FILES_SERVER_TABLE_NAME + " ("
   		   + FILES_SERVER_COLUMN_ID +" INTEGER PRIMARY KEY, "
           + FILES_SERVER_COLUMN__FILE_NAME + " TEXT NOT NULL, "
           + FILES_SERVER_COLUMN__FILE_TIME+ " TEXT NOT NULL);";    
    
    private static final String SQL_CREATE_FILES_SYN_TABLE =
      		 "CREATE TABLE " +FILES_SYN_TABLE_NAME + " ("
      		  + FILES_SYN_COLUMN_ID +" INTEGER PRIMARY KEY, "
              + FILES_SYN_COLUMN__FILE_NAME + " TEXT NOT NULL, "
              + FILES_SYN_COLUMN__FILE_TIME+ " TEXT NOT NULL,"
              + FILES_SYN_COLUMN__FILE_IP+ " TEXT NOT NULL);";   
     
    private static final String SQL_CREATE_FILECACHE_TABLE =
        "CREATE TABLE " + FILECACHE_TABLE_NAME + " ("
        + FILECACHE_COLUMN_ID + " INTEGER PRIMARY KEY, "
        + FILECACHE_COLUMN_FILEID + " TEXT NOT NULL, "
        + FILECACHE_COLUMN_PATH + " TEXT NOT NULL, "
        + FILECACHE_COLUMN_REPO_NAME + " TEXT NOT NULL, "
        + FILECACHE_COLUMN_REPO_ID + " TEXT NOT NULL, "
        + FILECACHE_COLUMN_ACCOUNT + " TEXT NOT NULL);";

    private static final String SQL_CREATE_REPODIR_TABLE =
        "CREATE TABLE " + REPODIR_TABLE_NAME + " ("
        + REPODIR_COLUMN_ID + " INTEGER PRIMARY KEY, "
        + REPODIR_COLUMN_ACCOUNT + " TEXT NOT NULL, "
        + REPODIR_COLUMN_REPO_NAME + " TEXT NOT NULL, "
        + REPODIR_COLUMN_REPO_ID + " TEXT NOT NULL, "
        + REPODIR_COLUMN_REPO_DIR + " TEXT NOT NULL);";

    private static final String SQL_CREATE_DIRENTS_CACHE_TABLE =
        "CREATE TABLE " + DIRENTS_CACHE_TABLE_NAME + " ("
        + DIRENTS_CACHE_COLUMN_ID + " INTEGER PRIMARY KEY, "
        + DIRENTS_CACHE_COLUMN_REPO_ID + " TEXT NOT NULL, "
        + DIRENTS_CACHE_COLUMN_PATH + " TEXT NOT NULL, "
        + DIRENTS_CACHE_COLUMN_DIR_ID + " TEXT NOT NULL, "
        + DIRENTS_CACHE_COLUMN_CONTENT + " TEXT NOT NULL);";

    // Use only single dbHelper to prevent multi-thread issue and db is closed exception
    // Reference http://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android
    private static DatabaseHelper dbHelper = null;
    private SQLiteDatabase database = null;

    public static DatabaseHelper getDatabaseHelper()
    {
        if (dbHelper != null)
            return dbHelper;
        dbHelper = new DatabaseHelper(SeadroidApplication.getAppContext());
        dbHelper.database = dbHelper.getWritableDatabase();//打开数据库
        return dbHelper;
    }

    private DatabaseHelper(Context context) 
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) 
    {
        createFileCacheTable(db);
        createRepoDirTable(db);
        createDirentsCacheTable(db);
        createFileModifiedTable(db);
        createFileServerTable(db);
        createSynTable(db);
    }
    /**
     * 新建LOCAL表
     * @param db
     */
    
    private void createFileModifiedTable(SQLiteDatabase db) 
    {
        db.execSQL(SQL_CREATE_FILES_MODIFIED_TABLE);
    }
    
    
    /**
     * 新建server表
     */
    private void createFileServerTable(SQLiteDatabase db)
    {
    	db.execSQL(SQL_CREATE_FILES_SERVER_TABLE);
    }
    
    
    /**
     * 新建syn表
     */
    private void createSynTable(SQLiteDatabase db)
    {
    	db.execSQL(SQL_CREATE_FILES_SYN_TABLE);
    }
    
   
    
    
    
    
    /**
     * 在local表中插入数据
     * @param filename
     * @param m_time
     * @param type
     */
    
    public void saveFileModifiedTable(String filename,long m_time) 
    {
    	//deleteFileModified(filename);
        // Create a new map of values, where column names are the keys
    	//在存放之前，先对local表进行查找，如果找到同文件名的且时间小于当前要存的时间则替换，为了文件更新时保存新的时间
    	String time=getFileTime(filename);
    	if(time!=null)
    	{
    		long stime=Long.parseLong(time);
    		if(m_time>stime)
    		{
    			deleteFileModified(filename);//将原来的旧记录删除掉
    			System.out.println("@@将local表中原来的旧记录删除掉");
    		}
    	}
        ContentValues values = new ContentValues();
        values.put(FILES_MODIFIED_COLUMN_FILE_NAME, filename);
        values.put(FILES_MODIFIED_COLUMN_FILE_TIME, m_time);
        database.insert(FILES_MODIFIED_TABLE_NAME, null, values);
        System.out.println("存到local表里");
        Log.d(DEBUG_TAG,  "FileModifiedTable item has saved to FileModifiedTable!");
    }
    
    /**
     * 在server表中插入数据
     */
    public void saveFileServerTable(String sfilename,long sm_time)
    {
    	//在存放之前，先对server表进行查找，如果找到同文件名的且时间小于当前要存的时间则替换，为了文件更新时保存新的时间 
    	String time=getServerFileTime(sfilename);
    	if(time!=null)
    	{
    		long stime=Long.parseLong(time);
    		if(sm_time>stime)
    		{
    			deleteFileServer(sfilename);//将原来的旧记录删除掉
    			System.out.println("@@将server表中原来的旧记录删除掉");
    		}
    	}
    	ContentValues values = new ContentValues();
         values.put(FILES_SERVER_COLUMN__FILE_NAME, sfilename);
         values.put(FILES_SERVER_COLUMN__FILE_TIME, sm_time);
         database.insert(FILES_SERVER_TABLE_NAME, null, values);
         System.out.println("存到server表里");
         Log.d(DEBUG_TAG,  "FileServerTable item has saved to FileServerTable!");
    }
    
    /**
     * 在syn表中插入数据
     * 
     */
    
    public void saveSynTable(String sfilename,long sm_time,String ip)
    {
    	 ContentValues values = new ContentValues();
         values.put(FILES_SYN_COLUMN__FILE_NAME, sfilename);
         values.put(FILES_SYN_COLUMN__FILE_TIME, sm_time);
         values.put(FILES_SYN_COLUMN__FILE_IP,ip);
    	 
    	 
         
         database.insert(FILES_SYN_TABLE_NAME, null, values);
         System.out.println(ip+"  "+sfilename+"   存到syn表里");
         Log.d(DEBUG_TAG,  "FileSynTable item has saved to FileSynTable!");
    }
    
    /**
     * 
     * 从sync表中读出数据
     */
    public ArrayList<String[]> getSynTable()
    {
    	 String[] projection = {
    			 FILES_SYN_COLUMN__FILE_NAME,
    			 FILES_SYN_COLUMN__FILE_TIME,
    			 FILES_SYN_COLUMN__FILE_IP
    			 
         		
             
         };
    	 Cursor cursor = database.query(
    			 FILES_SYN_TABLE_NAME,
    	            projection,
    	            null,
    	            null,
    	            null,   // don't group the rows
    	            null,   // don't filter by row groups
    	            FILES_SYN_COLUMN__FILE_NAME);  // The sort order
    	  if (cursor.moveToFirst() == false)
          {
              cursor.close();
              System.out.println("syn表中没有文件");
              return null;
          }
    	  ArrayList<String[]> infos=new ArrayList<String[]>();
          while(!cursor.isAfterLast())
          {
          String[] dir=new String[3];
          dir[0]= cursor.getString(0);
          dir[1]= cursor.getString(1);
          dir[2]= cursor.getString(2);
        
          infos.add(dir);
          cursor.moveToNext();
          }
          cursor.close();
          return infos;
    	 
    }
    
    /**
     * 整理计时器到后的sync表1：得到文件的种类a,b,c
     * 
     */
    public  ArrayList<String> settleSynTable1()
    {
    	String sql="select synfile_name from SynTable group by synfile_name";
    	Cursor cursor=database.rawQuery(sql,null);
    	ArrayList<String> infos=new ArrayList<String>();
    	if (cursor.moveToFirst() == false)
        {
            cursor.close();
            System.out.println("settleSynTable1()——整理后的表1中没有文件");
            return null;
        }
    	
    	while(!cursor.isAfterLast())
    	{
    		 String dir=new String();
    	     dir= cursor.getString(0);
    	     infos.add(dir);
    	     cursor.moveToNext();
    	}
    	cursor.close();
        return infos;
    	
    }
    
    /**
     * 将syn表中的数据清空
     */
    public void emptySynTable()
    {
    	database.execSQL("DELETE FROM SynTable");
    }
    
    /**
     * 整理计时器到后的sync表2:得到时间最大的文件，有多个最大文件都列出来
     * 
     */
    
    public ArrayList<String[]> settleSynTable2()
    {
    	String sql="select synfile_name,synfile_time," +
    			"synfile_ip from SynTable as a where " +
    			"synfile_time= (select max(b.synfile_time)" +
    			"from SynTable as b where a.synfile_name=" +
    			"b.synfile_name) order by synfile_name";
    	//String sql="select * from FILES_SYN_TABLE";
    	ArrayList<String[]> infos=new ArrayList<String[]>();
        Cursor cursor=database.rawQuery(sql, null);
        if (cursor.moveToFirst() == false)
        {
            cursor.close();
            System.out.println("settleSynTable2()——整理后的表中没有文件");
            return null;
        }
  	    
        while(!cursor.isAfterLast())
        {
        String[] dir=new String[3];
        dir[0]= cursor.getString(0);
        dir[1]= cursor.getString(1);
        dir[2]= cursor.getString(2);
      
        infos.add(dir);
        cursor.moveToNext();
        }
        cursor.close();
        return infos;

    }
    
    
    
    
    /**
     * 
     * 将local表的内容读出
     */
    public ArrayList<String[]> getFileFromModifiedTable()
    {
    	 String[] projection = {
         		FILES_MODIFIED_COLUMN_FILE_NAME,
         		FILES_MODIFIED_COLUMN_FILE_TIME
         		
             
         };
    	 Cursor cursor = database.query(
    			 FILES_MODIFIED_TABLE_NAME,
    	            projection,
    	            null,
    	            null,
    	            null,   // don't group the rows
    	            null,   // don't filter by row groups
    	            null);  // The sort order
    	  if (cursor.moveToFirst() == false)
          {
              cursor.close();
              System.out.println("LOCAL表中没有文件");
              return null;
          }
    	  ArrayList<String[]> infos=new ArrayList<String[]>();
          while(!cursor.isAfterLast())
          {
          String[] dir=new String[2];
          dir[0]= cursor.getString(0);
          dir[1]= cursor.getString(1);
        
          infos.add(dir);
          cursor.moveToNext();
          }
          cursor.close();
          return infos;
    	 
    }
    
    /**
     * 
     * 将server表中的内容读出
     */
    public ArrayList<String[]> getFileFromServerTable()
    {
    	 String[] projection = {
    			 FILES_SERVER_COLUMN__FILE_NAME,
    			 FILES_SERVER_COLUMN__FILE_TIME    
          };
    	 Cursor cursor = database.query(
    			 FILES_SERVER_TABLE_NAME,
    	            projection,
    	            null,
    	            null,
    	            null,   // don't group the rows
    	            null,   // don't filter by row groups
    	            null);  // The sort order
    	 if (cursor.moveToFirst() == false)
         {
             cursor.close();
             System.out.println("SERVER表中没有文件");
             return null;
         }
    	  ArrayList<String[]> infos=new ArrayList<String[]>();
          while(!cursor.isAfterLast())
          {
          String[] dir=new String[2];
          dir[0]= cursor.getString(0);
          dir[1]= cursor.getString(1);
        
          infos.add(dir);
          cursor.moveToNext();
          }
          cursor.close();
          return infos;
    	 
    }
    
/**
 * 在server表中删除数据
 * @param filename
 */
    
     public void deleteFileModified(String filename)
     
    {
   
        database.delete(FILES_SERVER_TABLE_NAME,FILES_SERVER_COLUMN__FILE_NAME + "=?",
                new String[] {filename});
        Log.d(DEBUG_TAG,  "servertable item has deleted from serverTable !");
    }
     
     /**
      * 在local表中删除数据
      * @param filename
      */
         
          public void deleteFileServer(String filename)
          
         {
        
             database.delete(FILES_MODIFIED_TABLE_NAME,FILES_MODIFIED_COLUMN_FILE_NAME + "=?",
                     new String[] {filename});
             Log.d(DEBUG_TAG,  "FileModifiedTable item has deleted from fileModifiedTable !");
         }
   
    
    
    
/**
 * 在local表中根据文件名找到文件的修改时间
 * @param filename
 * @return
 */
    public String getFileTime(String filename)
    {
        String[] projection = {
        		FILES_MODIFIED_COLUMN_FILE_NAME,
        		FILES_MODIFIED_COLUMN_FILE_TIME
        };

        String selectClause = String.format("%s = ?",
        		FILES_MODIFIED_COLUMN_FILE_NAME);

        String[] selectArgs = {filename };


        Cursor cursor = database.query(
        	FILES_MODIFIED_TABLE_NAME,
            projection,
            selectClause,
            selectArgs,
            null,   // don't group the rows
            null,   // don't filter by row groups
            null);  // The sort order

        if (cursor.moveToFirst() == false)
        {
            cursor.close();
            return null;
        }

        //String dir[]=new String[2];
        //dir[0]= cursor.getString(0);
        String time= cursor.getString(1);
        cursor.close();

        return time;
    }
    
    /**
     * 在server表中根据文件名找到文件的修改时间
     * @param filename
     * @return
     */
    public String getServerFileTime(String filename)
    {
        String[] projection = {
        		FILES_SERVER_COLUMN__FILE_NAME,
   			    FILES_SERVER_COLUMN__FILE_TIME  
        };

        String selectClause = String.format("%s = ?",
        		FILES_SERVER_COLUMN__FILE_NAME);

        String[] selectArgs = {filename };


        Cursor cursor = database.query(
        		FILES_SERVER_TABLE_NAME,
            projection,
            selectClause,
            selectArgs,
            null,   // don't group the rows
            null,   // don't filter by row groups
            null);  // The sort order

        if (cursor.moveToFirst() == false)
        {
            cursor.close();
            return null;
        }

        //String dir[]=new String[2];
        //dir[0]= cursor.getString(0);
        String time= cursor.getString(1);
        cursor.close();

        return time;
    }

 
    
   /**
  * 创建文件缓存表   
  * @param db
  */
    private void createFileCacheTable(SQLiteDatabase db) 
    {
        db.execSQL(SQL_CREATE_FILECACHE_TABLE);
        db.execSQL("CREATE INDEX fileid_index ON " + FILECACHE_TABLE_NAME
                + " (" + FILECACHE_COLUMN_FILEID + ");");
        db.execSQL("CREATE INDEX repoid_index ON " + FILECACHE_TABLE_NAME
                + " (" + FILECACHE_COLUMN_REPO_ID + ");");
        db.execSQL("CREATE INDEX account_index ON " + FILECACHE_TABLE_NAME
                + " (" + FILECACHE_COLUMN_ACCOUNT + ");");
    }

    private void createRepoDirTable(SQLiteDatabase db) 
    {
        db.execSQL(SQL_CREATE_REPODIR_TABLE);

        String sql;
        sql = String.format("CREATE INDEX account_reponame_index ON %s (%s, %s)",
                            REPODIR_TABLE_NAME,
                            REPODIR_COLUMN_ACCOUNT,
                            REPODIR_COLUMN_REPO_NAME);
        db.execSQL(sql);
        sql = String.format("CREATE UNIQUE INDEX account_reponame_repoid_index ON %s (%s, %s, %s)",
                            REPODIR_TABLE_NAME,
                            REPODIR_COLUMN_ACCOUNT,
                            REPODIR_COLUMN_REPO_NAME,
                            REPODIR_COLUMN_REPO_ID);
        db.execSQL(sql);
    }

    private void createDirentsCacheTable(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_DIRENTS_CACHE_TABLE);

        String sql;
        sql = String.format("CREATE INDEX repo_path_index ON %s (%s, %s)",
                            DIRENTS_CACHE_TABLE_NAME,
                            DIRENTS_CACHE_COLUMN_REPO_ID,
                            DIRENTS_CACHE_COLUMN_PATH);
        db.execSQL(sql);
    }
  /**
   * 若是Seafile目录中有文件时，删除，并删除相关的RepoDir，DirentsCache，FileCache表
   */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // This database is only a cache for online data, so its upgrade（提升，向上） policy is
        // to simply to discard the data and start over

        File dir = new File(DataManager.getExternalRootDirectory());
        for (File f : dir.listFiles())
        {
            if (f.isFile()) 
            {
                f.delete();
            }
        }
        db.execSQL("DROP TABLE IF EXISTS " + FILECACHE_TABLE_NAME + ";");
        db.execSQL("DROP TABLE IF EXISTS " + REPODIR_TABLE_NAME + ";");
        db.execSQL("DROP TABLE IF EXISTS " + DIRENTS_CACHE_TABLE_NAME + ";");
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onUpgrade(db, oldVersion, newVersion);
    }
/**
 * 根据目录id，路径，获取文件缓存表FileCache表中的对应的文件信息
 * @param repoID
 * @param path
 * @param dataManager
 * @return
 */
    public SeafCachedFile getFileCacheItem(String repoID,
                                           String path, DataManager dataManager)
    {
        String[] projection = {
                FILECACHE_COLUMN_ID,
                FILECACHE_COLUMN_FILEID,
                FILECACHE_COLUMN_REPO_NAME,
                FILECACHE_COLUMN_REPO_ID,
                FILECACHE_COLUMN_PATH,
                FILECACHE_COLUMN_ACCOUNT
        };

        Cursor c = database.query(
             FILECACHE_TABLE_NAME,
             projection,
             FILECACHE_COLUMN_REPO_ID
             + "=? and " + FILECACHE_COLUMN_PATH + "=?",
             new String[] { repoID, path },
             null,   // don't group the rows
             null,   // don't filter by row groups
             null    // The sort order
        );

        if (c.moveToFirst() == false) {
            c.close();
            return null;
        }

        SeafCachedFile item = cursorToFileCacheItem(c, dataManager);
        c.close();
        return item;
    }

    // XXX: Here we can use SQLite3  "INSERT OR REPLACE" for convience
    /**
     * 将SeafCachedFile项存到文件缓存表中FileCache（id，fileid , repo_name , repo_id, path, account ）
     * @param item
     * @param dataManager
     */
    public void saveFileCacheItem(SeafCachedFile item, DataManager dataManager) 
    {
        SeafCachedFile old = getFileCacheItem(item.repoID, item.path, dataManager);
        if (old != null) 
        {
            deleteFileCacheItem(old);
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(FILECACHE_COLUMN_FILEID, item.fileID);
        values.put(FILECACHE_COLUMN_REPO_NAME, item.repoName);
        values.put(FILECACHE_COLUMN_REPO_ID, item.repoID);
        values.put(FILECACHE_COLUMN_PATH, item.path);
        values.put(FILECACHE_COLUMN_ACCOUNT, item.accountSignature);

        // Insert the new row, returning the primary key value of the new row
        database.insert(FILECACHE_TABLE_NAME, null, values);
        Log.d(DEBUG_TAG,  "SeafCachedFile item has saved to FileCache!");
    }
/**
 * 根据SeafCachedFile，删除FileCahe表对应的表项
 * @param item
 */
    public void deleteFileCacheItem(SeafCachedFile item)
    {
        if (item.id != -1) 
        {
            database.delete(FILECACHE_TABLE_NAME,  FILECACHE_COLUMN_ID + "=?",
                    new String[] { String.valueOf(item.id) });
        } else
            database.delete(FILECACHE_TABLE_NAME,  FILECACHE_COLUMN_REPO_ID + "=? and " + FILECACHE_COLUMN_PATH + "=?",
                new String[] { item.repoID, item.path });
        Log.d(DEBUG_TAG,  "SeafCachedFile item has deleted from FileCache!");
    }
/**
 * 返回该dataManager的所存账号的全部文件缓存项目（文件缓存表）
 * @param dataManager
 * @return
 */
    public List<SeafCachedFile> getFileCacheItems(DataManager dataManager)
    {
        List<SeafCachedFile> files = new ArrayList<SeafCachedFile>();

        String[] projection = {
                FILECACHE_COLUMN_ID,
                FILECACHE_COLUMN_FILEID,
                FILECACHE_COLUMN_REPO_NAME,
                FILECACHE_COLUMN_REPO_ID,
                FILECACHE_COLUMN_PATH,
                FILECACHE_COLUMN_ACCOUNT
        };

        Cursor c = database.query(
             FILECACHE_TABLE_NAME,
             projection,
             FILECACHE_COLUMN_ACCOUNT + "=?",
             new String[] { dataManager.getAccount().getSignature() },
             null,   // don't group the rows
             null,   // don't filter by row groups
             null    // The sort order
        );

        c.moveToFirst();
        while (!c.isAfterLast()) {
            SeafCachedFile item = cursorToFileCacheItem(c, dataManager);
            files.add(item);
            c.moveToNext();
        }

        c.close();
        return files;
    }
/**
 * 将cuosor的内容转换成  SeafCachedFile项
 * @param cursor
 * @param dataManager
 * @return SeafCachedFile item
 */
    private SeafCachedFile cursorToFileCacheItem(Cursor cursor, DataManager dataManager)
    {
        SeafCachedFile item = new SeafCachedFile();
        item.id = cursor.getInt(0);
        item.fileID = cursor.getString(1);
        item.repoName = cursor.getString(2);
        item.repoID = cursor.getString(3);
        item.path = cursor.getString(4);
        item.accountSignature = cursor.getString(5);
        item.file = dataManager.getLocalRepoFile(item.repoName, item.repoID, item.path);
        return item;
    }

    /**
     * 根据参数获取该目录在本地的目录
     * @param account
     * @param repoName
     * @param repoID
     * @return
     */

    public String getRepoDir(Account account, String repoName, String repoID)
    {
        String[] projection = {
            REPODIR_COLUMN_REPO_DIR
        };

        String selectClause = String.format("%s = ? and %s = ? and %s = ?",
                                            REPODIR_COLUMN_ACCOUNT,
                                            REPODIR_COLUMN_REPO_NAME,
                                            REPODIR_COLUMN_REPO_ID);

        String[] selectArgs = { account.getSignature(), repoName, repoID };


        Cursor cursor = database.query(
            REPODIR_TABLE_NAME,
            projection,
            selectClause,
            selectArgs,
            null,   // don't group the rows
            null,   // don't filter by row groups
            null);  // The sort order

        if (cursor.moveToFirst() == false)
        {
            cursor.close();
            return null;
        }

        String dir = cursor.getString(0);
        cursor.close();

        return dir;
    }
    /**
     * 根据账号，仓库目录，获取具体的仓库信息
     * @param account
     * @param repodir
     * @return repoID，repoName
     */
    public String[] getRepoDirInfo(Account account,String repodir)
    {
    	String[] projection = {REPODIR_COLUMN_REPO_ID,REPODIR_COLUMN_REPO_NAME };

            String selectClause = String.format("%s = ? and %s = ?",
                    REPODIR_COLUMN_ACCOUNT,
                    REPODIR_COLUMN_REPO_DIR);

            String[] selectArgs = {account.getSignature(),repodir};

            Cursor cursor = database.query(
                REPODIR_TABLE_NAME,
                projection,
                selectClause,
                selectArgs,
                null,   // don't group the rows
                null,   // don't filter by row groups
                null);  // The sort order
            if (cursor.moveToFirst() == false) 
            {
                cursor.close();
                System.out.println("！！！数据表中没有找到");  
                return null;
            }
            String[] dir=new String[2];
            dir[0]= cursor.getString(0);
            dir[1]= cursor.getString(1);
            cursor.close();
            return dir;
    }
    /**
     * 根据仓库名称找到所有仓库信息
     */
    public ArrayList<String[]>getAllRepoDirInfo(Account account)
    {
    	String[] projection = {REPODIR_COLUMN_REPO_NAME ,REPODIR_COLUMN_REPO_ID,REPODIR_COLUMN_REPO_DIR };

            String selectClause = String.format("%s = ?", REPODIR_COLUMN_ACCOUNT);

            String[] selectArgs = {account.getSignature()};

            Cursor cursor = database.query(
                REPODIR_TABLE_NAME,
                projection,
                selectClause,
                selectArgs,
                null,   // don't group the rows
                null,   // don't filter by row groups
                null);  // The sort order
            if (cursor.moveToFirst() == false) 
            {
                cursor.close();
                System.out.println("！！！数据表中没有找到");  
                return null;
            }
            ArrayList<String[]> infos=new ArrayList<String[]>();
            while(!cursor.isAfterLast())
            {
            String[] dir=new String[3];
            dir[0]= cursor.getString(0);
            dir[1]= cursor.getString(1);
            dir[2]= cursor.getString(2);
            infos.add(dir);
            cursor.moveToNext();
            }
            cursor.close();
            return infos;
    }
    /**
     * 判断该仓库在repoDir中是否已经存在
     * @param account
     * @param repoName
     * @return
     */
    public boolean repoDirExists(Account account, String repoName) {

        String[] projection = {
            REPODIR_COLUMN_REPO_DIR
        };

        String selectClause = String.format("%s = ? and %s = ?",
                                            REPODIR_COLUMN_ACCOUNT,
                                            REPODIR_COLUMN_REPO_NAME);

        String[] selectArgs = { account.getSignature(), repoName };


        Cursor cursor = database.query(
            REPODIR_TABLE_NAME,
            projection,
            selectClause,
            selectArgs,
            null,   // don't group the rows
            null,   // don't filter by row groups
            null);  // The sort order

        boolean exist;
        if (cursor.moveToFirst() == false) {
            exist = false;
        } else {
            exist = false;
        }

        cursor.close();

        return exist;
    }
/**
 * 插数据到仓库目录表，RepoDir（id, account, repo_name, repo_id, repo_dir）
 * @param account
 * @param repoName
 * @param repoID
 * @param dir
 */
    public void saveRepoDirMapping(Account account, String repoName,
                                   String repoID, String dir) 
    {
        String log = String.format("Saving repo dir mapping: account = %s(%s) "
                                   + "repoName = %s"
                                   + "repoID = %s"
                                   + "dir = %s",
                                   account.getEmail(), account.getServerNoProtocol(),
                                   repoName, repoID, dir);

        Log.d(DEBUG_TAG, log);

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(REPODIR_COLUMN_ACCOUNT, account.getSignature());
        values.put(REPODIR_COLUMN_REPO_NAME, repoName);
        values.put(REPODIR_COLUMN_REPO_ID, repoID);
        values.put(REPODIR_COLUMN_REPO_DIR, dir);

        database.insert(REPODIR_TABLE_NAME, null, values);
    }
/**
 * 插数据到目录缓存表，DirentsCache（id, repo_id, path, dir_id, content）
 * @param repoID
 * @param path
 * @param dirID
 * @param content
 */
    public void saveDirents(String repoID, String path, String dirID, String content)
    {
        removeCachedDirents(repoID, path);
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(DIRENTS_CACHE_COLUMN_REPO_ID, repoID);
        values.put(DIRENTS_CACHE_COLUMN_PATH, path);
        values.put(DIRENTS_CACHE_COLUMN_DIR_ID, dirID);
        values.put(DIRENTS_CACHE_COLUMN_CONTENT, content);

        // Insert the new row, returning the primary key value of the new row
        database.insert(DIRENTS_CACHE_TABLE_NAME, null, values);
    }
/**
 * 根据仓库id和path,删除DirentsCache中的表项
 * @param repoID
 * @param path
 */
    public void removeCachedDirents(String repoID, String path) {
        String whereClause = String.format("%s = ? and %s = ?",
            DIRENTS_CACHE_COLUMN_REPO_ID, DIRENTS_CACHE_COLUMN_PATH);

        database.delete(DIRENTS_CACHE_TABLE_NAME, whereClause, new String[] { repoID, path });
    }
/**
 * 根据仓库id，路径，和仓库内具体目录的id,从DirentsCache表中找到context项
 * @param repoID
 * @param path
 * @param dirID
 * @return
 */
    public String getDirents(String repoID, String path, String dirID)
    {
        Pair<String, String> ret = getCachedDirents(repoID, path);
        if (ret == null) 
        {
            return null;
        }

        if (dirID != null && !ret.first.equals(dirID))
        {
            // cache is out of date
            return null;
        }

        return ret.second;
    }
/**
 * 根据仓库ID，path查找DirentsCache表
 * @param repoID
 * @param path
 * @return 返回值是dirID--此时访问的仓库中的具体目录Id, content
 */
    public Pair<String, String> getCachedDirents(String repoID, String path)
    {
        String[] projection = {
            DIRENTS_CACHE_COLUMN_DIR_ID,
            DIRENTS_CACHE_COLUMN_CONTENT
        };

        String selectClause = String.format("%s = ? and %s = ?",
                                            DIRENTS_CACHE_COLUMN_REPO_ID,
                                            DIRENTS_CACHE_COLUMN_PATH);

        String[] selectArgs = { repoID, path };

        Cursor cursor = database.query(
            DIRENTS_CACHE_TABLE_NAME,
            projection,
            selectClause,
            selectArgs,
            null,   // don't group the rows
            null,   // don't filter by row groups
            null);  // The sort order

        if (cursor.moveToFirst() == false) 
        {
            cursor.close();
            return null;
        }

        String dirID = cursor.getString(0);
        String content = cursor.getString(1);
        cursor.close();

        return new Pair<String, String>(dirID, content);
    }
}
