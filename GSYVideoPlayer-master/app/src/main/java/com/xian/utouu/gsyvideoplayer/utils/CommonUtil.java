package com.xian.utouu.gsyvideoplayer.utils;

import android.view.View;
import android.view.ViewGroup;

/**
 * Create by 李俊鹏 on 2016/12/20 20:50
 * Function：
 * Desc：
 */

public class CommonUtil {

    public static void setViewHeight(View view, int width, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (null == layoutParams)
            return;
        layoutParams.width = width;
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
    }


}
