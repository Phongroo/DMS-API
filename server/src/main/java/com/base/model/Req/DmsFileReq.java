package com.base.model.Req;

import com.base.model.DmsDoc;
import com.base.model.DmsFile;

import java.util.UUID;

public class DmsFileReq{
    private DmsDoc dmsDoc;

    public DmsDoc getDmsDoc() {
        return dmsDoc;
    }

    public void setDmsDoc(DmsDoc dmsDoc) {
        this.dmsDoc = dmsDoc;
    }
}
