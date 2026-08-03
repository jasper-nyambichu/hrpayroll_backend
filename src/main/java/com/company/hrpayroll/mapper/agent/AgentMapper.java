// mapper/agent/AgentMapper.java
package com.company.hrpayroll.mapper.agent;

import com.company.hrpayroll.dto.request.agent.AgentRequest;
import com.company.hrpayroll.dto.response.agent.AgentResponse;
import com.company.hrpayroll.entity.agent.Agent;
import org.springframework.stereotype.Service;

@Service
public class AgentMapper {

    public Agent toAgent(AgentRequest dto) {
        return Agent.builder()
                .name(dto.name())
                .contactPerson(dto.contactPerson())
                .phone(dto.phone())
                .email(dto.email())
                .address(dto.address())
                .bankName(dto.bankName())
                .bankAccountNumber(dto.bankAccountNumber())
                .kraPin(dto.kraPin())
                .contractStartDate(dto.contractStartDate())
                .contractEndDate(dto.contractEndDate())
                .chargeType(dto.chargeType())
                .chargeAmount(dto.chargeAmount())
                .chargePercentage(dto.chargePercentage())
                .vatRegistered(dto.vatRegistered())
                .build();
    }

    public void updateAgentFromRequest(Agent agent, AgentRequest dto) {
        agent.setName(dto.name());
        agent.setContactPerson(dto.contactPerson());
        agent.setPhone(dto.phone());
        agent.setEmail(dto.email());
        agent.setAddress(dto.address());
        agent.setBankName(dto.bankName());
        agent.setBankAccountNumber(dto.bankAccountNumber());
        agent.setKraPin(dto.kraPin());
        agent.setContractStartDate(dto.contractStartDate());
        agent.setContractEndDate(dto.contractEndDate());
        agent.setChargeType(dto.chargeType());
        agent.setChargeAmount(dto.chargeAmount());
        agent.setChargePercentage(dto.chargePercentage());
        agent.setVatRegistered(dto.vatRegistered());
    }

    public AgentResponse toAgentResponse(Agent agent, Integer employeeCount) {
        return new AgentResponse(
                agent.getId(),
                agent.getName(),
                agent.getContactPerson(),
                agent.getPhone(),
                agent.getEmail(),
                agent.getAddress(),
                agent.getBankName(),
                agent.getBankAccountNumber(),
                agent.getKraPin(),
                agent.getContractStartDate(),
                agent.getContractEndDate(),
                agent.getChargeType(),
                agent.getChargeAmount(),
                agent.getChargePercentage(),
                agent.isVatRegistered(),
                agent.getStatus(),
                employeeCount
        );
    }
}