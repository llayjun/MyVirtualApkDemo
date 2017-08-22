package com.millet.myhost;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.didi.virtualapk.PluginManager;

import java.io.File;

public class MyHostMainActivity extends AppCompatActivity implements View.OnClickListener {
    //ui
    private TextView mTextAdd;

    //data
    private String mApkName = "plugin.apk";
    private String mPackageName = "com.millet.myplugin";
    private String mClassName = "com.millet.myplugin.MyPluginMainActivity";
    private boolean mFileExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_host_main);
        mTextAdd = (TextView) findViewById(R.id.text_add_plugin);
        mTextAdd.setOnClickListener(this);
        loadPlugin();
    }

    @Override
    public void onClick(View _view) {
        if (mFileExists) {
            Intent _intent = new Intent();
            _intent.setClassName(mPackageName, mClassName);
            startActivity(_intent);
        } else {
            Toast.makeText(this, "文件包不存在!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPlugin() {
        String _path = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/" + mApkName);
        File _file = new File(_path);
        mFileExists = _file.exists();
        if (mFileExists) {
            try {
                PluginManager.getInstance(this).loadPlugin(_file);
            } catch (Exception _e) {
                _e.printStackTrace();
            }
        }
    }

}
