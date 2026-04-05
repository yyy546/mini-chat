package com.minichat.friend.controller;

import com.minichat.friend.dto.FriendRemarkUpdateDTO;
import com.minichat.friend.vo.FriendDetailVO;
import com.minichat.friend.vo.FriendGroupItemVO;
import com.minichat.friend.vo.FriendGroupVO;
import com.minichat.friend.vo.FriendVO;
import com.minichat.common.result.Result;
import com.minichat.friend.dto.FriendGroupUpdateDTO;
import com.minichat.friend.service.FriendService;
import com.minichat.common.util.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    //获取好友列表
    @GetMapping("/list")
    public Result<List<FriendVO>> getFriendList(){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        List<FriendVO> friendVOList = friendService.getFriendList(currentUserId);
        return Result.success(friendVOList);
    }

    //更新好友备注
    @PutMapping("/remark")
    public Result<String> updateFriendRemark(@Valid @RequestBody FriendRemarkUpdateDTO friendRemarkUpdateDTO){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        friendService.updateFriendRemark(currentUserId,friendRemarkUpdateDTO);
        return Result.success("好友备注成功");
    }

    //获取好友分组列表（含统计）
    @GetMapping("/group/list")
    public Result<List<FriendGroupVO>> getFriendGroupList(){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        List<FriendGroupVO> friendGroupVOList = friendService.getFriendGroupList(currentUserId);
        return Result.success(friendGroupVOList);
    }

    //获取好友分组下的好友列表
    @GetMapping("/group/{groupName}")
    public Result<List<FriendGroupItemVO>> getFriendGroupItemList(@PathVariable("groupName") String groupName){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        List<FriendGroupItemVO> friendGroupItemVOList = friendService.getFriendGroupItemList(currentUserId,groupName);
        return Result.success(friendGroupItemVOList);
    }

    //修改好友分组
    @PutMapping("/group")
    public Result<String> updateFriendGroup(@Valid @RequestBody FriendGroupUpdateDTO friendGroupUpdateDTO){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        friendService.updateFriendGroup(currentUserId,friendGroupUpdateDTO);
        return Result.success("好友分组修改成功");
    }

    //获取好友详情信息
    @GetMapping("/detail/{friendId}")
    public Result<FriendDetailVO> getFriendDetail(@PathVariable("friendId") Long friendId){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            return Result.error("用户未登录");
        }
        FriendDetailVO friendDetailVO = friendService.getFriendDetail(currentUserId,friendId);
        return Result.success(friendDetailVO);
    }

    // 删除好友
    @DeleteMapping("/delete/{friendId}")
    public Result<String> deleteFriend(@PathVariable("friendId") Long friendId) {
        Long currentUserId = UserContext.getCurUserId();
        if (currentUserId == null) {
            return Result.error("用户未登录");
        }
        try {
            friendService.deleteFriend(currentUserId, friendId);
            return Result.success("好友删除成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

}
