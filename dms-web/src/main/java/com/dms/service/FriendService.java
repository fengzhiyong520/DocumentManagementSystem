package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.dto.FriendDTO;
import com.dms.entity.Friend;
import com.dms.vo.FriendVO;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService extends IService<Friend> {
    /**
     * 获取当前用户的好友列表（已同意的）
     */
    List<FriendVO> getFriendList(Long userId);
    
    /**
     * 获取收到的好友申请列表
     */
    List<FriendVO> getFriendRequestList(Long userId);
    
    /**
     * 获取发送的好友申请列表
     */
    List<FriendVO> getSentFriendRequestList(Long userId);
    
    /**
     * 申请添加好友
     */
    void requestFriend(Long userId, FriendDTO friendDTO);
    
    /**
     * 同意好友申请
     */
    void acceptFriendRequest(Long userId, Long requestId);
    
    /**
     * 拒绝好友申请
     */
    void rejectFriendRequest(Long userId, Long requestId);
    
    /**
     * 更新好友备注
     */
    void updateFriend(Long userId, FriendDTO friendDTO);
    
    /**
     * 删除好友
     */
    void deleteFriend(Long userId, Long friendId);
    
    /**
     * 根据ID获取好友信息
     */
    FriendVO getFriendById(Long userId, Long id);
}

