package com.hify.provider.constant;

/**
 * provider模块常量
 */
public final class ProviderConstant {

    private ProviderConstant() {
    }

    public static final String TYPE_OPENAI = "OPENAI";

    public static final String TYPE_OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE";

    public static final String TYPE_ANTHROPIC = "ANTHROPIC";

    public static final String TYPE_OLLAMA = "OLLAMA";

    public static final int STATUS_DISABLED = 0;

    public static final int STATUS_ENABLED = 1;

    public static final int STATUS_FAULT = 2;
}
