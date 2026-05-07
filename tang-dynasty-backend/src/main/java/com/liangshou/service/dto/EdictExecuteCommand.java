package com.liangshou.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 执行任务模板命令（下旨）。
 *
 * @author LiangshouX
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdictExecuteCommand {

    /**
     * 模板参数键值对
     */
    private Map<String, String> params;
}
