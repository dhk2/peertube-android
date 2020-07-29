package net.schueller.peertube.service;

import android.app.DownloadManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
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

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.LibTorrent;
import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.TorrentBuilder;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
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
import java.util.concurrent.CountDownLatch;

import static com.google.android.exoplayer2.ui.PlayerNotificationManager.ACTION_PAUSE;
import static com.google.android.exoplayer2.ui.PlayerNotificationManager.ACTION_PLAY;
import static com.google.android.exoplayer2.ui.PlayerNotificationManager.ACTION_STOP;
import static net.schueller.peertube.helper.Constants.BACKGROUND_AUDIO;

public class SeedService extends IntentService {
    private static final String ACTION_SEED_TORRENT = "net.schueller.peertube.service.action.seed";
    private static final String ACTION_DELETE_TORRENT = "net.schueller.peertube.service.action.delete";
    private static final String ACTION_TORRENT_DOWNLOADED = "net.schueller.peertube.service.action.torrent.downloaded";;

    private static final String EXTRA_TORRENTURL = "net.schueller.peertube.service.extra.TORRENTURL";
    private static final String EXTRA_VIDEO_UUID = "net.schueller.peertube.service.extra.VIDEO_UUID";
    private static final String  EXTRA_DOWNLOAD_ID = "net.schueller.peertube.service.extra.DOWNLOAD_ID";

    private static final String TAG = "SeedService";


    public SeedService() {
        super("SeedService");
    }

    private static ArrayList<Seed> seeds;
    VideoRoomDatabase videoDatabase;
    VideoDao videoDao;
    ArrayList<Video> videos;
    private BroadcastReceiver receiver;
    static Integer seedLimit;
    private long notDownloadId=42069;
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
        seedLimit=3;
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

 //   public static void startActionDownloadFinished(Context context,S)

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
            } else if (ACTION_TORRENT_DOWNLOADED.equals(action)){
                Long downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID,-1);
                Log.e(TAG,"hell yeah son we downloaded that torrent"+downloadId);
                handleActionTorrentDownloaded(downloadId);
            }

        }
    }
    private void handleActionTorrentDownloaded(Long downloadId){
        Log.e(TAG,"handling torrent download");
        for (Seed testSeed:seeds){
            Log.e(TAG,downloadId+" "+testSeed.getUUid());
            if(testSeed.getDownloadId().equals(downloadId)){
                Log.e(TAG,"downloaded torrent for video "+testSeed.getUUid());

                java.io.File torrentFile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),testSeed.getTorrentFileLocal());

                SessionManager s = new SessionManager();

                final CountDownLatch signal = new CountDownLatch(1);

                s.addListener(new AlertListener() {
                    @Override
                    public int[] types() {
                        return null;
                    }

                    @Override
                    public void alert(Alert<?> alert) {
                        AlertType type = alert.type();

                        switch (type) {
                            case ADD_TORRENT:
                                System.out.println("Torrent added");
                                ((AddTorrentAlert) alert).handle().resume();
                                break;
                            case BLOCK_FINISHED:
                                BlockFinishedAlert a = (BlockFinishedAlert) alert;
                                int p = (int) (a.handle().status().progress() * 100);
                                //System.out.println("Progress: " + p + " for torrent name: " + a.torrentName());
                                //System.out.println(s.stats().totalDownload());
                                break;
                            case TORRENT_FINISHED:
                                System.out.println("Torrent finished");
                                signal.countDown();
                                break;
                        }
                    }
                });

                s.start();

                TorrentInfo ti = new TorrentInfo(torrentFile);
                s.download(ti, torrentFile.getParentFile());
                testSeed.setSessionManager(s);
                testSeed.setMp4FilePath(ti.files().name());
            }
        }
    }
    private void handleActionDeleteTorrent(String torrentUrl, String videoUuid) {
        Log.v(TAG,"removing seed torrent ");

        if (seeds.size()==0){
            return;
        }
        Seed seedToDelete=null;
        for (Seed testSeed:seeds) {
            Log.e(TAG,testSeed.toString());
            if (videoUuid.equals(testSeed.getUUid())) {
                seedToDelete = testSeed;
            }
            if(torrentUrl.equals(testSeed.getTorrentFileLocal())){
                seedToDelete =testSeed;
            }
        }
        if (seedToDelete != null) {

            //stop seed and mark to remove seed from active torrents

            //remove video from seeded video db
            Log.v(TAG,"removing video from db:"+seedToDelete.getUUid());
            videoDao.delete_byUuid(seedToDelete.getUUid());
            Log.v(TAG,"expunging seed"+seedToDelete.getUUid());
            seeds.remove(seedToDelete);
            //seedToDelete=null;

            String torrentFileFullPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + seedToDelete.getTorrentFileLocal();
            java.io.File torrentFile = new java.io.File(torrentFileFullPath);
            boolean deleted = torrentFile.delete();
            if (deleted) {
                Log.v(TAG, "deleted file "+torrentFile.getAbsolutePath());
            } else {
                Log.e(TAG, "failed to delete file "+torrentFile.getAbsolutePath());
            }
            String mp4FileFullPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + seedToDelete.getMp4FilePath();
            java.io.File mp4File = new java.io.File(mp4FileFullPath);
            deleted = mp4File.delete();
            if (deleted) {
                Log.v(TAG, "deleted file "+mp4File.getAbsolutePath());
            } else {
                Log.e(TAG, "failed to delete file "+mp4File.getAbsolutePath());
            }

        }
    }
    private void handleActionSeedTorrent(String torrentUrl, String videoUuid) {
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

            if (sharedPref.getBoolean("pref_torrent_seed_external",false) && !(wifiConnection())){
                Log.v(TAG,"not using internal seed service");
                return;
            }
            //remove oldest stream if space needs to be made.
            if (seeds.size()+1>seedLimit) {
                Seed oldSeed=seeds.get(0);
                Log.v(TAG,"replacing stream "+oldSeed.toString());
                handleActionDeleteTorrent("",oldSeed.getUUid());

            }

            Log.v(TAG,"passed tests, seeding "+torrentUrl);


            int spot = torrentUrl.lastIndexOf("/");
            String torrentFileName=torrentUrl.substring(spot+1);
            String torrentFileFullPath=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + torrentFileName;


            Log.e(TAG,"checking existince of file "+torrentFileFullPath);
            java.io.File mPath = new java.io.File(torrentFileFullPath);
            if (mPath.getAbsoluteFile().exists()) {
                Log.e(TAG,"torrent file already exists");
                seeds.add(new Seed(notDownloadId,videoUuid,torrentFileName));
                handleActionTorrentDownloaded(notDownloadId);
                notDownloadId++;
                return;
            }


            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(torrentUrl));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,torrentFileName);

            // get download service and enqueue file
            DownloadManager manager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
            Long downloadID =manager.enqueue(request);
            Log.e(TAG,"starting download "+downloadID);
            seeds.add(new Seed(downloadID,videoUuid,torrentFileName));
/*
        TorrentBuilder tb = new TorrentBuilder();
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
                Log.v(TAG,"seed created for "+torrentUrl);
  */

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
