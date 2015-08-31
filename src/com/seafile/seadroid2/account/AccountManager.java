package com.seafile.seadroid2.account;

import java.util.ArrayList;
import java.util.List;


import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


public class AccountManager 
{
    @SuppressWarnings("unused")
    private static String DEBUG_TAG = "AccountManager";

    AccountDbHelper dbHelper;
    Context context;

    public AccountManager(Context context) 
    {
        dbHelper = new AccountDbHelper(context);
        this.context = context;
    }

    public void login() {

    }

    public void getAccount(String server) 
    {

    }
/**
 * 使用SharedPreferences中存储的账号
 * @return
 */
    public Account getDefaultAccount() 
    {
        SharedPreferences settings = context.getSharedPreferences("Account", 0);
        String defaultServer = settings.getString("server", "");
        String defaultEmail = settings.getString("email", "");

        if (defaultServer.length() == 0 || defaultEmail.length() == 0)
            return null;

        return getAccount(defaultServer, defaultEmail);
    }
    /**
     * 根据server,email获取account的信息
     * @param server
     * @param email
     * @return
     */
    public Account getAccount(String server, String email) 
    {
        SQLiteDatabase db = dbHelper.getReadableDatabase(); //以读方式打开数据库

        String[] projection = {
                AccountDbHelper.COLUMN_SERVER,
                AccountDbHelper.COLUMN_EMAIL,
                AccountDbHelper.COLUMN_TOKEN
        };

        Cursor c = db.query(
             AccountDbHelper.TABLE_NAME,
             projection,
             "server=? and email=?",
             new String[] { server, email },
             null,   // don't group the rows
             null,   // don't filter by row groups
             null    // The sort order
         );

        if (c.moveToFirst() == false) 
        {
            c.close();
            db.close();
            return null;
        }

        Account account = cursorToAccount(c);
        c.close();
        db.close();
        return account;
    }
    /**
     * 根据signature找到对应的account,没有返回null
     * @param signature
     * @return
     */
    public Account getAccountBySignature(String signature)
    {
        List<Account> accounts = getAccountList();
        for (int i = 0; i < accounts.size(); ++i) {
            if (signature.equals(accounts.get(i).getSignature())) 
            {
                return accounts.get(i);
            }
        }
        return null;
    }
/**
 * 保存新的account到Account表
 * 根据account.token判断是否是新account，若是account.token已经变化，则删除旧版本，将新版插入Account表
 * @param account
 */
    public void saveAccount(Account account) 
    {
        Account old = getAccount(account.server, account.email);
        if (old != null)
        {
            if (old.token.equals(account.token))
                return;
            else
                deleteAccount(old);
        }

        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(AccountDbHelper.COLUMN_SERVER, account.server);
        values.put(AccountDbHelper.COLUMN_EMAIL, account.email);
        values.put(AccountDbHelper.COLUMN_TOKEN, account.token);

        // Insert the new row, returning the primary key value of the new row
        db.replace(AccountDbHelper.TABLE_NAME, null, values);
        db.close();
    }
/**
 * 将 account保存到SharedPreferences中，只保存serve，email
 * @param account
 */
    public void saveDefaultAccount(Account account) 
    {
        // save to shared preference
        SharedPreferences sharedPref = context.getSharedPreferences("Account", 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("server", account.server);
        editor.putString("email", account.email);
        editor.commit();

        // save to db
        saveAccount(account);
    }
/**
 * 将newAccount的账户信息保存到SharedPreferences中，并更新account表
 * @param oldAccount
 * @param newAccount
 */
    public void updateAccount(Account oldAccount, Account newAccount) {
     // save to shared preference
        SharedPreferences sharedPref = context.getSharedPreferences("Account", 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("server", newAccount.server);
        editor.putString("email", newAccount.email);
        editor.commit();
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(AccountDbHelper.COLUMN_SERVER, newAccount.server);
        values.put(AccountDbHelper.COLUMN_EMAIL, newAccount.email);
        values.put(AccountDbHelper.COLUMN_TOKEN, newAccount.token);
        
        db.update(AccountDbHelper.TABLE_NAME, values, "server=? and email=?",
                new String[] { oldAccount.server, oldAccount.email });
        db.close();
    }
    
    public void deleteAccount(Account account)
    {
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(AccountDbHelper.TABLE_NAME,  "server=? and email=?",
                new String[] { account.server, account.email });
        db.close();
    }

    public List<Account> getAccountList() 
    {
        List<Account> accounts = new ArrayList<Account>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                AccountDbHelper.COLUMN_SERVER,
                AccountDbHelper.COLUMN_EMAIL,
                AccountDbHelper.COLUMN_TOKEN
        };

        Cursor c = db.query(
             AccountDbHelper.TABLE_NAME,
             projection,
             null,
             null,
             null,   // don't group the rows
             null,   // don't filter by row groups
             null    // The sort order
        );

        c.moveToFirst();
        while (!c.isAfterLast()) {
            Account account = cursorToAccount(c);
            accounts.add(account);
            c.moveToNext();
        }

        c.close();
        db.close();
        return accounts;
    }

    public boolean accountExist(String url, String username) {
        // TODO
        return false;
    }
/**
 * 返回默认的账户 （"http://cloud.seafile.com", "demo@seafile.com", "demo", null）
 * @return
 */
    public  Account getDemoAccout()
    {
        return new Account("http://cloud.seafile.com", "demo@seafile.com", "demo", null);
    }

    private Account cursorToAccount(Cursor cursor) {
        Account account = new Account();
        account.server = cursor.getString(0);
        account.email = cursor.getString(1);
        account.token = cursor.getString(2);
        return account;
    }

}
