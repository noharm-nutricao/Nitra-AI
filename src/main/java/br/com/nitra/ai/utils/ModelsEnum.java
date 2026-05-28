package br.com.nitra.ai.utils;

import java.util.Arrays;

public enum ModelsEnum {

    GPT,
    ANTHROPIC,
    AMAZON_LITE,
    MAGISTRAL;

    public static String valuesAsText() {
        return Arrays.stream(values())
                .map(Enum::name)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }
}
