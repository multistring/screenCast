package multi.string.screencast.fragment;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.yuyh.library.imgsel.adapter.FolderListAdapter;
import com.yuyh.library.imgsel.adapter.ImageListAdapter;
import com.yuyh.library.imgsel.adapter.PreviewAdapter;
import com.yuyh.library.imgsel.bean.Folder;
import com.yuyh.library.imgsel.bean.Image;
import com.yuyh.library.imgsel.common.Constant;
import com.yuyh.library.imgsel.common.OnFolderChangeListener;
import com.yuyh.library.imgsel.common.OnItemClickListener;
import com.yuyh.library.imgsel.config.ISListConfig;
import com.yuyh.library.imgsel.utils.DisplayUtils;
import com.yuyh.library.imgsel.widget.CustomViewPager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PhotoFragment extends Fragment implements View.OnClickListener, ViewPager.OnPageChangeListener {

    private RecyclerView rvImageList;
    private Button btnAlbumSelected;
    private View rlBottom;
    private CustomViewPager viewPager;

    private ISListConfig config;
    private List<Folder> folderList = new ArrayList<>();
    private List<Image> imageList = new ArrayList<>();

    private ListPopupWindow folderPopupWindow;
    private ImageListAdapter imageListAdapter;
    private FolderListAdapter folderListAdapter;
    private PreviewAdapter previewAdapter;

    private boolean hasFolderGened = false;
    private boolean needReloaded = false;

    private static final int LOADER_ALL = 0;
    private static final int LOADER_CATEGORY = 1;
    private static final int REQUEST_CAMERA = 5;

    private static final int CAMERA_REQUEST_CODE = 1;

    private File tempFile;
    private String curFoldPath = null;
    public static PhotoFragment instance() {
        PhotoFragment fragment = new PhotoFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    public void setCurFoldPath(String path){
        curFoldPath = path;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(com.yuyh.library.imgsel.R.layout.fragment_photo, container, false);
        rvImageList = view.findViewById(com.yuyh.library.imgsel.R.id.rvImageList);
        btnAlbumSelected = view.findViewById(com.yuyh.library.imgsel.R.id.btnAlbumSelected);
        btnAlbumSelected.setOnClickListener(this);
        rlBottom = view.findViewById(com.yuyh.library.imgsel.R.id.rlBottom);
        viewPager = view.findViewById(com.yuyh.library.imgsel.R.id.viewPager);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(this);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        config = new ISListConfig.Builder()
                .multiSelect(true)
                // 是否记住上次选中记录
                .rememberSelected(false)
                .needCamera(false)
                // 使用沉浸式状态栏
                .statusBarColor(Color.parseColor("#3F51B5"))
                // 设置状态栏字体风格浅色
                .isDarkStatusStyle(false)
                .build();

        btnAlbumSelected.setText(config.allImagesText);

        rvImageList.setLayoutManager(new GridLayoutManager(rvImageList.getContext(), 3));
        rvImageList.addItemDecoration(new RecyclerView.ItemDecoration() {
            int spacing = DisplayUtils.dip2px(rvImageList.getContext(), 6);
            int halfSpacing = spacing >> 1;

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = halfSpacing;
                outRect.right = halfSpacing;
                outRect.top = halfSpacing;
                outRect.bottom = halfSpacing;
            }
        });

        imageListAdapter = new ImageListAdapter(getActivity(), imageList, config);
        imageListAdapter.setShowCamera(config.needCamera);
        imageListAdapter.setMutiSelect(config.multiSelect);
        rvImageList.setAdapter(imageListAdapter);
        imageListAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public int onCheckedClick(int position, Image image) {
                return checkedImage(position, image);
            }

            @Override
            public void onImageClick(int position, Image image) {
            }
        });

        folderListAdapter = new FolderListAdapter(getActivity(), folderList, config);
        getActivity().getSupportLoaderManager().initLoader(LOADER_ALL, null, mLoaderCallback);
    }

    private int checkedImage(int position, Image image) {
        if (image != null) {
            if (Constant.imageList.contains(image.path)) {
                Constant.imageList.remove(image.path);
            } else {
                if (config.maxNum <= Constant.imageList.size()) {
                    Toast.makeText(getActivity(), String.format(getString(com.yuyh.library.imgsel.R.string.maxnum), config.maxNum), Toast.LENGTH_SHORT).show();
                    return 0;
                }
                Constant.imageList.add(image.path);
            }
            return 1;
        }
        return 0;
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        private final String[] IMAGE_PROJECTION = {
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media._ID};

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ALL) {
                return new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
            } else if (id == LOADER_CATEGORY) {
                return new CursorLoader(getActivity(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_PROJECTION,
                        IMAGE_PROJECTION[0] + " not like '%.gif%'", null, MediaStore.Images.Media.DATE_ADDED + " DESC");
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                int count = data.getCount();
                if (count > 0) {
                    List<Image> tempImageList = new ArrayList<>();
                    data.moveToFirst();
                    do {
                        String path = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[0]));
                        String name = data.getString(data.getColumnIndexOrThrow(IMAGE_PROJECTION[1]));
                        Image image = new Image(path, name);
                        tempImageList.add(image);
                        if (!hasFolderGened) {
                            File imageFile = new File(path);
                            File folderFile = imageFile.getParentFile();
                            if (folderFile == null || !imageFile.exists() || imageFile.length() < 10) {
                                continue;
                            }

                            Folder parent = null;
                            for (Folder folder : folderList) {
                                if (TextUtils.equals(folder.path, folderFile.getAbsolutePath())) {
                                    parent = folder;
                                }
                            }
                            if (parent != null) {
                                parent.images.add(image);
                            } else {
                                parent = new Folder();
                                parent.name = folderFile.getName();
                                parent.path = folderFile.getAbsolutePath();
                                parent.cover = image;

                                List<Image> imageList = new ArrayList<>();
                                imageList.add(image);

                                parent.images = imageList;
                                folderList.add(parent);
                            }
                        }
                    } while (data.moveToNext());

                    imageList.clear();
                    if (config.needCamera)
                        imageList.add(new Image());
                    imageList.addAll(tempImageList);

                    imageListAdapter.notifyDataSetChanged();
                    folderListAdapter.notifyDataSetChanged();

                    if (needReloaded == false) {
                        needReloaded = true;
                        if (curFoldPath != null){

                            Folder fold = folderListAdapter.getFoldByPath(curFoldPath);
                            if (fold != null) {
                                imageList.clear();
                                imageList.addAll(fold.images);
                                imageListAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    hasFolderGened = true;
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };


    public void needReload(){
        needReloaded = false;
    }

    private void createPopupFolderList(int width, int height) {
        folderPopupWindow = new ListPopupWindow(getActivity());
        folderPopupWindow.setAnimationStyle(com.yuyh.library.imgsel.R.style.PopupAnimBottom);
        folderPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        folderPopupWindow.setAdapter(folderListAdapter);
        folderPopupWindow.setContentWidth(width);
        folderPopupWindow.setWidth(width);
        folderPopupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        folderPopupWindow.setAnchorView(rlBottom);
        folderPopupWindow.setModal(true);
        folderListAdapter.setOnFloderChangeListener(new OnFolderChangeListener() {
            @Override
            public void onChange(int position, Folder folder) {
                folderPopupWindow.dismiss();
                if (position == 0) {
                    getActivity().getSupportLoaderManager().restartLoader(LOADER_ALL, null, mLoaderCallback);
                    btnAlbumSelected.setText(config.allImagesText);
                } else {
                    imageList.clear();
                    if (config.needCamera)
                        imageList.add(new Image());
                    imageList.addAll(folder.images);
                    imageListAdapter.notifyDataSetChanged();

                    btnAlbumSelected.setText(folder.name);
                }
            }
        });
        folderPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                setBackgroundAlpha(1.0f);
            }
        });
    }

    public void setBackgroundAlpha(float bgAlpha) {
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha = bgAlpha;
        getActivity().getWindow().setAttributes(lp);
    }

    @Override
    public void onClick(View v) {
        WindowManager wm = getActivity().getWindowManager();
        final int size = wm.getDefaultDisplay().getWidth() / 3 * 2;
        if (v.getId() == btnAlbumSelected.getId()) {
            if (folderPopupWindow == null) {
                createPopupFolderList(size, size);
            }

            if (folderPopupWindow.isShowing()) {
                folderPopupWindow.dismiss();
            } else {
                folderPopupWindow.show();
                if (folderPopupWindow.getListView() != null) {
                    folderPopupWindow.getListView().setDivider(new ColorDrawable(ContextCompat.getColor(getActivity(), com.yuyh.library.imgsel.R.color.bottom_bg)));
                }
                int index = folderListAdapter.getSelectIndex();
                index = index == 0 ? index : index - 1;
                folderPopupWindow.getListView().setSelection(index);

                folderPopupWindow.getListView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            folderPopupWindow.getListView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            folderPopupWindow.getListView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        int h = folderPopupWindow.getListView().getMeasuredHeight();
                        if (h > size) {
                            folderPopupWindow.setHeight(size);
                            folderPopupWindow.show();
                        }
                    }
                });
                setBackgroundAlpha(0.6f);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                if (tempFile != null) {
                }
            } else {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    public boolean hidePreview() {
        if (viewPager.getVisibility() == View.VISIBLE) {
            TransitionManager.go(new Scene(viewPager), new Fade().setDuration(200));
            viewPager.setVisibility(View.GONE);
            imageListAdapter.notifyDataSetChanged();
            return true;
        } else {
            return false;
        }
    }
}
