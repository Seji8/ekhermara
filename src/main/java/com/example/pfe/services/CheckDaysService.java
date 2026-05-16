package com.example.pfe.services;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CheckDaysService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(CheckDaysService.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // Récupérer le nombre de jours
        Integer days = (Integer) execution.getVariable("days");

        if (days == null) {
            days = 0;
        }

        execution.setVariable("days", days);

        logger.info("=== CheckDaysService ===");
        logger.info("Nombre de jours demandés: {}", days);

        // Logique supplémentaire : alerte si plus de 10 jours
        if (days > 10) {
            execution.setVariable("highDaysWarning", true);
            logger.warn("Attention: Demande de {} jours (>10)", days);
        }

        // Logique pour jours normaux
        if (days <= 5) {
            logger.info("Demande auto-approuvée ({} jours <= 5)", days);
        } else if (days <= 10) {
            logger.info("Demande nécessite approbation du chef ({} jours)", days);
        } else {
            logger.warn("Demande exceptionnelle de {} jours - nécessite approbation spéciale", days);
        }
    }
}