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
public class FriendRemarkUpdateDTO {
    @NotNull(message = "好友id不能为空")
    private Long friendId;
    @Length(max = 20, message = "好友备注长度不能超过20个字符")
    private String remarkName;
}
