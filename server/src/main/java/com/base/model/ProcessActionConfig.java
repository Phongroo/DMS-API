package com.base.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessActionConfig {
    public static final Map<String, List<ActionButton>> ACTIONS =
            Map.of(

                    "DRAFT",
                    List.of(
                            new ActionButton(
                                    "Gửi duyệt",
                                    "submit",
                                    "primary",
                                    "",
                                    "0",
                                    "STAFF"
                            )
                    ),

                    "PENDING_MANAGER_APPROVAL",
                    List.of(
                            new ActionButton(
                                    "Duyệt",
                                    "approve",
                                    "success",
                                    "1",
                                    "1",
                                    "MANAGER"
                            ),
                            new ActionButton(
                                    "Từ chối",
                                    "reject",
                                    "danger",
                                    "0",
                                    "1",
                                    "MANAGER"
                            )
                    ),

                    "MANAGER_APPROVED",
                    List.of(
                            new ActionButton(
                                    "Director duyệt",
                                    "approve",
                                    "success",
                                    "1",
                                    "2",
                                    "DIRECTOR"
                            ),
                            new ActionButton(
                                    "Director từ chối",
                                    "reject",
                                    "danger",
                                    "0",
                                    "2",
                                    "DIRECTOR"
                            )
                    )
            );
    public static List<ActionButton> getButtons(
            String status,
            String position
    ) {

        return ACTIONS
                .getOrDefault(
                        status,
                        Collections.emptyList()
                )
                .stream()
                .filter(x ->
                        x.getPosition()
                                .equals(position)
                ).collect(Collectors.toList());

    }

}


