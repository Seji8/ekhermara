package com.example.pfe.services;

import com.example.pfe.dto.CreateUserRequest;
import com.example.pfe.dto.UpdateUserRequest;
import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Character sets for password generation
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%&*_";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom random = new SecureRandom();

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Generate random password
        String plainPassword = generateRandomPassword(12); // 12 characters

        User user = new User();
        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setPassword(passwordEncoder.encode(plainPassword));

        User savedUser = userRepository.save(user);

        // Send email with credentials
        try {
            emailService.sendAccountEmail(
                    savedUser.getEmail(),
                    savedUser.getEmail(),
                    plainPassword
            );
        } catch (Exception e) {
            // Log error but don't rollback user creation
            System.err.println("Failed to send email to " + savedUser.getEmail() + ": " + e.getMessage());
            // You might want to store that email failed in database
        }

        return savedUser;
    }

    /**
     * Generate a random password with at least one uppercase, one lowercase,
     * one digit, and one special character
     */
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
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        Optional<User> optionalUser = userRepository.findById(id);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        User user = optionalUser.get();

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

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        userRepository.deleteById(id);
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
        return generateRandomPassword(10); // Use the existing method
    }

    @Transactional
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}