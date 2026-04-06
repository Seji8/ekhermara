package com.example.pfe.services;

import com.example.pfe.dto.CreateUserRequest;
import com.example.pfe.dto.CreateUserResponse;
import com.example.pfe.dto.UpdateUserRequest;
import com.example.pfe.models.DemandeTeletravail;
import com.example.pfe.models.Equipe;
import com.example.pfe.models.User;
import com.example.pfe.repository.DemandeTeletravailRepository;
import com.example.pfe.repository.EquipeRepository;
import com.example.pfe.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EquipeRepository equipeRepository;
    private final EmailService emailService;
    private final DemandeTeletravailRepository demandeTeletravailRepository;

    // Character sets for password generation
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%&*_";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom random = new SecureRandom();

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService, EquipeRepository equipeRepository, DemandeTeletravailRepository demandeTeletravailRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.equipeRepository = equipeRepository;
        this.demandeTeletravailRepository = demandeTeletravailRepository;
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Vérifier si le matricule existe déjà
        if (request.getMatricule() != null && !request.getMatricule().isEmpty()
                && userRepository.findByMatricule(request.getMatricule()).isPresent()) {
            throw new RuntimeException("Matricule déjà utilisé");
        }

        // Generate random password
        String plainPassword = generateRandomPassword(12);

        User user = new User();
        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setPassword(passwordEncoder.encode(plainPassword));

        // Informations personnelles
        user.setTelephone(request.getTelephone());
        user.setAdresse(request.getAdresse());
        user.setVille(request.getVille());

        // Informations professionnelles
        user.setMatricule(request.getMatricule());

        // Assigner l'équipe si spécifiée
        Equipe assignedEquipe = null;
        if (request.getEquipeId() != null) {
            assignedEquipe = equipeRepository.findById(request.getEquipeId())
                    .orElseThrow(() -> new RuntimeException("Équipe non trouvée"));
            user.setEquipe(assignedEquipe);
            System.out.println("🔍 Équipe assignée: " + assignedEquipe.getNom() + " (ID: " + assignedEquipe.getId() + ")");
        }

        User savedUser = userRepository.save(user);
        System.out.println("🔍 Utilisateur sauvegardé: " + savedUser.getNom() + ", Role: " + savedUser.getRole());
        System.out.println("🔍 Est chef? " + savedUser.estChef());
        System.out.println("🔍 AssignedEquipe est null? " + (assignedEquipe == null));

        // Si l'utilisateur est un chef d'équipe et a une équipe assignée
        if (savedUser.estChef() && assignedEquipe != null) {
            System.out.println("🔍 Tentative d'assignation du chef...");

            // Vérifier si l'équipe a déjà un chef
            if (assignedEquipe.getChef() != null) {
                System.out.println("⚠️ L'équipe a déjà un chef: " + assignedEquipe.getChef().getNom());
                throw new RuntimeException("Cette équipe a déjà un chef: " + assignedEquipe.getChef().getNom());
            }

            // Définir l'utilisateur comme chef de l'équipe
            assignedEquipe.setChef(savedUser);
            equipeRepository.save(assignedEquipe);
            System.out.println("✅ Chef d'équipe assigné: " + savedUser.getNom() + " -> Équipe: " + assignedEquipe.getNom());
        } else {
            System.out.println("🔍 Condition non remplie: estChef=" + savedUser.estChef() + ", assignedEquipe=" + (assignedEquipe != null));
        }

        // Send email with credentials
        try {
            emailService.sendAccountEmail(
                    savedUser.getEmail(),
                    savedUser.getEmail(),
                    plainPassword
            );
        } catch (Exception e) {
            System.err.println("Failed to send email to " + savedUser.getEmail() + ": " + e.getMessage());
        }

        return new CreateUserResponse(savedUser);
    }

    private String generateRandomPassword(int length) {
        StringBuilder password = new StringBuilder(length);

        // Ensure at least one character from each category
        password.append(getRandomChar(UPPER));
        password.append(getRandomChar(LOWER));
        password.append(getRandomChar(DIGITS));
        password.append(getRandomChar(SPECIAL));

        // Fill the rest with random characters from all categories
        for (int i = 4; i < length; i++) {
            password.append(getRandomChar(ALL_CHARS));
        }

        // Shuffle the password to avoid predictable pattern
        return shuffleString(password.toString());
    }

    private char getRandomChar(String characterSet) {
        return characterSet.charAt(random.nextInt(characterSet.length()));
    }

    private String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }

    // Rest of your methods remain the same
    public List<CreateUserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(CreateUserResponse::new)
                .collect(Collectors.toList());
    }

    public CreateUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return new CreateUserResponse(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public CreateUserResponse updateUser(Long id, UpdateUserRequest request) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        User user = optionalUser.get();

        // Sauvegarder l'ancienne équipe pour référence
        Equipe oldEquipe = user.getEquipe();
        boolean wasChef = user.estChef();

        if (request.getNom() != null && !request.getNom().isBlank()) {
            user.setNom(request.getNom());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                throw new RuntimeException("Email déjà utilisé par un autre utilisateur");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        // Mettre à jour les nouveaux champs
        if (request.getTelephone() != null) {
            user.setTelephone(request.getTelephone());
        }
        if (request.getAdresse() != null) {
            user.setAdresse(request.getAdresse());
        }
        if (request.getVille() != null) {
            user.setVille(request.getVille());
        }
        if (request.getMatricule() != null) {
            user.setMatricule(request.getMatricule());
        }

        // Gérer le changement d'équipe
        Equipe newEquipe = null;
        if (request.getEquipeId() != null) {
            newEquipe = equipeRepository.findById(request.getEquipeId())
                    .orElseThrow(() -> new RuntimeException("Équipe non trouvée"));
            user.setEquipe(newEquipe);
        } else {
            user.setEquipe(null);
        }

        User updatedUser = userRepository.save(user);

        // Gérer la relation chef-équipe
        if (updatedUser.estChef() && updatedUser.getEquipe() != null) {
            Equipe equipe = updatedUser.getEquipe();

            // Si l'équipe a déjà un chef différent, lever une erreur
            if (equipe.getChef() != null && !equipe.getChef().getId().equals(updatedUser.getId())) {
                throw new RuntimeException("Cette équipe a déjà un chef: " + equipe.getChef().getNom());
            }

            // Assigner l'utilisateur comme chef
            equipe.setChef(updatedUser);
            equipeRepository.save(equipe);
            System.out.println("✅ Utilisateur promu chef d'équipe: " + updatedUser.getNom());
        }
        // Si l'utilisateur n'est plus chef mais était chef avant, retirer son rôle de chef
        else if (wasChef && !updatedUser.estChef() && oldEquipe != null) {
            if (oldEquipe.getChef() != null && oldEquipe.getChef().getId().equals(updatedUser.getId())) {
                oldEquipe.setChef(null);
                equipeRepository.save(oldEquipe);
                System.out.println("❌ Utilisateur retiré du poste de chef: " + updatedUser.getNom());
            }
        }

        return new CreateUserResponse(updatedUser);
    }


    @Transactional
    public void deleteUser(Long id) {
        // Vérifier si l'utilisateur existe
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 1. Supprimer toutes les demandes de l'utilisateur
        List<DemandeTeletravail> demandes = demandeTeletravailRepository.findByUtilisateurId(id);
        if (!demandes.isEmpty()) {
            demandeTeletravailRepository.deleteAll(demandes);
            System.out.println("✅ " + demandes.size() + " demande(s) supprimée(s) pour l'utilisateur ID: " + id);
        }

        // 2. Si l'utilisateur est chef d'équipe, retirer son statut de chef
        if (user.estChef() && user.getEquipe() != null) {
            Equipe equipe = user.getEquipe();
            if (equipe.getChef() != null && equipe.getChef().getId().equals(id)) {
                equipe.setChef(null);
                equipeRepository.save(equipe);
                System.out.println("✅ Chef d'équipe retiré de l'équipe: " + equipe.getNom());
            }
        }

        // 3. Dissocier l'utilisateur de son équipe
        if (user.getEquipe() != null) {
            user.setEquipe(null);
            userRepository.save(user);
        }

        // 4. Supprimer l'utilisateur
        userRepository.deleteById(id);
        System.out.println("✅ Utilisateur supprimé avec succès: " + user.getNom());
    }
    @Transactional
    public void deleteUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            userRepository.delete(user.get());
        } else {
            throw new RuntimeException("Utilisateur non trouvé");
        }
    }

    public String generateTemporaryPassword() {
        return generateRandomPassword(10);
    }

    @Transactional
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<CreateUserResponse> getUsersByRole(User currentUser) {
        // Si ADMIN, voir tous les utilisateurs
        if (currentUser.estAdmin()) {
            return getAllUsers();
        }
        // Si CHEF_EQUIPE, voir seulement les membres de son équipe
        if (currentUser.estChef()) {
            // Trouver l'équipe dont il est le chef
            Equipe equipe = equipeRepository.findByChef(currentUser)
                    .orElse(null);

            if (equipe == null) {
                // Si le chef n'a pas d'équipe assignée, retourner une liste vide
                return List.of();
            }

            // Retourner tous les membres de l'équipe
            return equipe.getMembres().stream()
                    .map(CreateUserResponse::new)
                    .collect(Collectors.toList());
        }

        // Pour RH, EMPLOYE, retourner une liste vide
        return List.of();
    }

    public Equipe getEquipeForChef(User chef) {
        if (!chef.estChef()) {
            throw new RuntimeException("L'utilisateur n'est pas un chef d'équipe");
        }

        return equipeRepository.findByChef(chef)
                .orElseThrow(() -> new RuntimeException("Aucune équipe trouvée pour ce chef"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'email: " + email));
    }
}