package com.huan.aihelper.utils;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

@Data
public class ThinkingResult {
    private String thinking;
    private String answer;
    private boolean hasThinking;

    private static final String THINKING_REGEX = "<thinking>\\s*([\\s\\S]*?)\\s*</thinking>";
    private static final String ANSWER_REGEX = "<answer>\\s*([\\s\\S]*?)\\s*</answer>";

    public static ThinkingResult parse(String modelOutput) {
        if (StrUtil.isBlank(modelOutput)) {
            ThinkingResult result = new ThinkingResult();
            result.setAnswer("");
            result.setHasThinking(false);
            return result;
        }

        ThinkingResult result = new ThinkingResult();

        String thinkingContent = ReUtil.getGroup1(THINKING_REGEX, modelOutput);
        if (StrUtil.isNotBlank(thinkingContent)) {
            result.setThinking(thinkingContent.trim());
            result.setHasThinking(true);
        } else {
            result.setHasThinking(false);
        }

        String answerContent = ReUtil.getGroup1(ANSWER_REGEX, modelOutput);
        if (StrUtil.isNotBlank(answerContent)) {
            result.setAnswer(answerContent.trim());
        } else {
            result.setAnswer(modelOutput.trim());
        }

        return result;
    }

    public String getDisplayText(boolean showThinking) {
        if (showThinking && hasThinking) {
            return "💭 思考过程：\n" + thinking + "\n\n📝 回答：\n" + answer;
        }
        return answer;
    }
}