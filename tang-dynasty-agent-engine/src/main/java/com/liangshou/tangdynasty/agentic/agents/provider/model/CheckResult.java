package com.liangshou.tangdynasty.agentic.agents.provider.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * CheckResult：标准检查结果。
 * <p>
 * 用于连接性/存在性等布尔检查的统一返回结构。
 */
@Data
@AllArgsConstructor
public class CheckResult {
    /** 是否通过（true=通过） */
    private boolean ok;
    /** 说明消息（失败原因或附加信息） */
    private String message;

    /**
     * 返回成功结果。
     *
     * @return ok=true，message=""
     */
    public static CheckResult ok() {
        return new CheckResult(true, "");
    }

    /**
     * 返回失败结果。
     *
     * @param msg 失败原因
     * @return ok=false，message=msg
     */
    public static CheckResult error(String msg) {
        return new CheckResult(false, msg);
    }
}
