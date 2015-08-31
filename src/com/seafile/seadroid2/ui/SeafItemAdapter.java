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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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

public class SeafItemAdapter extends BaseAdapter {

    private ArrayList<SeafItem> items;
    private BrowserActivity mActivity;
    private boolean repoIsEncrypted;

    public SeafItemAdapter(BrowserActivity activity) {
        this.mActivity = activity;
        items = new ArrayList<SeafItem>();
    }

    private static final int ACTION_ID_DOWNLOAD = 0;
    private static final int ACTION_ID_UPDATE = 1;
    private static final int ACTION_ID_EXPORT = 2;
    private static final int ACTION_ID_RENAME = 3;
    private static final int ACTION_ID_DELETE = 4;
    private static final int ACTION_ID_SHARE = 5;

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void addEntry(SeafItem entry) {
        items.add(entry);
        // Collections.sort(items);
        notifyDataSetChanged();
    }

    public void add(SeafItem entry) {
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
    public SeafItem getItem(int position) {
        return items.get(position);
    }

    public void setItem(SeafItem item, int listviewPosition) {
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

    public boolean isEnable(int position) {
        SeafItem item = items.get(position);
        return !(item instanceof SeafGroup);
    }

    public boolean isClickable(int position) {
        SeafItem item = items.get(position);
        return !(item instanceof SeafGroup);
    }

    public int getViewTypeCount() {
        return 2;
    }

    public int getItemViewType(int position) {
        SeafItem item = items.get(position);
        if (item instanceof SeafGroup)
            return 0;
        else
            return 1;
    }

    private View getRepoView(SeafRepo repo, View convertView, ViewGroup parent)
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
            viewHolder = new Viewholder(title, subtitle, icon, action);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (Viewholder) convertView.getTag();
        }

        viewHolder.title.setText(repo.getTitle());
        viewHolder.subtitle.setText(repo.getSubtitle());
        viewHolder.icon.setImageResource(repo.getIcon());
        viewHolder.action.setVisibility(View.INVISIBLE);
        return view;
    }

    private View getGroupView(SeafGroup group) 
    {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.group_item, null);
        TextView tv = (TextView) view.findViewById(R.id.textview_groupname);
        tv.setText(group.getTitle());
        return view;
    }
/**
 * 若是目录，设置目录项的视图，否则设置文件项视图
 * @param dirent
 * @param convertView
 * @param parent
 * @param position
 * @return
 */
    private View getDirentView(SeafDirent dirent, View convertView, ViewGroup parent, int position) 
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
            viewHolder = new Viewholder(title, subtitle, icon, action);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (Viewholder) convertView.getTag();
        }

        viewHolder.title.setText(dirent.getTitle());
        if (dirent.isDir())
        {
            viewHolder.subtitle.setText(dirent.getSubtitle());
            viewHolder.icon.setImageResource(dirent.getIcon());
            viewHolder.action.setVisibility(View.VISIBLE);
            setDirAction(dirent, viewHolder, position);
        }
        else {
            setFileView(dirent, viewHolder, position);
        }

        return view;
    }
