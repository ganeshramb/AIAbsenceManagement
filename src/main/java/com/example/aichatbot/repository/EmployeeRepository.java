package com.example.aichatbot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.aichatbot.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByNameIgnoreCase(String name);
    Optional<Employee> findByEmailIgnoreCase(String email);
    Optional<Employee> findByEmployeeId(String employeeId);
    
    List<Employee> findAllByNameIgnoreCase(String name);
    
}
