package com.hify.knowledge.dto;

import lombok.Data;
import java.util.List;

@Data
public class RagResp {
    private String answer;
    private List<String> sources;
    private int tokenCount;
    private int latencyMs;
}
