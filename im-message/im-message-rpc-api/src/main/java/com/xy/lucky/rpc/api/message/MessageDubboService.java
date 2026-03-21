package com.xy.lucky.rpc.api.message;

import com.xy.lucky.core.model.IMGroupAction;
import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;

public interface MessageDubboService {

    IMSingleMessage sendSingleMessage(IMSingleMessage dto);

    IMGroupMessage sendGroupMessage(IMGroupMessage dto);

    void sendGroupAction(IMGroupAction dto);
}
