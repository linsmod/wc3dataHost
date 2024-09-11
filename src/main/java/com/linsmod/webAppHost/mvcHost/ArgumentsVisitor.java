package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.LinqList;
import com.linsmod.webAppHost.httpserver.Payload;

import java.util.List;

public class ArgumentsVisitor {
    private final List<ProtoArgument> p;
    private final Object[] parsedArgs;
    private boolean endVisit;

    public ArgumentsVisitor(List<ProtoArgument> parameters) {
        this.p = parameters;
        this.parsedArgs = new Object[parameters.size()];
    }

    public Payload typedFirst(Payload[] payloads, Class<? extends Payload> type) {
        for (Payload payload : payloads) {
            if (payload.getClass() == type)
                return payload;
        }
        return null;
    }

    public Object[] visit(Payload[] payloads) {
        Payload.JsonPayload payload = (Payload.JsonPayload) typedFirst(payloads, Payload.JsonPayload.class);
        if (payload != null)
            return payload.parse(this.p);
        else {
            Payload payload1 = new LinqList<>(payloads).find(x -> x.getScope().ns() == Payload.Scope.NS_HTTP_PARAMS);
            if (payload1 != null) {
                return payload1.parse(this.p);
            }
        }
        return parsedArgs;
    }
}