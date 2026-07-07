package de.kugidev.battleoftheuniverse.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/player")
@PreAuthorize("hasRole('PLAYER')")
public class PlayerController {

    @GetMapping("/dashboard")
    public String playerDashboard(Model model) {
        model.addAttribute("pageTitle", "Spieler Dashboard - Battle of the Universe");
        model.addAttribute("welcomeMessage", "Willkommen im Weltraum, Kommandant!");
        return "player/dashboard";
    }

    @GetMapping("/planets")
    public String managePlanets(Model model) {
        model.addAttribute("pageTitle", "Planeten - Battle of the Universe");
        return "player/planets";
    }

    @GetMapping("/fleet")
    public String manageFleet(Model model) {
        model.addAttribute("pageTitle", "Flotte - Battle of the Universe");
        return "player/fleet";
    }

    @GetMapping("/research")
    public String research(Model model) {
        model.addAttribute("pageTitle", "Forschung - Battle of the Universe");
        return "player/research";
    }
}
