package com.procureai.ai.mcp;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
@PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2','VENDOR_MANAGER')")
public class McpController {

    private final McpToolRegistry registry;

    public McpController(McpToolRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/tools")
    public List<McpToolDescriptor> tools() {
        return registry.listTools();
    }

    @PostMapping("/tools/{toolName}/call")
    public McpCallResponse call(@PathVariable String toolName, @RequestBody(required = false) McpCallRequest request) {
        return new McpCallResponse(toolName, registry.call(toolName, request == null ? null : request.arguments()));
    }
}