/**
 * 设置显示文件项的视图
 * @param dirent
 * @param viewHolder
 * @param position
 */
    private void setFileView(SeafDirent dirent, Viewholder viewHolder, int position)
    {
        NavContext nav = mActivity.getNavContext();
        DataManager dataManager = mActivity.getDataManager();
        String repoName = nav.getRepoName();
        String repoID = nav.getRepoID();
        String filePath = Utils.pathJoin(nav.getDirPath(), dirent.name);
        File file = dataManager.getLocalRepoFile(repoName, repoID, filePath);
        boolean cacheExists = false;

        if (file.exists()) 
        {
            SeafCachedFile cf = dataManager.getCachedFile(repoName, repoID, filePath);
            String subtitle = null;
            subtitle = dirent.getSubtitle();
            if (cf != null)
            {
                cacheExists = true;
                subtitle += "," + mActivity.getString(R.string.cached);
            }
            viewHolder.subtitle.setText(subtitle);

            if (Utils.isViewableImage(file.getName())) 
            {
                setImageThumbNail(file, dirent, dataManager, viewHolder);
            }
            else
                viewHolder.icon.setImageResource(dirent.getIcon());

        } 
        else {
            viewHolder.subtitle.setText(dirent.getSubtitle());
            viewHolder.icon.setImageResource(dirent.getIcon());
        }

        setFileAction(dirent, viewHolder, position, cacheExists);
    }

    private void setImageThumbNail(File file, SeafDirent dirent,
            DataManager dataManager, Viewholder viewHolder) {
        if (file.length() < DataManager.MAX_DIRECT_SHOW_THUMB) {
            Bitmap imageBitmap = dataManager.getThumbnail(file);
            if (imageBitmap != null)
                viewHolder.icon.setImageBitmap(imageBitmap);
            else
                viewHolder.icon.setImageResource(dirent.getIcon());
        } else {
            File thumbFile = DataManager.getThumbFile(dirent.id);
            if (thumbFile.exists()) {
                Bitmap imageBitmap;
                final int THUMBNAIL_SIZE = DataManager.caculateThumbnailSizeOfDevice();
                try {
                    // setImageURI does not work correctly under high screen density
                    //viewHolder.icon.setScaleType(ImageView.ScaleType.FIT_XY);
                    //viewHolder.icon.setImageURI(Uri.fromFile(thumbFile));
                    imageBitmap = BitmapFactory.decodeStream(new FileInputStream(thumbFile));
                    imageBitmap = Bitmap.createScaledBitmap(imageBitmap, THUMBNAIL_SIZE,
                            THUMBNAIL_SIZE, false);
                    viewHolder.icon.setImageBitmap(imageBitmap);
                } catch (FileNotFoundException e) {
                    viewHolder.icon.setImageResource(dirent.getIcon());
                }
            } else {
                viewHolder.icon.setImageResource(dirent.getIcon());
            }
        }
    }

    private View getCacheView(SeafCachedFile item, View convertView, ViewGroup parent) {
        View view = convertView;
        Viewholder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(mActivity).inflate(R.layout.list_item_entry, null);
            TextView title = (TextView) view.findViewById(R.id.list_item_title);
            TextView subtitle = (TextView) view.findViewById(R.id.list_item_subtitle);
            ImageView icon = (ImageView) view.findViewById(R.id.list_item_icon);
            ImageView action = (ImageView) view.findViewById(R.id.list_item_action);
            viewHolder = new Viewholder(title, subtitle, icon, action);
            view.setTag(viewHolder);
        } else {
            viewHolder = (Viewholder) convertView.getTag();
        }

        viewHolder.title.setText(item.getTitle());
        viewHolder.subtitle.setText(item.getSubtitle());
        viewHolder.icon.setImageResource(item.getIcon());
        viewHolder.action.setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) 
    {
        SeafItem item = items.get(position);
        if (item instanceof SeafRepo) {
            return getRepoView((SeafRepo)item, convertView, parent);
        } else if (item instanceof SeafGroup) {
            return getGroupView((SeafGroup)item);
        } else if (item instanceof SeafCachedFile) {
            return getCacheView((SeafCachedFile)item, convertView, parent);
        } else {
            return getDirentView((SeafDirent)item, convertView, parent, position);
        }
    }

    private class Viewholder {
        TextView title, subtitle;
        ImageView icon, action;

        public Viewholder(TextView title, TextView subtitle, ImageView icon, ImageView action) {
            super();
            this.icon = icon;
            this.action = action;
            this.title = title;
            this.subtitle = subtitle;
        }
    }
/**
 * 为文件设置可以操作的功能，点击“倒三角”，就可看见这些功能，包括上传，下载，重命名等
 * @param dirent
 * @param viewHolder
 * @param position
 * @param cacheExists
 */
    private void setFileAction(SeafDirent dirent, Viewholder viewHolder,
            final int position, final boolean cacheExists)
    {

        viewHolder.action.setImageResource(R.drawable.drop_down_button);
        viewHolder.action.setVisibility(View.VISIBLE);
        viewHolder.action.setOnClickListener(new OnClickListener() 
        {
            @Override
            public void onClick(View view) 
            {
                SeafDirent dirent = (SeafDirent)items.get(position);
                QuickAction mQuickAction = prepareFileAction(dirent, cacheExists);
                mQuickAction.show(view);
            }
        });
    }
/**
 * 设置目录项的操作
 * @param dirent
 * @param viewHolder
 * @param position
 */
    private void setDirAction(SeafDirent dirent, Viewholder viewHolder, final int position)
    {
        if (repoIsEncrypted) 
        {
            viewHolder.action.setVisibility(View.GONE);
            return;
        }
        viewHolder.action.setImageResource(R.drawable.drop_down_button);
        viewHolder.action.setVisibility(View.VISIBLE);
        viewHolder.action.setOnClickListener(new OnClickListener() 
        {
            @Override
            public void onClick(View view)
            {
                SeafDirent dirent = (SeafDirent)items.get(position);
                QuickAction mQuickAction = prepareDirAction(dirent);
                mQuickAction.show(view);
            }
        });
    }
