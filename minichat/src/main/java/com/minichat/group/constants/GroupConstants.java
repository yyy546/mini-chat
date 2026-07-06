package com.minichat.group.constants;

public class GroupConstants {
    /*
    * 加入群组策略
    * 0：自由加入
    * 1：需要审批
    * 2：需要邀请
    */
    public static final Integer JOIN_POLICY_FREEDOM = 0;
    public static final Integer JOIN_POLICY_APPROVAL = 1;
    public static final Integer JOIN_POLICY_INVITE = 2;

    /*
    * 邀请加入群组策略
    * 0：所有成员都可以邀请
    * 1：只有管理员和群主可以邀请
    * 2：只有群主可以邀请
    */
    public static final Integer INVITE_POLICY_ALL = 0;
    public static final Integer INVITE_POLICY_ADMIN = 1;
    public static final Integer INVITE_POLICY_GROUPOWNER = 2;

    /*
     * 群组成员角色
     * 0：普通成员
     * 1：管理员
     * 2：群主
     */
    public static final Integer ROLE_MEMBER = 0;
    public static final Integer ROLE_ADMIN = 1;
    public static final Integer ROLE_GROUPOWNER = 2;

    /*
     * 删除状态
     * 0：未删除
     * 1：已删除
     */
    public static final Integer NOT_DELETED = 0;
    public static final Integer DELETED = 1;

    /*
     * 禁言状态
     * 0：未禁言
     * 1：已禁言
     */
    public static final Integer NOT_MUTED = 0;
    public static final Integer MUTED = 1;

     /*
     * 默认群组头像
     */
    public static final String DEFAULT_GROUP_AVATAR = "https://czy-mini-chat.oss-cn-hangzhou.aliyuncs.com/group-avatar/default_group_avatar.png";

    /*
     * 默认最大成员数
     */
    public static final Integer DEFAULT_MAX_MEMBERS = 200;

     private GroupConstants(){}
}