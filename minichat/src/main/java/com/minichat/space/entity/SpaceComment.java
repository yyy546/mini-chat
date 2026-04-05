package com.minichat.space.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceComment {
    private Long id;    // 主键ID
    private Long postId;    // 评论的帖子ID
    private Long publishId;    // 评论的发布者ID
    private String content;    // 评论内容
    private Integer status;    // 评论状态（0：正常，1：已删除）
    private LocalDateTime createdTime;    // 创建时间
}
