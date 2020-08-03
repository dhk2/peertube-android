package net.schueller.peertube.model;

import androidx.annotation.NonNull;

import com.frostwire.jlibtorrent.SessionManager;
import com.frostwire.jlibtorrent.Sha1Hash;
import com.github.se_bastiaan.torrentstream.TorrentStream;

import java.util.Date;

public class Seed {
    String uuid;
    String torrentFileLocal;
    Long started;
    Long downloadId;
    String mp4FilePath;
    Sha1Hash hash;

    public Seed(Long downloadId,String uuid,String torrentFileLocal){
        this.downloadId=downloadId;
        this.uuid = uuid;
        this.torrentFileLocal = torrentFileLocal;
    }
    public String getUUid() {
        return uuid;
    }

    public Long getStarted() {
        return started;
    }

    public void setStarted(Long started) {
        this.started = started;
    }
    @NonNull
    @Override
    public String toString() {
        String seedInfo ="Seeding :"+mp4FilePath;
        return seedInfo;
    }
    public Long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(Long downloadId) {
        this.downloadId = downloadId;
    }

    public String getTorrentFileLocal() {
        return torrentFileLocal;
    }

    public void setTorrentFileLocal(String torrentFileLocal) {
        this.torrentFileLocal = torrentFileLocal;
    }

    public String getMp4FilePath() {
        return mp4FilePath;
    }

    public void setMp4FilePath(String mp4FilePath) {
        this.mp4FilePath = mp4FilePath;
    }

    public Sha1Hash getHash() {
        return hash;
    }

    public void setHash(Sha1Hash hash) {
        this.hash = hash;
    }
}