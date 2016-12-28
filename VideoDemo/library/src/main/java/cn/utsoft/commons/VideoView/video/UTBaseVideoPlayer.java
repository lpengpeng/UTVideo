package cn.utsoft.commons.VideoView.video;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.shuyu.gsyvideoplayer.R;
import com.transitionseverywhere.TransitionManager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import cn.utsoft.commons.UTTextureView;
import cn.utsoft.commons.UTVideoManager;
import cn.utsoft.commons.UTVideoPlayer;
import cn.utsoft.commons.UTVideoTouch;
import cn.utsoft.commons.VideoView.listener.UTMediaPlayerListener;
import cn.utsoft.commons.VideoView.listener.VideoAllCallBack;
import cn.utsoft.commons.VideoView.utils.CommonUtil;
import cn.utsoft.commons.VideoView.utils.Debuger;
import cn.utsoft.commons.VideoView.utils.OrientationUtils;

/**
 * Created by lijunpeng on 2016/12/21.
 */

public abstract class UTBaseVideoPlayer extends FrameLayout implements UTMediaPlayerListener {

    public static final int SMALL_ID = 84778;

    protected static final int FULLSCREEN_ID = 85597;

    protected static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    protected boolean mActionBar = false;//是否需要在利用window实现全屏幕的时候隐藏actionbar

    protected boolean mStatusBar = false;//是否需要在利用window实现全屏幕的时候隐藏statusbar

    protected boolean mHideKey = true;//是否隐藏虚拟按键

    protected boolean mCache = false;//是否播边边缓冲

    private boolean mShowFullAnimation = true;//是否使用全屏动画效果

    protected int[] mListItemRect;//当前item框的屏幕位置

    protected int[] mListItemSize;//当前item的大小

    protected int mCurrentState = -1; //当前的播放状态

    protected int mRotate = 0; //针对某些视频的旋转信息做了旋转处理

    private int mSystemUiVisibility;

    protected float mSpeed = 1;//播放速度，只支持6.0以上

    protected boolean mRotateViewAuto = true; //是否自动旋转

    protected boolean mIfCurrentIsFullscreen = false;//当前是否全屏

    protected boolean mLockLand = false;//当前全屏是否锁定全屏

    protected boolean mLooping = false;//循环

    protected boolean mHadPlay = false;//是否播放过

    protected Context mContext;

    protected String mOriginUrl; //原来的url

    protected String mUrl; //转化后的URL

    protected Object[] mObjects;

    protected File mCachePath;

    protected ViewGroup mTextureViewContainer; //渲染控件父类

    protected View mSmallClose; //小窗口关闭按键

    protected VideoAllCallBack mVideoAllCallBack;

    protected Map<String, String> mMapHeadData = new HashMap<>();

    protected UTTextureView mTextureView;

    protected ImageView mCoverImageView; //内部使用，请勿操作哟~

    protected SeekBar mProgressBar;

    protected ImageView mFullscreenButton;

    protected TextView mCurrentTimeTextView, mTotalTimeTextView;

    protected ViewGroup mTopContainer, mBottomContainer;

    protected ImageView mBackButton;

    protected Bitmap mFullPauseBitmap = null;//暂停时的全屏图片；

    private OrientationUtils mOrientationUtils; //旋转工具类

    private Handler mHandler = new Handler();
    private OnClickListener buttonListener;


    public UTBaseVideoPlayer(Context context) {
        super(context);
    }

    public UTBaseVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UTBaseVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    /**
     * 1.5.0开始加入，如果需要不同布局区分功能，需要重载
     */
    public UTBaseVideoPlayer(Context context, Boolean fullFlag) {
        super(context);
        mIfCurrentIsFullscreen = fullFlag;
    }

