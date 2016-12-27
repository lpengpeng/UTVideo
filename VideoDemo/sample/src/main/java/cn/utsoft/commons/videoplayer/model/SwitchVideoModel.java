package cn.utsoft.commons.videoplayer.model;

/**
 * Create by 李俊鹏 on 2016/12/20 20:36
 * Function：
 * Desc：
 */

public class SwitchVideoModel {
    private String url;
    private String name;

    public SwitchVideoModel(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}