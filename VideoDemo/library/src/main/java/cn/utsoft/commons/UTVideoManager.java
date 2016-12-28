package cn.utsoft.commons;


import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;
import android.view.View;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.Md5FileNameGenerator;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

import cn.utsoft.commons.UTVideo.listener.UTMediaPlayerListener;
import cn.utsoft.commons.UTVideo.model.GSYModel;
import cn.utsoft.commons.UTVideo.utils.CommonUtil;
import cn.utsoft.commons.UTVideo.utils.Debuger;
import cn.utsoft.commons.UTVideo.utils.FileUtils;
import cn.utsoft.commons.UTVideo.utils.StorageUtils;
import cn.utsoft.commons.UTVideo.utils.UTVideoType;
import tv.danmaku.ijk.media.exo.IjkExoMediaPlayer;
import tv.danmaku.ijk.media.player.AbstractMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 视频管理，单例
 * 目前使用的是IJK封装的谷歌EXOPlayer
 * Created by shuyu on 2016/11/11.
 */

public class UTVideoManager implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener, CacheListener {

    public static String TAG = "UTVideoManager";

    private static UTVideoManager videoManager;

    public static final int HANDLER_PREPARE = 0;
    public static final int HANDLER_SETDISPLAY = 1;
    public static final int HANDLER_RELEASE = 2;

    private AbstractMediaPlayer mediaPlayer;
    private HandlerThread mMediaHandlerThread;
    private MediaHandler mMediaHandler;
    private Handler mainThreadHandler;

    private WeakReference<UTMediaPlayerListener> listener;
    private WeakReference<UTMediaPlayerListener> lastListener;

    private HttpProxyCacheServer proxy; //视频代理

    private File cacheFile;

    private String playTag = ""; //播放的tag，防止错位置，因为普通的url也可能重复

    private Context context;

    private int currentVideoWidth = 0; //当前播放的视频宽的高

    private int currentVideoHeight = 0; //当前播放的视屏的高

    private int lastState;//当前视频的最后状态

    private int playPosition = -22; //播放的tag，防止错位置，因为普通的url也可能重复

    private int buffterPoint;

    private int videoType = UTVideoType.IJKPLAYER;

    private boolean showBottomProgressBar;
    private int startResId;
    private int pauseResId;
    private int errorResId;
    private int backButtoResId;
    private int lockResId;
    private int unLockResId;
    private int fullScreen;
    private int unFullScreen;
    private int centerIcon;
    private int highLightColor;
    private int forwardIcon;
    private int backwardIcon;
    private int backButtonColor;
    private int centerDialogBackground;
    private View.OnClickListener buttonListener;
    private int landLayout;
    private int portLayout;
    private int moreButtonBackground;

    public static synchronized UTVideoManager instance() {
        if (videoManager == null) {
            videoManager = new UTVideoManager();
        }
        return videoManager;
    }

    /**
     * 获取缓存代理服务
     */
    public static HttpProxyCacheServer getProxy(Context context) {
        HttpProxyCacheServer proxy = UTVideoManager.instance().proxy;
        return proxy == null ? (UTVideoManager.instance().proxy =
                UTVideoManager.instance().newProxy(context)) : proxy;
    }

    /**
     * 删除默认所有缓存文件
     */
    public static void clearAllDefaultCache(Context context) {
        String path = StorageUtils.getIndividualCacheDirectory
                (context.getApplicationContext()).getAbsolutePath();
        FileUtils.deleteFiles(new File(path));
    }

    /**
     * 删除url对应默认缓存文件
     */
    public static void clearDefaultCache(Context context, String url) {
        Md5FileNameGenerator md5FileNameGenerator = new Md5FileNameGenerator();
        String name = md5FileNameGenerator.generate(url);
        String pathTmp = StorageUtils.getIndividualCacheDirectory
                (context.getApplicationContext()).getAbsolutePath()
                + File.separator + name + ".download";
        String path = StorageUtils.getIndividualCacheDirectory
                (context.getApplicationContext()).getAbsolutePath()
                + File.separator + name;
        CommonUtil.deleteFile(pathTmp);
        CommonUtil.deleteFile(path);

    }

