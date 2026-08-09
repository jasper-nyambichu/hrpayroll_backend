// dto/request/auth/AgentLoginRequest.java
package com.company.hrpayroll.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Agent portal credential.
 *
 * `agents` has no code column, so the agent code IS the username on the
 * AGENT-role user account linked to that agent (`users.agent_id`). Usernames
 * are already unique, which is the only property a login code needs. If a
 * distinct, business-visible agent code is wanted later, add
 * `agents.code UNIQUE` and resolve it to the user here instead — the rest of
 * this flow is unchanged.
 */
public record AgentLoginRequest(
        @NotBlank(message = "Agent code is required") String agentCode,
        @NotBlank(message = "Password is required") String password
) {}
