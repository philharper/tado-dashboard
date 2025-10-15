package uk.co.philharper.tadodashboard;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;


@Controller
@Slf4j
public class LoginController {

    @Autowired
    private TadoService tadoService;

    @GetMapping("/login")
    public String loginForm(Model model) {
        var deviceAuthorisationResponse = tadoService.authoriseDevice();
        model.addAttribute("verificationUriComplete", deviceAuthorisationResponse.verificationUriComplete());
        return "verification";
    }

    @GetMapping("/login-wait")
    public String loginWait() {
        var tokenResponse = tadoService.getTokenResponse();
        if (tokenResponse != null) {
            return "redirect:/schedule";
        }
        return "login-wait";
    }

    @PostMapping("/signout")
    public String signOut(HttpSession session) {
        session.invalidate();
        return "logout";
    }
}