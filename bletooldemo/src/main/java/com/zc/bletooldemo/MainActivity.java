package com.zc.bletooldemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.zc.zbletool.Beacon;
import com.zc.zbletool.BleManager;
import com.zc.zbletool.ScanResult;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Bind(R.id.rv_devices)
    RecyclerView rvDevices;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.toolbar_stop)
    TextView toolbarStop;
    @Bind(R.id.toolbar_count)
    TextView toolbarCount;
    private DevicesAdapter adapter;
    private ArrayList<ScanResult> scanResults;
    private BleManager bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        scanResults = new ArrayList<>();
        bleManager = new BleManager(this);
        initView();
        startScan();
    }

    private void startScan() {
        toolbarStop.setText("stop");
        bleManager.startBleScan(new BleManager.MyScanCallBack() {
            @Override
            public void onScanCallBack(ArrayList<ScanResult> results) {
                scanResults.clear();
                scanResults.addAll(results);
                Collections.sort(scanResults, new ComparatorScanResultByRssi());
                adapter.refreshData(scanResults);
                toolbarCount.setText("(" + scanResults.size() + ")");
            }

            @Override
            public void onScanBeaconsCallBack(ArrayList<Beacon> beacons) {
                Log.i(TAG, "beacons.size = " + beacons.size());
            }
        });
    }

    private void initView() {
        adapter = new DevicesAdapter();
        rvDevices.setAdapter(adapter);
        adapter.setOnItemClickListener(new ItemClickListener() {
            @Override
            public void onItemClickListener(View v, int position) {
                bleManager.stopBleScan();
                Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
                intent.putExtra("scanResult", scanResults.get(position));
                startActivity(intent);
            }
        });
    }

    @OnClick({R.id.toolbar_stop, R.id.toolbar_clear})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.toolbar_stop:
                if (bleManager.isStartScan()) {
                    bleManager.stopBleScan();
                    toolbarStop.setText("start");
                } else {
                    startScan();
                }
                break;
            case R.id.toolbar_clear:
                scanResults.clear();
                adapter.refreshData(scanResults);
                toolbarCount.setText("(" + scanResults.size() + ")");
                break;
        }
    }

    @Override
    protected void onStop() {
        if (bleManager.isStartScan()) {
            bleManager.stopBleScan();
            toolbarStop.setText("start");
        }
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!bleManager.isStartScan()) {
            startScan();
        }
    }
}
