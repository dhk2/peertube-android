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

package net.schueller.peertube.activity;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import net.schueller.peertube.R;
import net.schueller.peertube.adapter.VideoAdapter;
import net.schueller.peertube.database.Server;
import net.schueller.peertube.database.VideoViewModel;
import net.schueller.peertube.model.Video;
import net.schueller.peertube.model.VideoList;
import net.schueller.peertube.provider.SearchSuggestionsProvider;
import net.schueller.peertube.service.SeedService;
import net.schueller.peertube.service.VideoPlayerService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class SeedListActivity extends CommonActivity {

    private String TAG = "SeedListActivity";

    public static final String EXTRA_VIDEOID = "VIDEOID";
    public static final String EXTRA_ACCOUNTDISPLAYNAME = "ACCOUNTDISPLAYNAMEANDHOST";
    public static final Integer SWITCH_INSTANCE = 2;

    private VideoAdapter seedAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;

    private int currentStart = 0;
    private int count = 12;
    private String sort = "-createdAt";
    private String filter = null;
    private String searchQuery = "";
    private Boolean subscriptions = false;

    private TextView emptyView;
    private RecyclerView recyclerView;
    private VideoViewModel viewModel;
    private ArrayList seeds;
    private boolean isLoading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_seed_list);
        //todo create new seed list layout with seeding information
        //todo get video thumbnails and such to work when not connected to source server

        createList();

    }

    private void createList() {
        recyclerView = findViewById(R.id.recyclerView);
        //swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
//        findViewById(R.id.navigation).setVisibility(View.INVISIBLE);

        emptyView = findViewById(R.id.empty_view);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(SeedListActivity.this);
        recyclerView.setLayoutManager(layoutManager);

        seedAdapter = new VideoAdapter(new ArrayList<>(), SeedListActivity.this);
        recyclerView.setAdapter(seedAdapter);
        setTitle("title");
        viewModel = ViewModelProviders.of(this).get(VideoViewModel.class);
        //bind to Livedata
        viewModel.getAllVideos().observe(this, new Observer<List<Video>>() {
            @Override
            public void onChanged(@Nullable List<Video> changedSeeds) {
                Log.v(TAG,"Video DB changed");
                if (changedSeeds != null) {
                    seeds = (ArrayList) changedSeeds;
                    if (changedSeeds.size()>0) {
                        seedAdapter.clearData();
                        seedAdapter.setData((ArrayList) changedSeeds);
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                }
            }
        });

        ItemTouchHelper helper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                         int direction) {

                        Log.e(TAG,"swiped to delete seed "+viewHolder.getAdapterPosition());
                        new AlertDialog.Builder(SeedListActivity.this)
                                .setTitle(getString(R.string.seed_del_alert_title))
                                .setMessage(getString(R.string.seed_del_alert_msg))
                                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                    int position = viewHolder.getAdapterPosition();
                                    Video toBeDeleted = (Video)seeds.get(position);
                                    Log.e(TAG,"going to delete"+toBeDeleted.toString());
//                                    Toast.makeText(ServerAddressBookActivity.this, "Deleting " +
//                                            server.getServerName(), Toast.LENGTH_LONG).show();
                                    // Delete the server
                                    //viewModel.delete(toBeDeleted);
                                    SeedService.startActionDeleteTorrent(getApplicationContext(),"whoops",toBeDeleted.getUuid());
                                })
                                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                                    seedAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                });
        helper.attachToRecyclerView(recyclerView);
    }
}
