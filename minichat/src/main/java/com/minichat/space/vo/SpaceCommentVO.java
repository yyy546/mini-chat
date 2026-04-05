package com.minichat.space.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceCommentVO {
    private Long id;
    private Long postId;
    private Long publishId;
    private String publishName;
    private String publishAvatar;
    private String content;
}
