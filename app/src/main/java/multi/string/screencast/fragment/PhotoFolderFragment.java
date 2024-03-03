package multi.string.screencast.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import multi.string.screencast.R;
import multi.string.screencast.activity.PhotoDetailActivity;
import multi.string.screencast.adapters.FolderGridAdapter;
import multi.string.screencast.models.Album;
import multi.string.screencast.utils.PhotoUtils;
import multi.string.screencast.utils.TouchUtil;

public class PhotoFolderFragment extends Fragment {
    private RecyclerView recyclerView;
    private Album album;
    private FolderGridAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_folder_fragment, null);
        init(view);
        return view;
    }

    private void init(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        initRecycler();
        getAlbum();

    }

    private void initRecycler() {
        mAdapter = new FolderGridAdapter(requireContext());
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(albumItem -> {
            if (TouchUtil.isFastClick()) {
                Intent intent = new Intent(getActivity(), PhotoDetailActivity.class);
                intent.putExtra("AlbumItem", albumItem);
                startActivity(intent);
            }
        });
    }

    private void getAlbum() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                album = PhotoUtils.getAlbum(requireContext());
                mHandler.sendEmptyMessage(0);
            }
        }).start();
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (album != null) {
                mAdapter.setNewInstance(album.albumItems);
            }
        }
    };
}