/**
 * 为每一个文件添加如下功能:共享、下载、更新、导出、重命名Action
 * @param dirent
 * @param cacheExists--文件是否在本地已经存在
 * @return
 */
    private QuickAction prepareFileAction(final SeafDirent dirent, boolean cacheExists)
    {
        final QuickAction mQuickAction = new QuickAction(mActivity);
        Resources resources = mActivity.getResources();
        ActionItem shareAction, downloadAction, updateAction, exportAction, renameAction, deleteAction;

        if (!repoIsEncrypted) 
        {
            shareAction = new ActionItem(ACTION_ID_SHARE,
                    resources.getString(R.string.file_action_share),
                    resources.getDrawable(R.drawable.action_share));
            mQuickAction.addActionItem(shareAction);
        }

        // deleteAction = new ActionItem(ACTION_ID_DELETE,
        //                               resources.getString(R.string.file_action_delete),
        //                               resources.getDrawable(R.drawable.action_delete));
        // mQuickAction.addActionItem(deleteAction);

        renameAction = new ActionItem(ACTION_ID_RENAME,
                resources.getString(R.string.file_action_rename),
                resources.getDrawable(R.drawable.action_rename));
        mQuickAction.addActionItem(renameAction);

        exportAction = new ActionItem(ACTION_ID_EXPORT,
                resources.getString(R.string.file_action_export),
                resources.getDrawable(R.drawable.action_export));
        mQuickAction.addActionItem(exportAction);

        if (cacheExists)
        {
            if (mActivity.hasRepoWritePermission()) 
            {
                updateAction = new ActionItem(ACTION_ID_UPDATE,
                        resources.getString(R.string.file_action_update),
                        resources.getDrawable(R.drawable.action_update));
                mQuickAction.addActionItem(updateAction);
            }

        } 
        else 
        {
            downloadAction = new ActionItem(ACTION_ID_DOWNLOAD,
                    resources.getString(R.string.file_action_download),
                    resources.getDrawable(R.drawable.action_download));
            mQuickAction.addActionItem(downloadAction);
        }

        //setup the action item click listener
        mQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener()
        {
            @Override
            public void onItemClick(QuickAction quickAction, int pos, int actionId) 
            {
                NavContext nav = mActivity.getNavContext();
                String repoName = nav.getRepoName();
                String repoID = nav.getRepoID();
                String dir = nav.getDirPath();
                String path = Utils.pathJoin(dir, dirent.name);
                DataManager dataManager = mActivity.getDataManager();
                String localPath = dataManager.getLocalRepoFile(repoName, repoID, path).getPath();
                switch (actionId) 
                {
                case ACTION_ID_SHARE:
                    mActivity.shareFile(repoID, path);
                    break;
                case ACTION_ID_EXPORT:
                    mActivity.exportFile(dirent.name);
                    break;
                case ACTION_ID_DOWNLOAD:
                    mActivity.onFileSelected(dirent);
                    break;
                case ACTION_ID_UPDATE:
                    mActivity.addUpdateTask(repoID, repoName, dir, localPath);
                    break;
                case ACTION_ID_RENAME:
                    mActivity.renameFile(repoID, repoName, path);
                    break;
                }
            }
        });

        mQuickAction.mAnimateTrack(false);
        return mQuickAction;
    }
/**
 * 为目录设置“分享”功能
 * @param dirent
 * @return
 */
    private QuickAction prepareDirAction(final SeafDirent dirent) 
    {
        final QuickAction mQuickAction = new QuickAction(mActivity);
        Resources resources = mActivity.getResources();
        ActionItem shareAction;
        shareAction = new ActionItem(ACTION_ID_SHARE,
                resources.getString(R.string.file_action_share),
                resources.getDrawable(R.drawable.action_share));
        mQuickAction.addActionItem(shareAction);

        //setup the action item click listener
        mQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() 
        {
            @Override
            public void onItemClick(QuickAction quickAction, int pos, int actionId) {
                NavContext nav = mActivity.getNavContext();
                String repoName = nav.getRepoName();
                String repoID = nav.getRepoID();
                String dir = nav.getDirPath();
                String path = Utils.pathJoin(dir, dirent.name);
                switch (actionId) {
                case ACTION_ID_SHARE:
                    mActivity.shareDir(repoID, path);
                    break;
                }
            }
        });

        mQuickAction.mAnimateTrack(false);
        return mQuickAction;
    }

    public void setEncryptedRepo(boolean encrypted) {
        repoIsEncrypted = encrypted;
    }
}

