package com.minichat.space.vo;

import com.alibaba.fastjson2.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpacePostVO {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private JSONArray images;
    private Integer imagesCount;
    private Integer commentsCount;
    private Integer likesCount;
    private Boolean liked;
    private LocalDateTime createdTime;
}
