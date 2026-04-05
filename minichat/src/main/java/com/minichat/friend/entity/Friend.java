package com.minichat.friend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friend {
    private Long id;        //好友关系ID
    private Long userId;    //用户ID
    private Long friendId;    //好友ID
    private String remarkName;      //备注名
    private String groupName;      //分组名  默认分组为"我的好友"
    private Integer isDeleted;      //是否删除 0:否 1:是
    private Integer isBlocked;     //是否拉黑 0:否 1:是
    private LocalDateTime createdTime;    //创建时间
    private LocalDateTime deletedTime;    //删除时间
}
