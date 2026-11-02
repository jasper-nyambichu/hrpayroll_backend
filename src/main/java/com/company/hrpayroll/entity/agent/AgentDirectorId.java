// entity/agent/AgentDirectorId.java
package com.company.hrpayroll.entity.agent;

import com.company.hrpayroll.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// A photo of the agent's director's ID: PNG or JPEG. Apart from Agent so the image only loads when asked for.
@Entity
@Table(name = "agent_director_ids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentDirectorId extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long agentId;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;

    @Column(nullable = false, length = 20)
    private String contentType;
}
