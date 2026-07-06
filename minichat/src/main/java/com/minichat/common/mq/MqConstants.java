package com.minichat.common.mq;

public class MqConstants {
    /*
    * 私聊交换机,队列及路由键
    * */
    public static final String PRIVATE_EXCHANGE = "minichat_private_exchange";
    public static final String PRIVATE_ROUTING_KEY = "minichat_private_key";
    public static final String PRIVATE_QUEUE = "minichat_private_queue";
    public static final String PRIVATE_RECALL_EXCHANGE = "minichat_private_recall_exchange";
    public static final String PRIVATE_RECALL_ROUTING_KEY = "minichat_private_recall_key";
    public static final String PRIVATE_RECALL_QUEUE = "minichat_private_recall_queue";
    public static final String PRIVATE_DLX_EXCHANGE = "minichat_private_dlx_exchange";

    public static final String PRIVATE_DLX_ROUTING_KEY = "minichat_private_dlx_key";
    public static final String PRIVATE_DLX_QUEUE = "minichat_private_dlx_queue";

    /*
    * 群聊交换机,队列及路由键
    * */
    public static final String GROUP_EXCHANGE = "minichat_group_exchange";
    public static final String GROUP_ROUTING_KEY = "minichat_group_key";
    public static final String GROUP_QUEUE = "minichat_group_queue";
    public static final String GROUP_RECALL_EXCHANGE = "minichat_group_recall_exchange";
    public static final String GROUP_RECALL_ROUTING_KEY = "minichat_group_recall_key";
    public static final String GROUP_RECALL_QUEUE = "minichat_group_recall_queue";
    public static final String GROUP_DLX_EXCHANGE = "minichat_group_dlx_exchange";

    public static final String GROUP_DLX_ROUTING_KEY = "minichat_group_dlx_key";
    public static final String GROUP_DLX_QUEUE = "minichat_group_dlx_queue";

    /**
     * 空间帖子交换机,队列及路由键
     */
    public static final String SPACE_POST_EXCHANGE = "space.post.exchange";
    public static final String SPACE_POST_ROUTING_KEY = "space.post.key";
    public static final String SPACE_POST_QUEUE = "space.post.queue";
    public static final String SPACE_POST_DLX_EXCHANGE = "space.post.dlx.exchange";
    public static final String SPACE_POST_DLX_ROUTING_KEY = "space.post.dlx.key";
    public static final String SPACE_POST_DLX_QUEUE = "space.post.dlx.queue";
    public static final String SPACE_POST_DELETE_QUEUE = "space.post.delete.queue";
    public static final String SPACE_POST_DELETE_ROUTING_KEY = "space.post.delete.key";
    public static final String SPACE_POST_RECOVER_ROUTING_KEY = "space.post.recover.key";
    public static final String SPACE_POST_DELETEALL_QUEUE = "space.post.deleteAll.queue";
    public static final String SPACE_POST_DELETEALL_ROUTING_KEY = "space.post.deleteAll.key";

    /**
     * IM 广播交换机,队列及路由键
     */

    public static final String IM_BROADCAST_ROUTING_KEY = "";    //Fanout 交换机不需要路由键,仅占位置

    /**
     * IM 私聊广播交换机
     */
    public static final String IM_PRIVATE_BROADCAST_FANOUT_EXCHANGE = "im.private.broadcast.fanout.exchange";

    /**
     * IM 群聊广播交换机
     */
    public static final String IM_GROUP_BROADCAST_FANOUT_EXCHANGE = "im.group.broadcast.fanout.exchange";

    /**
     * IM 私聊撤回广播交换机
     */
    public static final String IM_PRIVATE_RECALL_BROADCAST_FANOUT_EXCHANGE = "im.private.recall.broadcast.fanout.exchange";

     /**
      * IM 群聊撤回广播交换机
      */
    public static final String IM_GROUP_RECALL_BROADCAST_FANOUT_EXCHANGE = "im.group.recall.broadcast.fanout.exchange";

    /**
     * 缓存同步交换机
     */
    public static final String CACHE_SYNC_BATCH_EXCHANGE = "cache.sync.fanout.exchange";
    public static final String CACHE_SYNC_SIMPLE_EXCHANGE = "cache.sync.simple.fanout.exchange";
     /**
      * 缓存同步路由键
      */
    public static final String CACHE_SYNC_ROUTING_KEY = "";

    private MqConstants(){}
}
