package com.example.aichatbot.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "absence_requests")
public class AbsenceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String absenceType;
    private String employeeId;

    @Column(length = 1000)
    private String reason;

    private LocalDate requestDate;
    private LocalDate startDate;
    private LocalDate endDate;

    public AbsenceRequest() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAbsenceType() { return absenceType; }
    public void setAbsenceType(String absenceType) { this.absenceType = absenceType; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
