package net.schueller.peertube.model;

import androidx.annotation.NonNull;

import com.frostwire.jlibtorrent.SessionManager;
import com.github.se_bastiaan.torrentstream.TorrentStream;

import java.util.Date;

public class Seed {
    String uuid;
    String torrentFileLocal;
    Long started;
    Long downloadId;
    TorrentStream torrent;
    SessionManager sessionManager;
    String mp4FilePath;



    public Seed(TorrentStream torrentStream,String uuid) {
        this.uuid = uuid;
        this.torrent = torrentStream;
        started = new Date().getTime();
    }
    public Seed(Long downloadId,String uuid,String torrentFileLocal){
        this.downloadId=downloadId;
        this.uuid = uuid;
        this.torrentFileLocal = torrentFileLocal;
    }
    public String getUUid() {
        return uuid;
    }
    public void stop(){
        sessionManager.stop();
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
        String seedInfo ="Seed"+ "\n"+sessionManager.toString();
            seedInfo = seedInfo + "\n" + sessionManager.totalDownload();
            seedInfo = seedInfo + "\n" + sessionManager.totalUpload();
            seedInfo = seedInfo + "\n" + sessionManager.uploadRate();
            seedInfo = seedInfo + "\n" + sessionManager.externalAddress();
        return seedInfo;
    }
    public Long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(Long downloadId) {
        this.downloadId = downloadId;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
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
}