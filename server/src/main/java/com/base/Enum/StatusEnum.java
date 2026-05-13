package com.base.Enum;

public enum StatusEnum {
    // Người tạo khởi tạo
    DRAFT,

    // Đã gửi lên manager
    PENDING_MANAGER_APPROVAL,

    // Manager đã duyệt
    MANAGER_APPROVED,

    // Manager từ chối
    MANAGER_REJECTED,

    // Chờ director duyệt
    PENDING_DIRECTOR_APPROVAL,

    // Director đã duyệt hoàn tất
    DIRECTOR_APPROVED,

    // Director từ chối
    DIRECTOR_REJECTED,

    // Hoàn thành quy trình
    COMPLETED;
}