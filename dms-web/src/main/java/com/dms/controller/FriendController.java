package com.dms.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dms.core.annotation.RequiresPermission;
import com.dms.core.domain.R;
import com.dms.dto.FriendDTO;
import com.dms.entity.Friend;
import com.dms.service.FriendService;
import com.dms.vo.FriendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 好友管理控制器
 */
@Tag(name = "好友管理")
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendService friendService;
    
    @Operation(summary = "获取当前用户的好友列表")
    @GetMapping("/list")
    public R<List<FriendVO>> getFriendList() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FriendVO> friends = friendService.getFriendList(userId);
        return R.success(friends);
    }
    
    @Operation(summary = "根据ID获取好友信息")
    @GetMapping("/{id}")
    public R<FriendVO> getFriendById(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        FriendVO friend = friendService.getFriendById(userId, id);
        return R.success(friend);
    }
    
    @Operation(summary = "申请添加好友")
    @RequiresPermission(resource = "friend", method = "POST")
    @PostMapping("/request")
    public R<?> requestFriend(@Valid @RequestBody FriendDTO friendDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        friendService.requestFriend(userId, friendDTO);
        return R.success("申请已发送");
    }
    
    @Operation(summary = "获取收到的好友申请列表")
    @GetMapping("/requests")
    public R<List<FriendVO>> getFriendRequestList() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FriendVO> requests = friendService.getFriendRequestList(userId);
        return R.success(requests);
    }
    
    @Operation(summary = "获取发送的好友申请列表")
    @GetMapping("/sent-requests")
    public R<List<FriendVO>> getSentFriendRequestList() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FriendVO> requests = friendService.getSentFriendRequestList(userId);
        return R.success(requests);
    }
    
    @Operation(summary = "取消好友申请")
    @RequiresPermission(resource = "friend/*", method = "DELETE")
    @DeleteMapping("/request/{requestId}")
    public R<?> cancelFriendRequest(@PathVariable Long requestId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 验证申请记录是否存在且属于当前用户（userId是当前用户）
        Friend request = friendService.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getId, requestId)
                .eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 2));
        if (request == null) {
            throw new RuntimeException("申请记录不存在或已处理");
        }
        // 删除申请记录（软删除：状态改为0）
        request.setStatus(0);
        friendService.updateById(request);
        return R.success("已取消");
    }
    
    @Operation(summary = "同意好友申请")
    @RequiresPermission(resource = "friend", method = "PUT")
    @PutMapping("/accept/{requestId}")
    public R<?> acceptFriendRequest(@PathVariable Long requestId) {
        Long userId = StpUtil.getLoginIdAsLong();
        friendService.acceptFriendRequest(userId, requestId);
        return R.success("已同意");
    }
    
    @Operation(summary = "拒绝好友申请")
    @RequiresPermission(resource = "friend", method = "PUT")
    @PutMapping("/reject/{requestId}")
    public R<?> rejectFriendRequest(@PathVariable Long requestId) {
        Long userId = StpUtil.getLoginIdAsLong();
        friendService.rejectFriendRequest(userId, requestId);
        return R.success("已拒绝");
    }
    
    @Operation(summary = "更新好友备注")
    @RequiresPermission(resource = "friend", method = "PUT")
    @PutMapping
    public R<?> updateFriend(@Valid @RequestBody FriendDTO friendDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        friendService.updateFriend(userId, friendDTO);
        return R.success("更新成功");
    }
    
    @Operation(summary = "删除好友")
    @RequiresPermission(resource = "friend/*", method = "DELETE")
    @DeleteMapping("/{friendId}")
    public R<?> deleteFriend(@PathVariable Long friendId) {
        Long userId = StpUtil.getLoginIdAsLong();
        friendService.deleteFriend(userId, friendId);
        return R.success("删除成功");
    }
}

