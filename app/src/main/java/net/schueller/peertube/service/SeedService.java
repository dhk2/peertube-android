package net.schueller.peertube.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.ViewModelStoreOwner;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import net.schueller.peertube.database.VideoViewModel;
import net.schueller.peertube.intents.Intents;
import net.schueller.peertube.model.File;
import net.schueller.peertube.model.Video;

import java.util.ArrayList;
import java.util.List;

public class SeedService extends IntentService {
    private static final String ACTION_SEED_TORRENT = "net.schueller.peertube.service.action.seed";
    private static final String ACTION_DELETE_TORRENT = "net.schueller.peertube.service.action.delete";


    private static final String EXTRA_TORRENTURL = "net.schueller.peertube.service.extra.TORRENTURL";
    private static final String EXTRA_VIDEO_UUID = "net.schueller.peertube.service.extra.VIDEO_UUID";

    private static final String TAG = "SeedService";

    public SeedService() {
        super("SeedService");
    }


    public static void startActionSeedTorrent(Context context, String torrentUrl, String videoUuid) {
        Intent intent = new Intent(context, SeedService.class);
        intent.setAction(ACTION_SEED_TORRENT);
        intent.putExtra(EXTRA_TORRENTURL, torrentUrl);
        intent.putExtra(EXTRA_VIDEO_UUID, videoUuid);
        context.startService(intent);

    }



    public static void startActionDeleteTorrent(Context context, String torrentUrl, String videoUuid) {
        Intent intent = new Intent(context, SeedService.class);
        intent.setAction(ACTION_DELETE_TORRENT);
        intent.putExtra(EXTRA_TORRENTURL, torrentUrl);
        intent.putExtra(EXTRA_VIDEO_UUID, videoUuid);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final String torrentUrl = intent.getStringExtra(EXTRA_TORRENTURL);
            final String videoUuid = intent.getStringExtra(EXTRA_VIDEO_UUID);
            if (ACTION_SEED_TORRENT.equals(action)) {
                handleActionSeedTorrent(torrentUrl, videoUuid);
            } else if (ACTION_DELETE_TORRENT.equals(action)) {
                handleActionDeleteTorrent(torrentUrl, videoUuid);
            }
        }
    }

    private void handleActionDeleteTorrent(String torrentUrl, String videoUuid) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented delete torrent "+videoUuid);
    }
    private void handleActionSeedTorrent(String torrentUrl, String VideoUuid) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (!sharedPref.getBoolean("pref_torrent_background_seed",false)){
                Log.e(TAG,"We should not be seeding because seeding not enabled");
                return;
            }

            if (sharedPref.getBoolean("pref_torrent_seed_wifi_only",false) && !(wifiConnection())){
                Log.e(TAG,"not connected to wifi which is required to seed");
                return;
            }
            TorrentStream torrentStream;
            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    .removeFilesAfterStop(false)
                    .build();
            torrentStream = TorrentStream.init(torrentOptions);
                torrentStream.addListener(new TorrentListener() {


                    @Override
                    public void onStreamStopped() {
                        Log.e(TAG, "Stopped");
                    }

                    @Override
                    public void onStreamPrepared(Torrent torrent) {
                        Log.e(TAG, "Prepared "+torrentUrl);
                    }

                    @Override
                    public void onStreamStarted(Torrent torrent) {
                        Log.e(TAG, "Started"+torrentUrl);
                    }

                    @Override
                    public void onStreamError(Torrent torrent, Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }

                    @Override
                    public void onStreamReady(Torrent torrent) {
                        Log.e(TAG, "stream ready " + torrentUrl);
                    }

                    @Override
                    public void onStreamProgress(Torrent torrent, StreamStatus status) {
                        Log.e(TAG, "streamprogress " + torrentUrl);
                    }
                });


                torrentStream.startStream(torrentUrl);
                Log.e("started seeding",torrentUrl);
   }
   private Boolean wifiConnection(){
       ConnectivityManager connMgr =
               (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
       for (Network network : connMgr.getAllNetworks()) {
           NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
           if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
               Log.d(TAG, "wifi connected: " );
               return true;
           }
       }
       Log.d(TAG, "wifi not connected: " );
       return false;
   }
}
