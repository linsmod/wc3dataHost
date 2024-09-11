package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.LinqList;
import com.linsmod.common.Strings;
import com.linsmod.webAppHost.ann.ServiceMethod;
import com.linsmod.webAppHost.httpserver.CacheControl;
import com.linsmod.webAppHost.httpserver.Payload;

import java.util.*;

public abstract class ProtoHost {
    static final Map<String, ServerAskUser> hungDialogs = new HashMap<>();
    public static int BUTTON_OK = 100;
    public static int BUTTON_CANCEL = 101;
    static int MAX_WAIT_SECONDS = 30;
    static Map<String, Long> clients = new HashMap<>();
    // createdOps and polledOps is designed sharing in global
    protected final CacheControl cacheControl;
    private final Class<?> payloadType;
    private final ServiceMethodInvoker invoker;
    public Map<String, String> responseHeaders = new LinkedHashMap<>();
    protected ProtoRequest request;
    private Route route;
    private LinqList<String> clientTypes = new LinqList<>("vueClient");
//
//    protected Result waitPendingOp(ServerAskUser data, int timeout) {
//        CancellationSignal localSignal = cancelThenNew("waitPendingOp");
//        queuedEvents.offer(data);
//        int sleepN = 0;
//        int waits = timeout > MAX_WAIT_SECONDS ? MAX_WAIT_SECONDS : timeout;
//        while (!localSignal.isCanceled()) {
//            if (data.getAnswered() > 0) {
//                data.invalidate("Result collected");
//                break;
//            }
//            try {
//                Thread.sleep(100);
//                sleepN += 100;
//                if (sleepN > waits * 1000) {
//                    data.invalidate("WaitTimeout");
//                    break;
//                }
//            } catch (InterruptedException e) {
//                break;
//            }
//        }
//
//        // remove data if no consumer poll from it
//        if (queuedEvents.contains(data)) {
//            queuedEvents.remove(data);
//        }
//        if (localSignal.isCanceled()) {
//            return Result.error("cancelled");
//        }
//        if (data.getAnswered() > 0)
//            return data(data.getAnswered());
//        return Null;
//    }

    public ProtoHost(Class<?> payloadType, Route route, CacheControl cc) {
        this.route = route;
        this.cacheControl = cc;
        this.payloadType = payloadType;
        this.invoker = new ServiceMethodInvoker(this);
    }

    public abstract Result process(Payload[] payloads);

    protected Route getRoute() {
        return this.route;
    }

    protected Result processMvc(Payload[] payloads) throws Throwable {
        this.request = this.request == null ? new ProtoRequest(this.route, payloads) : this.request;
        String actionName = route.getMethod();
        Object d = invoker.invoke(actionName, payloads);
        if (d == null) {
            return Result.internalServerError("invoker should not returns NULL");
        }
        if (d instanceof Result) {
            return (Result) d;
        }
        return data(d);
    }

    @ServiceMethod
    public Result onAuth() {
        String clientId = request.getClientId();

        if (!checkClient(clientId)) {
            return error("客户端校验失败", 401);
        }

        //继续挂起的询问
        if (!route.getMethod().equalsIgnoreCase("performOp")) {
            ServerAskUser serverAskUser = hungDialogs.get(this.request.getClientId());
            if (serverAskUser != null) {
                return serverAskUser.createAlert();
            }
        }
        return null;
    }

    public String getTargetMethod() {
        return route.getMethod();
    }

    protected Result data(Object obj) {
        return Result.json(obj);
    }

    public Result authClient(String clientId) {

        // invalid client if no client provided.
        if (Strings.isNullOrEmpty(clientId)) {
            return error("此处妖气过重，无法立坛！");
        }
        // initialize from valid types.
        if (clientId.length() == 36 || clientTypes.contains(clientId)) {
            return data(genAllowClient(clientId));
        } else {
            // invalid client if not in whitelist.
            return error("此处妖气过重，无法立坛！");
        }
    }

    public boolean checkClient(String clientId) {

        // invalid client if no client provided.
        if (Strings.isNullOrEmpty(clientId)) {
            return false;
        }
        // initialize from valid types.
        if (clientId.length() == 36 || clientTypes.contains(clientId)) {
            return true;
        } else {
            // invalid client if not in whitelist.
            return false;
        }
    }

    private HandshakeResult genAllowClient(String clientId) {
        if (!clients.containsKey(clientId)) {
            clientId = clientId.length() == 36 ? clientId : UUID.randomUUID().toString();
            long l = System.currentTimeMillis();
            clients.put(clientId, l);
        }
        return new HandshakeResult(clientId, clients.get(clientId));
    }

    public Result authUser(String userName, String password) {
        return ok();
    }

    protected Result error(String err) {
        return Result.error(err);
    }

    protected Result error(String err, int code) {
        return Result.error(err, code);
    }

    protected Result error(Throwable err) {
        return Result.error(err);
    }

    protected Result ok() {
        return Result.Void;
    }

    protected Result createOpDialog(String title, String message, PendingAction pendingAction) {
        ServerAskUser askUser = new ServerAskUser(title, message, pendingAction, BUTTON_OK, BUTTON_CANCEL);
        hungDialogs.put(this.request.getClientId(), askUser);
        return askUser.createAlert();
    }

    protected Result performOp(String id, int button) throws Exception {
        ServerAskUser serverAskUser = hungDialogs.get(request.getClientId());
        if (serverAskUser == null) {
            return error("请重试。错误码: ERR_DIALOG_NOT_FOUND");
        } else if (!serverAskUser.getId().equals(id)) {
            return error("请重试。错误码: ERR_DIALOG_NOT_MATCH");
        } else {
            synchronized (hungDialogs) {
                ServerAskUser remove = hungDialogs.remove(request.getClientId());
                return remove.performAction(button);
            }
        }
    }

    private void rmAsk() {
        List<String> rmList = new ArrayList<>();
        for (String s : hungDialogs.keySet()) {
            if (hungDialogs.get(s).cancelRequested()) {
                rmList.add(s);
            }
        }
        for (String s : rmList) {
            hungDialogs.remove(s);
        }
    }

    public void setRequest(ProtoRequest request) {
        this.request = request;
    }
}
