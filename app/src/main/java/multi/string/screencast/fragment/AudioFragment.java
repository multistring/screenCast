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
import multi.string.screencast.adapters.AudioListAdapter;
import multi.string.screencast.dataloaders.AudioLoader;
import multi.string.screencast.widgets.BaseRecyclerView;
import multi.string.screencast.widgets.DividerItemDecoration;
import multi.string.screencast.widgets.FastScroller;

public class AudioFragment extends Fragment {
    private BaseRecyclerView recyclerView;
    private AudioListAdapter mAdapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_audio_recyclerview, container, false);

        recyclerView = rootView.findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setEmptyView(getActivity(), rootView.findViewById(R.id.list_empty), "No media found");

        FastScroller fastScroller =  rootView.findViewById(R.id.fastscroller);
        fastScroller.setRecyclerView(recyclerView);

        new loadAudios().execute("");
        return rootView;
    }

    private class loadAudios extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (getActivity() != null)
                mAdapter = new AudioListAdapter((FragmentActivity) getActivity(), AudioLoader.getAudios(getActivity()));
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            recyclerView.setAdapter(mAdapter);
            if (getActivity() != null)
                recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        }

        @Override
        protected void onPreExecute() {
        }
    }
}

