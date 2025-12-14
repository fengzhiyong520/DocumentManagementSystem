package com.dms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 借阅DTO
 */
@Data
public class BorrowDTO {
    private Long id;
    
    @NotNull(message = "资料ID不能为空")
    private Long documentId;
    
    private Long processDefinitionId; // 流程定义ID
    
    private String reason;
    private LocalDateTime expectedReturnTime;
}

