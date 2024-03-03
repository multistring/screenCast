package multi.string.screencast.fragment;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import multi.string.screencast.R;
import multi.string.screencast.adapters.VideosListAdapter;
import multi.string.screencast.dataloaders.VideoLoader;
import multi.string.screencast.widgets.BaseRecyclerView;
import multi.string.screencast.widgets.FastScroller;

public class VideoFragment extends Fragment {
    private BaseRecyclerView mRecyclerView;
    private VideosListAdapter mAdapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_recyclerview, container, false);

        mRecyclerView = rootView.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setEmptyView(getActivity(), rootView.findViewById(R.id.list_empty), "No media found");
        FastScroller fastScroller =  rootView.findViewById(R.id.fastscroller);
        fastScroller.setRecyclerView(mRecyclerView);
        new loadVideos().execute("");
        return rootView;
    }


    private class loadVideos extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (getActivity() != null)
                mAdapter = new VideosListAdapter((FragmentActivity) getActivity(), VideoLoader.getVideos(getActivity()));

            showLoading();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            mRecyclerView.setAdapter(mAdapter);
            closeLoading();
        }

        @Override
        protected void onPreExecute() {
        }
    }

    private void showLoading(){
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mRecyclerView.setClickable(false);
                }
            });
        }
    }

    //关闭加载中
    private void closeLoading() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mRecyclerView.setClickable(true);
                }
            });
        }
    }
}

