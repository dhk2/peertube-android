/*
 * Copyright 2018 Stefan Schüller <sschueller@techdroid.com>
 *
 * License: GPL-3.0+
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.schueller.peertube.intents;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;


import com.github.se_bastiaan.torrentstream.TorrentOptions;

import net.schueller.peertube.R;
import net.schueller.peertube.helper.APIUrlHelper;
import net.schueller.peertube.model.File;
import net.schueller.peertube.model.Video;

import androidx.core.app.ActivityCompat;


public class Intents {


    /**
     * https://troll.tv/videos/watch/6edbd9d1-e3c5-4a6c-8491-646e2020469c
     *
     * @param context context
     * @param video video
     */
    // TODO, offer which version to download
    public static void Share(Context context, Video video) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, video.getName());
        intent.putExtra(Intent.EXTRA_TEXT, APIUrlHelper.getShareUrl(context, video.getUuid()) );
        intent.setType("text/plain");

        context.startActivity(intent);

    }

    public static void SeedWithLibre(Context context, Video video) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        Intent intent = new Intent();
        Integer videoQuality = sharedPref.getInt("pref_quality", 0);
        String urlToTorrent = video.getFiles().get(0).getTorrentUrl();
        for (File file :video.getFiles()) {
            // Set quality if it matches
            if (file.getResolution().getId().equals(videoQuality)) {
                urlToTorrent = file.getTorrentUrl();
            }
        }
        if (sharedPref.getBoolean("pref_torrent_seed_libre_auto",false)) {
            int spot = urlToTorrent.lastIndexOf("/");
            String torrentFileName=urlToTorrent.substring(spot+1);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlToTorrent));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,torrentFileName);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

        } else {
            Log.e("intents-sendto libre",urlToTorrent);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_SUBJECT, video.getName());
            intent.putExtra(Intent.EXTRA_TEXT, APIUrlHelper.getShareUrl(context, video.getUuid()));
            intent.setData(Uri.parse(urlToTorrent));
            intent.setPackage("org.proninyaroslav.libretorrent");
            context.startActivity(intent);
        }
    }

    /**
     *
     * @param context context
     * @param video video
     */
    // TODO, offer which version to download
    public static void Download(Context context, Video video) {

        String url = video.getFiles().get(0).getFileDownloadUrl();
        // make sure it is a valid filename
        String destFilename = video.getName().replaceAll("[^a-zA-Z0-9]", "_") + "." + MimeTypeMap.getFileExtensionFromUrl(URLUtil.guessFileName(url,null,null));

        //Toast.makeText(context, destFilename, Toast.LENGTH_LONG).show();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(video.getDescription());
        request.setTitle(video.getName());
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, destFilename);

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }
}
