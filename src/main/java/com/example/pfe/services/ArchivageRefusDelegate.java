package com.example.pfe.services;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("archivageRefusDelegate")
public class ArchivageRefusDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        System.out.println("Archivage refus exécuté");

        // Exemple logique :
        Long demandeId = (Long) execution.getVariable("demandeId");

        System.out.println("Archivage de la demande refusée ID = " + demandeId);
    }
}