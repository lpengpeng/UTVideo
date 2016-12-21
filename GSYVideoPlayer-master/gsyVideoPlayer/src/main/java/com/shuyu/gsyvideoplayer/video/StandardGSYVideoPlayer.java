package com.shuyu.gsyvideoplayer.video;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.GSYVideoPlayer;
import com.shuyu.gsyvideoplayer.R;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.listener.StandardVideoAllCallBack;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.NetworkUtils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import moe.codeest.enviews.ENDownloadView;
import moe.codeest.enviews.ENPlayView;

import static com.shuyu.gsyvideoplayer.R.id.volume_progressbar;


/**
 * 标准播放器
 * Created by shuyu on 2016/11/11.
 */

public class StandardGSYVideoPlayer extends GSYVideoPlayer {


    protected Timer DISSMISS_CONTROL_VIEW_TIMER;

    protected ProgressBar mBottomProgressBar;

    private ENDownloadView mLoadingProgressBar;

    protected TextView mTitleTextView; //title

    protected RelativeLayout mThumbImageViewLayout;//封面父布局

    private View mThumbImageView; //封面

    protected Dialog mBrightnessDialog;

    protected TextView mBrightnessDialogTv;

    protected Dialog mVolumeDialog;

    protected ProgressBar mDialogVolumeProgressBar;

    protected StandardVideoAllCallBack mStandardVideoAllCallBack;//标准播放器的回调

    protected DismissControlViewTimerTask mDismissControlViewTimerTask;

    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;
    protected ImageView mLockScreen;

    protected Drawable mBottomProgressDrawable;
    protected Drawable mBottomShowProgressDrawable;
    protected Drawable mBottomShowProgressThumbDrawable;
    protected Drawable mDialogProgressBarDrawable;

    protected boolean mLockCurScreen;//锁定屏幕点击

    protected boolean mNeedLockFull;//是否需要锁定屏幕

    private boolean mThumbPlay;//是否点击封面播放

    private Context context;
    private int startButtonType;
    private ImageView mBrightnessDialogIv;
    private ImageView mDialogVolumeIcon;
    private int startButtonBackground;
    private LinearLayout mDialogContent;
    private FrameLayout mCustomFrameLayout;
    private TextView mSettingButton;
    protected LockClickListener mLockClickListener;

    public void setStandardVideoAllCallBack(StandardVideoAllCallBack standardVideoAllCallBack) {
        this.mStandardVideoAllCallBack = standardVideoAllCallBack;
        setVideoAllCallBack(standardVideoAllCallBack);
    }

    public StandardGSYVideoPlayer(Context context) {
        super(context);
        this.context = context;
    }

