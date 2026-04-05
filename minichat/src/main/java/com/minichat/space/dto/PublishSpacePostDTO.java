package com.minichat.space.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishSpacePostDTO {
    @NotNull(message = "作者ID不能为空")
    private Long authorId;
    private String content;
    private List<String> images;
}
