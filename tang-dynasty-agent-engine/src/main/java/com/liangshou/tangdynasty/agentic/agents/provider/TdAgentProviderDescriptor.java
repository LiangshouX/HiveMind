package com.liangshou.tangdynasty.agentic.agents.provider;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LiangshouX
 */
@Getter
@Setter
public class TdAgentProviderDescriptor {

    private String id;

    private String name;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private String endpointPath;

    private String chatModel;

    private String formatter;

    private List<TdAgentModelDescriptor> models = new ArrayList<>();

    private String apiKeyPrefix;

    private boolean freezeUrl;

    private boolean isCustom;

    private boolean supportModelDiscovery;

    private boolean supportConnectionCheck = true;

    private Map<String, Object> generateKwargs = new LinkedHashMap<>();
}

