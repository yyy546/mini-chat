package com.minichat.space.entity;

import com.alibaba.fastjson2.JSONArray;
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
@TableName(autoResultMap = true)
public class SpacePost {
    private Long id;    // 主键ID
    private Long authorId;    // 帖子作者ID
    private String content;    // 帖子内容

    private JSONArray images;   // 帖子图片（JSON数组）
    
    private Integer imagesCount;    // 图片数量
    private Integer commentsCount;    // 评论数量
    private Integer likesCount;    // 点赞数量
    private Integer status;    // 帖子状态（0：正常，1：已删除）
    private LocalDateTime createdTime;    // 创建时间
    private LocalDateTime updatedTime;    // 更新时间
}
