package com.zc.bletooldemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.zc.zbletool.ScanResult;

public class DeviceDetailActivity extends AppCompatActivity {
    private ScanResult scanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        scanResult = getIntent().getParcelableExtra("scanResult");
    }
}