    /**
     * 获取缓存代理服务,带文件目录的
     */
    public static HttpProxyCacheServer getProxy(Context context, File file) {

        //如果为空，返回默认的
        if (file == null) {
            return getProxy(context);
        }

        //如果已经有缓存文件路径，那么判断缓存文件路径是否一致
        if (UTVideoManager.instance().cacheFile != null
                && !UTVideoManager.instance().cacheFile.getAbsolutePath().equals(file.getAbsolutePath())) {
            //不一致先关了旧的
            HttpProxyCacheServer proxy = UTVideoManager.instance().proxy;

            if (proxy != null) {
                proxy.shutdown();
            }
            //开启新的
            return (UTVideoManager.instance().proxy =
                    UTVideoManager.instance().newProxy(context, file));
        } else {
            //还没有缓存文件的或者一致的，返回原来
            HttpProxyCacheServer proxy = UTVideoManager.instance().proxy;

            return proxy == null ? (UTVideoManager.instance().proxy =
                    UTVideoManager.instance().newProxy(context, file)) : proxy;
        }
    }

    /**
     * 创建缓存代理服务,带文件目录的.
     */
    private HttpProxyCacheServer newProxy(Context context, File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
        HttpProxyCacheServer.Builder builder = new HttpProxyCacheServer.Builder(context);
        builder.cacheDirectory(file);
        cacheFile = file;
        return builder.build();
    }


    /**
     * 创建缓存代理服务
     */
    private HttpProxyCacheServer newProxy(Context context) {
        return new HttpProxyCacheServer(context.getApplicationContext());
    }


    public UTMediaPlayerListener listener() {
        if (listener == null)
            return null;
        return listener.get();
    }

    public UTMediaPlayerListener lastListener() {
        if (lastListener == null)
            return null;
        return lastListener.get();
    }

    public void setListener(UTMediaPlayerListener listener) {
        if (listener == null)
            this.listener = null;
        else
            this.listener = new WeakReference<>(listener);
    }

    public void setLastListener(UTMediaPlayerListener lastListener) {
        if (lastListener == null)
            this.lastListener = null;
        else
            this.lastListener = new WeakReference<>(lastListener);
    }

    public UTVideoManager() {
        mediaPlayer = new IjkMediaPlayer();
        mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler((mMediaHandlerThread.getLooper()));
        mainThreadHandler = new Handler();
    }

