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

import androidx.annotation.Nullable;
import androidx.room.Room;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import net.schueller.peertube.database.VideoDao;
import net.schueller.peertube.database.VideoRoomDatabase;
import net.schueller.peertube.model.File;
import net.schueller.peertube.model.Seed;
import net.schueller.peertube.model.Video;

import java.util.ArrayList;

public class SeedService extends IntentService {
    private static final String ACTION_SEED_TORRENT = "net.schueller.peertube.service.action.seed";
    private static final String ACTION_DELETE_TORRENT = "net.schueller.peertube.service.action.delete";


    private static final String EXTRA_TORRENTURL = "net.schueller.peertube.service.extra.TORRENTURL";
    private static final String EXTRA_VIDEO_UUID = "net.schueller.peertube.service.extra.VIDEO_UUID";

    private static final String TAG = "SeedService";

    public SeedService() {
        super("SeedService");
    }

    private static ArrayList<Seed> seeds;
    VideoRoomDatabase videoDatabase;
    VideoDao videoDao;
    ArrayList<Video> videos;

    static Integer seedLimit;
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Log.v(TAG, "onstart called");
        //Connect to database of seeded videos
        videoDatabase = Room.databaseBuilder(getApplicationContext() , VideoRoomDatabase.class, "video_database")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build();
        videoDao = videoDatabase.videoDao();
        videos = (ArrayList)videoDao.getSeeds();

        //if seeds list already exists the no need to reseed database.
        if (seeds != null) {
            Log.v(TAG, "existing torrents already:"+String.valueOf(seeds.size()));
            super.onStart(intent, startId);
            return;
        } else {
            Log.v(TAG,"creating empty list of torrents.");
            seeds = new ArrayList<Seed>();
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //hardcoded to 1 seed for now
        seedLimit=1;
        //seedLimit = sharedPref.getInt("pref_torrent_seeds",1);
        Integer videoQuality = sharedPref.getInt("pref_quality", 0);

        //TODO implement better truncation algorythm for downsizing seed limit
        if (videos.size()>seedLimit){
            Video only = videos.get(videos.size()-1);
            videos.clear();
            videos.add(only);
            videoDao.deleteAll();
            videoDao.insert(only);
        }
        Log.v(TAG,"videos loaded:"+videos.size());

        //start seeding seeds on initial start of service
        for (Video seed:videos){
            Log.v(TAG,"on start seed initializing :"+seed.getName());
            String urlToTorrent = seed.getFiles().get(0).getTorrentUrl();
            for (File file : seed.getFiles()) {
                // Set quality if it matches
                if (file.getResolution().getId().equals(videoQuality)) {
                    urlToTorrent = file.getTorrentUrl();
                }
            }
            handleActionSeedTorrent(urlToTorrent,seed.getUuid());
        }
        super.onStart(intent, startId);
    }

    @Override
    public void onCreate() {
        Log.v(TAG,"oncreate called");
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
        Log.v(TAG,"removing seed torrent ");

        if (seeds.size()==0){
            return;
        }
        for (Seed testSeed:seeds){
            if (videoUuid.equals(testSeed.getUUid())){
                //delete file
                java.io.File file = new java.io.File(testSeed.getFilePath());
                boolean deleted = file.delete();
                if(deleted){
                    Log.v(TAG,"deleted file from system");
                }
                //stop and remove seed from active torrents
                testSeed.stop();
                seeds.remove(testSeed);
                //remove video from seeded video db
                videos=(ArrayList)videoDao.getSeeds();
                for (Video vid:videos){
                    if (vid.getUuid().equals(videoUuid)){
                        videoDao.delete(vid);
                    }
                }

            }
        }
    }
    private void handleActionSeedTorrent(String torrentUrl, String videoUuid) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (!sharedPref.getBoolean("pref_torrent_seed",false)){
                Log.v(TAG,"not seeding because seeding not enabled");
                return;
            }

            if (sharedPref.getBoolean("pref_torrent_seed_wifi_only",false) && !(wifiConnection())){
                Log.v(TAG,"not connected to wifi which is required to seed");
                return;
            }

            if (sharedPref.getBoolean("pref_torrent_seed_exteral",false) && !(wifiConnection())){
                Log.v(TAG,"not using internal seed service");
                return;
            }
            //remove oldest stream if space needs to be made.
            if (seeds.size()+1>seedLimit) {
                Seed oldSeed=seeds.get(0);
                handleActionDeleteTorrent("",oldSeed.getUUid());
                Log.v(TAG,"replacing stream "+oldSeed.toString());
            }

            Log.v(TAG,"passed tests, starting the seeding of torrent "+torrentUrl);
            TorrentStream torrentStream;
            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    .removeFilesAfterStop(false)
                    .build();
            torrentStream = TorrentStream.init(torrentOptions);
                torrentStream.addListener(new TorrentListener() {

                    @Override
                    public void onStreamStopped() {
                        Log.v(TAG, "Stopped");
                      //  seeds.remove(0);
                    }

                    @Override
                    public void onStreamPrepared(Torrent torrent) {
                        Log.v(TAG, "Prepared "+torrentUrl);
                    }

                    @Override
                    public void onStreamStarted(Torrent torrent) {
                        Log.v(TAG, "Started "+ torrent.getFileNames()[0]);
                    }

                    @Override
                    public void onStreamError(Torrent torrent, Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }

                    @Override
                    public void onStreamReady(Torrent torrent) {
                        Log.v(TAG, "stream ready " + torrentUrl);
                    }

                    @Override
                    public void onStreamProgress(Torrent torrent, StreamStatus status) {
                     //   Log.v(TAG, "streamprogress " + status.toString());
                    }
                });
                torrentStream.startStream(torrentUrl);
                seeds.add(new Seed(torrentStream,videoUuid));
                Log.v(TAG,"started seeding "+torrentUrl);
   }
   private Boolean wifiConnection(){
       ConnectivityManager connMgr =
               (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
       for (Network network : connMgr.getAllNetworks()) {
           NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
           if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
               return true;
           }
       }
       return false;
   }
}
