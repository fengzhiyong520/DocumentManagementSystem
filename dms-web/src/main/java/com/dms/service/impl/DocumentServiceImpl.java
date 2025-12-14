package com.dms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dms.entity.Borrow;
import com.dms.utils.PageUtils;
import com.dms.dto.DocumentDTO;
import com.dms.entity.Document;
import com.dms.mapper.DocumentMapper;
import com.dms.service.DocumentService;
import com.dms.service.FileStorageService;
import com.dms.vo.DocumentVO;
import com.dms.mapper.BorrowMapper;
import com.dms.entity.User;
import com.dms.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import cn.dev33.satoken.stp.StpUtil;

/**
 * 资料服务实现
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;
    private final BorrowMapper borrowMapper;

    @Override
    public PageUtils<DocumentVO> pageDocument(Long current, Long size, String keyword, String category, Integer isPublic) {
        Page<Document> page = new Page<>(current, size);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();

        // 只查询当前用户创建的文档
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId != null) {
            wrapper.eq(Document::getUploadUserId, currentUserId);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Document::getTitle, keyword)
                    .or().like(Document::getDescription, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Document::getCategory, category);
        }
        if (isPublic != null) {
            wrapper.eq(Document::getIsPublic, isPublic);
        }
        Page<Document> result = this.page(page, wrapper);
        PageUtils<DocumentVO> pageUtils = new PageUtils<>();
        pageUtils.setCurrent(result.getCurrent());
        pageUtils.setSize(result.getSize());
        pageUtils.setTotal(result.getTotal());
        pageUtils.setRecords(result.getRecords().stream().map(doc -> {
            DocumentVO vo = new DocumentVO();
            BeanUtils.copyProperties(doc, vo);
            // 查询上传人名称
            if (doc.getUploadUserId() != null) {
                User user = userMapper.selectById(doc.getUploadUserId());
                if (user != null) {
                    vo.setUploadUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                }
            }
            return vo;
        }).toList());
        return pageUtils;
    }

    @Override
    public DocumentVO getDocumentById(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new RuntimeException("资料不存在");
        }

        // 检查权限
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId != null && document.getUploadUserId() != null
                && !document.getUploadUserId().equals(currentUserId)) {
            // 如果不是自己创建的文档，检查是否有审批通过的借阅记录
            // 查询当前用户对该文档的借阅记录，状态为已批准(1)或已归还(3)
            LambdaQueryWrapper<Borrow> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.eq(Borrow::getDocumentId, id)
                    .eq(Borrow::getUserId, currentUserId)
                    .in(Borrow::getStatus, 1, 3); // 1-已批准 3-已归还

            long approvedCount = borrowMapper.selectCount(borrowWrapper);
            if (approvedCount == 0) {
                throw new RuntimeException("您没有权限查看该资料");
            }
        }

        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);
        // 查询上传人名称
        if (document.getUploadUserId() != null) {
            User user = userMapper.selectById(document.getUploadUserId());
            if (user != null) {
                vo.setUploadUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadDocument(DocumentDTO documentDTO, MultipartFile file) {
        // 文件非必传
        String fileUrl = null;
        String fileName = null;
        Long fileSize = null;

        if (file != null && !file.isEmpty()) {
            // 获取当前登录用户信息
            Long userId = StpUtil.getLoginIdAsLong();
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            String username = user.getUsername();

            // 上传文件（按用户名隔离）
            fileUrl = fileStorageService.uploadFile(file, username);
            fileName = file.getOriginalFilename();
            fileSize = file.getSize();
        }

        // 保存资料信息
        Document document = new Document();
        BeanUtils.copyProperties(documentDTO, document);
        document.setFileUrl(fileUrl);
        document.setFileName(fileName);
        document.setFileSize(fileSize);
        // 如果category有值且fileType为空，则将category的值设置到fileType
        if (document.getFileType() == null && document.getCategory() != null) {
            document.setFileType(document.getCategory());
        }
        document.setUploadUserId(StpUtil.getLoginIdAsLong());
        if (document.getStatus() == null) {
            document.setStatus(1);
        }
        this.save(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(DocumentDTO documentDTO) {
        Document document = this.getById(documentDTO.getId());
        if (document == null) {
            throw new RuntimeException("资料不存在");
        }

        // 检查权限：只能修改自己创建的文档
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId != null && document.getUploadUserId() != null
                && !document.getUploadUserId().equals(currentUserId)) {
            throw new RuntimeException("您没有权限修改该资料");
        }

        // 如果文件URL发生变化，删除旧文件
        String oldFileUrl = document.getFileUrl();
        if (StringUtils.hasText(oldFileUrl) && StringUtils.hasText(documentDTO.getFileUrl())
                && !oldFileUrl.equals(documentDTO.getFileUrl())) {
            // 文件URL已变化，删除旧文件
            try {
                fileStorageService.deleteFile(oldFileUrl);
            } catch (Exception e) {
                // 忽略删除旧文件失败的情况
            }
        }

        BeanUtils.copyProperties(documentDTO, document, "fileUrl", "fileName", "fileSize", "uploadUserId");
        // 如果category有值且fileType为空，则将category的值设置到fileType
        if (document.getFileType() == null && document.getCategory() != null) {
            document.setFileType(document.getCategory());
        }
        this.updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new RuntimeException("资料不存在");
        }

        // 检查权限：只能删除自己创建的文档
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId != null && document.getUploadUserId() != null
                && !document.getUploadUserId().equals(currentUserId)) {
            throw new RuntimeException("您没有权限删除该资料");
        }

        if (StringUtils.hasText(document.getFileUrl())) {
            // 删除文件
            fileStorageService.deleteFile(document.getFileUrl());
        }
        this.removeById(id);
    }

    @Override
    public PageUtils<DocumentVO> listPublicDocuments(Long current, Long size, String keyword, String category, String fileName, String uploadUserName) {
        Page<Document> page = new Page<>(current, size);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        // 只查询公开的资料
        wrapper.eq(Document::getIsPublic, 1);
        wrapper.eq(Document::getStatus, 1); // 只查询启用的资料

        // 排除当前用户创建的文档
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId != null) {
            wrapper.ne(Document::getUploadUserId, currentUserId);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Document::getTitle, keyword)
                    .or().like(Document::getDescription, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Document::getCategory, category);
        }
        if (StringUtils.hasText(fileName)) {
            wrapper.like(Document::getFileName, fileName);
        }
        if (StringUtils.hasText(uploadUserName)) {
            // 需要通过用户表查询
            // 这里简化处理，如果uploadUserName有值，需要通过关联查询
            // 暂时先不处理，后续可以通过SQL关联查询
        }

        Page<Document> result = this.page(page, wrapper);
        PageUtils<DocumentVO> pageUtils = new PageUtils<>();
        pageUtils.setCurrent(result.getCurrent());
        pageUtils.setSize(result.getSize());
        pageUtils.setTotal(result.getTotal());
        pageUtils.setRecords(result.getRecords().stream().map(doc -> {
            DocumentVO vo = new DocumentVO();
            BeanUtils.copyProperties(doc, vo);
            // 查询上传人名称
            if (doc.getUploadUserId() != null) {
                User user = userMapper.selectById(doc.getUploadUserId());
                if (user != null) {
                    vo.setUploadUserName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                    // 如果搜索条件包含上传人名称，进行过滤
                    if (StringUtils.hasText(uploadUserName)) {
                        String userName = user.getNickname() != null ? user.getNickname() : user.getUsername();
                        if (userName != null && userName.contains(uploadUserName)) {
                            // 匹配，保留
                        } else {
                            return null; // 不匹配，过滤掉
                        }
                    }
                }
            }
            return vo;
        }).filter(vo -> vo != null).toList());
        return pageUtils;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeDocumentStatus(Long id, Integer status) {
        Document document = this.getById(id);
        if (document == null) {
            throw new RuntimeException("资料不存在");
        }

        // 检查权限：只能修改自己创建的文档
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId != null && document.getUploadUserId() != null
                && !document.getUploadUserId().equals(currentUserId)) {
            throw new RuntimeException("您没有权限修改该资料状态");
        }
        if (document == null) {
            throw new RuntimeException("资料不存在");
        }
        document.setStatus(status);
        this.updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importDocuments(java.util.List<DocumentDTO> documentList) {
        for (DocumentDTO documentDTO : documentList) {
            Document document = new Document();
            BeanUtils.copyProperties(documentDTO, document);
            if (document.getStatus() == null) {
                document.setStatus(1);
            }
            if (document.getIsPublic() == null) {
                document.setIsPublic(0);
            }
            document.setUploadUserId(StpUtil.getLoginIdAsLong());
            this.save(document);
        }
    }

    @Override
    public java.util.List<DocumentVO> exportDocuments() {
        // 只导出当前用户创建的文档
        Long currentUserId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (currentUserId != null) {
            wrapper.eq(Document::getUploadUserId, currentUserId);
        }
        java.util.List<Document> documents = this.list(wrapper);
        return documents.stream().map(doc -> {
            DocumentVO vo = new DocumentVO();
            BeanUtils.copyProperties(doc, vo);
            return vo;
        }).toList();
    }
}

