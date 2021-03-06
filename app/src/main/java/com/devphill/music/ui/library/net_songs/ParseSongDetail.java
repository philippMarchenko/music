package com.devphill.music.ui.library.net_songs;

import android.net.Uri;
import android.util.Log;

import com.devphill.music.model.ArtistSong;
import com.devphill.music.model.Song;
import com.devphill.music.model.SongDetail;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ParseSongDetail {

    private Document doc = null;
    private static final String LOG_TAG = "ParseSongDetail";

    public Observable getSongDetail(final String songUrl) {

        final Observable<SongDetail> observable =
                Observable.create(new ObservableOnSubscribe<SongDetail>() {
                                      @Override
                                      public void subscribe(ObservableEmitter<SongDetail> e) throws Exception {

                                          try {

                                             doc = Jsoup.connect("http://m.zk.fm" + songUrl).get();

                                          if (doc!=null){

                                              Log.d(LOG_TAG, "doc is true");

                                               List<Song> songList = new ArrayList<>();

                                              Element video = doc.select("div.video-container").first();
                                              Element iframe = video.select("iframe").first();
                                              String iframeSrc = iframe.attr("src");

                                              Element addition = doc.select("div.song-addition").first();

                                              Element info = addition.select("div.song-info").first();

                                              Element duration = info.select("div.song-info-duration").first();
                                              Element size = info.select("div.song-info-size").first();
                                              Element quality = info.select("div.song-info-quality").first();

                                              Log.d(LOG_TAG, "addition " + addition.text());

                                              Elements ul = doc.select("ul");
                                              Elements li = ul.select("li"); // select all li from ul

                                              for(int i = 0; i < li.size(); i++){

                                                  try{

                                                      String url = li.get(i).attr("data-url");

                                                      Element trackName = li.get(i).select("div.tracks-name").first();

                                                      Element a = trackName.select("a").first();
                                                      Element name = a.select("span.tracks-name-title").first();
                                                      Element artist = a.select("span.tracks-name-artist").first();

                                                      Element trackTime = li.get(i).select("div.tracks-time").first();

                                                      Uri uri;
                                                      uri = Uri.parse(url);

                                                      Song song = new Song.Builder()
                                                              .setSongName(name.text())
                                                              .setSongId(i)
                                                              .setArtistName(artist.text())
                                                              .setArtistId(i)
                                                              .setLocation(uri)
                                                              .setYear(2016)
                                                              .setDateAdded(System.currentTimeMillis())
                                                              .setTrackNumber(i)
                                                              .setInLibrary(true)
                                                              .build();
                                                      songList.add(song);
                                                  }
                                                  catch (Exception ex){
                                                      Log.d(LOG_TAG, "Не удалось распарсить" + ex.getMessage());
                                                  }
                                                  Log.d(LOG_TAG, "i " + i);
                                              }

                                              SongDetail songDetail = new SongDetail();
                                              songDetail.setArtistSongList(songList);
                                              songDetail.setBitRate(quality.text());
                                              songDetail.setDuration(duration.text());
                                              songDetail.setSize(size.text());
                                              songDetail.setVideoUrl(iframeSrc);

                                              e.onNext(songDetail);
                                              e.onComplete();
                                              Log.d(LOG_TAG, "onNext");

                                          }
                                          }  catch (Exception ex) {
                                              Log.d(LOG_TAG, "Ошибка парсинга " + ex.getMessage());

                                          }
                                      }
                                  }
                ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        return observable;
    }

}
