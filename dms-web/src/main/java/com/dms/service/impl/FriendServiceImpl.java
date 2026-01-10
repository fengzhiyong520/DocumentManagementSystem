package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.dto.FriendDTO;
import com.dms.entity.Friend;
import com.dms.entity.User;
import com.dms.mapper.FriendMapper;
import com.dms.mapper.UserMapper;
import com.dms.service.FriendService;
import com.dms.vo.FriendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友服务实现
 */
@Service
@RequiredArgsConstructor
public class FriendServiceImpl extends ServiceImpl<FriendMapper, Friend> implements FriendService {
    
    private final UserMapper userMapper;
    
    @Override
    public List<FriendVO> getFriendList(Long userId) {
        // 查询当前用户的所有好友（状态为1：已同意）
        List<Friend> friends = this.list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 1)
                .orderByDesc(Friend::getCreateTime));
        
        return friends.stream().map(friend -> {
            FriendVO vo = new FriendVO();
            BeanUtils.copyProperties(friend, vo);
            
            // 查询好友的用户信息
            User friendUser = userMapper.selectById(friend.getFriendId());
            if (friendUser != null) {
                vo.setFriendUsername(friendUser.getUsername());
                vo.setFriendNickname(friendUser.getNickname());
                vo.setFriendEmail(friendUser.getEmail());
                vo.setFriendPhone(friendUser.getPhone());
                vo.setFriendAvatar(friendUser.getAvatar());
                vo.setFriendStatus(friendUser.getStatus());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<FriendVO> getFriendRequestList(Long userId) {
        // 查询收到的好友申请（friendId是当前用户，status为2：待同意）
        List<Friend> requests = this.list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 2)
                .orderByDesc(Friend::getCreateTime));
        
        return requests.stream().map(request -> {
            FriendVO vo = new FriendVO();
            BeanUtils.copyProperties(request, vo);
            
            // 查询申请者的用户信息（userId是申请者）
            User applicant = userMapper.selectById(request.getUserId());
            if (applicant != null) {
                vo.setApplicantUsername(applicant.getUsername());
                vo.setApplicantNickname(applicant.getNickname());
                vo.setApplicantEmail(applicant.getEmail());
                vo.setApplicantPhone(applicant.getPhone());
                vo.setApplicantAvatar(applicant.getAvatar());
                vo.setApplicantStatus(applicant.getStatus());
                // 同时设置friend信息以便兼容
                vo.setFriendUsername(applicant.getUsername());
                vo.setFriendNickname(applicant.getNickname());
                vo.setFriendEmail(applicant.getEmail());
                vo.setFriendPhone(applicant.getPhone());
                vo.setFriendAvatar(applicant.getAvatar());
                vo.setFriendStatus(applicant.getStatus());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<FriendVO> getSentFriendRequestList(Long userId) {
        // 查询发送的好友申请（userId是当前用户，status为2：待同意）
        List<Friend> requests = this.list(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 2)
                .orderByDesc(Friend::getCreateTime));
        
        return requests.stream().map(request -> {
            FriendVO vo = new FriendVO();
            BeanUtils.copyProperties(request, vo);
            
            // 查询被申请者的用户信息（friendId是被申请者）
            User target = userMapper.selectById(request.getFriendId());
            if (target != null) {
                vo.setFriendUsername(target.getUsername());
                vo.setFriendNickname(target.getNickname());
                vo.setFriendEmail(target.getEmail());
                vo.setFriendPhone(target.getPhone());
                vo.setFriendAvatar(target.getAvatar());
                vo.setFriendStatus(target.getStatus());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestFriend(Long userId, FriendDTO friendDTO) {
        // 验证好友是否存在
        User friendUser = userMapper.selectById(friendDTO.getFriendId());
        if (friendUser == null) {
            throw new RuntimeException("好友用户不存在");
        }
        
        // 不能添加自己为好友
        if (userId.equals(friendDTO.getFriendId())) {
            throw new RuntimeException("不能添加自己为好友");
        }
        
        // 检查是否已经存在记录（包括所有状态）
        Friend existingRecord = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendDTO.getFriendId()));
        
        if (existingRecord != null) {
            // 如果状态为1（已同意），提示已经是好友
            if (existingRecord.getStatus() == 1) {
                throw new RuntimeException("该用户已经是您的好友");
            }
            // 如果状态为2（待同意），提示已经发送过申请
            if (existingRecord.getStatus() == 2) {
                throw new RuntimeException("您已经向该用户发送了好友申请，请等待对方同意");
            }
            // 如果状态为0（已删除），更新现有记录为待同意状态
            if (existingRecord.getStatus() == 0) {
                existingRecord.setStatus(2);
                existingRecord.setRemark(friendDTO.getRemark());
                this.updateById(existingRecord);
                return;
            }
        }
        
        // 检查对方是否已经向自己发送了申请，如果是，直接同意
        Friend reverseRequest = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, friendDTO.getFriendId())
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 2));
        
        if (reverseRequest != null) {
            // 直接同意：将对方申请状态改为1，并创建自己的好友记录
            reverseRequest.setStatus(1);
            this.updateById(reverseRequest);
            
            // 检查自己是否已经有记录（可能是status=0的情况）
            Friend myRecord = this.getOne(new LambdaQueryWrapper<Friend>()
                    .eq(Friend::getUserId, userId)
                    .eq(Friend::getFriendId, friendDTO.getFriendId()));
            
            if (myRecord != null) {
                // 更新现有记录
                myRecord.setStatus(1);
                myRecord.setRemark(friendDTO.getRemark());
                this.updateById(myRecord);
            } else {
                // 创建新的好友记录
                Friend friend = new Friend();
                friend.setUserId(userId);
                friend.setFriendId(friendDTO.getFriendId());
                friend.setRemark(friendDTO.getRemark());
                friend.setStatus(1);
                this.save(friend);
            }
            return;
        }
        
        // 创建新的好友申请（状态为2：待同意）
        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendDTO.getFriendId());
        friend.setRemark(friendDTO.getRemark());
        friend.setStatus(2); // 待同意
        this.save(friend);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptFriendRequest(Long userId, Long requestId) {
        // 查找申请记录（friendId是当前用户，userId是申请者）
        Friend request = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getId, requestId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 2));
        
        if (request == null) {
            throw new RuntimeException("好友申请不存在或已处理");
        }
        
        // 将申请状态改为1（已同意）
        request.setStatus(1);
        this.updateById(request);
        
        // 创建反向的好友关系（当前用户 -> 申请者）
        Friend reverseFriend = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, request.getUserId()));
        
        if (reverseFriend == null) {
            Friend newFriend = new Friend();
            newFriend.setUserId(userId);
            newFriend.setFriendId(request.getUserId());
            newFriend.setStatus(1);
            this.save(newFriend);
        } else if (reverseFriend.getStatus() != 1) {
            reverseFriend.setStatus(1);
            this.updateById(reverseFriend);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectFriendRequest(Long userId, Long requestId) {
        // 查找申请记录
        Friend request = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getId, requestId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 2));
        
        if (request == null) {
            throw new RuntimeException("好友申请不存在或已处理");
        }
        
        // 删除申请记录（软删除：状态改为0）
        request.setStatus(0);
        this.updateById(request);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFriend(Long userId, FriendDTO friendDTO) {
        Friend friend = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getId, friendDTO.getId())
                .eq(Friend::getStatus, 1));
        
        if (friend == null) {
            throw new RuntimeException("好友记录不存在");
        }
        
        if (friendDTO.getRemark() != null) {
            friend.setRemark(friendDTO.getRemark());
        }
        this.updateById(friend);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendId) {
        Friend friend = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, 1));
        
        if (friend == null) {
            throw new RuntimeException("好友记录不存在");
        }
        
        // 软删除：设置状态为0
        friend.setStatus(0);
        this.updateById(friend);
    }
    
    @Override
    public FriendVO getFriendById(Long userId, Long id) {
        Friend friend = this.getOne(new LambdaQueryWrapper<Friend>()
                .eq(Friend::getUserId, userId)
                .eq(Friend::getId, id)
                .eq(Friend::getStatus, 1));
        
        if (friend == null) {
            throw new RuntimeException("好友记录不存在");
        }
        
        FriendVO vo = new FriendVO();
        BeanUtils.copyProperties(friend, vo);
        
        // 查询好友的用户信息
        User friendUser = userMapper.selectById(friend.getFriendId());
        if (friendUser != null) {
            vo.setFriendUsername(friendUser.getUsername());
            vo.setFriendNickname(friendUser.getNickname());
            vo.setFriendEmail(friendUser.getEmail());
            vo.setFriendPhone(friendUser.getPhone());
            vo.setFriendAvatar(friendUser.getAvatar());
            vo.setFriendStatus(friendUser.getStatus());
        }
        
        return vo;
    }
}

