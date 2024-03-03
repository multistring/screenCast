package multi.string.screencast.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import multi.string.screencast.R;
import multi.string.screencast.fragment.PhotoFragment;
import multi.string.screencast.models.AlbumItem;
import multi.string.screencast.utils.FileUtil;
import multi.string.screencast.utils.StatusUtil;
import multi.string.screencast.utils.TouchUtil;
import multi.string.screencast.widgets.StatusBar;

public class PhotoDetailActivity extends BaseActivity{
    private RelativeLayout rlback = null;
    private TextView foldTitle = null;
    private ImageView startCast = null;
    private AlbumItem albumItem = null;
    PhotoFragment fragment = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置顶部状态栏透明度
        StatusBar statusBar = new StatusBar(PhotoDetailActivity.this);
        //设置颜色为半透明
        statusBar.setColor(R.color.transparent);

        setContentView(R.layout.activity_photo_detail);
        fragment = PhotoFragment.instance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frameLayout, fragment);
        transaction.show(fragment).commitAllowingStateLoss();

        rlback = (RelativeLayout) findViewById(R.id.photo_back_rl);
        rlback.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    finish();
                }
            }
        });

        startCast = (ImageView) findViewById(R.id.start_cast);
        startCast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TouchUtil.isFastClick()) {
                    fragment.needReload();
                    StatusUtil.GotoPhotoCast(PhotoDetailActivity.this, albumItem, true);
                }
            }
        });

        foldTitle = (TextView)findViewById(R.id.fold_title);

        Intent intent = getIntent();
        if (intent!=null) {
            albumItem = intent.getParcelableExtra("AlbumItem");
            if (albumItem != null){
                foldTitle.setText(albumItem.name);
            }
            String foldpath = FileUtil.getFoldPathByFile(albumItem.coverImagePath);
            fragment.setCurFoldPath(foldpath);
            intent.removeExtra("AlbumItem");
        }
    }

    @Override
    protected void onPause(){
        if (fragment != null) {
            fragment.needReload();
        }
        super.onPause();
    }
}

