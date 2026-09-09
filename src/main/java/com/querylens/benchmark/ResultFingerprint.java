package com.querylens.benchmark;

public record ResultFingerprint(int columnCount, long rowCount, long rowHashSum, long rowHashXor) {
}
