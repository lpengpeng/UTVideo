package com.xian.utouu.gsyvideoplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.NestedScrollView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.utouu.gsyvideoplayer.R;
import com.xian.utouu.gsyvideoplayer.listener.SampleListener;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.transitionseverywhere.TransitionManager;
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
//http://baobab.wdjcdn.com/14564977406580.mp4
        String url = "http://baobab.wdjcdn.com/14564977406580.mp4";
        GSYVideoManager.instance().setSettingButtonListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(DetailPlayer.this, "niiooijiojio", Toast.LENGTH_SHORT).show();
            }
        });
        detailPlayer.setUp(url, false, null, "测试视频");

        //增加封面
        final ImageView imageView = new ImageView(this);
        ImageView imageView1 = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);
        imageView1.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView1.setImageResource(R.mipmap.xxx1);

        detailPlayer.setThumbImageView(imageView1);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(400, 400);
        layoutParams.gravity = Gravity.CENTER;
        imageView.setLayoutParams(layoutParams);
        detailPlayer.addView(imageView);
        detailPlayer.getSettingButton().setEnabled(false);
        detailPlayer.getFullscreenButton().setEnabled(false);
        detailPlayer.getSeekBar().setEnabled(false);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageView.setVisibility(View.GONE);
                detailPlayer.getSettingButton().setEnabled(true);
                detailPlayer.getFullscreenButton().setEnabled(true);
                detailPlayer.getSeekBar().setEnabled(true);
            }
        });

        resolveNormalVideoUI();

        orientationUtils = new OrientationUtils(this, detailPlayer);
        //初始化不打开外部的旋转
        orientationUtils.setEnable(false);

        detailPlayer.setIsTouchWiget(true);// TODO: 2016/12/16 这个还不知道杂用
        //打开自动旋转
        detailPlayer.setRotateViewAuto(false);
        detailPlayer.setLockLand(false);
        detailPlayer.setShowFullAnimation(false);
        detailPlayer.getStartButton();
        detailPlayer.setIsShowBottomProgressBar(false);
//        detailPlayer.setShowStartButtonType(1);

        detailPlayer.setPlayButtonBackgroung(R.drawable.video_click_play_selector, R.drawable.video_click_pause_selector, R.drawable.video_click_error_selector);
//        detailPlayer.setBackButtonBackGround(R.mipmap.xxx2);
//        detailPlayer.setLockBackground(R.mipmap.lock,R.mipmap.unlock);
//        detailPlayer.setFullScreen(R.mipmap.video_enlarge,R.mipmap.video_shrink);
//        detailPlayer.setBrightnessIcon(R.mipmap.xxx2);
//        detailPlayer.setDialogVolumeProgressBar(getResources().getDrawable(R.drawable.video_new_volume_progress_bg));
//        detailPlayer.setVolumeIcon(R.mipmap.xxx2);

//        detailPlayer.getFullscreenButton().setImageResource(R.mipmap.xxx1);
//        detailPlayer.getBackButton().setImageResource(R.mipmap.xxx2);
//        detailPlayer.setCenterDialogProgressTextColor(0xfff16060, 0xffffffff);
//        detailPlayer.setCenterProgressIcon(R.mipmap.video_enlarge, R.mipmap.video_shrink);
//        detailPlayer.setCenterDialogProgressBar(getResources().getDrawable(R.drawable.video_new_seekbar_progress));
//        detailPlayer.setBrightnessTextColor(0xfff16060);
//        detailPlayer.setTitleTextColor(0xfff16060);
//        detailPlayer.setCenterDialogBackground(getResources().getDrawable(R.drawable.video_new_seekbar_progress));
//        detailPlayer.setVolumeDialogBackground(getResources().getDrawable(R.drawable.video_new_seekbar_progress));
//        detailPlayer.getSettingButton().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                    Toast.makeText(DetailPlayer.this, "niiooijiojio", Toast.LENGTH_SHORT).show();
//
//            }
//        });
//        GSYVideoManager.instance().setSettingButtonListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Toast.makeText(DetailPlayer.this, "niiooijiojio", Toast.LENGTH_SHORT).show();
//            }
//        });
//        detailPlayer.addView(imageView1);
        //显示锁屏的按钮
        detailPlayer.setNeedLockFull(true);
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

                //这是以前旧的方式
                //toDo();
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
                    boolean needLockFull = detailPlayer.isNeedLockFull();
                }
                if (orientationUtils != null) {
                    orientationUtils.setEnable(true);
                }
            }
        }
    }

    private void toFull() {
        isFull = true;

        TransitionManager.beginDelayedTransition(activityDetailPlayer);

        CommonUtil.setViewHeight(detailPlayer, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        resolveFullVideoUI();
        orientationUtils.setEnable(true);
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

    private void toDo() {
        if (isFull) {
            toNormal();
        } else {
            toFull();
        }
    }

    private void resolveNormalVideoUI() {
        //增加title
//        detailPlayer.getTitleTextView().setVisibility(View.GONE);
        detailPlayer.getTitleTextView().setText("测试视频");
//        detailPlayer.getBackButton().setVisibility(View.GONE);
    }

    private void resolveFullVideoUI() {
        detailPlayer.getTitleTextView().setVisibility(View.VISIBLE);
        detailPlayer.getTitleTextView().setText("测试视频");
        detailPlayer.getBackButton().setVisibility(View.VISIBLE);
    }

}
