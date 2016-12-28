package cn.utsoft.commons.videoplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.transitionseverywhere.TransitionManager;
import com.utouu.gsyvideoplayer.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.utsoft.commons.UTVideoManager;
import cn.utsoft.commons.UTVideoPlayer;
import cn.utsoft.commons.videoplayer.listener.SampleListener;
import cn.utsoft.commons.videoplayer.utils.CommonUtil;
import cn.utsoft.commons.UTVideo.listener.LockClickListener;
import cn.utsoft.commons.UTVideo.utils.OrientationUtils;
import cn.utsoft.commons.UTVideo.video.UTStandardGSYVideoPlayer;

/**
 * Create by 李俊鹏 on 2016/12/20 20:49
 * Function：
 * Desc：
 */
public class MyVideoActivity extends FragmentActivity {

    @BindView(R.id.post_detail_nested_scroll)
    NestedScrollView postDetailNestedScroll;
    @BindView(R.id.detail_player)
    UTStandardGSYVideoPlayer detailPlayer;
    @BindView(R.id.activity_detail_player)
    RelativeLayout activityDetailPlayer;

    private boolean isFull;
    private boolean isPlay;

    private OrientationUtils orientationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_player);
        ButterKnife.bind(this);
        String url = "http://baobab.wdjcdn.com/14564977406580.mp4";
        UTVideoManager.instance().setSettingButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MyVideoActivity.this, "你找我？", Toast.LENGTH_SHORT).show();
            }
        });
        detailPlayer.setUp(url, false, null, "测试视频");

        detailPlayer.getFullscreenButton().setEnabled(false);
        resolveNormalVideoUI();
        orientationUtils = new OrientationUtils(this, detailPlayer);
        //初始化状态不能旋转
        orientationUtils.setEnable(false);
        detailPlayer.setIsTouchWiget(true);
        //关闭自动旋转
        detailPlayer.setRotateViewAuto(false);
        //显示锁屏的按钮
        detailPlayer.setNeedLockFull(true);
        //禁止显示全屏动画
        detailPlayer.setShowFullAnimation(false);
        detailPlayer.setLockClickListener(new LockClickListener() {
            @Override
            public void onClick(View view, boolean lock) {
                orientationUtils.setEnable(!lock);
            }
        });
        detailPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                orientationUtils.resolveByClick();
                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                detailPlayer.startWindowFullscreen(MyVideoActivity.this, true, true);
            }
        });
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);
//        imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                detailPlayer.deleteView();
//            }
//        });
//        //添加自定义布局
//        detailPlayer.addView(imageView);
        //添加封面
        detailPlayer.setThumbImageView(imageView);
        detailPlayer.setThumbPlay(true);//点击封面是否可以播放
//        final RelativeLayout thumbImageViewLayout = detailPlayer.getThumbImageViewLayout();
//        thumbImageViewLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                thumbImageViewLayout.setVisibility(View.GONE);
//                detailPlayer.setThumbPlay(true);
//            }
//        });
        detailPlayer.getBackButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFull) {
                    finish();
                } else {
                    toNormal();
                }

            }
        });


        detailPlayer.setStandardVideoAllCallBack(new SampleListener() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                //开始播放了才能旋转和全屏
                orientationUtils.setEnable(true);
                isPlay = true;
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                super.onAutoComplete(url, objects);
            }

            @Override
            public void onClickStartError(String url, Object... objects) {
                super.onClickStartError(url, objects);
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                super.onQuitFullscreen(url, objects);
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                    detailPlayer.getFullscreenButton().setImageResource(R.drawable.video_enlarge);
                }
            }
        });

    }

    @Override
    public void onBackPressed() {

        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }

        if (UTStandardGSYVideoPlayer.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UTVideoPlayer.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
        //释放所有的设置
        UTVideoManager.instance().releaseAllSettting();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
        if (isPlay) {
            if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
                if (!detailPlayer.isIfCurrentIsFullscreen()) {
                    detailPlayer.startWindowFullscreen(MyVideoActivity.this, true, true);
                }
            } else {
                //新版本isIfCurrentIsFullscreen的标志位内部提前设置了，所以不会和手动点击冲突
                if (detailPlayer.isIfCurrentIsFullscreen()) {
                    UTStandardGSYVideoPlayer.backFromWindowFull(this);
                }
                if (orientationUtils != null) {
                    orientationUtils.setEnable(true);
                }
            }
        }
    }

    private void toNormal() {
        isFull = false;
        orientationUtils.setEnable(false);
        int delay = orientationUtils.backToProtVideo();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(activityDetailPlayer);
                CommonUtil.setViewHeight(detailPlayer, ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) getResources().getDimension(R.dimen.post_media_height));
            }
        }, delay);
    }

    private void resolveNormalVideoUI() {
        detailPlayer.getTitleTextView().setText("测试视频");
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        detailPlayer.onVideoPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        detailPlayer.onVideoResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
//        detailPlayer.onVideoPause();
    }
}
