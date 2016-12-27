package cn.utsoft.commons.videoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.utouu.gsyvideoplayer.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.utsoft.commons.UTVideoManager;
import cn.utsoft.commons.videoplayer.utils.JumpUtils;
import cn.utsoft.commons.utils.Debuger;

/**
 * Create by 李俊鹏 on 2016/12/20 20:49
 * Function：
 * Desc：
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.open_btn)
    Button openBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Debuger.enable();
        ButterKnife.bind(this);
    }

    @OnClick({R.id.open_btn,  R.id.list_detail, R.id.clear_cache})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.open_btn:
                //直接一个页面播放的
                JumpUtils.goToVideoPlayer(this, openBtn);
                break;
            case R.id.list_detail:
                //支持全屏重力旋转的列表播放，滑动后不会被销毁
                JumpUtils.goToDetailPlayer(this);
                break;
            case R.id.clear_cache:
                UTVideoManager.clearAllDefaultCache(MainActivity.this);
                //String url = "http://baobab.wdjcdn.com/14564977406580.mp4";
                //UTVideoManager.clearDefaultCache(MainActivity.this, url);
                break;
        }
    }


}