    public StandardGSYVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        super.setOnClickListener(l);
    }

    @Override
    protected void init(final Context context) {
        super.init(context);
        final GSYVideoManager instance = GSYVideoManager.instance();
        boolean showBottomProgressBar = instance.getShowBottomProgressBar();
        if (showBottomProgressBar) {
            mBottomProgressBar = (ProgressBar) findViewById(R.id.bottom_progressbar);
        }
        mCustomFrameLayout = (FrameLayout) findViewById(R.id.fl_custom);
        startButtonBackground = instance.getStartButtonBackground();
        if (startButtonBackground == 0) {
            mStartButtonNormal.setVisibility(View.GONE);
        } else {
            mStartButtonAnim.setVisibility(View.GONE);

        }
        int backButtonBackGround = instance.getBackButtonBackground();
        if (backButtonBackGround != 0) {
            mBackButton.setImageResource(backButtonBackGround);
        }
        mTitleTextView = (TextView) findViewById(R.id.title);
        mThumbImageViewLayout = (RelativeLayout) findViewById(R.id.thumb);
        mLockScreen = (ImageView) findViewById(R.id.lock_screen);
        mSettingButton = (TextView) findViewById(R.id.btn_set);
        mBottomContainer.setVisibility(View.VISIBLE);
        mTopContainer.setVisibility(View.VISIBLE);
        mLoadingProgressBar = (ENDownloadView) findViewById(R.id.loading);
        mThumbImageViewLayout.setOnClickListener(this);
        mBackButton.setOnClickListener(this);
        mStartButtonNormal.setOnClickListener(this);
//        mSettingButton.setOnClickListener(this);
        int backButtonTextColor = instance.getTitleTextColor();
        if (backButtonTextColor != 0) {
            mTitleTextView.setTextColor(backButtonTextColor);
        }
        if (mThumbImageView != null && !mIfCurrentIsFullscreen) {
            mThumbImageViewLayout.removeAllViews();
            resolveThumbImage(mThumbImageView);
        }
        if (mBottomProgressDrawable != null) {
            mBottomProgressBar.setProgressDrawable(mBottomProgressDrawable);
        }
        if (mBottomShowProgressDrawable != null) {
            mProgressBar.setProgressDrawable(mBottomProgressDrawable);
        }
        if (mBottomShowProgressThumbDrawable != null) {
            mProgressBar.setThumb(mBottomShowProgressThumbDrawable);
        }
        mLockScreen.setVisibility(GONE);
        final int lockBackground = instance.getLockBackground();
        final int unLockBackground = instance.getUnLockBackground();
        mLockScreen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLockCurScreen) {
                    if (unLockBackground != 0) {
                        mLockScreen.setImageResource(instance.getUnLockBackground());
                    } else {
                        mLockScreen.setImageResource(R.drawable.unlock);
                    }
                    mLockCurScreen = false;
                } else {
                    if (lockBackground != 0) {
                        mLockScreen.setImageResource(instance.getLockBackground());
                    } else {
                        mLockScreen.setImageResource(R.drawable.lock);
                    }
                    mLockCurScreen = true;
                    hideAllWidget();
                }
                if (mLockClickListener != null) {
                    mLockClickListener.onClick(v, mLockCurScreen);
                }
            }
        });
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param objects       object[0]目前为title
     * @return
     */
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, Object... objects) {
        return setUp(url, cacheWithPlay, (File) null, objects);
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param cachePath     缓存路径，如果是M3U8或者HLS，请设置为false
     * @param objects       object[0]目前为title
     * @return
     */
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, File cachePath, Object... objects) {
        if (super.setUp(url, cacheWithPlay, cachePath, objects)) {
            if (objects != null && objects.length > 0) {
                mTitleTextView.setText(objects[0].toString());
            }
            GSYVideoManager instance = GSYVideoManager.instance();
            int fullScreenBackground = instance.getFullScreenBackground();
            int unFullScreenBackground = instance.getUnFullScreenBackground();
            OnClickListener settingListener = instance.getSettingListener();
            mSettingButton.setOnClickListener(settingListener);
            if (mIfCurrentIsFullscreen) {
                if (fullScreenBackground != 0) {
                    mFullscreenButton.setImageResource(unFullScreenBackground);
                } else {
                    mFullscreenButton.setImageResource(R.drawable.video_shrink);
                }
            } else {
                if (unFullScreenBackground != 0) {
                    mFullscreenButton.setImageResource(fullScreenBackground);
                } else {
                    mFullscreenButton.setImageResource(R.drawable.video_enlarge);
                }
//                mBackButton.setVisibility(View.GONE);//// TODO: 2016/12/14 开始的时候显示
            }
            return true;
        }
        return false;
    }

    @Override
    public int getLayoutId() {
        return R.layout.video_layout_standard;
    }

    @Override
    protected void setStateAndUi(int state) {
        super.setStateAndUi(state);
        switch (mCurrentState) {
            case CURRENT_STATE_NORMAL:
                changeUiToNormal();
                break;
            case CURRENT_STATE_PREPAREING:
                changeUiToPrepareingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING:
                changeUiToPlayingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PAUSE:
                changeUiToPauseShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_ERROR:
                changeUiToError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                changeUiToCompleteShow();
                cancelDismissControlViewTimer();
                if (mBottomProgressBar != null) {
                    mBottomProgressBar.setProgress(100);
                }
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                changeUiToPlayingBufferingShow();
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    if (mChangePosition) {
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        if (mBottomProgressBar != null) {
                            mBottomProgressBar.setProgress(progress);
                        }

                    }
                    if (!mChangePosition && !mChangeVolume && !mBrightness) {
                        onClickUiToggle();
                    }
                    break;
            }
        } else if (id == R.id.progress) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    break;
            }
        }

        if (mIfCurrentIsFullscreen && mLockCurScreen && mNeedLockFull) {
            return true;
        }

        return super.onTouch(v, event);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int i = v.getId();
        if (i == R.id.thumb) {
            if (!mThumbPlay) {
                return;
            }
            if (TextUtils.isEmpty(mUrl)) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (mCurrentState == CURRENT_STATE_NORMAL) {
                if (!mUrl.startsWith("file") && !CommonUtil.isWifiConnected(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                startPlayLogic();
            } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
                onClickUiToggle();
            }
        } else if (i == R.id.surface_container) {
            if (mStandardVideoAllCallBack != null && isCurrentMediaListener()) {
                if (mIfCurrentIsFullscreen) {
                    Debuger.printfLog("onClickBlankFullscreen");
                    mStandardVideoAllCallBack.onClickBlankFullscreen(mUrl, mObjects);
                } else {
                    Debuger.printfLog("onClickBlank");
                    mStandardVideoAllCallBack.onClickBlank(mUrl, mObjects);
                }
            }
            startDismissControlViewTimer();
        }
    }

    @Override
    public void showWifiDialog() {
        super.showWifiDialog();
        if (!NetworkUtils.isAvailable(mContext)) {
            Toast.makeText(mContext, getResources().getString(R.string.no_net), Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(getResources().getString(R.string.tips_not_wifi));
        builder.setPositiveButton(getResources().getString(R.string.tips_not_wifi_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startPlayLogic();
                WIFI_TIP_DIALOG_SHOWED = true;
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.tips_not_wifi_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


    @Override
    public void startPlayLogic() {
        if (mStandardVideoAllCallBack != null) {
            Debuger.printfLog("onClickStartThumb");
            mStandardVideoAllCallBack.onClickStartThumb(mUrl, mObjects);
        }
        prepareVideo();
        startDismissControlViewTimer();
    }

    @Override
    protected void onClickUiToggle() {
        if (mIfCurrentIsFullscreen && mLockCurScreen && mNeedLockFull) {
            mLockScreen.setVisibility(VISIBLE);
            return;
        }
        if (mCurrentState == CURRENT_STATE_PREPAREING) {
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPrepareingClear();
            } else {
                changeUiToPrepareingShow();
            }
        } else if (mCurrentState == CURRENT_STATE_PLAYING) {
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingClear();
            } else {
                changeUiToPlayingShow();
            }
        } else if (mCurrentState == CURRENT_STATE_PAUSE) {
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPauseClear();
            } else {
                changeUiToPauseShow();
            }
        } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToCompleteClear();
            } else {
                changeUiToCompleteShow();
            }
        } else if (mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            if (mBottomContainer.getVisibility() == View.VISIBLE) {
                changeUiToPlayingBufferingClear();
            } else {
                changeUiToPlayingBufferingShow();
            }
        }
    }

    @Override
    protected void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime) {
        super.setProgressAndTime(progress, secProgress, currentTime, totalTime);
        if (mBottomProgressBar != null) {
            if (progress != 0) mBottomProgressBar.setProgress(progress);
            if (secProgress != 0 && !mCacheFile)
                mBottomProgressBar.setSecondaryProgress(secProgress);
        }

    }

    @Override
    protected void resetProgressAndTime() {
        super.resetProgressAndTime();
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setProgress(0);
            mBottomProgressBar.setSecondaryProgress(0);
        }

    }

    //Unified management Ui
    private void changeUiToNormal() {
        Debuger.printfLog("changeUiToNormal");
        mTopContainer.setVisibility(View.VISIBLE);
//        mBottomContainer.setVisibility(View.INVISIBLE);
        mStartView.setVisibility(View.VISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        GSYVideoManager instance = GSYVideoManager.instance();
        mLoadingProgressBar.reset();
        mThumbImageViewLayout.setVisibility(View.VISIBLE);
        mCoverImageView.setVisibility(View.VISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility((mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    private void changeUiToPrepareingShow() {
        Debuger.printfLog("changeUiToPrepareingShow");
        mTopContainer.setVisibility(View.VISIBLE);
        mBottomContainer.setVisibility(View.VISIBLE);
        mStartView.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.setVisibility(View.VISIBLE);
        mLoadingProgressBar.start();
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        mCoverImageView.setVisibility(View.VISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility(GONE);
    }

    private void changeUiToPrepareingClear() {
        Debuger.printfLog("changeUiToPrepareingClear");
        mTopContainer.setVisibility(View.INVISIBLE);
        mBottomContainer.setVisibility(View.INVISIBLE);
        mStartView.setVisibility(View.INVISIBLE);
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mCoverImageView.setVisibility(View.VISIBLE);
        mLockScreen.setVisibility(GONE);
    }

    private void changeUiToPlayingShow() {
        Debuger.printfLog("changeUiToPlayingShow");
        mTopContainer.setVisibility(View.VISIBLE);
        mBottomContainer.setVisibility(View.VISIBLE);
        mStartView.setVisibility(View.VISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.reset();
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        mFullscreenButton.setEnabled(true);
        mCoverImageView.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility((mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    private void changeUiToPlayingClear() {
        Debuger.printfLog("changeUiToPlayingClear");
        changeUiToClear();
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void changeUiToPauseShow() {
        Debuger.printfLog("changeUiToPauseShow");
        mTopContainer.setVisibility(View.VISIBLE);
        mBottomContainer.setVisibility(View.VISIBLE);
        mStartView.setVisibility(View.VISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.reset();
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        mCoverImageView.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility((mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
        updatePauseCover();
    }

    private void changeUiToPauseClear() {
        Debuger.printfLog("changeUiToPauseClear");
        changeUiToClear();
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.VISIBLE);
        }
        updatePauseCover();
    }

    private void changeUiToPlayingBufferingShow() {
        Debuger.printfLog("changeUiToPlayingBufferingShow");
        mTopContainer.setVisibility(View.VISIBLE);
        mBottomContainer.setVisibility(View.VISIBLE);
        mStartView.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.setVisibility(View.VISIBLE);
        mLoadingProgressBar.start();
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        mCoverImageView.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility(GONE);
    }

    private void changeUiToPlayingBufferingClear() {
        Debuger.printfLog("changeUiToPlayingBufferingClear");
        mTopContainer.setVisibility(View.INVISIBLE);
        mBottomContainer.setVisibility(View.INVISIBLE);
        mStartView.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.setVisibility(View.VISIBLE);
        mLoadingProgressBar.start();
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        mCoverImageView.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.VISIBLE);
        }
        mLockScreen.setVisibility(GONE);
        updateStartImage();
    }

    private void changeUiToClear() {
        Debuger.printfLog("changeUiToClear");
        mTopContainer.setVisibility(View.INVISIBLE);
        mBottomContainer.setVisibility(View.INVISIBLE);
        mStartView.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.reset();
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        mCoverImageView.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility(GONE);
    }

    private void changeUiToCompleteShow() {
        Debuger.printfLog("changeUiToCompleteShow");
        mTopContainer.setVisibility(View.VISIBLE);
        mBottomContainer.setVisibility(View.VISIBLE);
        mStartView.setVisibility(View.VISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.reset();
        mThumbImageViewLayout.setVisibility(View.VISIBLE);
        mCoverImageView.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility((mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    private void changeUiToCompleteClear() {
        Debuger.printfLog("changeUiToCompleteClear");
        mTopContainer.setVisibility(View.INVISIBLE);
        mBottomContainer.setVisibility(View.INVISIBLE);
        mStartView.setVisibility(View.VISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.reset();
        mThumbImageViewLayout.setVisibility(View.VISIBLE);
        mCoverImageView.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility((mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    private void changeUiToError() {
        Debuger.printfLog("changeUiToError");
        mTopContainer.setVisibility(View.INVISIBLE);
        mBottomContainer.setVisibility(View.INVISIBLE);
        mStartView.setVisibility(View.VISIBLE);
        mLoadingProgressBar.setVisibility(View.INVISIBLE);
        mLoadingProgressBar.reset();
        mThumbImageViewLayout.setVisibility(View.INVISIBLE);
        mCoverImageView.setVisibility(View.VISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.INVISIBLE);
        }
        mLockScreen.setVisibility((mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    private void updateStartImage() {

        if (startButtonBackground != 0) {
            GSYVideoManager instance = GSYVideoManager.instance();
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                mStartButtonNormal.setBackgroundResource(instance.getPauseButtonBackground());
            } else if (mCurrentState == CURRENT_STATE_ERROR) {
                mStartButtonNormal.setBackgroundResource(instance.getErrorButtonBackground());
            } else {
                mStartButtonNormal.setBackgroundResource(instance.getStartButtonBackground());
            }

        } else {
            ENPlayView enPlayView = (ENPlayView) mStartButtonAnim;
            enPlayView.setDuration(500);
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                enPlayView.play();
//                mStartView.setImageResource(R.drawable.video_click_pause_selector);
            } else if (mCurrentState == CURRENT_STATE_ERROR) {
                enPlayView.pause();
                //mStartView.setImageResource(R.drawable.video_click_error_selector);
            } else {
                enPlayView.pause();
                //mStartView.setImageResource(R.drawable.video_click_play_selector);
            }
        }

    }


    private void updatePauseCover() {
        if (mFullPauseBitmap == null || mFullPauseBitmap.isRecycled()) {
            try {
                mFullPauseBitmap = mTextureView.getBitmap(mTextureView.getSizeW(), mTextureView.getSizeH());
            } catch (Exception e) {
                e.printStackTrace();
                mFullPauseBitmap = null;
            }
        }
        showPauseCover();
    }

    @Override
    protected void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        super.showProgressDialog(deltaX, seekTime, seekTimePosition, totalTime, totalTimeDuration);
        GSYVideoManager instance = GSYVideoManager.instance();
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.video_progress_dialog, null);
            mDialogProgressBar = ((ProgressBar) localView.findViewById(R.id.duration_progressbar));
            Drawable draw = instance.getCenterProgressBarBackground();
            if (draw != null) {
                draw.setBounds(mDialogProgressBar.getProgressDrawable().getBounds());
                mDialogProgressBar.setProgressDrawable(draw);
                mDialogProgressBar.setProgress(0);
            }
            mDialogSeekTime = ((TextView) localView.findViewById(R.id.tv_current));
            mDialogTotalTime = ((TextView) localView.findViewById(R.id.tv_duration));
            mDialogIcon = ((ImageView) localView.findViewById(R.id.duration_image_tip));
            mDialogContent = (LinearLayout) localView.findViewById(R.id.content);
            mProgressDialog = new Dialog(getContext(), R.style.video_style_dialog_progress);
            mProgressDialog.setContentView(localView);
            mProgressDialog.getWindow().addFlags(Window.FEATURE_ACTION_BAR);
            mProgressDialog.getWindow().addFlags(32);
            mProgressDialog.getWindow().addFlags(16);
            mProgressDialog.getWindow().setLayout(getWidth(), getHeight());
            int centerHighLightColor = instance.getCenterHighLightColor();
            int centerNormalColor = instance.getCenterNormalColor();
            Drawable centerDialogBackground = instance.getCenterDialogBackground();
            if (centerNormalColor != 0) {
                mDialogTotalTime.setTextColor(centerNormalColor);
            }
            if (centerHighLightColor != 0) {
//                mDialogSeekTime.setTextColor(centerHighLightColor);
                mDialogSeekTime.setTextColor(centerHighLightColor);
            }
            if (centerDialogBackground != null) {
                mDialogContent.setBackground(centerDialogBackground);
            }
            WindowManager.LayoutParams localLayoutParams = mProgressDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.TOP;
            localLayoutParams.width = getWidth();
            localLayoutParams.height = getHeight();
            int location[] = new int[2];
            getLocationOnScreen(location);
            localLayoutParams.x = location[0];
            localLayoutParams.y = location[1];
            mProgressDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        mDialogSeekTime.setText(seekTime);
        mDialogTotalTime.setText(" / " + totalTime);
        if (totalTimeDuration > 0) {
            mDialogProgressBar.setProgress(seekTimePosition * 100 / totalTimeDuration);
        }
        int centerForwardIcon = instance.getCenterForwardIcon();
        int centerBackwardIcon = instance.getCenterBackwardIcon();
        if (deltaX > 0) {
            if (centerForwardIcon != 0) {
                mDialogIcon.setBackgroundResource(centerForwardIcon);
            } else {
                mDialogIcon.setBackgroundResource(R.drawable.video_forward_icon);
            }
        } else {
            if (centerBackwardIcon != 0) {
                mDialogIcon.setBackgroundResource(centerBackwardIcon);
            } else {
                mDialogIcon.setBackgroundResource(R.drawable.video_backward_icon);
            }
        }

    }

    @Override
    protected void dismissProgressDialog() {
        super.dismissProgressDialog();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void showVolumeDialog(float deltaY, int volumePercent) {
        super.showVolumeDialog(deltaY, volumePercent);
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.video_volume_dialog, null);
            mDialogVolumeProgressBar = ((ProgressBar) localView.findViewById(volume_progressbar));
            mDialogVolumeIcon = (ImageView) localView.findViewById(R.id.volume_icon);
            LinearLayout mDialogBackground = (LinearLayout) localView.findViewById(R.id.content);
            GSYVideoManager instance = GSYVideoManager.instance();
            Drawable draw = instance.getVolumeProgressBarBackground();
            Drawable volumeDialogBackground = instance.getVolumeDialogBackground();
            if (draw != null) {//需要验证是否管用
                draw.setBounds(mDialogVolumeProgressBar.getProgressDrawable().getBounds());
                mDialogVolumeProgressBar.setProgressDrawable(draw);
                mDialogVolumeProgressBar.setProgress(0);
            }
            int volumeIcon = instance.getVolumeIcon();
            if (mDialogVolumeIcon != null && volumeIcon != 0) {
                mDialogVolumeIcon.setImageResource(volumeIcon);
            }
            if (volumeDialogBackground != null) {
                mDialogBackground.setBackground(volumeDialogBackground);
            }
            mVolumeDialog = new Dialog(getContext(), R.style.video_style_dialog_progress);
            mVolumeDialog.setContentView(localView);
            mVolumeDialog.getWindow().addFlags(8);
            mVolumeDialog.getWindow().addFlags(32);
            mVolumeDialog.getWindow().addFlags(16);
            mVolumeDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mVolumeDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            localLayoutParams.width = getWidth();
            localLayoutParams.height = getHeight();
            int location[] = new int[2];
            getLocationOnScreen(location);
            localLayoutParams.x = location[0];
            localLayoutParams.y = location[1];
            mVolumeDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }

        mDialogVolumeProgressBar.setProgress(volumePercent);
    }

    @Override
    protected void dismissVolumeDialog() {
        super.dismissVolumeDialog();
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

    @Override
    protected void showBrightnessDialog(float percent) {
        GSYVideoManager instance = GSYVideoManager.instance();
        if (mBrightnessDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.video_brightness, null);
            mBrightnessDialogTv = (TextView) localView.findViewById(R.id.app_video_brightness);
            mBrightnessDialogIv = (ImageView) localView.findViewById(R.id.app_video_brightness_icon);
            mBrightnessDialog = new Dialog(getContext(), R.style.video_style_dialog_progress);
            mBrightnessDialog.setContentView(localView);
            mBrightnessDialog.getWindow().addFlags(8);
            mBrightnessDialog.getWindow().addFlags(32);
            mBrightnessDialog.getWindow().addFlags(16);
            mBrightnessDialog.getWindow().setLayout(-2, -2);
            WindowManager.LayoutParams localLayoutParams = mBrightnessDialog.getWindow().getAttributes();
            localLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
            localLayoutParams.width = getWidth();
            localLayoutParams.height = getHeight();
            int location[] = new int[2];
            getLocationOnScreen(location);
            localLayoutParams.x = location[0];
            localLayoutParams.y = location[1];
            mBrightnessDialog.getWindow().setAttributes(localLayoutParams);
        }
        if (mBrightnessDialogIv != null && instance.getBrightnessIcon() != 0) {
            mBrightnessDialogIv.setImageResource(instance.getBrightnessIcon());
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
        }
        int brightnessTextColor = instance.getBrightnessTextColor();
        if (brightnessTextColor != 0) {
            mBrightnessDialogTv.setTextColor(brightnessTextColor);
        }
        if (mBrightnessDialogTv != null)
            mBrightnessDialogTv.setText((int) (percent * 100) + "%");
    }

    @Override
    protected void dismissBrightnessDialog() {
        super.dismissVolumeDialog();
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
        }
    }

    @Override
    protected void loopSetProgressAndTime() {
        super.loopSetProgressAndTime();
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setProgress(0);
        }
    }

    @Override
    public void onBackFullscreen() {
        clearFullscreenLayout();
    }

    @Override
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        GSYBaseVideoPlayer gsyBaseVideoPlayer = super.startWindowFullscreen(context, actionBar, statusBar);
        if (gsyBaseVideoPlayer != null) {
            StandardGSYVideoPlayer gsyVideoPlayer = (StandardGSYVideoPlayer) gsyBaseVideoPlayer;
            gsyVideoPlayer.setStandardVideoAllCallBack(mStandardVideoAllCallBack);
            gsyVideoPlayer.setNeedLockFull(isNeedLockFull());
            gsyVideoPlayer.setLockClickListener(mLockClickListener);
        }
        return gsyBaseVideoPlayer;
    }

    @Override
    public GSYBaseVideoPlayer showSmallVideo(Point size, boolean actionBar, boolean statusBar) {
        GSYBaseVideoPlayer gsyBaseVideoPlayer = super.showSmallVideo(size, actionBar, statusBar);
        if (gsyBaseVideoPlayer != null) {
            StandardGSYVideoPlayer gsyVideoPlayer = (StandardGSYVideoPlayer) gsyBaseVideoPlayer;
            gsyVideoPlayer.setStandardVideoAllCallBack(mStandardVideoAllCallBack);
        }
        return gsyBaseVideoPlayer;
    }

    /**
     * 初始化为正常状态
     */
    public void initUIState() {
        setStateAndUi(CURRENT_STATE_NORMAL);
    }

    private void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        DISSMISS_CONTROL_VIEW_TIMER = new Timer();
        mDismissControlViewTimerTask = new DismissControlViewTimerTask();
        DISSMISS_CONTROL_VIEW_TIMER.schedule(mDismissControlViewTimerTask, 2500);
    }

    private void cancelDismissControlViewTimer() {
        if (DISSMISS_CONTROL_VIEW_TIMER != null) {
            DISSMISS_CONTROL_VIEW_TIMER.cancel();
        }
        if (mDismissControlViewTimerTask != null) {
            mDismissControlViewTimerTask.cancel();
        }
    }

    protected class DismissControlViewTimerTask extends TimerTask {

        @Override
        public void run() {
            if (mCurrentState != CURRENT_STATE_NORMAL
                    && mCurrentState != CURRENT_STATE_ERROR
                    && mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
                if (getContext() != null && getContext() instanceof Activity) {
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideAllWidget();
                            mLockScreen.setVisibility(GONE);
                        }
                    });
                }
            }
        }
    }

    protected void hideAllWidget() {
        mBottomContainer.setVisibility(View.INVISIBLE);
        mTopContainer.setVisibility(View.INVISIBLE);
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.VISIBLE);
        }
        mStartView.setVisibility(View.INVISIBLE);
    }

    private void resolveThumbImage(View thumb) {
        mThumbImageViewLayout.addView(thumb);
        ViewGroup.LayoutParams layoutParams = thumb.getLayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        thumb.setLayoutParams(layoutParams);
    }

    /***
     * 设置封面
     */
    public void setThumbImageView(View view) {
        if (mThumbImageViewLayout != null) {
            mThumbImageView = view;
            resolveThumbImage(view);
        }
    }

    /***
     * 清除封面
     */
    public void clearThumbImageView() {
        if (mThumbImageViewLayout != null) {
            mThumbImageViewLayout.removeAllViews();
        }
    }

    /**
     * 获取title
     */
    public TextView getTitleTextView() {
        return mTitleTextView;
    }

    /**
     * 底部进度条-弹出的
     */
    public void setBottomShowProgressBarDrawable(Drawable drawable, Drawable thumb) {
        mBottomShowProgressDrawable = drawable;
        mBottomShowProgressThumbDrawable = thumb;
        if (mProgressBar != null) {
            mProgressBar.setProgressDrawable(drawable);
            mProgressBar.setThumb(thumb);
        }
    }

    /**
     * 底部进度条-非弹出
     */
    public void setBottomProgressBarDrawable(Drawable drawable) {
        mBottomProgressDrawable = drawable;
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 设置是否显示底部的进度条
     *
     * @param showBottomProgressBar
     */
    public void setIsShowBottomProgressBar(boolean showBottomProgressBar) {
        GSYVideoManager.instance().setShowBottomProgressBar(showBottomProgressBar);
    }

    /**
     * 设置显示按钮的类型
     *
     * @param type
     */
    public void setShowStartButtonType(int type) {
        GSYVideoManager.instance().setStartButtonType(type);
    }

    /**
     * 设置播放按钮的背景
     *
     * @param start 开始按钮
     * @param pause 暂停按钮
     */
    public void setPlayButtonBackgroung(int start, int pause, int error) {
        GSYVideoManager.instance().setStartButtonBackround(start, pause, error);
        mStartButtonNormal.setBackgroundResource(GSYVideoManager.instance().getStartButtonBackground());
    }

    /**
     * 声音进度条
     */
    public void setDialogVolumeProgressBar(Drawable drawable) {
        GSYVideoManager.instance().setVolumeProgressBarBackground(drawable);
    }

    /**
     * 中间进度条
     */
    public void setCenterDialogProgressBar(Drawable drawable) {
        GSYVideoManager.instance().setCenterProgressBarBackground(drawable);
    }

    /**
     * 中间进度条字体颜色
     */
    public void setCenterDialogProgressTextColor(int highLightColor, int normalColor) {
        GSYVideoManager.instance().setDialogProgressColor(highLightColor, normalColor);
    }

    /**
     * 是否点击封面可以播放
     */
    public void setThumbPlay(boolean thumbPlay) {
        this.mThumbPlay = thumbPlay;
    }

    /**
     * 封面布局
     */
    public RelativeLayout getThumbImageViewLayout() {
        return mThumbImageViewLayout;
    }

    /**
     * 是否需要锁屏
     *
     * @return
     */
    public boolean isNeedLockFull() {
        return mNeedLockFull;
    }

    /**
     * 是否需要全屏锁定屏幕功能
     * 如果单独使用请设置setIfCurrentIsFullscreen为true
     */
    public void setNeedLockFull(boolean needLoadFull) {
        this.mNeedLockFull = needLoadFull;
}

    /**
     * 锁屏点击
     */
    public void setLockClickListener(LockClickListener lockClickListener) {
        this.mLockClickListener = lockClickListener;
    }

    /**
     * 设置返回按钮的背景
     *
     * @param resId
     */
    public void setBackButtonBackGround(int resId) {
        GSYVideoManager.instance().setBackButtonBackground(resId);
    }

    /**
     * 设置锁的背景
     *
     * @param lockResId
     * @param unLockResId
     */
    public void setLockBackground(int lockResId, int unLockResId) {
        GSYVideoManager.instance().setLockBackground(lockResId, unLockResId);
    }

    /**
     * 设置全屏按钮的背景
     *
     * @param fullScreen
     * @param unFullScreen
     */
    public void setFullScreen(int fullScreen, int unFullScreen) {
        GSYVideoManager.instance().setFullScreen(fullScreen, unFullScreen);
    }

    /**
     * 设置亮度图标
     *
     * @param resId
     */
    public void setBrightnessIcon(int resId) {
        GSYVideoManager.instance().setBrightnessIcon(resId);
    }

    /**
     * 设置亮度值的颜色
     *
     * @param brightnessTextColor
     */
    public void setBrightnessTextColor(int brightnessTextColor) {
        GSYVideoManager.instance().setBrightnessTextColor(brightnessTextColor);
    }

    /**
     * 设置音量的图标
     *
     * @param resId
     */
    public void setVolumeIcon(int resId) {
        GSYVideoManager.instance().setVolumeIcon(resId);
    }

    /**
     * 设置中间快进，快退的图标
     *
     * @param forwardIcon
     * @param backwardIcon
     */
    public void setCenterProgressIcon(int forwardIcon, int backwardIcon) {
        GSYVideoManager.instance().setCenterDurationIcon(forwardIcon, backwardIcon);
    }

    /**
     * 设置标题的颜色
     *
     * @param resId
     */
    public void setTitleTextColor(int resId) {
        GSYVideoManager.instance().setTitleTextColor(resId);
    }

    /**
     * 声音dialog背景
     */
    public void setVolumeDialogBackground(Drawable drawable) {
        GSYVideoManager.instance().setVolumeDialogBackground(drawable);
    }

    /**
     * 中间dialog背景
     */
    public void setCenterDialogBackground(Drawable drawable) {
        GSYVideoManager.instance().setCenterDialogBackground(drawable);
    }

    /**
     * 获取seekBar
     *
     * @return
     */
    public SeekBar getSeekBar() {
        return mProgressBar;
    }

    /**
     * 最外层添加自己的view
     *
     * @param view
     */
    public void addView(View view) {
        mCustomFrameLayout.setVisibility(View.VISIBLE);
        mCustomFrameLayout.addView(view);
    }

    /**
     * 删除view
     */
    public void deleteView() {
        mCustomFrameLayout.removeAllViews();
    }

    public TextView getSettingButton() {
        return mSettingButton;
    }
}
