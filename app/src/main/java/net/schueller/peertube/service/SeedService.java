package net.schueller.peertube.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

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

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SeedService extends IntentService {
    //VideoViewModel mVideoViewModel;
    //List<Video> history;
    //ViewModelStore videoViewModelStore;
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_SEED_TORRENT = "net.schueller.peertube.service.action.seed";
    private static final String ACTION_DELETE_TORRENT = "net.schueller.peertube.service.action.delete";

    // TODO: Rename parameters
    private static final String EXTRA_TORRENTURL = "net.schueller.peertube.service.extra.TORRENTURL";
    private static final String EXTRA_VIDEO_UUID = "net.schueller.peertube.service.extra.VIDEO_UUID";

    private static final String TAG = "SeedService";

    public SeedService() {
        super("SeedService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionSeedTorrent(Context context, String torrentUrl, String videoUuid) {
        Intent intent = new Intent(context, SeedService.class);
        intent.setAction(ACTION_SEED_TORRENT);
        intent.putExtra(EXTRA_TORRENTURL, torrentUrl);
        intent.putExtra(EXTRA_VIDEO_UUID, videoUuid);
        context.startService(intent);

    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
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
            if (ACTION_SEED_TORRENT.equals(action)) {
                final String torrentUrl = intent.getStringExtra(EXTRA_TORRENTURL);
                final String videoUuid = intent.getStringExtra(EXTRA_VIDEO_UUID);
                handleActionSeedTorrent(torrentUrl, videoUuid);
            } else if (ACTION_DELETE_TORRENT.equals(action)) {
                final String torrentUrl = intent.getStringExtra(EXTRA_TORRENTURL);
                final String videoUuid = intent.getStringExtra(EXTRA_VIDEO_UUID);
                handleActionDeleteTorrent(torrentUrl, videoUuid);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionDeleteTorrent(String torrentUrl, String videoUuid) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented delete torrent "+videoUuid);
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionSeedTorrent(String torrentUrl, String VideoUuid) {

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Intent intent = new Intent();
            TorrentStream torrentStream;
            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    .removeFilesAfterStop(false)
                    .build();
            torrentStream = TorrentStream.init(torrentOptions);

                torrentStream.addListener(new TorrentListener() {


                    @Override
                    public void onStreamStopped() {
                        Log.d(TAG, "Stopped");
                    }

                    @Override
                    public void onStreamPrepared(Torrent torrent) {
                        Log.d(TAG, "Prepared");
                    }

                    @Override
                    public void onStreamStarted(Torrent torrent) {
                        Log.d(TAG, "Started");
                    }

                    @Override
                    public void onStreamError(Torrent torrent, Exception e) {
                        Log.d(TAG, "Error: " + e.getMessage());
                    }

                    @Override
                    public void onStreamReady(Torrent torrent) {
                    }

                    @Override
                    public void onStreamProgress(Torrent torrent, StreamStatus status) {
                    }
                });
                //torrentStream.startStream(torrentUrl);
                Log.e("started seeding",torrentUrl);
   }
}
