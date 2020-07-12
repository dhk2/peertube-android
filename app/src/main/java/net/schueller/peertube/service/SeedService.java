package net.schueller.peertube.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.room.Room;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import net.schueller.peertube.database.VideoDao;
import net.schueller.peertube.database.VideoRoomDatabase;
import net.schueller.peertube.database.VideoViewModel;
import net.schueller.peertube.model.File;
import net.schueller.peertube.model.Video;

import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;
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
    VideoRoomDatabase videoDatabase;
    VideoDao videoDao;
    ArrayList<Video>videos;
    @Override
    public void onCreate() {
        Log.e(TAG,"oncreate called");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Integer videoQuality = sharedPref.getInt("pref_quality", 0);
        //TODO change service to run on background thread instead of mainthread
        videoDatabase = Room.databaseBuilder(getApplicationContext() , VideoRoomDatabase.class, "video_database")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        videoDao = videoDatabase.videoDao();
        videos = (ArrayList)videoDao.getSeeds();
        Log.e(TAG,"videos loaded:"+videos.size());
        for (Video seed:videos){
            Log.e(TAG,"oncreate:"+seed.getName());
            String urlToTorrent = seed.getFiles().get(0).getTorrentUrl();
            Log.e(TAG,"default torrent "+urlToTorrent);
            for (File file : seed.getFiles()) {
                // Set quality if it matches
                if (file.getResolution().getId().equals(videoQuality)) {
                    urlToTorrent = file.getTorrentUrl();
                    Log.v(TAG,"proper resolution found");
                }
            }
            handleActionSeedTorrent(urlToTorrent,seed.getUuid());

        }
        super.onCreate();
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
                        Log.e(TAG, "streamprogress " + status.toString());
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
