package de.kugidev.battleoftheuniverse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "type", defaultValue = "player") String type,
                       @RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {

        model.addAttribute("loginType", type);
        model.addAttribute("isAdmin", "admin".equals(type));

        if (error != null) {
            model.addAttribute("errorMessage", "Ungültiger Benutzername oder Passwort!");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "Sie wurden erfolgreich abgemeldet!");
        }

        return "login";
    }

    @GetMapping("/login/admin")
    public String adminLogin(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        return login("admin", error, logout, model);
    }

    @GetMapping("/login/player")
    public String playerLogin(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        return login("player", error, logout, model);
    }
}
