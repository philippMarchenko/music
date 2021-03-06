package com.devphill.music.ui.library.my_downloads;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.devphill.music.R;
import com.devphill.music.data.store.MediaStoreUtil;
import com.devphill.music.model.Song;
import com.devphill.music.ui.BaseFragment;
import com.devphill.music.ui.common.LibraryEmptyState;
import com.devphill.music.ui.library.SongSection;
import com.devphill.music.ui.library.net_songs.Downloader;
import com.devphill.music.view.BackgroundDecoration;
import com.devphill.music.view.DividerDecoration;
import com.devphill.music.view.HeterogeneousFastScrollAdapter;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MyDownloadsFragment extends BaseFragment {

    private FastScrollRecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;
    private List<Song> mSongs = new ArrayList<>();

    private static final String LOG_TAG = "MyDownloadsFragment";
    public static final String SEARCH_MY_DOWNLOADED = "search_my_downloaded";
    private BroadcastReceiver br;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_library_page, container, false);
        mRecyclerView = view.findViewById(R.id.library_page_list);
        mRecyclerView.addItemDecoration(new BackgroundDecoration());
        mRecyclerView.addItemDecoration(new DividerDecoration(getContext(), R.id.empty_layout));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        try{
            String selections = MediaStoreUtil.getDirectoryInclusionExclusionSelection(Downloader.folder.getAbsolutePath());
            mSongs =  MediaStoreUtil.getSongs(getContext(),selections, null);  //список песен там
        }
        catch(Exception e){

        }


        if (mAdapter == null) {
            setupAdapter();
        } else {
            mRecyclerView.setAdapter(mAdapter);
        }

        int paddingH = (int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOG_TAG, intent.getCharSequenceExtra("search").toString());
                String charString = intent.getCharSequenceExtra("search").toString();
                if (charString.isEmpty()) {
                    mSongSection.setData(mSongs);
                    mAdapter.notifyDataSetChanged();
                } else {
                    List<Song> filteredList = new ArrayList<>();
                    for (Song row : mSongs) {
                        if (row.getSongName().toLowerCase().contains(charString.toLowerCase())
                                || row.getAlbumName().toLowerCase().contains(charString.toLowerCase())
                                || row.getArtistName().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(row);
                        }
                    }
                    mSongSection.setData(filteredList);
                    mAdapter.notifyDataSetChanged();
                }
            }
        };
        getContext().registerReceiver(br,new IntentFilter(SEARCH_MY_DOWNLOADED));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
        mAdapter = null;
        mSongSection = null;
        try {
            getContext().unregisterReceiver(br);
        }
        catch (Exception e){
            Log.d(LOG_TAG, "Не удалось снять с регистрации приемник" + e.getMessage());
        }    }

    private void setupAdapter() {
        if (mRecyclerView == null || mSongs == null) {
            return;
        }

        if (mSongSection != null) {
            mSongSection.setData(mSongs);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new HeterogeneousFastScrollAdapter();
            mAdapter.setHasStableIds(true);
            mRecyclerView.setAdapter(mAdapter);

            mSongSection = new SongSection(this, mSongs);
            mAdapter.addSection(mSongSection);
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity()));
        }
    }
}
