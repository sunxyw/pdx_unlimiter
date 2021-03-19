package com.crschnick.pdx_unlimiter.app.installation;

import com.crschnick.pdx_unlimiter.app.core.PdxuI18n;
import com.crschnick.pdx_unlimiter.app.util.LocalisationHelper;

public final class InvalidInstallationException extends Exception {

    private final String msgId;
    private final String[] variables;

    public InvalidInstallationException(String msgId, String... vars) {
        super(PdxuI18n.get(LocalisationHelper.Language.ENGLISH).getLocalised(msgId, vars));
        this.msgId = msgId;
        this.variables = vars;
    }

    public InvalidInstallationException(Throwable cause) {
        super(cause);
        this.msgId = "ERROR_OCCURED";
        this.variables = new String[0];
    }

    public String getMessageId() {
        return msgId;
    }

    public String getLocalisedMessage() {
        return PdxuI18n.get(msgId, variables);
    }
}