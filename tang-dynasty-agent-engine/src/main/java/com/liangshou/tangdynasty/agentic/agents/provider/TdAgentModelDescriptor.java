package com.liangshou.tangdynasty.agentic.agents.provider;

import lombok.Getter;
import lombok.Setter;

/**
 * @author LiangshouX
 */
@Getter
@Setter
public class TdAgentModelDescriptor {

    private String id;

    private String name;

    private boolean supportsMultimodal;

    private boolean supportsVideo;

    private String probeSource;
}
