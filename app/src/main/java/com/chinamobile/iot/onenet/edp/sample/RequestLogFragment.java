/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by chenglei on 2016/2/1.
 */
public class RequestLogFragment extends Fragment {

    public static final String ARG_LOG_LIST = "arg_log_list";

    private LogListAdapter mListAdapter;
    private Activity mActivity;
    private LayoutInflater mLayoutInflater;
    private View mContentView;

    public RequestLogFragment() {

    }

    public static RequestLogFragment newInstance(ArrayList<String> logList) {
        RequestLogFragment fragment = new RequestLogFragment();
        Bundle b = new Bundle();
        b.putStringArrayList(ARG_LOG_LIST, logList);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<String> logList = getArguments().getStringArrayList(ARG_LOG_LIST);
        mListAdapter = new LogListAdapter(logList);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLayoutInflater = inflater;
        if (null == mContentView) {
            mContentView = inflater.inflate(R.layout.fragment_request_log, container, false);
        }
        return mContentView;
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mContentView != null) {
            ListView listView = (ListView) mContentView.findViewById(R.id.log_list_view);
            if (mListAdapter != null) {
                listView.setAdapter(mListAdapter);
            }
        }
    }

    class LogListAdapter extends BaseAdapter {

        ArrayList<String> mLogList;

        public LogListAdapter(ArrayList<String> logList) {
            mLogList = logList;
        }

        public void update(ArrayList<String> logList) {
            mLogList = logList;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mLogList != null ? mLogList.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mLogList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (null == convertView) {
                convertView = mLayoutInflater.inflate(R.layout.log_list_item, null, false);
                holder = new ViewHolder();
                holder.log = (TextView) convertView.findViewById(R.id.text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            String text = mLogList.get(position % mLogList.size());
            holder.log.setText(text);
            return convertView;
        }

        class ViewHolder {
            TextView log;
        }
    }

    public void updateList(ArrayList<String> logList) {
        mListAdapter.update(logList);
    }
}