    private ViewGroup getViewGroup() {
        return (ViewGroup) (CommonUtil.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
    }

    /**
     * 移除没用的
     */
    private void removeVideo(ViewGroup vp, int id) {
        View old = vp.findViewById(id);
        if (old != null) {
            if (old.getParent() != null) {
                ViewGroup viewGroup = (ViewGroup) old.getParent();
                vp.removeView(viewGroup);
            }
        }
    }

    /**
     * 保存大小和状态
     */
    private void saveLocationStatus(Context context, boolean statusBar, boolean actionBar) {
        getLocationOnScreen(mListItemRect);
        int statusBarH = CommonUtil.getStatusBarHeight(context);
        int actionBerH = CommonUtil.getActionBarHeight((Activity) context);
        if (statusBar) {
            mListItemRect[1] = mListItemRect[1] - statusBarH;
        }
        if (actionBar) {
            mListItemRect[1] = mListItemRect[1] - actionBerH;
        }
        mListItemSize[0] = getWidth();
        mListItemSize[1] = getHeight();
    }

    /**
     * 全屏
     */
    private void resolveFullVideoShow(Context context, final UTBaseVideoPlayer gsyVideoPlayer) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) gsyVideoPlayer.getLayoutParams();
        lp.setMargins(0, 0, 0, 0);
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.CENTER;
        gsyVideoPlayer.setLayoutParams(lp);
        gsyVideoPlayer.setIfCurrentIsFullscreen(true);
        mOrientationUtils = new OrientationUtils((Activity) context, gsyVideoPlayer);

