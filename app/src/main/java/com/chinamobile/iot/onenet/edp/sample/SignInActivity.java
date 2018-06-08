/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.sample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.chinamobile.iot.onenet.edp.Common;
import com.chinamobile.iot.onenet.edp.sample.utility.PermissionUtil;
import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;

/**
 * Created by chenglei on 2016/2/1.
 */
public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar mToolbar;
    private RadioGroup mConnectTypeGroup;

    private String mSignInId;
    private String mAuthInfo;
    private int mEncryptType;
    private int mConnectType;

    private TextInputLayout mDeviceIdInputLayout;
    private TextInputLayout mAuthInfoInputLayout;

    private Spinner mEncryptSpinner;
    private Button mConnectButton;

    private static final String[] ENCRYPT_SPINNER_ITEMS = {"不加密", "AES/ECB/ISO10126Padding"};

    public static void actionSignIn(Context context) {
        Intent intent = new Intent(context, SignInActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        initViews();
        PermissionUtil.requestPermissions(this, new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, 1, null);
    }

    private void initViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolBar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("登录认证");
        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        mDeviceIdInputLayout = (TextInputLayout) findViewById(R.id.device_id);
        mAuthInfoInputLayout = (TextInputLayout) findViewById(R.id.auth_info);
        mConnectButton = (Button) findViewById(R.id.connect_button);
        mConnectTypeGroup = (RadioGroup) findViewById(R.id.connect_type);
        mEncryptSpinner = (Spinner) findViewById(R.id.encrypt_spinner);

        mDeviceIdInputLayout.getEditText().addTextChangedListener(mTextWatcher);
        mAuthInfoInputLayout.getEditText().addTextChangedListener(mTextWatcher);

        // 测试
        mDeviceIdInputLayout.getEditText().setText("282749");
        mAuthInfoInputLayout.getEditText().setText("L5lrqkQ3ubFq+i2ouBPuHrYAVAw=");

        mConnectTypeGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mConnectTypeGroup.check(R.id.connect_type_1);

        mEncryptSpinner.setAdapter(new EncryptSpinnerAdapter());
        mEncryptSpinner.setOnItemSelectedListener(mOnSpinnerItemSelectedListener);
        mEncryptSpinner.setSelection(0);

        mConnectButton.setOnClickListener(this);
    }

    private RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId) {
                case R.id.connect_type_1:
                    mDeviceIdInputLayout.setHint("设备ID");
                    mAuthInfoInputLayout.setHint("ApiKey");
                    mConnectType = EdpClient.CONNECT_TYPE_1;
                    break;

                case R.id.connect_type_2:
                    mDeviceIdInputLayout.setHint("项目ID");
                    mAuthInfoInputLayout.setHint("鉴权信息");
                    mConnectType = EdpClient.CONNECT_TYPE_2;
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connect_button:
                if (Utils.checkNetwork(this)) {
                    mSignInId = mDeviceIdInputLayout.getEditText().getText().toString();
                    mAuthInfo = mAuthInfoInputLayout.getEditText().getText().toString();
                    MainActivity.actionMain(this, mSignInId, mAuthInfo, mConnectType, mEncryptType);
                    finish();
                }
                break;

            default:
                break;
        }
    }

    private class EncryptSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

        @Override
        public int getCount() {
            return ENCRYPT_SPINNER_ITEMS.length;
        }

        @Override
        public Object getItem(int position) {
            return ENCRYPT_SPINNER_ITEMS[position % getCount()];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (null == convertView) {
                convertView = getLayoutInflater().inflate(R.layout.encrypt_spinner_item, parent, false);
                holder = new ViewHolder();
                holder.item = (TextView) convertView.findViewById(R.id.item);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.item.setText(ENCRYPT_SPINNER_ITEMS[position % getCount()]);
            return convertView;
        }
    }

    class ViewHolder {
        TextView item;
    }

    private AdapterView.OnItemSelectedListener mOnSpinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0:
                    mEncryptType = Common.Algorithm.NO_ALGORITHM;
                    break;

                case 1:
                    mEncryptType = Common.Algorithm.ALGORITHM_AES;
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mSignInId = mDeviceIdInputLayout.getEditText().getText().toString();
            mAuthInfo = mAuthInfoInputLayout.getEditText().getText().toString();
            mConnectButton.setEnabled(!TextUtils.isEmpty(mSignInId) && !TextUtils.isEmpty(mAuthInfo));
        }
    };
}
