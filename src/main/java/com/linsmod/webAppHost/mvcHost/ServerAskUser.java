package com.linsmod.webAppHost.mvcHost;

import java.util.UUID;

public class ServerAskUser {
    private final String title;
    private final String message;
    private final PendingAction action;
    private final int[] optionsAllowed;
    private final String id;
    private String cancelReason;
    private int result;

    public ServerAskUser(String title, String message, PendingAction action, int... optionsAllowed) {
        this.title = title;
        this.message = message;
        this.action = action;
        this.optionsAllowed = optionsAllowed;
        this.id = UUID.randomUUID().toString();
    }

    public Result performAction(int button) throws Exception {
        return action.perform(button);
    }

    public void invalidate(String reason) {
        this.cancelReason = reason;
    }

    public int getAnswered() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public boolean cancelRequested() {
        return this.cancelReason != null;
    }

    public Result createAlert() {
        return new Result.AlertData(title, message, getId(), false);
    }
}
