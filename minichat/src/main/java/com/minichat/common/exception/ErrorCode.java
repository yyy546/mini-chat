package com.minichat.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // ===== 通用 =====
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限执行此操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // ===== 用户模块 1xxx =====
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_ALREADY_EXISTS(1003, "用户名已存在"),
    USER_DISABLED(1004, "用户已被禁用"),
    USER_NOT_LOGIN(1005, "用户未登录"),

    // ===== 好友模块 2xxx =====
    FRIEND_NOT_FOUND(2001, "好友关系不存在"),
    FRIEND_ALREADY_EXISTS(2002, "已经是好友"),
    FRIEND_REQUEST_EXPIRED(2003, "好友申请已过期"),
    FRIEND_SELF_REQUEST(2004, "不能申请自己为好友"),

    // ===== 群组模块 3xxx =====
    GROUP_NOT_FOUND(3001, "群组不存在"),
    GROUP_FULL(3002, "群成员已满"),
    NOT_GROUP_MEMBER(3003, "不是群成员"),
    GROUP_PERMISSION_DENIED(3004, "群组权限不足"),
    GROUP_OWNER_CANNOT_EXIT(3005, "群主不能退出群聊"),
    GROUP_ALREADY_IN(3006, "已经在群中"),

    // ===== 聊天模块 4xxx =====
    MESSAGE_TOO_LONG(4001, "消息内容过长"),
    MESSAGE_RECALL_TIMEOUT(4002, "消息已超撤回时限"),
    NOT_FRIEND_CANNOT_CHAT(4003, "非好友无法发送消息"),
    MESSAGE_NOT_FOUND(4004, "消息不存在"),
    MESSAGE_FILE_INVALID(4005, "文件信息不合法"),

    // ===== 空间模块 5xxx =====
    POST_NOT_FOUND(5001, "动态不存在"),
    POST_ALREADY_DELETED(5002, "动态已被删除"),
    POST_PERMISSION_DENIED(5003, "无权限操作该动态"),
    COMMENT_NOT_FOUND(5004, "评论不存在"),

    // ===== 文件模块 6xxx =====
    UPLOAD_EXPIRED(6001, "上传任务已过期"),
    FILE_TOO_LARGE(6002, "文件大小超限"),
    UPLOAD_TASK_NOT_FOUND(6003, "上传任务不存在"),
    UPLOAD_CHUNK_INVALID(6004, "分片参数不合法"),
    UPLOAD_PERMISSION_DENIED(6005, "无权操作该上传任务"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() {
        if (this.code >= 1000) {
            return this.code / 1000 * 100;
        }
        return this.code;
    }
}
