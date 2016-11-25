package com.zc.bletooldemo;

import com.zc.zbletool.ScanResult;

import java.util.Comparator;

public class ComparatorScanResultByRssi implements Comparator<ScanResult> {

	@Override
	public int compare(ScanResult lhs, ScanResult rhs) {
		return rhs.getRssi() - lhs.getRssi();
	}
}
