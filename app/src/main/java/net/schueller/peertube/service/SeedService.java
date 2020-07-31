package net.schueller.peertube.service;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.TorrentInfo;

import net.schueller.peertube.database.VideoDao;
import net.schueller.peertube.database.VideoRoomDatabase;
import net.schueller.peertube.model.File;
import net.schueller.peertube.model.Seed;
import net.schueller.peertube.model.Video;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class SeedService extends IntentService {
    private static final String ACTION_SEED_TORRENT = "net.schueller.peertube.service.action.seed";
    private static final String ACTION_DELETE_TORRENT = "net.schueller.peertube.service.action.delete";
    private static final String ACTION_TORRENT_DOWNLOADED = "net.schueller.peertube.service.action.torrent.downloaded";
    private static final String ACTION_TORRENT_STATUS = "net.schueller.peertube.service.action.status";

    private static final String EXTRA_TORRENTURL = "net.schueller.peertube.service.extra.TORRENTURL";
    private static final String EXTRA_VIDEO_UUID = "net.schueller.peertube.service.extra.VIDEO_UUID";
    private static final String EXTRA_DOWNLOAD_ID = "net.schueller.peertube.service.extra.DOWNLOAD_ID";

    private static final String TAG = "SeedService";

    public SeedService() {
        super("SeedService");
    }

    private static ArrayList<Seed> seeds;
    private static SessionManager sessionManager;
    private VideoRoomDatabase videoDatabase;
    private VideoDao videoDao;
    private static Integer seedLimit = 3;
    private long notDownloadId=42069;
    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        Log.v(TAG, "onstart called");


        //if seeds list already exists the no need to reseed database.
        if (seeds != null) {
            Log.v(TAG, "existing torrents:"+String.valueOf(seeds.size()));
            super.onStart(intent, startId);
            return;
        } else {
            Log.v(TAG,"no existing torrents.");
            sessionn
            seeds = new ArrayList<Seed>();
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Integer videoQuality = sharedPref.getInt("pref_quality", 0);
        new Thread(new Runnable() {
            public void run() {
                //Connect to database of seeded videos
                videoDatabase = Room.databaseBuilder(getApplicationContext() , VideoRoomDatabase.class, "video_database")
                        .fallbackToDestructiveMigration()
                        .build();
                videoDao = videoDatabase.videoDao();
                ArrayList<Video> videos;
                videos = (ArrayList)videoDao.getSeeds();


                //TODO implement better truncation algorythm for downsizing seed limit
                if (videos.size()>seedLimit){
                    Log.e(TAG,"trimming video list "+videos.size()+" is greater than "+seedLimit);
                    videos=(ArrayList)videos.subList(0,seedLimit-1);
                    videoDao.deleteAll();
                    for (Video tempVideo:videos){
                        videoDao.insert(tempVideo);
                    }

                }
                Log.v(TAG,"videos loaded:"+videos.size());
                videoDatabase.close();
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
            }
        }).start();
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

    public static void startActionTorrentDownloaded(Context context, Long downloadId) {
        Intent intent = new Intent(context, SeedService.class);
        intent.setAction(ACTION_TORRENT_DOWNLOADED);
        intent.putExtra(EXTRA_DOWNLOAD_ID, downloadId);
        context.startService(intent);

    }

    public static void startActionDeleteTorrent(Context context, String torrentUrl, String videoUuid) {
        Intent intent = new Intent(context, SeedService.class);
        intent.setAction(ACTION_DELETE_TORRENT);
        intent.putExtra(EXTRA_TORRENTURL, torrentUrl);
        intent.putExtra(EXTRA_VIDEO_UUID, videoUuid);
        context.startService(intent);
    }

    public static void startActionStatusUpdate(Context context) {
        Intent intent = new Intent(context, SeedService.class);
        intent.setAction(ACTION_TORRENT_STATUS);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG,"handling intent for "+intent.getAction());
        if (intent != null) {
            final String action = intent.getAction();
            final String torrentUrl = intent.getStringExtra(EXTRA_TORRENTURL);
            final String videoUuid = intent.getStringExtra(EXTRA_VIDEO_UUID);
            if (ACTION_SEED_TORRENT.equals(action)) {
                handleActionSeedTorrent(torrentUrl, videoUuid);
            } else if (ACTION_DELETE_TORRENT.equals(action)) {
                handleActionDeleteTorrent(torrentUrl, videoUuid);
            } else if (ACTION_TORRENT_DOWNLOADED.equals(action)){
                Long downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID,-1);
                handleActionTorrentDownloaded(downloadId);
            }else if (ACTION_TORRENT_STATUS.equals(action)) {
                handleActionTorrentStatus();
            }

        }
    }
    private void handleActionTorrentDownloaded(Long downloadId){
        Log.v(TAG,"File download finished, Check if there was a seed waiting for that file "+downloadId);
        for (Seed testSeed:seeds){
            if(testSeed.getDownloadId().equals(downloadId)){
                Log.v(TAG,"matched torrent download for video "+testSeed.getTorrentFileLocal());
                java.io.File torrentFile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),testSeed.getTorrentFileLocal());
                //Start seeding torrent file for video
                SessionManager sessionManager = new SessionManager();
                final CountDownLatch signal = new CountDownLatch(1);
                sessionManager.start();
                TorrentInfo ti = new TorrentInfo(torrentFile);
                sessionManager.download(ti, torrentFile.getParentFile());
                testSeed.setSessionManager(sessionManager);
                testSeed.setMp4FilePath(ti.files().name());
            }
        }
    }
    private void handleActionDeleteTorrent(String torrentUrl, String videoUuid) {
        Log.v(TAG,"handle delete seed torrent ");

        if (seeds.size()==0){
            return;
        }
        Seed seedToDelete=null;
        for (Seed testSeed:seeds) {
            if (videoUuid.equals(testSeed.getUUid())) {
                seedToDelete = testSeed;
            }
            if(torrentUrl.equals(testSeed.getTorrentFileLocal())){
                seedToDelete = testSeed;
            }
        }
        if (seedToDelete != null) {
            Seed finalSeedToDelete = seedToDelete;
            // slow torrent shutdown moved off main thread
            new Thread(new Runnable() {
                public void run() {
                    Log.v(TAG,"expunging seed:"+ finalSeedToDelete.getUUid());
                    seeds.remove(finalSeedToDelete);
                    String torrentFileFullPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + finalSeedToDelete.getTorrentFileLocal();
                    java.io.File torrentFile = new java.io.File(torrentFileFullPath);
                    boolean deleted = torrentFile.delete();
                    if (deleted) {
                        Log.v(TAG, "deleted file "+torrentFile.getAbsolutePath());
                    } else {
                        Log.e(TAG, "failed to delete file "+torrentFile.getAbsolutePath());
                    }
                    String mp4FileFullPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + finalSeedToDelete.getMp4FilePath();
                    java.io.File mp4File = new java.io.File(mp4FileFullPath);
                    deleted = mp4File.delete();
                    if (deleted) {
                        Log.v(TAG, "deleted file "+mp4File.getAbsolutePath());
                    } else {
                        Log.e(TAG, "failed to delete file "+mp4File.getAbsolutePath());
                    }
                }
            }).start();
        }
    }
    private void handleActionSeedTorrent(String torrentUrl, String videoUuid) {
            Log.v(TAG,"handle action seed torrent");
            if (torrentUrl == null){
                Log.e(TAG,"trying to seed null torrenturl");
                return;
            }
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (!sharedPref.getBoolean("pref_torrent_seed",false)){
                Log.v(TAG,"not seeding because seeding not enabled");
                return;
            }

            if (sharedPref.getBoolean("pref_torrent_seed_wifi_only",false) && !(wifiConnection())){
                Log.v(TAG,"not connected to wifi which is required to seed");
                return;
            }

            if (sharedPref.getBoolean("pref_torrent_seed_external",false) || sharedPref.getBoolean("pref_torrent_seed_external_interactive",false)){
                Log.v(TAG,"not using internal seed service");
                return;
            }
            for (Seed testSeed:seeds){
                if (testSeed.getUUid().equals(videoUuid)){
                    Log.v(TAG,"already seeding this video");
                    return;
                }
            }

            //remove oldest stream if at size limit.
            if (seeds.size()+1>seedLimit) {
                Seed oldSeed=seeds.get(0);
                Log.v(TAG,"replacing seed "+oldSeed.getMp4FilePath());
                handleActionDeleteTorrent("",oldSeed.getUUid());
            }

            Log.v(TAG,"passed tests, starting to seed "+torrentUrl);
            //
            int spot = torrentUrl.lastIndexOf("/");
            String torrentFileName=torrentUrl.substring(spot+1);
            String torrentFileFullPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + torrentFileName;

            Log.e(TAG,"checking existence of file "+torrentFileFullPath);
            java.io.File testFile = new java.io.File(torrentFileFullPath);
            if (testFile.getAbsoluteFile().exists()) {
                seeds.add(new Seed(notDownloadId,videoUuid,torrentFileName));
                Log.e(TAG,"torrent file already downloaded, skipping redownload");
                handleActionTorrentDownloaded(notDownloadId);
                notDownloadId++;
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(torrentUrl));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,torrentFileName);
            DownloadManager manager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            Long downloadID =manager.enqueue(request);
            Log.e(TAG,"starting torrent download "+downloadID);
            seeds.add(new Seed(downloadID,videoUuid,torrentFileName));
   }
    private void handleActionTorrentStatus() {
        for(Seed testSeed:seeds){
            Log.v(TAG, testSeed.toString());
        }
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
