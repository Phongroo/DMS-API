package com.base.service.impl;

import com.base.model.ActionButton;
import com.base.model.Authority;
import com.base.model.DmsDoc;
import com.base.model.ProcessActionConfig;
import com.base.model.Req.WorkflowContext;
import com.base.model.Res.BaseResponse;
import com.base.model.Res.TaskResponse;
import com.base.repo.DmsDocRepository;
import com.base.service.DmsDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DmsDocServiceImpl implements DmsDocService {
    @Autowired
    private DmsDocRepository dmsDocRepository;
    @Override
    public BaseResponse fetchDoc() {
        try {
            Authentication auth =
                    SecurityContextHolder.getContext()
                            .getAuthentication();

            String position = auth.getAuthorities()
                    .stream()
                    .filter(a -> a instanceof Authority)
                    .map(a -> ((Authority) a).getPosition())
                    .findFirst()
                    .orElse(null);
            List<TaskResponse> taskResponses = new ArrayList<>();
            List<DmsDoc> dmsDoc = dmsDocRepository.findAll();
            dmsDoc.forEach(x->{
                TaskResponse task = new TaskResponse();
                task.setId(x.getId());
                task.setProcessInstanceId(x.getProcessInstanceId());
                task.setDocId(x.getDocId());
                task.setCreatedDate(x.getCreatedDate());
                task.setStatus(x.getStatus());
                if(x.getStatus() != null){
                    List<ActionButton> buttons =
                            ProcessActionConfig.getButtons(
                                    x.getStatus(),
                                    position
                            );

                    task.setActionButtons(buttons);
                    task.setDmsFiles(x.getFiles());
                }
                taskResponses.add(task);

            });


            return new BaseResponse(200, taskResponses, "success");
        }catch (Exception e){
            return new BaseResponse(200, null, e.getMessage());
        }

    }

    @Override
    public BaseResponse handleTask(WorkflowContext context) {
        try {
            DmsDoc dmsDoc = context.getDmsDoc();
            dmsDoc.setStatus(context.getStatusHandle());
            dmsDocRepository.save(dmsDoc);
            return null;
        }catch (Exception e){
            return new BaseResponse(200, null, e.getMessage());
        }
    }
}