    public class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    initVideo(msg);
                    break;
                case HANDLER_SETDISPLAY:
                    showDisplay(msg);
                    break;
                case HANDLER_RELEASE:
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                    if (proxy != null) {
                        proxy.unregisterCacheListener(UTVideoManager.this);
                    }
                    buffterPoint = 0;
                    break;
            }
        }

    }

    private void initVideo(Message msg) {
        try {
            currentVideoWidth = 0;
            currentVideoHeight = 0;
            mediaPlayer.release();

            if (videoType == UTVideoType.IJKPLAYER) {
                initIJKPlayer(msg);
            } else if (videoType == UTVideoType.IJKEXOPLAYER) {
                initEXOPlayer(msg);
            }

            mediaPlayer.setOnCompletionListener(UTVideoManager.this);
            mediaPlayer.setOnBufferingUpdateListener(UTVideoManager.this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(UTVideoManager.this);
            mediaPlayer.setOnSeekCompleteListener(UTVideoManager.this);
            mediaPlayer.setOnErrorListener(UTVideoManager.this);
            mediaPlayer.setOnInfoListener(UTVideoManager.this);
            mediaPlayer.setOnVideoSizeChangedListener(UTVideoManager.this);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initIJKPlayer(Message msg) {
        mediaPlayer = new IjkMediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            if (UTVideoType.isMediaCodec()) {
                Debuger.printfLog("enable mediaCodec");
                ((IjkMediaPlayer) mediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                ((IjkMediaPlayer) mediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                ((IjkMediaPlayer) mediaPlayer).setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
            }
            ((IjkMediaPlayer) mediaPlayer).setDataSource(((GSYModel) msg.obj).getUrl(), ((GSYModel) msg.obj).getMapHeadData());
            mediaPlayer.setLooping(((GSYModel) msg.obj).isLooping());
            if (((GSYModel) msg.obj).getSpeed() != 1 && ((GSYModel) msg.obj).getSpeed() > 0) {
                ((IjkMediaPlayer) mediaPlayer).setSpeed(((GSYModel) msg.obj).getSpeed());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initEXOPlayer(Message msg) {
        mediaPlayer = new IjkExoMediaPlayer(context);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(context, Uri.parse(((GSYModel) msg.obj).getUrl()), ((GSYModel) msg.obj).getMapHeadData());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void showDisplay(Message msg) {
        if (msg.obj == null && mediaPlayer != null) {
            mediaPlayer.setSurface(null);
        } else {
            Surface holder = (Surface) msg.obj;
            if (mediaPlayer != null && holder.isValid()) {
                mediaPlayer.setSurface(holder);
            }
            if (mediaPlayer instanceof IjkExoMediaPlayer) {
                if (mediaPlayer != null && mediaPlayer.getDuration() > 30
                        && mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration()) {
                    mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 20);
                }
            }
        }
    }


    public void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed) {
        if (TextUtils.isEmpty(url)) return;
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        GSYModel fb = new GSYModel(url, mapHeadData, loop, speed);
        msg.obj = fb;
        mMediaHandler.sendMessage(msg);
    }

    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        mMediaHandler.sendMessage(msg);
        playTag = "";
        playPosition = -22;
    }

    public void setDisplay(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_SETDISPLAY;
        msg.obj = holder;
        mMediaHandler.sendMessage(msg);
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onPrepared();
                }
            }
        });
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, final int percent) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    if (percent > buffterPoint) {
                        listener().onBufferingUpdate(percent);
                    } else {
                        listener().onBufferingUpdate(buffterPoint);
                    }
                }
            }
        });
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onSeekComplete();
                }
            }
        });
    }

    @Override
    public boolean onError(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, final int what, final int extra) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        currentVideoWidth = mp.getVideoWidth();
        currentVideoHeight = mp.getVideoHeight();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener().onVideoSizeChanged();
                }
            }
        });
    }


    @Override
    public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
        buffterPoint = percentsAvailable;
    }

    /**
     * 暂停播放
     */
    public static void onPause() {
        if (UTVideoManager.instance().listener() != null) {
            UTVideoManager.instance().listener().onVideoPause();
        }
    }

    /**
     * 恢复播放
     */
    public static void onResume() {
        if (UTVideoManager.instance().listener() != null) {
            UTVideoManager.instance().listener().onVideoResume();
        }
    }


    public AbstractMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getCurrentVideoWidth() {
        return currentVideoWidth;
    }

    public int getCurrentVideoHeight() {
        return currentVideoHeight;
    }

    public int getLastState() {
        return lastState;
    }

    public void setLastState(int lastState) {
        this.lastState = lastState;
    }

    public void setCurrentVideoHeight(int currentVideoHeight) {
        this.currentVideoHeight = currentVideoHeight;
    }

    public void setCurrentVideoWidth(int currentVideoWidth) {
        this.currentVideoWidth = currentVideoWidth;
    }

    public String getPlayTag() {
        return playTag;
    }

    public void setPlayTag(String playTag) {
        this.playTag = playTag;
    }

    public int getPlayPosition() {
        return playPosition;
    }

    public void setPlayPosition(int playPosition) {
        this.playPosition = playPosition;
    }


    public int getVideoType() {
        return videoType;
    }

    /**
     * 设置了视频的播放类型
     * UTVideoType IJKPLAYER = 0 or IJKEXOPLAYER = 1;
     */
    public void setVideoType(Context context, int videoType) {
        this.context = context.getApplicationContext();
        this.videoType = videoType;
    }


    /**
     * 设置是否显示底部的进度条
     *
     * @param showBottomProgressBar
     */
    public void setShowBottomProgressBar(boolean showBottomProgressBar) {
        this.showBottomProgressBar = showBottomProgressBar;
    }

    /**
     * 获取是否显示底部进度条
     *
     * @return
     */
    public boolean getShowBottomProgressBar() {
        return showBottomProgressBar;
    }

    /**
     * 设置开始按钮的背景
     *
     * @param startResId
     * @param pauseResId
     */
    public void setStartButtonBackround(int startResId, int pauseResId, int errorResId) {
        this.startResId = startResId;
        this.pauseResId = pauseResId;
        this.errorResId = errorResId;
    }

    /**
     * 获取开始的情况下按钮的背景
     *
     * @return
     */
    public int getStartButtonBackground() {
        return startResId;
    }

    /**
     * 获取暂停的情况下按钮的背景
     *
     * @return
     */
    public int getPauseButtonBackground() {
        return pauseResId;
    }

    /**
     * 获取错误情况下的背景
     *
     * @return
     */
    public int getErrorButtonBackground() {
        return errorResId;
    }

    /**
     * 设置返回键的背景
     *
     * @param backButtoResId
     */
    public void setBackButtonBackground(int backButtoResId) {
        this.backButtoResId = backButtoResId;
    }

    /**
     * 获取返回键的背景
     *
     * @return
     */
    public int getBackButtonBackground() {
        return backButtoResId;
    }

    /**
     * 设置标题的颜色
     *
     * @param backButtonColor
     */
    public void setTitleTextColor(int backButtonColor) {
        this.backButtonColor = backButtonColor;
    }

    /**
     * 获取返标题颜色
     *
     * @return
     */
    public int getTitleTextColor() {
        return backButtonColor;
    }

    /**
     * 设置锁的点击背景
     *
     * @param lockResId
     * @param unLockResId
     */
    public void setLockBackground(int lockResId, int unLockResId) {
        this.lockResId = lockResId;
        this.unLockResId = unLockResId;
    }

    /**
     * 获取锁着状态的按钮背景
     *
     * @return
     */
    public int getLockBackground() {
        return lockResId;
    }

    /**
     * 获取不加锁的时候的按钮背景
     *
     * @return
     */
    public int getUnLockBackground() {
        return unLockResId;
    }

    /**
     * 设置更多按钮的背景
     * @param moreButtonBackground
     */
    public void setMoreButtonBackground(int moreButtonBackground){
        this.moreButtonBackground = moreButtonBackground;
    }

    /**
     * 获取更多按钮的背景
     * @return
     */
    public int getMoreButtonBackground(){
        return moreButtonBackground;
    }
    /**
     * 设置全屏的按钮背景
     *
     * @param fullScreen
     * @param unFullScreen
     */
    public void setFullScreen(int fullScreen, int unFullScreen) {
        this.fullScreen = fullScreen;
        this.unFullScreen = unFullScreen;
    }

    /**
     * 获取点击后全屏的按钮
     *
     * @return
     */
    public int getFullScreenBackground() {
        return fullScreen;
    }

    /**
     * 获取点击后小屏幕的按钮
     *
     * @return
     */
    public int getUnFullScreenBackground() {
        return unFullScreen;
    }

    /**
     * 设置快进快退按钮
     *
     * @param forwardIcon
     * @param backwardIcon
     */
    public void setCenterDurationIcon(int forwardIcon, int backwardIcon) {
        this.forwardIcon = forwardIcon;
        this.backwardIcon = backwardIcon;

    }

    /**
     * 获取快进按钮
     *
     * @return
     */
    public int getCenterForwardIcon() {
        return forwardIcon;
    }

    /**
     * 获取快退按钮
     *
     * @return
     */
    public int getCenterBackwardIcon() {
        return backwardIcon;
    }

    /**
     * 中间进度条字体颜色
     */
    public void setDialogProgressColor(int highLightColor) {
        this.highLightColor = highLightColor;
    }

    /**
     * 获取中间当前时间的文字颜色
     *
     * @return
     */
    public int getCenterHighLightColor() {
        return highLightColor;
    }

    /**
     * 设置更多按钮的监听
     * @param buttonListener
     */
    public void setSettingButtonListener(View.OnClickListener buttonListener) {
        this.buttonListener = buttonListener;
    }

    /**
     *获取更多监听按钮
     * @return
     */
    public View.OnClickListener getSettingListener() {
        return buttonListener;
    }

    /**
     * 设置两种布局
     * @param landLayout
     * @param portLayout
     */
    public void setTwoLayout(int landLayout, int portLayout) {
        this.landLayout = landLayout;
        this.portLayout = portLayout;
    }

    /**
     * 获取横屏布局
     * @return
     */
    public int getLandLayout() {
        return landLayout;
    }

    /**
     * 获取竖屏布局
     * @return
     */
    public int getPortLayout() {
        return portLayout;
    }

    /**
     * 设置dialog的背景
     * @param centerDialogBackground
     */
    public void setCenterDialogBackGround(int centerDialogBackground){
        this.centerDialogBackground = centerDialogBackground;
    }

    /**
     * 获取中间dialog的背景
     * @return
     */
    public int getCenterDialogBackground(){
        return centerDialogBackground;
    }

    /**
     * 释放掉所有的设置
     */
    public void releaseAllSettting() {
        showBottomProgressBar = false;
        startResId = 0;
        landLayout = 0;
        portLayout = 0;
        pauseResId = 0;
        errorResId = 0;
        backButtoResId = 0;
        lockResId = 0;
        unLockResId = 0;
        fullScreen = 0;
        unFullScreen = 0;
        highLightColor = 0;
        forwardIcon = 0;
        backwardIcon = 0;
        backButtonColor = 0;
        moreButtonBackground = 0;
        centerDialogBackground = 0;
        buttonListener = null;
    }
}