        mOrientationUtils.setEnable(mRotateViewAuto);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mLockLand) {
                    mOrientationUtils.resolveByClick();
                }
                gsyVideoPlayer.setVisibility(VISIBLE);
            }
        }, ismShowFullAnimation() ? 300 : 0);


        if (mVideoAllCallBack != null) {
            Debuger.printfError("onEnterFullscreen");
            mVideoAllCallBack.onEnterFullscreen(mUrl, mObjects);
        }
        mIfCurrentIsFullscreen = true;
    }

    /**
     * 恢复
     */
    private void resolveNormalVideoShow(View oldF, ViewGroup vp, UTVideoPlayer gsyVideoPlayer) {

        if (oldF != null && oldF.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) oldF.getParent();
            vp.removeView(viewGroup);
        }
        mCurrentState = UTVideoManager.instance().getLastState();
        if (gsyVideoPlayer != null) {
            mCurrentState = gsyVideoPlayer.getCurrentState();
        }
        UTVideoManager.instance().setListener(UTVideoManager.instance().lastListener());
        UTVideoManager.instance().setLastListener(null);
        setStateAndUi(mCurrentState);
        addTextureView();
        CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        if (mVideoAllCallBack != null) {
            Debuger.printfError("onQuitFullscreen");
            mVideoAllCallBack.onQuitFullscreen(mUrl, mObjects);
        }
        mIfCurrentIsFullscreen = false;
        if (mHideKey) {
            CommonUtil.showNavKey(mContext, mSystemUiVisibility);
        }
        CommonUtil.showSupportActionBar(mContext, mActionBar, mStatusBar);
    }

    /**
     * 利用window层播放全屏效果
     *
     * @param context
     * @param actionBar 是否有actionBar，有的话需要隐藏
     * @param statusBar 是否有状态bar，有的话需要隐藏
     */
    public UTBaseVideoPlayer startWindowFullscreen(final Context context, final boolean actionBar, final boolean statusBar) {

        mSystemUiVisibility = ((Activity) context).getWindow().getDecorView().getSystemUiVisibility();

        CommonUtil.hideSupportActionBar(context, actionBar, statusBar);

        if (mHideKey) {
            CommonUtil.hideNavKey(context);
        }

        this.mActionBar = actionBar;

        this.mStatusBar = statusBar;
        this.

        mListItemRect = new int[2];

        mListItemSize = new int[2];

        final ViewGroup vp = getViewGroup();

        removeVideo(vp, FULLSCREEN_ID);


        //处理暂停的逻辑
        pauseFullCoverLogic();

        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }


        saveLocationStatus(context, statusBar, actionBar);
        boolean hadNewConstructor = true;

        try {
            UTBaseVideoPlayer.this.getClass().getConstructor(Context.class, Boolean.class);
        } catch (Exception e) {
            hadNewConstructor = false;
        }
        try {
            //通过被重载的不同构造器来选择
            Constructor<UTBaseVideoPlayer> constructor;
            final UTBaseVideoPlayer gsyVideoPlayer;
            if (!hadNewConstructor) {
                constructor = (Constructor<UTBaseVideoPlayer>) UTBaseVideoPlayer.this.getClass().getConstructor(Context.class);
                gsyVideoPlayer = constructor.newInstance(getContext());
            } else {
                constructor = (Constructor<UTBaseVideoPlayer>) UTBaseVideoPlayer.this.getClass().getConstructor(Context.class, Boolean.class);
                gsyVideoPlayer = constructor.newInstance(getContext(), true);
            }
            gsyVideoPlayer.setId(FULLSCREEN_ID);
            gsyVideoPlayer.setIfCurrentIsFullscreen(true);
            gsyVideoPlayer.setVideoAllCallBack(mVideoAllCallBack);
            gsyVideoPlayer.setLooping(isLooping());
            gsyVideoPlayer.setSpeed(getSpeed());
            final FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            final FrameLayout frameLayout = new FrameLayout(context);
            frameLayout.setBackgroundColor(Color.BLACK);

            if (mShowFullAnimation) {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(getWidth(), getHeight());
                lp.setMargins(mListItemRect[0], mListItemRect[1], 0, 0);
                frameLayout.addView(gsyVideoPlayer, lp);
                vp.addView(frameLayout, lpParent);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        TransitionManager.beginDelayedTransition(vp);
                        resolveFullVideoShow(context, gsyVideoPlayer);
                    }
                }, 300);
            } else {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(getWidth(), getHeight());
                frameLayout.addView(gsyVideoPlayer, lp);
                vp.addView(frameLayout, lpParent);
                gsyVideoPlayer.setVisibility(INVISIBLE);
                resolveFullVideoShow(context, gsyVideoPlayer);
            }
            gsyVideoPlayer.mHadPlay = mHadPlay;
            gsyVideoPlayer.mFullPauseBitmap = mFullPauseBitmap;
            gsyVideoPlayer.setUp(mUrl, mCache, mCachePath, mMapHeadData, mObjects);
            gsyVideoPlayer.setStateAndUi(mCurrentState);
            gsyVideoPlayer.addTextureView();

            gsyVideoPlayer.getFullscreenButton().setImageResource(R.drawable.video_shrink);
            gsyVideoPlayer.getFullscreenButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearFullscreenLayout();
                }
            });

            gsyVideoPlayer.getBackButton().setVisibility(VISIBLE);
            gsyVideoPlayer.getBackButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearFullscreenLayout();
                }
            });

            UTVideoManager.instance().setLastListener(this);
            UTVideoManager.instance().setListener(gsyVideoPlayer);
            return gsyVideoPlayer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 退出window层播放全屏效果
     */
    public void clearFullscreenLayout() {
        mIfCurrentIsFullscreen = false;
        int delay = mOrientationUtils.backToProtVideo();
        mOrientationUtils.setEnable(false);
        if (mOrientationUtils != null)
            mOrientationUtils.releaseListener();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                backToNormal();
            }
        }, delay);
    }

    /**
     * 回到正常效果
     */
    private void backToNormal() {

        final ViewGroup vp = getViewGroup();

        final View oldF = vp.findViewById(FULLSCREEN_ID);
        final UTVideoPlayer gsyVideoPlayer;
        if (oldF != null) {
            gsyVideoPlayer = (UTVideoPlayer) oldF;
            //如果暂停了
            pauseFullBackCoverLogic(gsyVideoPlayer);
            if (mShowFullAnimation) {
                TransitionManager.beginDelayedTransition(vp);

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) gsyVideoPlayer.getLayoutParams();
                lp.setMargins(mListItemRect[0], mListItemRect[1], 0, 0);
                lp.width = mListItemSize[0];
                lp.height = mListItemSize[1];
                //注意配置回来，不然动画效果会不对
                lp.gravity = Gravity.NO_GRAVITY;
                gsyVideoPlayer.setLayoutParams(lp);

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resolveNormalVideoShow(oldF, vp, gsyVideoPlayer);
                    }
                }, 400);
            } else {
                resolveNormalVideoShow(oldF, vp, gsyVideoPlayer);
            }

        } else {
            resolveNormalVideoShow(null, vp, null);
        }
    }

    /**
     * 全屏的暂停的时候返回页面不黑色
     */
    private void pauseFullCoverLogic() {
        if (mCurrentState == UTVideoPlayer.CURRENT_STATE_PAUSE && mTextureView != null
                && (mFullPauseBitmap == null || mFullPauseBitmap.isRecycled())) {
            try {
                mFullPauseBitmap = mTextureView.getBitmap(mTextureView.getSizeW(), mTextureView.getSizeH());
            } catch (Exception e) {
                e.printStackTrace();
                mFullPauseBitmap = null;
            }
        }
    }

    /**
     * 全屏的暂停返回的时候返回页面不黑色
     */
    private void pauseFullBackCoverLogic(UTBaseVideoPlayer gsyVideoPlayer) {
        //如果是暂停状态
        if (gsyVideoPlayer.mCurrentState == UTVideoPlayer.CURRENT_STATE_PAUSE
                && gsyVideoPlayer.mTextureView != null) {
            //全屏的位图还在，说明没播放，直接用原来的
            if (gsyVideoPlayer.mFullPauseBitmap != null
                    && !gsyVideoPlayer.mFullPauseBitmap.isRecycled()) {
                mFullPauseBitmap = gsyVideoPlayer.mFullPauseBitmap;
            } else {
                //不在了说明已经播放过，还是暂停的话，我们拿回来就好
                try {
                    mFullPauseBitmap = mTextureView.getBitmap(mTextureView.getSizeW(), mTextureView.getSizeH());
                } catch (Exception e) {
                    e.printStackTrace();
                    mFullPauseBitmap = null;
                }
            }
        }
    }


    /**
     * 显示小窗口
     */
    public UTBaseVideoPlayer showSmallVideo(Point size, final boolean actionBar, final boolean statusBar) {

        final ViewGroup vp = getViewGroup();

        removeVideo(vp, SMALL_ID);

        if (mTextureViewContainer.getChildCount() > 0) {
            mTextureViewContainer.removeAllViews();
        }

        try {
            Constructor<UTBaseVideoPlayer> constructor = (Constructor<UTBaseVideoPlayer>) UTBaseVideoPlayer.this.getClass().getConstructor(Context.class);
            UTBaseVideoPlayer gsyVideoPlayer = constructor.newInstance(getContext());
            gsyVideoPlayer.setId(SMALL_ID);

            FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            FrameLayout frameLayout = new FrameLayout(mContext);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size.x, size.y);
            int marginLeft = CommonUtil.getScreenWidth(mContext) - size.x;
            int marginTop = CommonUtil.getScreenHeight(mContext) - size.y;

            if (actionBar) {
                marginTop = marginTop - CommonUtil.getActionBarHeight((Activity) mContext);
            }

            if (statusBar) {
                marginTop = marginTop - CommonUtil.getStatusBarHeight(mContext);
            }

            lp.setMargins(marginLeft, marginTop, 0, 0);
            frameLayout.addView(gsyVideoPlayer, lp);

            vp.addView(frameLayout, lpParent);
            gsyVideoPlayer.mHadPlay = mHadPlay;
            gsyVideoPlayer.setUp(mUrl, mCache, mCachePath, mMapHeadData, mObjects);
            gsyVideoPlayer.setStateAndUi(mCurrentState);
            gsyVideoPlayer.addTextureView();
            //隐藏掉所有的弹出状态哟
            gsyVideoPlayer.onClickUiToggle();
            gsyVideoPlayer.setVideoAllCallBack(mVideoAllCallBack);
            gsyVideoPlayer.setLooping(isLooping());
            gsyVideoPlayer.setSpeed(getSpeed());
            gsyVideoPlayer.setSmallVideoTextureView(new UTVideoTouch(gsyVideoPlayer, marginLeft, marginTop));

            UTVideoManager.instance().setLastListener(this);
            UTVideoManager.instance().setListener(gsyVideoPlayer);

            if (mVideoAllCallBack != null) {
                Debuger.printfError("onEnterSmallWidget");
                mVideoAllCallBack.onEnterSmallWidget(mUrl, mObjects);
            }

            return gsyVideoPlayer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 隐藏小窗口
     */
    public void hideSmallVideo() {
        final ViewGroup vp = getViewGroup();
        UTVideoPlayer gsyVideoPlayer = (UTVideoPlayer) vp.findViewById(SMALL_ID);
        removeVideo(vp, SMALL_ID);
        mCurrentState = UTVideoManager.instance().getLastState();
        if (gsyVideoPlayer != null) {
            mCurrentState = gsyVideoPlayer.getCurrentState();
        }
        UTVideoManager.instance().setListener(UTVideoManager.instance().lastListener());
        UTVideoManager.instance().setLastListener(null);
        setStateAndUi(mCurrentState);
        addTextureView();
        CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        if (mVideoAllCallBack != null) {
            Debuger.printfLog("onQuitSmallWidget");
            mVideoAllCallBack.onQuitSmallWidget(mUrl, mObjects);
        }
    }


    /**
     * 设置播放URL
     *
     * @param url
     * @param cacheWithPlay 是否边播边缓存
     * @param objects
     * @return
     */
    public abstract boolean setUp(String url, boolean cacheWithPlay, File cachePath, Object... objects);

    /**
     * 设置播放URL
     *
     * @param url
     * @param cacheWithPlay 是否边播边缓存
     * @param mapHeadData
     * @param objects
     * @return
     */

    public abstract boolean setUp(String url, boolean cacheWithPlay, File cachePath, Map<String, String> mapHeadData, Object... objects);

    /**
     * 设置播放显示状态
     *
     * @param state
     */
    protected abstract void setStateAndUi(int state);

    /**
     * 添加播放的view
     */
    protected abstract void addTextureView();

    /**
     * 小窗口
     **/
    protected abstract void setSmallVideoTextureView(View.OnTouchListener onTouchListener);


    protected abstract void onClickUiToggle();

    /**
     * 获取全屏按键
     */
    public abstract ImageView getFullscreenButton();

    /**
     * 获取返回按键
     */
    public abstract ImageView getBackButton();


    public boolean isIfCurrentIsFullscreen() {
        return mIfCurrentIsFullscreen;
    }

    public void setIfCurrentIsFullscreen(boolean ifCurrentIsFullscreen) {
        this.mIfCurrentIsFullscreen = ifCurrentIsFullscreen;
    }


    public boolean ismShowFullAnimation() {
        return mShowFullAnimation;
    }

    /**
     * 全屏动画
     *
     * @param showFullAnimation 是否使用全屏动画效果
     */
    public void setShowFullAnimation(boolean showFullAnimation) {
        this.mShowFullAnimation = showFullAnimation;
    }


    public boolean isLooping() {
        return mLooping;
    }

    /**
     * 设置循环
     */
    public void setLooping(boolean looping) {
        this.mLooping = looping;
    }


    /**
     * 设置播放过程中的回调
     *
     * @param mVideoAllCallBack
     */
    public void setVideoAllCallBack(VideoAllCallBack mVideoAllCallBack) {
        this.mVideoAllCallBack = mVideoAllCallBack;
    }


    public boolean isRotateViewAuto() {
        return mRotateViewAuto;
    }

    /**
     * 是否开启自动旋转
     */
    public void setRotateViewAuto(boolean rotateViewAuto) {
        this.mRotateViewAuto = rotateViewAuto;
    }

    public boolean isLockLand() {
        return mLockLand;
    }

    /**
     * 一全屏就锁屏横屏，默认false竖屏，可配合setRotateViewAuto使用
     */
    public void setLockLand(boolean lockLand) {
        this.mLockLand = lockLand;
    }


    public float getSpeed() {
        return mSpeed;
    }

    /**
     * 播放速度，只支持6.0以上
     */
    public void setSpeed(float speed) {
        this.mSpeed = speed;
    }

    public boolean isHideKey() {
        return mHideKey;
    }

    /**
     * 全屏隐藏虚拟按键，默认打开
     */
    public void setHideKey(boolean hideKey) {
        this.mHideKey = hideKey;
    }

}
