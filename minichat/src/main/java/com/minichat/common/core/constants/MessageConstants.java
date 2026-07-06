package com.minichat.common.core.constants;

public class MessageConstants {
    /**
     * 消息类型 1:文本 2:图片 3:文件 4：系统消息
     */
    public static final Integer TEXT = 1;
    public static final Integer IMAGE = 2;
    public static final Integer FILE = 3;
    public static final Integer SYSTEM = 4;
    public static final Integer RECALL_MESSAGE = 5;



    /**
     * 是否已读 0:否 1:是
     */
    public static final Integer NOT_READ = 0;
    public static final Integer READ = 1;

     /**
     * 是否已撤回 0:否 1:是
     */
    public static final Integer NOT_RECALL = 0;
    public static final Integer RECALL = 1;



    public static final int MESSAGE_RECALL_TIME_LIMIT = 10; // 撤回时间限制（单位：分钟）

    /**
     * 聊天记录索引
     */
    public static final String ES_CHAT_MESSAGE_INDEX = "chat_message";
    public static final String ES_CHAT_MESSAGE_TYPE_PRIVATE = "private_";
    public static final String ES_CHAT_MESSAGE_TYPE_GROUP = "group_";
    /**
     * 默认撤回消息内容
     */

    public static final String DEFAULT_RECALL_MESSAGE = "撤回了一条消息";

    private MessageConstants(){}
}
