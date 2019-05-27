package de.danoeh.antennapod.core.service.download;

import android.support.annotation.NonNull;

public class LocalFeedDownloaderStub extends Downloader {

    public LocalFeedDownloaderStub(@NonNull DownloadRequest request) {
        super(request);
    }

    @Override
    protected void download() {
        result.setSuccessful();
    }
}
