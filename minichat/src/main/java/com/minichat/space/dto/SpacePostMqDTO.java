package com.minichat.space.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpacePostMqDTO {
    private Long spacePostId;    //空间帖子ID
    private Long authorId;    //作者ID
    private Long targetUserId;    //目标用户ID
    private Long timestamp;    //时间戳
}
