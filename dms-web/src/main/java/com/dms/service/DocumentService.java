package com.dms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dms.utils.PageUtils;
import com.dms.dto.DocumentDTO;
import com.dms.entity.Document;
import com.dms.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资料服务接口
 */
public interface DocumentService extends IService<Document> {
    /**
     * 分页查询资料
     */
    PageUtils<DocumentVO> pageDocument(Long current, Long size, String keyword, String category, Integer isPublic);

    /**
     * 根据ID获取资料
     */
    DocumentVO getDocumentById(Long id);

    /**
     * 上传资料
     */
    void uploadDocument(DocumentDTO documentDTO, MultipartFile file);

    /**
     * 更新资料
     */
    void updateDocument(DocumentDTO documentDTO);

    /**
     * 删除资料
     */
    void deleteDocument(Long id);

    /**
     * 大厅公开资料列表
     */
    PageUtils<DocumentVO> listPublicDocuments(Long current, Long size, String keyword, String category, String fileName, String uploadUserName);

    /**
     * 切换资料状�?
     */
    void changeDocumentStatus(Long id, Integer status);

    /**
     * 导入资料
     */
    void importDocuments(java.util.List<DocumentDTO> documentList);

    /**
     * 导出资料
     */
    java.util.List<DocumentVO> exportDocuments();
}

