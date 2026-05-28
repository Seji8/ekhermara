package com.example.pfe.services;


import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("archivageApprouveDelegate")
public class ArchivageApprouveDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("Archivage APPROUVÉ exécuté");
    }
}