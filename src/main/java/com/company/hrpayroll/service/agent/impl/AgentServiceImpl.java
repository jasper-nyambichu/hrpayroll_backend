// service/agent/impl/AgentServiceImpl.java
package com.company.hrpayroll.service.agent.impl;

import com.company.hrpayroll.dto.request.agent.AgentRequest;
import com.company.hrpayroll.dto.response.agent.AgentResponse;
import com.company.hrpayroll.dto.response.common.GeneratedDocument;
import com.company.hrpayroll.entity.agent.Agent;
import com.company.hrpayroll.entity.agent.AgentDirectorId;
import com.company.hrpayroll.entity.agent.ChargeType;
import com.company.hrpayroll.exception.InvalidOperationException;
import com.company.hrpayroll.exception.ResourceNotFoundException;
import com.company.hrpayroll.mapper.agent.AgentMapper;
import com.company.hrpayroll.repository.agent.AgentDirectorIdRepository;
import com.company.hrpayroll.repository.agent.AgentRepository;
import com.company.hrpayroll.repository.auth.UserRepository;
import com.company.hrpayroll.repository.employee.EmployeeRepository;
import com.company.hrpayroll.service.agent.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final int MAX_DIRECTOR_ID_BYTES = 5 * 1024 * 1024;

    private final AgentRepository agentRepository;
    private final AgentDirectorIdRepository agentDirectorIdRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AgentMapper agentMapper;

    @Override
    public AgentResponse createAgent(AgentRequest request) {
        validateChargeConfiguration(request);
        validateTaxAndContract(request);

        Agent agent = agentMapper.toAgent(request);
        Agent saved = agentRepository.save(agent);
        return toResponseWithEmployeeCount(saved);
    }

    @Override
    public AgentResponse updateAgent(Long id, AgentRequest request) {
        validateChargeConfiguration(request);
        validateTaxAndContract(request);

        Agent agent = findAgentOrThrow(id);
        agentMapper.updateAgentFromRequest(agent, request);
        Agent saved = agentRepository.save(agent);
        return toResponseWithEmployeeCount(saved);
    }

    @Override
    public AgentResponse getAgentById(Long id) {
        return toResponseWithEmployeeCount(findAgentOrThrow(id));
    }

    @Override
    public List<AgentResponse> getAllAgents() {
        return agentRepository.findAll().stream()
                .map(this::toResponseWithEmployeeCount)
                .toList();
    }

    @Override
    public List<AgentResponse> getAgentsByIds(List<Long> ids) {
        return agentRepository.findByIdIn(ids).stream()
                .map(this::toResponseWithEmployeeCount)
                .toList();
    }

    @Override
    @Transactional
    public void deactivateAgent(Long id) {
        Agent agent = findAgentOrThrow(id);
        agent.setStatus("INACTIVE");
        agentRepository.save(agent);

        // A deactivated agent must not keep access to the Agent Portal —
        // disabling the linked login blocks authentication for it.
        userRepository.findByAgentId(id).ifPresent(user -> {
            user.setActive(false);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public AgentResponse uploadDirectorId(Long id, MultipartFile file) {
        Agent agent = findAgentOrThrow(id);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded photo", e);
        }

        if (content.length == 0) {
            throw new InvalidOperationException("Choose a photo of the director's ID");
        }
        if (content.length > MAX_DIRECTOR_ID_BYTES) {
            throw new InvalidOperationException("The photo must be 5 MB or smaller");
        }

        // Going by the file's first bytes, not the name or type the browser claims it is.
        String contentType = detectImageType(content);
        if (contentType == null) {
            throw new InvalidOperationException("The photo must be a PNG or JPEG image");
        }

        AgentDirectorId directorId = agentDirectorIdRepository.findByAgentId(id)
                .orElseGet(() -> AgentDirectorId.builder().agentId(id).build());
        directorId.setContent(content);
        directorId.setContentType(contentType);
        agentDirectorIdRepository.save(directorId);

        return toResponseWithEmployeeCount(agent);
    }

    @Override
    public GeneratedDocument getDirectorId(Long id) {
        AgentDirectorId directorId = agentDirectorIdRepository.findByAgentId(id)
                .orElseThrow(() -> new ResourceNotFoundException("No director ID on file for agent: " + id));
        String extension = "image/png".equals(directorId.getContentType()) ? "png" : "jpg";
        return new GeneratedDocument("director-id-" + id + "." + extension, directorId.getContentType(), directorId.getContent());
    }

    private static String detectImageType(byte[] content) {
        if (content.length >= 4 && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return "image/png";
        }
        if (content.length >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        return null;
    }

    // The charge type determines which amount field is actually required —
    // this is exactly the kind of rule a DTO annotation can't express, so it
    // lives here in the service instead.
    private void validateChargeConfiguration(AgentRequest request) {
        ChargeType type = request.chargeType();

        if (type == ChargeType.PERCENTAGE_OF_GROSS) {
            if (request.chargePercentage() == null) {
                throw new InvalidOperationException(
                        "Charge percentage is required when charge type is PERCENTAGE_OF_GROSS");
            }
        } else {
            if (request.chargeAmount() == null) {
                throw new InvalidOperationException(
                        "Charge amount is required for charge type " + type);
            }
        }
    }

    // A VAT-registered agent's invoice is a tax document, so it must carry
    // the agent's KRA PIN; and a contract can't end before it starts.
    private void validateTaxAndContract(AgentRequest request) {
        if (request.vatRegistered()
                && (request.kraPin() == null || request.kraPin().isBlank())) {
            throw new InvalidOperationException(
                    "KRA PIN is required for a VAT-registered agent");
        }

        if (request.contractStartDate() != null && request.contractEndDate() != null
                && request.contractEndDate().isBefore(request.contractStartDate())) {
            throw new InvalidOperationException(
                    "Contract end date cannot be before contract start date");
        }
    }

    private Agent findAgentOrThrow(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + id));
    }

    private AgentResponse toResponseWithEmployeeCount(Agent agent) {
        long count = employeeRepository.countByAgentIdAndEmploymentStatus(agent.getId(), "ACTIVE");
        return agentMapper.toAgentResponse(agent, (int) count, agentDirectorIdRepository.hasPhoto(agent.getId()));
    }
}