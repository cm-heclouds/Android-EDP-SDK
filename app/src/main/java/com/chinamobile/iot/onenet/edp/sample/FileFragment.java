/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.chinamobile.iot.onenet.edp.SaveDataMsg;
import com.chinamobile.iot.onenet.edp.sample.utility.FileUtil;
import com.chinamobile.iot.onenet.edp.sample.widget.IconView;
import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by chenglei on 2016/3/22.
 */
public class FileFragment extends Fragment implements AdapterView.OnItemClickListener {

    private View mContentView;
    private Activity mActivity;
    private ListView mListView;

    private ArrayList<String> mStack = new ArrayList<>();
    private ArrayList<Item> mDataList = new ArrayList<>();

    private static final String ROOT_TAB_TITLE = "本地存储";
    private Item mInternalSD = new Item();
    private FileListAdapter mAdapter;
    private LoadFileTask mLoadFileTask;

    public FileFragment() {

    }

    public static FileFragment newInstance() {
        FileFragment fragment = new FileFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == mContentView) {
            mContentView = inflater.inflate(R.layout.fragment_file, container, false);
            mStack.clear();
            mDataList.clear();
            mStack.add("/");
        }
        return mContentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mContentView != null) {
            mListView = (ListView) mContentView.findViewById(R.id.file_list_view);

            mInternalSD.setName("设备存储").setPath(FileUtil.getSdcard()).setDrawableId(R.string.icon_font_device).setIsDir(true);
            mDataList.add(mInternalSD);

            mListView.setOnItemClickListener(this);
            mAdapter = new FileListAdapter();
            mListView.setAdapter(mAdapter);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Item item = mDataList.get(position);

        if (item.isDir()) {
            mStack.add(item.getPath());
            mLoadFileTask = new LoadFileTask(item.getPath());
            mLoadFileTask.execute();
        } else {
            new AlertDialog.Builder(mActivity).setMessage("是否上传？").setPositiveButton("是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    JSONObject token = new JSONObject();
                    File f = new File(item.getPath());
                    try {
                        SaveDataMsg.packSaveData2Token(token, "12345", "pic", new Date(), "");
                        FileInputStream fis = new FileInputStream(f);
                        byte[] data = new byte[fis.available()];
                        fis.read(data);
                        EdpClient.getInstance().saveData(((MainActivity) mActivity).mDeviceId, 2, token.toString(), data);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).setNegativeButton("否", null).show();
        }

    }

    class FileListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (null == convertView) {
                convertView = mActivity.getLayoutInflater().inflate(R.layout.file_list_item, parent, false);
                holder = new ViewHolder();
                holder.image = (IconView) convertView.findViewById(R.id.image);
                holder.name = (TextView) convertView.findViewById(R.id.file_name);
                holder.size = (TextView) convertView.findViewById(R.id.file_size);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Item item = mDataList.get(position);
            holder.name.setText(item.getName());
            if (isRootDir()) {
                holder.size.setVisibility(View.GONE);
            } else {
                holder.size.setVisibility(View.VISIBLE);
                holder.size.setText(item.getSize());
            }
            holder.image.setText(item.getDrawableId());
            return convertView;
        }
    }

    class ViewHolder {
        IconView image;
        TextView name;
        TextView size;
    }

    private TextView buildNavigatorTab(String text) {
        if (mActivity != null) {
            TextView textView = new TextView(mActivity);
            textView.setTextColor(0xFFFFE57F);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            textView.setText(text);
            return textView;
        } else {
            return null;
        }
    }

    class Item {
        String name;
        String path;
        String size;
        int drawableId;
        boolean isDir;

        public String getName() {
            return name;
        }

        public Item setName(String name) {
            this.name = name;
            return this;
        }

        public String getPath() {
            return path;
        }

        public Item setPath(String path) {
            this.path = path;
            return this;
        }

        public String getSize() {
            return size;
        }

        public Item setSize(String size) {
            this.size = size;
            return this;
        }

        public int getDrawableId() {
            return drawableId;
        }

        public Item setDrawableId(int drawableId) {
            this.drawableId = drawableId;
            return this;
        }

        public boolean isDir() {
            return isDir;
        }

        public Item setIsDir(boolean isDir) {
            this.isDir = isDir;
            return this;
        }
    }

    private boolean isRootDir() {
        return 1 == mStack.size() && "/".equals(mStack.get(0));
    }

    public boolean onBackPressed() {
        int size = mStack.size();
        if (size > 2) {
            mStack.remove(size - 1);
            size--;
            String path = mStack.get(size - 1);
            mLoadFileTask = new LoadFileTask(path);
            mLoadFileTask.execute();
            return true;
        } else if (2 == size) {
            mStack.remove(size - 1);
            size--;
            mDataList.clear();
            mDataList.add(mInternalSD);
            mAdapter.notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }

    private class LoadFileTask extends AsyncTask<Void, Void, Void> {

        private String mPath;

        public LoadFileTask(String path) {
            mPath = path;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mStack.size() > 1) {

            } else {

            }
            File[] files = FileUtil.listFiles(mPath);
            mDataList.clear();
            if (files != null) {
                for (File f : files) {
                    Item child = new Item();
                    child.setName(f.getName());
                    child.setPath(f.getAbsolutePath());
                    if (f.isDirectory()) {
                        child.setIsDir(true);
                    } else {
                        child.setIsDir(false);
                        child.setSize(FileUtil.formatFileSize(FileUtil.getFileSize(f)));
                    }
                    //Integer drawableId = FileUtil.getDrawableId(f);
                    if (child.isDir()) {
                        child.setDrawableId(R.string.icon_font_dir);
                    } else {
                        child.setDrawableId(R.string.icon_font_file);
                    }
                    mDataList.add(child);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mAdapter.notifyDataSetChanged();
        }
    }

}
