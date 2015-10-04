package com.github.ompc.greys.core.manager;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.TimeFragment;
import com.github.ompc.greys.core.manager.impl.DefaultTimeFragmentManager;

import java.util.ArrayList;
import java.util.Date;

/**
 * 时间片段管理
 * Created by vlinux on 15/10/3.
 */
public interface TimeFragmentManager {


    /**
     * 生成全局过程ID
     *
     * @return 过程ID
     */
    int generateProcessId();

    /**
     * 追加时间片段
     *
     * @param processId 过程ID
     * @param advice    通知数据
     * @param gmtCreate 记录时间戳
     * @param cost      片段耗时
     * @param stack     片段堆栈
     * @return
     */
    TimeFragment append(int processId, Advice advice, Date gmtCreate, long cost, String stack);

    /**
     * 列出所有时间碎片
     *
     * @return 时间碎片列表
     */
    ArrayList<TimeFragment> list();

    /**
     * 搜索碎片内容
     *
     * @param express 搜索表达式
     * @return 搜索时间碎片集合
     */
    ArrayList<TimeFragment> search(String express);

    /**
     * 根据ID获取时间碎片
     *
     * @param id 时间碎片ID
     * @return 时间碎片
     */
    TimeFragment get(int id);

    /**
     * 根据ID删除时间碎片
     *
     * @param id 时间碎片ID
     * @return 被的时间碎片;若时间碎片不存在返回null
     */
    TimeFragment delete(int id);

    /**
     * 清除所有的时间碎片
     *
     * @return 清除的时间碎片数量
     */
    int clean();

    /**
     * 工厂
     */
    class Factory {

        private static volatile TimeFragmentManager instance = null;

        public static TimeFragmentManager getInstance() {
            if (null == instance) {
                synchronized (TimeFragmentManager.class) {
                    if (instance == null) {
                        instance = new DefaultTimeFragmentManager();
                    }
                }
            }

            return instance;
        }

    }

}
