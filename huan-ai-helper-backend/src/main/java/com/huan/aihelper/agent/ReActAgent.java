package com.huan.aihelper.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent{
    // 进行思考
    public abstract boolean think ();
    // 执行动作
    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "思考完毕-无需执行";
            }
            return act();
        } catch (Exception e) {
            log.error("step error: ", e);
            return "步骤执行失败：" + e.getMessage();
        }
    }
}
