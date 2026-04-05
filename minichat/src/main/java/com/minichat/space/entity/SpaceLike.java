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
public class SpaceLike {
    private Long id;    // 主键ID
    private Long postId;    // 点赞的帖子ID
    private Long userId;    // 点赞的用户ID
    private LocalDateTime createdTime;    // 创建时间
}
