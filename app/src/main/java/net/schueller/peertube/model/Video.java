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
package net.schueller.peertube.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;

import com.squareup.picasso.Picasso;

import net.schueller.peertube.R;
import net.schueller.peertube.helper.APIUrlHelper;

import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import net.schueller.peertube.database.Converters;

@Entity(tableName = "Video_table")
public class Video {

    @PrimaryKey(autoGenerate = true)
    @NonNull
    private Integer id;

    @NonNull
    @ColumnInfo(name = "uuid")
    private String uuid;

    @ColumnInfo(name = "name")
    private String name;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "category")
    private Category category;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "licence")
    private Licence licence;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "language")
    private Language language;

    @ColumnInfo(name = "nsfw")
    private Boolean nsfw;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "local")
    private Boolean isLocal;

    @ColumnInfo(name = "duration")
    private Integer duration;

    @ColumnInfo(name = "views")
    private Integer views;

    @ColumnInfo(name = "likes")
    private Integer likes;

    @ColumnInfo(name = "dislikes")
    private Integer dislikes;

    @ColumnInfo(name = "thumbnail_path")
    private String thumbnailPath;

    @ColumnInfo(name = "preview_path")
    private String previewPath;

    @ColumnInfo(name = "embed_path")
    private String embedPath;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "privacy")
    private Privacy privacy;

    @ColumnInfo(name = "support")
    private String support;

    @ColumnInfo(name = "description_path")
    private String descriptionPath;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "channel")
    private Channel channel;

    @TypeConverters(Converters.class) // add here
    @ColumnInfo(name = "account")
    private Account account;

    @TypeConverters(Converters.class) // add here
    @ColumnInfo(name = "tags")
    private ArrayList <String> tags;

    @ColumnInfo(name = "commentsEnabled")
    private Boolean commentsEnabled;

    @TypeConverters(Converters.class)
    @ColumnInfo(name = "files")
    private ArrayList<File> files;


    public Video() {

    }

    @NonNull
    public Integer getId() {
        return id;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    @NonNull
    public String getUuid() {
        return uuid;
    }

    public void setUuid(@NonNull String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Licence getLicence() {
        return licence;
    }

    public void setLicence(Licence licence) {
        this.licence = licence;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public void setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getLocal() {
        return isLocal;
    }

    public void setLocal(Boolean local) {
        isLocal = local;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getDislikes() {
        return dislikes;
    }

    public void setDislikes(Integer dislikes) {
        this.dislikes = dislikes;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getPreviewPath() {
        return previewPath;
    }

    public void setPreviewPath(String previewPath) {
        this.previewPath = previewPath;
    }

    public String getEmbedPath() {
        return embedPath;
    }

    public void setEmbedPath(String embedPath) {
        this.embedPath = embedPath;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Privacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy(Privacy privacy) {
        this.privacy = privacy;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public String getDescriptionPath() {
        return descriptionPath;
    }

    public void setDescriptionPath(String descriptionPath) {
        this.descriptionPath = descriptionPath;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public Boolean getCommentsEnabled() {
        return commentsEnabled;
    }

    public void setCommentsEnabled(Boolean commentsEnabled) {
        this.commentsEnabled = commentsEnabled;
    }

    public ArrayList<File> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
    }



    public static MediaDescriptionCompat getMediaDescription(Context context, Video video) {

//        String apiBaseURL = APIUrlHelper.getUrlWithVersion(context);

//        Bundle extras = new Bundle();
//        Bitmap bitmap = getBitmap(context, Uri.parse(apiBaseURL + video.thumbnailPath));
//        extras.putParcelable(MediaDescriptionCompat.DESCRIPTION_KEY_MEDIA_URI, bitmap);

        return new MediaDescriptionCompat.Builder()
                .setMediaId(video.getUuid())
//                .setIconBitmap(bitmap)
//                .setExtras(extras)
                .setTitle(video.getName())
                .setDescription(video.getDescription())
                .build();
    }

//   TODO: add support for the thumbnail
//    public static Bitmap getBitmap(Context context, Uri fullThumbnailUrl) {
//
//         return Picasso.with(context).load(fullThumbnailUrl)
//                 .placeholder(R.drawable.ic_peertube)
//                 .error(R.drawable.ic_peertube).get();
//    }
}
