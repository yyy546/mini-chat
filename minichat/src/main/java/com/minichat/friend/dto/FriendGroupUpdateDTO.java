package com.minichat.friend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendGroupUpdateDTO {
    @NotNull(message = "好友ID不能为空")
    private Long friendId;
    @Length(max = 10, message = "分组名长度不能超过10个字符")
    private String groupName;
}
