/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chinamobile.iot.onenet.edp.SaveDataMsg;
import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenglei on 2016/2/1.
 */
public class GpsFragment extends Fragment implements View.OnClickListener {

    private View mContentView;
    private Activity mActivity;
    private LocationManager mLocationManager;

    private Location mLocation;

    private TextView mLonView;
    private TextView mLatView;

    private Button mUploadButton;
    private EditText mDeviceIdView;

    private ArrayList<LocationListener> mLocationListeners;

    public GpsFragment() {

    }

    public static GpsFragment newInstance() {
        GpsFragment fragment = new GpsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == mContentView) {
            mContentView = inflater.inflate(R.layout.fragment_gps, container, false);
        }
        return mContentView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mLocationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mLocationManager != null && mLocationListeners != null) {
            for (LocationListener l : mLocationListeners) {
                if (l != null) {
                    mLocationManager.removeUpdates(l);
                }
            }
            mLocationListeners.clear();
        }
        mActivity = null;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mContentView != null) {
            mLonView = (TextView) mContentView.findViewById(R.id.longitude);
            mLatView = (TextView) mContentView.findViewById(R.id.latitude);
            mUploadButton = (Button) mContentView.findViewById(R.id.upload_gps);
            mDeviceIdView = (EditText) mContentView.findViewById(R.id.device_id);

            String deviceId = ((MainActivity) mActivity).mDeviceId;
            if (!TextUtils.isEmpty(deviceId)) {
                mDeviceIdView.setText(deviceId);
            }

            mUploadButton.setOnClickListener(this);

            if (isGpsOn()) {
                initGps();
            } else {
                new AlertDialog.Builder(mActivity)
                        .setMessage("读取位置信息需要打开您的位置服务，是否继续？")
                        .setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent locationSourceSettingsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(locationSourceSettingsIntent, 0);
                            }
                        })
                        .setNegativeButton("否", null)
                        .show();
            }
        }
    }

    private void initGps() {
        if (mLocationManager != null) {
            List<String> providers = mLocationManager.getProviders(true);
            mLocationListeners = new ArrayList<>(providers.size());
            for (String provider : providers) {
                LocationListener listener = new MyLocationListener();
                mLocationListeners.add(listener);
                mLocationManager.requestLocationUpdates(provider, 2000, 5f, listener);
                if (null == mLocation) {
                    mLocation = mLocationManager.getLastKnownLocation(provider);
                }
            }
            updateUI(mLocation);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isGpsOn()) {
            initGps();
        } else {
            new AlertDialog.Builder(mActivity)
                    .setMessage("读取位置信息需要打开您的位置服务，是否继续？")
                    .setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent locationSourceSettingsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(locationSourceSettingsIntent, 0);
                        }
                    })
                    .setNegativeButton("否", null)
                    .show();
        }
    }

    private void updateUI(Location location) {
        if (location != null) {
            mLonView.setText(String.valueOf(location.getLongitude()));
            mLatView.setText(String.valueOf(location.getLatitude()));
        }
    }

    private boolean isGpsOn() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upload_gps:
                if (Utils.checkNetwork(App.sInstance) && Utils.checkInputNonEmpty(mDeviceIdView, "设备ID不能为空")) {
                    if (!isGpsOn()) {
                        new AlertDialog.Builder(mActivity)
                                .setMessage("读取位置信息需要打开您的位置服务，是否继续？")
                                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent locationSourceSettingsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        startActivityForResult(locationSourceSettingsIntent, 0);
                                    }
                                })
                                .setNegativeButton("否", null)
                                .show();
                    } else {
                        if (mLocation != null) {
                            JSONObject gps = new JSONObject();
                            JSONObject data = new JSONObject();
                            try {
                                gps.put("lon", mLocation.getLongitude());
                                gps.put("lat", mLocation.getLatitude());
                                SaveDataMsg.packSaveData1Msg(data, null, "GPS", null, gps);
                                EdpClient.getInstance().saveData(mDeviceIdView.getText().toString(), 1, null, data.toString().getBytes());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(App.sInstance, "未能成功获取位置信息，请稍后再试", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.i(App.LOG_TAG, "location = " + location.toString());
            mLocation = location;
            updateUI(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.i(App.LOG_TAG, "provider = " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i(App.LOG_TAG, "onProviderEnabled : " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i(App.LOG_TAG, "onProviderDisabled : " + provider);
        }
    }
}
