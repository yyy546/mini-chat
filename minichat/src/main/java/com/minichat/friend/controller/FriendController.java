package com.minichat.friend.controller;

import com.minichat.common.exception.AuthException;
import com.minichat.common.result.Result;
import com.minichat.friend.dto.FriendGroupUpdateDTO;
import com.minichat.friend.dto.FriendRemarkUpdateDTO;
import com.minichat.friend.service.FriendService;
import com.minichat.friend.vo.FriendDetailVO;
import com.minichat.friend.vo.FriendGroupItemVO;
import com.minichat.friend.vo.FriendGroupVO;
import com.minichat.friend.vo.FriendVO;
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

    @GetMapping("/list")
    public Result<List<FriendVO>> getFriendList(){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            throw new AuthException("用户未登录");
        }
        List<FriendVO> friendVOList = friendService.getFriendList(currentUserId);
        return Result.success(friendVOList);
    }

    @PutMapping("/remark")
    public Result<String> updateFriendRemark(@Valid @RequestBody FriendRemarkUpdateDTO friendRemarkUpdateDTO){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            throw new AuthException("用户未登录");
        }
        friendService.updateFriendRemark(currentUserId,friendRemarkUpdateDTO);
        return Result.success("好友备注成功");
    }

    @GetMapping("/group/list")
    public Result<List<FriendGroupVO>> getFriendGroupList(){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            throw new AuthException("用户未登录");
        }
        List<FriendGroupVO> friendGroupVOList = friendService.getFriendGroupList(currentUserId);
        return Result.success(friendGroupVOList);
    }

    @GetMapping("/group/{groupName}")
    public Result<List<FriendGroupItemVO>> getFriendGroupItemList(@PathVariable("groupName") String groupName){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            throw new AuthException("用户未登录");
        }
        List<FriendGroupItemVO> friendGroupItemVOList = friendService.getFriendGroupItemList(currentUserId,groupName);
        return Result.success(friendGroupItemVOList);
    }

    @PutMapping("/group")
    public Result<String> updateFriendGroup(@Valid @RequestBody FriendGroupUpdateDTO friendGroupUpdateDTO){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            throw new AuthException("用户未登录");
        }
        friendService.updateFriendGroup(currentUserId,friendGroupUpdateDTO);
        return Result.success("好友分组修改成功");
    }

    @GetMapping("/detail/{friendId}")
    public Result<FriendDetailVO> getFriendDetail(@PathVariable("friendId") Long friendId){
        Long currentUserId = UserContext.getCurUserId();
        if(currentUserId == null){
            throw new AuthException("用户未登录");
        }
        FriendDetailVO friendDetailVO = friendService.getFriendDetail(currentUserId,friendId);
        return Result.success(friendDetailVO);
    }

    @DeleteMapping("/delete/{friendId}")
    public Result<String> deleteFriend(@PathVariable("friendId") Long friendId) {
        Long currentUserId = UserContext.getCurUserId();
        if (currentUserId == null) {
            throw new AuthException("用户未登录");
        }
        friendService.deleteFriend(currentUserId, friendId);
        return Result.success("好友删除成功");
    }

}
