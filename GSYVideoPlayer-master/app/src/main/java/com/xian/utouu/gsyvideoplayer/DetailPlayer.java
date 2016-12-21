package com.xian.utouu.gsyvideoplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.NestedScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.transitionseverywhere.TransitionManager;
import com.utouu.gsyvideoplayer.R;
import com.xian.utouu.gsyvideoplayer.listener.SampleListener;
import com.xian.utouu.gsyvideoplayer.utils.CommonUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Create by 李俊鹏 on 2016/12/20 20:49
 * Function：
 * Desc：
 */
public class DetailPlayer extends FragmentActivity {

    @BindView(R.id.post_detail_nested_scroll)
    NestedScrollView postDetailNestedScroll;
    @BindView(R.id.detail_player)
    StandardGSYVideoPlayer detailPlayer;
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
        GSYVideoManager.instance().setSettingButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DetailPlayer.this, "niiooijiojio", Toast.LENGTH_SHORT).show();
            }
        });
        detailPlayer.setUp(url, false, null, "测试视频");

        detailPlayer.getFullscreenButton().setEnabled(false);
        resolveNormalVideoUI();
        orientationUtils = new OrientationUtils(this, detailPlayer);
        //初始化状态不能旋转
        orientationUtils.setEnable(false);
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
                detailPlayer.startWindowFullscreen(DetailPlayer.this, true, true);
            }
        });
        detailPlayer.setThumbPlay(false);//点击封面是否可以播放
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

        if (StandardGSYVideoPlayer.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GSYVideoPlayer.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
        //释放所有的设置
        GSYVideoManager.instance().releaseAllSettting();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
        if (isPlay) {
            if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
                if (!detailPlayer.isIfCurrentIsFullscreen()) {
                    detailPlayer.startWindowFullscreen(DetailPlayer.this, true, true);
                }
            } else {
                //新版本isIfCurrentIsFullscreen的标志位内部提前设置了，所以不会和手动点击冲突
                if (detailPlayer.isIfCurrentIsFullscreen()) {
                    StandardGSYVideoPlayer.backFromWindowFull(this);
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
}
