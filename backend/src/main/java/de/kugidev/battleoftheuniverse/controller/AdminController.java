package de.kugidev.battleoftheuniverse.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard - Battle of the Universe");
        model.addAttribute("welcomeMessage", "Willkommen im Admin-Bereich!");
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("pageTitle", "Benutzerverwaltung - Battle of the Universe");
        return "admin/users";
    }

    @GetMapping("/settings")
    public String systemSettings(Model model) {
        model.addAttribute("pageTitle", "Systemeinstellungen - Battle of the Universe");
        return "admin/settings";
    }
}
