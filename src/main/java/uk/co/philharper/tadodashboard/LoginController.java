package uk.co.philharper.tadodashboard;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;



@Controller
@Slf4j
public class LoginController {

    @Autowired
    private TadoService tadoService;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        try {
            tadoService.authenticate(username, password);
            return "redirect:/schedule";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }
    }

    @PostMapping("/signout")
    public String signOut(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}