package com.shuyu.gsyvideoplayer;


import android.content.Context;
import android.graphics.drawable.Drawable;
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
import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener;
import com.shuyu.gsyvideoplayer.model.GSYModel;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.FileUtils;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.StorageUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

import tv.danmaku.ijk.media.exo.IjkExoMediaPlayer;
import tv.danmaku.ijk.media.player.AbstractMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 视频管理，单例
 * 目前使用的是IJK封装的谷歌EXOPlayer
 * Created by shuyu on 2016/11/11.
 */

public class GSYVideoManager implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener, CacheListener {

    public static String TAG = "GSYVideoManager";

    private static GSYVideoManager videoManager;

    public static final int HANDLER_PREPARE = 0;
    public static final int HANDLER_SETDISPLAY = 1;
    public static final int HANDLER_RELEASE = 2;

    private AbstractMediaPlayer mediaPlayer;
    private HandlerThread mMediaHandlerThread;
    private MediaHandler mMediaHandler;
    private Handler mainThreadHandler;

    private WeakReference<GSYMediaPlayerListener> listener;
    private WeakReference<GSYMediaPlayerListener> lastListener;

    private HttpProxyCacheServer proxy; //视频代理

    private File cacheFile;

    private String playTag = ""; //播放的tag，防止错位置，因为普通的url也可能重复

    private Context context;

    private int currentVideoWidth = 0; //当前播放的视频宽的高

    private int currentVideoHeight = 0; //当前播放的视屏的高

    private int lastState;//当前视频的最后状态

    private int playPosition = -22; //播放的tag，防止错位置，因为普通的url也可能重复

    private int buffterPoint;

    private int videoType = GSYVideoType.IJKPLAYER;

    private int startButtonType;
    private boolean showBottomProgressBar;
    private int startResId;
    private int pauseResId;
    private int errorResId;
    private int backButtoResId;
    private int lockResId;
    private int unLockResId;
    private int fullScreen;
    private int unFullScreen;
    private int brightnessIcon;
    private int volumeIcon;
    private Drawable volumeDrawable;
    private Drawable centerDrawable;
    private int centerIcon;
    private int highLightColor;
    private int normalColor;
    private int forwardIcon;
    private int backwardIcon;
    private int brightnessTextColor;
    private int backButtonColor;
    private Drawable centerDialogBackground;
    private Drawable volumeDialogBackground;
    private View.OnClickListener buttonListener;

    public static synchronized GSYVideoManager instance() {
        if (videoManager == null) {
            videoManager = new GSYVideoManager();
        }
        return videoManager;
    }

