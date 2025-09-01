package com.example.aichatbot.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.aichatbot.entity.AbsenceRequest;
import com.example.aichatbot.entity.Employee;
import com.example.aichatbot.repository.AbsenceRequestRepository;
import com.example.aichatbot.repository.EmployeeRepository;
import com.example.aichatbot.service.GeminiService;
import com.example.aichatbot.service.GeminiService.ParsedData;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private AbsenceRequestRepository absenceRepo;

    @Autowired
    private EmployeeRepository employeeRepo;

    private Map<String, List<String>> sessionHistories = new HashMap<>();

    @PostMapping("/message")
    public Mono<Map<String, String>> processMessage(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        String userMsg = request.getMessage().trim();

        sessionHistories.putIfAbsent(sessionId, new ArrayList<>());
        List<String> history = sessionHistories.get(sessionId);
        history.add("User: " + userMsg);

        return geminiService.chatCompletion(userMsg, history.toArray(new String[0]))
                .flatMap(llmRaw -> {
                    String aiReply = extractReplyFromRaw(llmRaw);
                    history.add("Assistant: " + aiReply);

                    ParsedData data = geminiService.parseStructuredData(aiReply);

                    if (data != null) {
                        // If employee ID provided, verify existence in DB
                        if (data.employeeId != null && !data.employeeId.isEmpty()) {
                            Optional<Employee> empOpt = employeeRepo.findByEmployeeId(data.employeeId);
                            if (empOpt.isEmpty()) {
                                return Mono.just(Map.of("response", "Employee ID not found. Please check and re-enter your name or ID."));
                            }
                        }

                        if (allRequiredDataPresent(data)) {
                            AbsenceRequest requestToSave = new AbsenceRequest();
                            requestToSave.setEmployeeId(data.employeeId);
                            requestToSave.setAbsenceType(data.absenceType);
                            requestToSave.setStartDate(LocalDate.parse(data.startDate));
                            requestToSave.setEndDate(LocalDate.parse(data.endDate));
                            requestToSave.setReason(data.reason);
                            requestToSave.setRequestDate(LocalDate.now());

                            absenceRepo.save(requestToSave);

                            return Mono.just(Map.of("response", "Your absence request has been successfully recorded. Thank you!"));
                        }
                    }

                    return Mono.just(Map.of("response", aiReply));
                });
    }

    private boolean allRequiredDataPresent(ParsedData data) {
        return data.employeeId != null && !data.employeeId.isEmpty()
                && data.absenceType != null && data.startDate != null
                && data.endDate != null && data.reason != null;
    }

    private String extractReplyFromRaw(String raw) {
        return raw;
    }

    public static class ChatRequest {
        private String sessionId;
        private String message;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
