package com.seafile.seadroid2.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.seafile.seadroid2.BrowserActivity;
import com.seafile.seadroid2.NavContext;
import com.seafile.seadroid2.R;
import com.seafile.seadroid2.Utils;
import com.seafile.seadroid2.data.DataManager;
import com.seafile.seadroid2.data.SeafCachedFile;
import com.seafile.seadroid2.data.SeafDirent;
import com.seafile.seadroid2.data.SeafGroup;
import com.seafile.seadroid2.data.SeafItem;
import com.seafile.seadroid2.data.SeafRepo;


public class dirsAdapter extends BaseAdapter {

    private ArrayList <String> items;
    private BrowserActivity mActivity;
    private boolean repoIsEncrypted;

    public dirsAdapter(BrowserActivity activity) 
    {
        this.mActivity = activity;
        items = new ArrayList<String>();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void addEntry(String  entry) {
        items.add(entry);
        // Collections.sort(items);
        notifyDataSetChanged();
    }

    public void add(String entry) {
        items.add(entry);
    }
    /**
     * 更新listview，直接会更新BrowserActivity 
     */
    public void notifyChanged() 
    {
        notifyDataSetChanged();  
    }

    @Override
    public String getItem(int position) {
        return items.get(position);
    }

    public void setItem(String item, int listviewPosition) 
    {
        items.set(listviewPosition, item);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        items.clear();
    }

    public boolean areAllItemsSelectable() {
        return false;
    }

    public int getViewTypeCount() {
        return 2;
    }

    private View getDirsView(String dir_name, View convertView, ViewGroup parent)
    {
        View view = convertView;
        Viewholder viewHolder;

        if (convertView == null) 
        {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            ImageView action = (ImageView) view.findViewById(R.id.list_item_action);
            viewHolder = new Viewholder(title, subtitle, icon,action);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (Viewholder) convertView.getTag();
        }

        viewHolder.title.setText(dir_name);
        viewHolder.icon.setImageResource(R.drawable.folder);
        viewHolder.action.setVisibility(View.INVISIBLE);
        return view;
    }


    private View getFilesView(String file_name, View convertView, ViewGroup parent, int position) 
    {
        View view = convertView;
        Viewholder viewHolder;

        if (convertView == null)
        {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            ImageView action = (ImageView) view.findViewById(R.id.list_item_action);
            viewHolder = new Viewholder(title, subtitle, icon,action);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (Viewholder) convertView.getTag();
        }

        viewHolder.title.setText(file_name);
        viewHolder.icon.setImageResource(Utils.getFileIcon(file_name));
        viewHolder.action.setVisibility(View.INVISIBLE);
        return view;
    }

        private void setDirAction(String dir_name, Viewholder viewHolder, final int position)
        {
            if (repoIsEncrypted) 
            {
                viewHolder.action.setVisibility(View.GONE);
                return;
            }
            viewHolder.action.setVisibility(View.VISIBLE);
            viewHolder.action.setOnClickListener(new OnClickListener() 
            {
                @Override
                public void onClick(View view)
                {
                	
                }
            });
        }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) 
    {
       String item = items.get(position);  //item中记载的事全部路径
       String filename=item.substring(item.lastIndexOf("/")+1);
       File f=new File(item);
        if (f.isDirectory())
        {
            return getDirsView(filename, convertView, parent);
        }
        else 
        {
            return getFilesView(filename, convertView, parent, position);
        }
    }

    private class Viewholder 
    {
        TextView title, subtitle;
        ImageView icon,action;

        public Viewholder(TextView title, TextView subtitle, ImageView icon,ImageView action) 
        {
            super();
            this.icon = icon;
            this.title = title;
            this.subtitle = subtitle;
            this.action = action;
        }
    }

    public void setEncryptedRepo(boolean encrypted) 
    {
        repoIsEncrypted = encrypted;
    }
}

