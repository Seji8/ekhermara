
package com.example.pfe.services;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    // Méthode pour Camunda (obligatoire pour JavaDelegate)
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String chefEmail = (String) execution.getVariable("chefEmail");
        String reason = (String) execution.getVariable("reason");
        Integer days = (Integer) execution.getVariable("days");

        // Récupérer le statut
        Boolean approved = (Boolean) execution.getVariable("approved");
        String status = approved != null ? (approved ? "APPROUVÉE" : "REJETÉE") : "EN ATTENTE";

        logger.info("Envoi notification email pour la demande de télétravail");
        logger.info("Email: {}, Jours: {}, Statut: {}", chefEmail, days, status);

        sendNotificationEmail(chefEmail, reason, days, status);
    }

    // Méthode pour l'envoi d'email de notification
    public void sendNotificationEmail(String to, String reason, Integer days, String status) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("📧 Mise à jour de votre demande de télétravail - RemoteFlow");

            String emailContent = String.format(
                    "Bonjour,\n\n" +
                            "Votre demande de télétravail a été mise à jour.\n\n" +
                            "═══════════════════════════════════════\n" +
                            "📋 DÉTAILS DE LA DEMANDE:\n" +
                            "═══════════════════════════════════════\n" +
                            "• Motif: %s\n" +
                            "• Nombre de jours: %d\n" +
                            "• Statut: %s\n" +
                            "═══════════════════════════════════════\n\n" +
                            "🌐 Connectez-vous à RemoteFlow pour plus de détails:\n" +
                            "%s/dashboard\n\n" +
                            "Cordialement,\n" +
                            "L'équipe RemoteFlow",
                    reason != null ? reason : "Non spécifié",
                    days != null ? days : 0,
                    status,
                    frontendUrl
            );

            message.setText(emailContent);

            logger.info("Envoi email à: {}", to);
            mailSender.send(message);
            logger.info("Email envoyé avec succès à: {}", to);

        } catch (Exception e) {
            logger.error("Échec d'envoi d'email à: {} - {}", to, e.getMessage());
            // Ne pas lancer d'exception pour ne pas bloquer le processus
        }
    }

    // Méthode pour l'envoi d'email de création de compte
    public void sendAccountEmail(String to, String email, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("🎉 Votre compte RemoteFlow est prêt !");

            String resetLink = frontendUrl + "/forgot-password";

            String emailContent = String.format(
                    "Bonjour,\n\n" +
                            "Bienvenue sur RemoteFlow ! Votre compte a été créé avec succès.\n\n" +
                            "═══════════════════════════════════════\n" +
                            "🔑 VOS IDENTIFIANTS DE CONNEXION:\n" +
                            "═══════════════════════════════════════\n" +
                            "Email:    %s\n" +
                            "Mot de passe: %s\n" +
                            "═══════════════════════════════════════\n\n" +
                            "🌐 Connectez-vous ici: %s/login\n\n" +
                            "⚠️ IMPORTANT: Pour des raisons de sécurité, veuillez changer votre mot de passe après la connexion.\n\n" +
                            "🔒 Mot de passe oublié ? Vous pouvez toujours le réinitialiser ici:\n" +
                            "%s\n\n" +
                            "Cordialement,\n" +
                            "L'équipe RemoteFlow",
                    email, password, frontendUrl, resetLink
            );

            message.setText(emailContent);

            logger.info("Envoi d'email de création de compte à: {}", to);
            mailSender.send(message);
            logger.info("Email envoyé avec succès à: {}", to);

        } catch (Exception e) {
            logger.error("Échec d'envoi d'email à: {} - {}", to, e.getMessage());
            throw new RuntimeException("Échec d'envoi d'email: " + e.getMessage());
        }
    }
}