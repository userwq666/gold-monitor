package com.goldmonitor.controller;

import com.goldmonitor.dto.RecipientRequest;
import com.goldmonitor.model.Recipient;
import com.goldmonitor.model.SenderMail;
import com.goldmonitor.model.User;
import com.goldmonitor.repository.RecipientRepository;
import com.goldmonitor.service.NotificationService;
import com.goldmonitor.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RecipientRepository recipientRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    public AdminController(RecipientRepository recipientRepository,
                           UserService userService,
                           NotificationService notificationService) {
        this.recipientRepository = recipientRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping("/recipients")
    public List<Recipient> getRecipients() {
        return recipientRepository.findAll();
    }

    @PostMapping("/recipients")
    public Recipient addRecipient(@RequestBody RecipientRequest request) {
        Recipient r = new Recipient();
        r.setEmail(request.getEmail());
        r.setName(request.getName());
        r.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
        return recipientRepository.save(r);
    }

    @PutMapping("/recipients/{id}")
    public Recipient updateRecipient(@PathVariable Long id, @RequestBody RecipientRequest request) {
        Recipient r = recipientRepository.findById(id).orElseThrow();
        r.setEmail(request.getEmail());
        r.setName(request.getName());
        if (request.getEnabled() != null) r.setEnabled(request.getEnabled());
        return recipientRepository.save(r);
    }

    @DeleteMapping("/recipients/{id}")
    public ResponseEntity<?> deleteRecipient(@PathVariable Long id) {
        recipientRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        try {
            return ResponseEntity.ok(userService.createUser(user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/senders")
    public List<SenderMail> getSenderMails() {
        return notificationService.getSenderAccounts();
    }

    @PostMapping("/senders")
    public SenderMail addSenderMail(@RequestBody SenderMail sm) {
        return notificationService.addSenderAccount(sm);
    }

    @DeleteMapping("/senders/{id}")
    public ResponseEntity<?> deleteSenderMail(@PathVariable Long id) {
        notificationService.deleteSenderAccount(id);
        return ResponseEntity.ok().build();
    }
}
