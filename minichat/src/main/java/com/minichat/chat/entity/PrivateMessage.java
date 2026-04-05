package com.minichat.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("private_message")
public class PrivateMessage {
    @TableId(type = IdType.AUTO)
    private Long id;            //消息ID
    private Long senderId;      //发送者ID
    private Long receiverId;    //接收者ID
    private Integer messageType;    //消息类型 1:文本 2:图片 3:文件 4：系统消息 5：撤回消息
    private String content;     //消息内容
    private String fileUrl;     //文件URL
    private String fileName;    //文件名
    private Long fileSize;      //文件大小
    private Integer isRead;     //是否已读 0:否 1:是
    private Integer isRecall;   //是否撤回 0:否 1:是
    private LocalDateTime sendTime;    //发送时间
    private LocalDateTime readTime;    //读取时间
    private LocalDateTime recallTime;    //撤回时间
}
