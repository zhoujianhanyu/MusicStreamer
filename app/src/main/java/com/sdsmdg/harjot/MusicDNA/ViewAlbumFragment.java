package com.sdsmdg.harjot.MusicDNA;


import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.sdsmdg.harjot.MusicDNA.CustomBottomSheetDialogs.CustomLocalBottomSheetDialog;
import com.sdsmdg.harjot.MusicDNA.Models.LocalTrack;
import com.sdsmdg.harjot.MusicDNA.Models.UnifiedTrack;
import com.sdsmdg.harjot.MusicDNA.imageLoader.ImageLoader;
import com.squareup.leakcanary.RefWatcher;


/**
 * A simple {@link Fragment} subclass.
 */
public class ViewAlbumFragment extends Fragment {

    LocalTrackListAdapter aslAdapter;

    RecyclerView rv;

    ImageLoader imgLoader = new ImageLoader(getContext());
    ImageView backCover, mainCover;
    FloatingActionButton fab;
    Context ctx;

    TextView albumDetails;

    HomeActivity activity;

    onAlbumSongClickListener mCallback;
    onAlbumPlayAllListener mCallback2;

    public ViewAlbumFragment() {
        // Required empty public constructor
    }

    public interface onAlbumSongClickListener {
        public void onAlbumSongClickListener();

        public void addToPlaylist(UnifiedTrack ut);
    }

    public interface onAlbumPlayAllListener {
        public void onAlbumPlayAll();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ctx = context;
        activity = (HomeActivity) context;
        try {
            mCallback = (onAlbumSongClickListener) context;
            mCallback2 = (onAlbumPlayAllListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_view_album, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        albumDetails = (TextView) view.findViewById(R.id.album_details);
        int tmp = HomeActivity.tempAlbum.getAlbumSongs().size();
        String details1;
        if (tmp == 1) {
            details1 = "1 Song ";
        } else {
            details1 = tmp + " Songs ";
        }

        int tmp2 = 0;

        for (int i = 0; i < tmp; i++) {
            tmp2 += HomeActivity.tempAlbum.getAlbumSongs().get(i).getDuration();
        }

        Pair<String, String> time = HomeActivity.getTime(tmp2);

        albumDetails.setText(details1 + " •  " + time.first + "m" + time.second + "s");

        fab = (FloatingActionButton) view.findViewById(R.id.play_all_from_album);
        fab.setBackgroundTintList(ColorStateList.valueOf(HomeActivity.themeColor));
        backCover = (ImageView) view.findViewById(R.id.back_album_cover);
        mainCover = (ImageView) view.findViewById(R.id.main_album_cover);
        rv = (RecyclerView) view.findViewById(R.id.album_songs_recycler);
        aslAdapter = new LocalTrackListAdapter(HomeActivity.tempAlbum.getAlbumSongs(), getContext());
        LinearLayoutManager mLayoutManager2 = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(mLayoutManager2);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setAdapter(aslAdapter);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeActivity.queue.getQueue().clear();
                for (int i = 0; i < HomeActivity.tempAlbum.getAlbumSongs().size(); i++) {
                    UnifiedTrack ut = new UnifiedTrack(true, HomeActivity.tempAlbum.getAlbumSongs().get(i), null);
                    HomeActivity.queue.getQueue().add(ut);
                }
                mCallback2.onAlbumPlayAll();
            }
        });

        rv.addOnItemTouchListener(new ClickItemTouchListener(rv) {
            @Override
            boolean onClick(RecyclerView parent, View view, int position, long id) {
                LocalTrack track = HomeActivity.tempAlbum.getAlbumSongs().get(position);
                if (HomeActivity.queue.getQueue().size() == 0) {
                    HomeActivity.queueCurrentIndex = 0;
                    HomeActivity.queue.getQueue().add(new UnifiedTrack(true, track, null));
                } else if (HomeActivity.queueCurrentIndex == HomeActivity.queue.getQueue().size() - 1) {
                    HomeActivity.queueCurrentIndex++;
                    HomeActivity.queue.getQueue().add(new UnifiedTrack(true, track, null));
                } else if (HomeActivity.isReloaded) {
                    HomeActivity.isReloaded = false;
                    HomeActivity.queueCurrentIndex = HomeActivity.queue.getQueue().size();
                    HomeActivity.queue.getQueue().add(new UnifiedTrack(true, track, null));
                } else {
                    HomeActivity.queue.getQueue().add(++HomeActivity.queueCurrentIndex, new UnifiedTrack(true, track, null));
                }
                HomeActivity.localSelectedTrack = track;
                HomeActivity.streamSelected = false;
                HomeActivity.localSelected = true;
                HomeActivity.queueCall = false;
                HomeActivity.isReloaded = false;
                mCallback.onAlbumSongClickListener();
                return true;
            }

            @Override
            boolean onLongClick(RecyclerView parent, View view, final int position, long id) {
                CustomLocalBottomSheetDialog localBottomSheetDialog = new CustomLocalBottomSheetDialog();
                localBottomSheetDialog.setPosition(position);
                localBottomSheetDialog.setLocalTrack(activity.tempAlbum.getAlbumSongs().get(position));
                localBottomSheetDialog.setFragment("Album");
                localBottomSheetDialog.show(activity.getSupportFragmentManager(), "local_song_bottom_sheet");
                return true;
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        Bitmap bmp = null;
        try {
            bmp = getBitmap(HomeActivity.tempAlbum.getAlbumSongs().get(0).getPath());
        } catch (Exception e) {
        }
        if (bmp != null) {
            mainCover.setImageBitmap(bmp);
            backCover.setImageBitmap(bmp);
        } else {
            mainCover.setImageResource(R.drawable.ic_default);
            backCover.setImageResource(R.drawable.ic_default);
        }

    }

    public Bitmap getBitmap(String url) {
        android.media.MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(url);
        Bitmap bitmap = null;

        byte[] data = mmr.getEmbeddedPicture();

        if (data != null) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            return bitmap;
        } else {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fab.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new OvershootInterpolator());
            }
        }, 500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RefWatcher refWatcher = MusicDNAApplication.getRefWatcher(getContext());
        refWatcher.watch(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = MusicDNAApplication.getRefWatcher(getContext());
        refWatcher.watch(this);
    }

    public void updateList() {
        if (aslAdapter != null) {
            aslAdapter.notifyDataSetChanged();
        }
    }
}