    /**
     * 获取缓存代理服务
     */
    public static HttpProxyCacheServer getProxy(Context context) {
        HttpProxyCacheServer proxy = GSYVideoManager.instance().proxy;
        return proxy == null ? (GSYVideoManager.instance().proxy =
                GSYVideoManager.instance().newProxy(context)) : proxy;
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
        if (GSYVideoManager.instance().cacheFile != null
                && !GSYVideoManager.instance().cacheFile.getAbsolutePath().equals(file.getAbsolutePath())) {
            //不一致先关了旧的
            HttpProxyCacheServer proxy = GSYVideoManager.instance().proxy;

            if (proxy != null) {
                proxy.shutdown();
            }
            //开启新的
            return (GSYVideoManager.instance().proxy =
                    GSYVideoManager.instance().newProxy(context, file));
        } else {
            //还没有缓存文件的或者一致的，返回原来
            HttpProxyCacheServer proxy = GSYVideoManager.instance().proxy;

            return proxy == null ? (GSYVideoManager.instance().proxy =
                    GSYVideoManager.instance().newProxy(context, file)) : proxy;
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


    public GSYMediaPlayerListener listener() {
        if (listener == null)
            return null;
        return listener.get();
    }

    public GSYMediaPlayerListener lastListener() {
        if (lastListener == null)
            return null;
        return lastListener.get();
    }

    public void setListener(GSYMediaPlayerListener listener) {
        if (listener == null)
            this.listener = null;
        else
            this.listener = new WeakReference<>(listener);
    }

    public void setLastListener(GSYMediaPlayerListener lastListener) {
        if (lastListener == null)
            this.lastListener = null;
        else
            this.lastListener = new WeakReference<>(lastListener);
    }

    public GSYVideoManager() {
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
                        proxy.unregisterCacheListener(GSYVideoManager.this);
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

            if (videoType == GSYVideoType.IJKPLAYER) {
                initIJKPlayer(msg);
            } else if (videoType == GSYVideoType.IJKEXOPLAYER) {
                initEXOPlayer(msg);
            }

            mediaPlayer.setOnCompletionListener(GSYVideoManager.this);
            mediaPlayer.setOnBufferingUpdateListener(GSYVideoManager.this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(GSYVideoManager.this);
            mediaPlayer.setOnSeekCompleteListener(GSYVideoManager.this);
            mediaPlayer.setOnErrorListener(GSYVideoManager.this);
            mediaPlayer.setOnInfoListener(GSYVideoManager.this);
            mediaPlayer.setOnVideoSizeChangedListener(GSYVideoManager.this);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initIJKPlayer(Message msg) {
        mediaPlayer = new IjkMediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            if (GSYVideoType.isMediaCodec()) {
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
        if (GSYVideoManager.instance().listener() != null) {
            GSYVideoManager.instance().listener().onVideoPause();
        }
    }

    /**
     * 恢复播放
     */
    public static void onResume() {
        if (GSYVideoManager.instance().listener() != null) {
            GSYVideoManager.instance().listener().onVideoResume();
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
     * GSYVideoType IJKPLAYER = 0 or IJKEXOPLAYER = 1;
     */
    public void setVideoType(Context context, int videoType) {
        this.context = context.getApplicationContext();
        this.videoType = videoType;
    }

    /**
     * 设置开始按钮的显示类型
     *
     * @param startButtonType
     */
    public void setStartButtonType(int startButtonType) {
        this.startButtonType = startButtonType;
    }

    /**
     * 获取按钮的显示类型
     *
     * @return
     */
    public int getStartButtonType() {
        return startButtonType;
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
     * 设置全屏的按钮
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
     * 设置亮度图标
     *
     * @param brightnessIcon
     */
    public void setBrightnessIcon(int brightnessIcon) {
        this.brightnessIcon = brightnessIcon;
    }

    /**
     * 获取亮度图标
     *
     * @return
     */
    public int getBrightnessIcon() {
        return brightnessIcon;
    }

    /**
     * 设置亮度值颜色
     *
     * @param brightnessTextColor
     */
    public void setBrightnessTextColor(int brightnessTextColor) {
        this.brightnessTextColor = brightnessTextColor;
    }

    /**
     * @return 亮度值颜色
     */
    public int getBrightnessTextColor() {
        return brightnessTextColor;
    }

    /**
     * 设置音量的图标
     *
     * @param volumeIcon
     */
    public void setVolumeIcon(int volumeIcon) {
        this.volumeIcon = volumeIcon;
    }

    /**
     * 获取音量图标
     *
     * @return
     */
    public int getVolumeIcon() {
        return volumeIcon;
    }

    /**
     * 设置音量进度的背景
     *
     * @param volumeDrawable
     */
    public void setVolumeProgressBarBackground(Drawable volumeDrawable) {
        this.volumeDrawable = volumeDrawable;
    }

    /**
     * @return 获取音量进度的背景
     */
    public Drawable getVolumeProgressBarBackground() {
        return volumeDrawable;
    }

    /**
     * 设置中间进度条的背景
     *
     * @param centerDrawable
     */
    public void setCenterProgressBarBackground(Drawable centerDrawable) {
        this.centerDrawable = centerDrawable;
    }

    /**
     * @return 中间进度条的背景
     */
    public Drawable getCenterProgressBarBackground() {
        return centerDrawable;
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
    public void setDialogProgressColor(int highLightColor, int normalColor) {
        this.highLightColor = highLightColor;
        this.normalColor = normalColor;
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
     * 获取中间总的时间的文字颜色
     *
     * @return
     */
    public int getCenterNormalColor() {
        return normalColor;
    }

    /**
     * 设置中间dialog的背景
     *
     * @param centerDialogBackground
     */
    public void setCenterDialogBackground(Drawable centerDialogBackground) {
        this.centerDialogBackground = centerDialogBackground;
    }

    /**
     * 获取中间dialog的背景
     *
     * @return
     */
    public Drawable getCenterDialogBackground() {
        return centerDialogBackground;
    }

    /**
     * 设置音量dialog的背景
     *
     * @param volumeDialogBackground
     */
    public void setVolumeDialogBackground(Drawable volumeDialogBackground) {
        this.volumeDialogBackground = volumeDialogBackground;
    }

    /**
     * 获取音量dialog的背景
     *
     * @return
     */
    public Drawable getVolumeDialogBackground() {
        return volumeDialogBackground;
    }

    public void setSettingButtonListener(View.OnClickListener buttonListener) {
        this.buttonListener = buttonListener;
    }

    public View.OnClickListener getSettingListener() {
        return buttonListener;
    }

    public void releaseAllSettting() {
        startButtonType = 0;
        showBottomProgressBar = false;
        startResId = 0;
        pauseResId = 0;
        errorResId= 0;
        backButtoResId= 0;
        lockResId = 0 ;
        unLockResId = 0;
        fullScreen = 0;
       unFullScreen = 0;
        brightnessIcon = 0;
        volumeIcon = 0;
        volumeDrawable = null;
        centerDrawable = null;
        centerIcon = 0;
        highLightColor = 0;
        normalColor = 0;
        forwardIcon = 0;
        backwardIcon = 0;
        brightnessTextColor = 0;
        backButtonColor = 0;
        centerDialogBackground= null;
        volumeDialogBackground= null;
        buttonListener= null;
    }
}