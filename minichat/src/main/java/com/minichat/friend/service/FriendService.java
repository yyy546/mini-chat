package com.minichat.friend.service;

import com.minichat.friend.dto.FriendGroupUpdateDTO;
import com.minichat.friend.dto.FriendRemarkUpdateDTO;
import com.minichat.friend.vo.FriendDetailVO;
import com.minichat.friend.vo.FriendGroupItemVO;
import com.minichat.friend.vo.FriendGroupVO;
import com.minichat.friend.vo.FriendVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FriendService {
    /**
     * 获取好友列表
     */
    List<FriendVO> getFriendList(Long currentUserId);

    /**
     * 更新好友备注
     */
    void updateFriendRemark(Long currentUserId, FriendRemarkUpdateDTO friendRemarkUpdateDTO);

    /**
     * 获取好友分组列表
     */
    List<FriendGroupVO> getFriendGroupList(Long currentUserId);

    /**
     * 获取好友分组下的好友列表
     */
    List<FriendGroupItemVO> getFriendGroupItemList(@Param("currentUserId") Long currentUserId, @Param("groupName") String groupName);

    /**
     * 获取好友详情
     */
    FriendDetailVO getFriendDetail(Long currentUserId, Long friendId);

    /**
     * 删除好友
     */
    void deleteFriend(Long currentUserId, Long friendId);

    /**
     * 修改好友分组
     */
    void updateFriendGroup(Long currentUserId, FriendGroupUpdateDTO friendGroupUpdateDTO);

}
