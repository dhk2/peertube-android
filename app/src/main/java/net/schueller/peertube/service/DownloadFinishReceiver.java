package net.schueller.peertube.service;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DownloadFinishReceiver extends BroadcastReceiver {
    final String TAG="DFR";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, "download finished " + intent.getAction());
        if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Log.e(TAG, "download ID " + id + " completed");
            SeedService.startActionTorrentDownloaded(context,id);
        }
    }
}
