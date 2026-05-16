package com.example.pfe.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TeleworkAnalysisRequest {
    private Long userId;
    private Long demandeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String demandeType; 
    private List<LocalDate> requestedDates;
}