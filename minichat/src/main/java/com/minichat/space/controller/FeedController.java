package com.minichat.space.controller;

import com.minichat.common.result.Result;
import com.minichat.common.result.ScrollResult;
import com.minichat.space.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // 获取关注用户的动态(滚动加载)
    @GetMapping("/follow")
    public Result<ScrollResult> feed(@RequestParam("lastId") Long maxTimeStamp, @RequestParam(name = "offset", defaultValue = "0") Long offset) {
        ScrollResult scrollResult = feedService.feed(maxTimeStamp, offset);
        return Result.success(scrollResult);
    }

}
