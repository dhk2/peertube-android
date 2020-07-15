package net.schueller.peertube.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.github.se_bastiaan.torrentstream.TorrentStream;

import java.util.Date;

public class Seed {
    String uuid;
    Long started;
    TorrentStream torrent;

    public Seed(TorrentStream torrentStream,String uuid) {
        this.uuid = uuid;
        this.torrent = torrentStream;
        started = new Date().getTime();
    }
    public String getUUid() {
        return uuid;
    }
    public void stop(){
        torrent.stopStream();
    }

    public Long getStarted() {
        return started;
    }

    public void setStarted(Long started) {
        this.started = started;
    }
    public String getFilePath (){
        if (torrent.getCurrentTorrent() == null){
            return "";
        }
        return torrent.getCurrentTorrent().getSaveLocation().getPath();
    }
    @NonNull
    @Override
    public String toString() {
        String seedInfo ="Seed"+ "\n"+torrent.getOptions().toString();
        seedInfo = seedInfo + "\n" + torrent.getOptions().toString();
        if (torrent.getCurrentTorrent() !=null) {
            seedInfo = seedInfo + "\n" + torrent.getCurrentTorrentUrl();
            seedInfo = seedInfo + "\n" + torrent.getCurrentTorrent().getState().toString();
            seedInfo = seedInfo + "\n" + torrent.getCurrentTorrent().getFileNames()[0];
            seedInfo = seedInfo + "\n" + torrent.getCurrentTorrent().getVideoFile().toString();
            seedInfo = seedInfo + "\n" + torrent.getCurrentTorrent().getSaveLocation().getName();
            seedInfo = seedInfo + "\n" + torrent.getCurrentTorrent().getSaveLocation().getPath();
        }
        return seedInfo;
    }
}