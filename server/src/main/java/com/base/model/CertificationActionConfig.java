package com.base.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CertificationActionConfig {

    public static final Map<String, List<ActionButton>> ACTIONS = Map.of(
            "STAGE_1_DESK_REVIEW",
            List.of(
                    new ActionButton(
                            "Duyệt GĐ 1 ➔ Đánh Giá Hiện Trường",
                            "STAGE_2_ON_SITE",
                            "success",
                            "1",
                            "1",
                            "LEAD_AUDITOR"
                    ),
                    new ActionButton(
                            "Yêu Cầu Bổ Sung Hồ Sơ",
                            "STAGE_1_DESK_REVIEW",
                            "warning",
                            "0",
                            "1",
                            "LEAD_AUDITOR"
                    )
            ),

            "STAGE_2_ON_SITE",
            List.of(
                    new ActionButton(
                            "Lấy Mẫu ➔ Gửi PTN Thử Nghiệm",
                            "TESTING",
                            "primary",
                            "1",
                            "2",
                            "LEAD_AUDITOR"
                    ),
                    new ActionButton(
                            "Phát Hiện Lỗi ➔ Chuyển Khắc Phục NCR",
                            "CAPA_PENDING",
                            "danger",
                            "0",
                            "2",
                            "LEAD_AUDITOR"
                    ),
                    new ActionButton(
                            "Hiện Trường Đạt Chuẩn ➔ Trình Thẩm Xét",
                            "TECHNICAL_REVIEW",
                            "success",
                            "2",
                            "2",
                            "LEAD_AUDITOR"
                    ),
                    new ActionButton(
                            "Lấy Mẫu ➔ Gửi PTN Thử Nghiệm",
                            "TESTING",
                            "primary",
                            "1",
                            "2",
                            "AUDITOR"
                    ),
                    new ActionButton(
                            "Phát Hiện Lỗi ➔ Chuyển Khắc Phục NCR",
                            "CAPA_PENDING",
                            "danger",
                            "0",
                            "2",
                            "AUDITOR"
                    )
            ),

            "TESTING",
            List.of(
                    new ActionButton(
                            "Kết Quả Thử Nghiệm Đạt ➔ Chuyển Tiếp",
                            "CAPA_PENDING",
                            "success",
                            "1",
                            "3",
                            "AUDITOR"
                    ),
                    new ActionButton(
                            "Kết Quả Thử Nghiệm Không Đạt ➔ Lập NCR",
                            "CAPA_PENDING",
                            "danger",
                            "0",
                            "3",
                            "AUDITOR"
                    ),
                    new ActionButton(
                            "Kết Quả Thử Nghiệm Đạt ➔ Chuyển Tiếp",
                            "CAPA_PENDING",
                            "success",
                            "1",
                            "3",
                            "LEAD_AUDITOR"
                    )
            ),

            "CAPA_PENDING",
            List.of(
                    new ActionButton(
                            "Đóng Lỗi NCR ➔ Trình Thẩm Xét Kỹ Thuật",
                            "TECHNICAL_REVIEW",
                            "primary",
                            "1",
                            "4",
                            "LEAD_AUDITOR"
                    )
            ),

            "TECHNICAL_REVIEW",
            List.of(
                    new ActionButton(
                            "Thẩm Xét ĐẠT ➔ Trình Giám Đốc Cấp GCN",
                            "APPROVED",
                            "success",
                            "1",
                            "5",
                            "TECHNICAL_REVIEWER"
                    ),
                    new ActionButton(
                            "Thẩm Xét Chưa Đạt ➔ Yêu Cầu Bổ Sung",
                            "CAPA_PENDING",
                            "warning",
                            "0",
                            "5",
                            "TECHNICAL_REVIEWER"
                    ),
                    new ActionButton(
                            "Từ Chối Cấp Chứng Nhận",
                            "REJECTED",
                            "danger",
                            "-1",
                            "5",
                            "TECHNICAL_REVIEWER"
                    )
            ),

            "APPROVED",
            List.of(
                    new ActionButton(
                            "Ký Quyết Định & Cấp Giấy Chứng Nhận",
                            "CERTIFIED",
                            "success",
                            "1",
                            "6",
                            "CERT_DIRECTOR"
                    ),
                    new ActionButton(
                            "Trả Lại Hồ Sơ Thẩm Xét",
                            "TECHNICAL_REVIEW",
                            "danger",
                            "0",
                            "6",
                            "CERT_DIRECTOR"
                    ),
                    new ActionButton(
                            "Ký Quyết Định & Cấp Giấy Chứng Nhận",
                            "CERTIFIED",
                            "success",
                            "1",
                            "6",
                            "DIRECTOR"
                    )
            ),

            "REJECTED",
            List.of(
                    new ActionButton(
                            "Tái Khởi Động Đánh Giá Lại",
                            "STAGE_1_DESK_REVIEW",
                            "warning",
                            "1",
                            "0",
                            "LEAD_AUDITOR"
                    )
            )
    );

    public static List<ActionButton> getButtons(String status, String position, boolean isAdmin) {
        if (status == null) {
            return Collections.emptyList();
        }

        List<ActionButton> buttonsForStatus = ACTIONS.getOrDefault(status, Collections.emptyList());

        if (isAdmin) {
            return buttonsForStatus.stream()
                    .collect(Collectors.toMap(
                            ActionButton::getAction,
                            btn -> btn,
                            (existing, replacement) -> existing
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());
        }

        if (position == null) {
            return Collections.emptyList();
        }

        return buttonsForStatus.stream()
                .filter(btn -> position.equalsIgnoreCase(btn.getPosition()))
                .collect(Collectors.toList());
    }
}
