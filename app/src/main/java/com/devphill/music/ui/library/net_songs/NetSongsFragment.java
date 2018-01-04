package com.devphill.music.ui.library.net_songs;

import android.os.Bundle;

import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.devphill.music.data.store.MediaStoreUtil;
import com.devphill.music.data.store.SharedPreferenceStore;
import com.devphill.music.model.Constants;
import com.devphill.music.player.PlayerController;
import com.devphill.music.player.ServicePlayerController;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.devphill.music.R;
import com.devphill.music.ui.common.ShuffleAllSection;
import com.devphill.music.model.Song;
import com.devphill.music.ui.BaseFragment;
import com.devphill.music.view.BackgroundDecoration;
import com.devphill.music.view.DividerDecoration;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import rx.Subscription;
import timber.log.Timber;

public class NetSongsFragment extends BaseFragment {

    ProgressBar progressBar;
    private FastScrollRecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private NetSongsAdapter netSongsAdapter;

    private ShuffleAllSection mShuffleAllSection;
    private NetSongSection mSongSection;
    private List<Song> mSongs = new ArrayList<>();
    private List<Song> downloadedSongsList = new ArrayList<>();

    private Boolean isLoading = false;
    private int numberOfPages = 1;
    private int numberOfItems = 0;
    public boolean stateOfFragment = false;

    protected PlayerController mPlayerController;

    private static final String LOG_TAG = "NetSongsFragment";

    Downloader downloader;

    int progressLast;
    int playingLastPosition;

    boolean isFirstloaded = false;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_library_page, container, false);

        mPlayerController = new ServicePlayerController(getContext(), new SharedPreferenceStore(getContext()));

        downloader = new Downloader(getContext());
        downloader.createMyFolder();


        getDownloadedSongList();

        progressBar =  view.findViewById(R.id.progressBar2) ;
        mRecyclerView = view.findViewById(R.id.library_page_list);
        mRecyclerView.addItemDecoration(new BackgroundDecoration());
        mRecyclerView.addItemDecoration(new DividerDecoration(getContext(), R.id.empty_layout));

        progressBar.setVisibility(View.VISIBLE);

        mRecyclerView.getItemAnimator().setChangeDuration(0);   //фикс бага мигания елемента

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.addOnScrollListener(
                new EndlessRecyclerViewScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore(int page, int totalItemsCount) {
                        if (page <= numberOfPages) {

                            progressBar.setVisibility(ProgressBar.VISIBLE);
                            listSong("Элджей" + Constants.ZF_FM_NEXT_PAGE + page);
                            Log.e(LOG_TAG, "" + page + "  " + numberOfPages);

                        }
                    }
                }
        );

        netSongsAdapter = new NetSongsAdapter(getContext(),getActivity(),mSongs);
        mRecyclerView.setAdapter(netSongsAdapter);

        initOnClickSongListener();

        int paddingH = (int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        listSong("Элджей");


        Log.d(LOG_TAG,"onCreateView");

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG,"onResume");

    }
    @Override
    public void onPause() {
        super.onPause();
        mSongs.clear();
        netSongsAdapter.notifyDataSetChanged();
        Log.d(LOG_TAG,"onPause");

    }
    private void initOnClickSongListener(){

        netSongsAdapter.setOnClickListenet(new NetSongsAdapter.OnClickListener() {
            @Override
            public void onDownloadClickStart(int position) {
                mSongs.get(position).setStoargeLocal(Downloader.folder.getAbsolutePath() + mSongs.get(position).getArtistName() + " - " + mSongs.get(position).getSongName());
                mSongs.get(position).setDownloadStatus(Song.SONG_DOWNLOADING);
                downloader.downloadFile("https://" +
                                mSongs.get(position).getLocation().getHost() +
                                mSongs.get(position).getLocation().getPath(),
                        mSongs.get(position).getArtistName() + " - " + mSongs.get(position).getSongName())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableObserver<Integer>() {
                            @Override
                            public void onNext(Integer value) {
                                if(progressLast < value) {
                                    mSongs.get(position).setDownloadProgress(value);
                                    netSongsAdapter.notifyItemChanged(position);
                                    Log.d(LOG_TAG,"onNext  persent " + value);
                                }
                                progressLast = value;
                            }

                            @Override
                            public void onError(Throwable e) {}
                            @Override
                            public void onComplete() {
                                Log.d(LOG_TAG,"Load file finish! ");
                                mSongs.get(position).setDownloadStatus(Song.SONG_DOWNLOADED);
                            //    netSongsAdapter.notifyItemChanged(position);
                            }
                        });

            }
            @Override
            public void onDownloadClickStop(int position) {

            }

            @Override
            public void onSongClick(int position) {

                mSongs.get(playingLastPosition).setPlaying(false);
                mSongs.get(position).setPlaying(true);
                netSongsAdapter.notifyDataSetChanged();

                mPlayerController.setQueue(mSongs, position);
                mPlayerController.play();
                playingLastPosition = position;
            }
        });


    }

    public void listSong (String keywords){
        ParserPageZFFM parserPageZFFM = new ParserPageZFFM();

        Observer<List> observer = new Observer<List>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(List value) {

                try{
                    mSongs.addAll(value);
                    Log.d(LOG_TAG, "getDownloadedSongList " + downloadedSongsList.size());

                    for (int i = 0; i < mSongs.size(); i++){

                        String songStoargeLocal = Downloader.folder.getAbsolutePath() + mSongs.get(i).getArtistName() + " - " + mSongs.get(i).getSongName();;

                        for (int j = 0; j < downloadedSongsList.size(); j++){
                            String downloadedSongStoargeLocal =  Downloader.folder.getAbsolutePath() + downloadedSongsList.get(j).getArtistName() + " - " + downloadedSongsList.get(j).getSongName();
                            if(downloadedSongStoargeLocal.equals(songStoargeLocal)){ //совпадает ссылка скачаной песни с песней с сети
                                mSongs.get(i).setDownloadStatus(Song.SONG_DOWNLOADED);
                            }
                        }
                    }

                    netSongsAdapter.notifyDataSetChanged();
                    Log.d(LOG_TAG, "onNext: size " + value.size());
                }
                catch (Exception e){

                }

            }

            @Override
            public void onError(Throwable e) {
                Log.e(LOG_TAG, "onError: ");
            }

            @Override
            public void onComplete() {
                numberOfPages = parserPageZFFM.getNumberOfItems()/50;
                int numberOfPages1 =  parserPageZFFM.getNumberOfItems()%50;
                if (numberOfPages1 != 0){ numberOfPages++;}
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                Log.d(LOG_TAG, "onComplete: All Done!");
            }
        };
        parserPageZFFM.getTrackList(Constants.ZF_FM, keywords).subscribe(observer);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecyclerView = null;
        netSongsAdapter = null;
        mSongs = null;
        downloader.release();
    }

    private void getDownloadedSongList(){

        try{
            String selections = MediaStoreUtil.getDirectoryInclusionExclusionSelection(Downloader.folder.getAbsolutePath());
            downloadedSongsList = MediaStoreUtil.getSongs(getContext(),selections, null);  //список песен там
        }
        catch(Exception e){
        }
    }
}