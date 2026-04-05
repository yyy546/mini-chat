package com.minichat.space.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishSpaceCommentDTO {
    @NotNull(message = "帖子ID不能为空")
    private Long postId;
    @NotNull(message = "发布人ID不能为空")
    private Long publishId;
    @NotNull(message = "评论内容不能为空")
    private String content;
}
