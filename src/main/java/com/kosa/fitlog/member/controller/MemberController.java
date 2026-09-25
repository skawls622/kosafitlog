package com.kosa.fitlog.member.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kosa.fitlog.member.dto.MemberDTO;
import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.member.service.MemberService;

@Controller
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/signup")
    public String signupForm(@RequestParam(value = "success", required = false) String success, Model model) {
        model.addAttribute("signupSuccess", success != null);
        return "member/signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute MemberDTO member, RedirectAttributes redirectAttributes) {
        try {
            memberService.signup(member);
            return "redirect:/member/signup?success";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("signupError", exception.getMessage());
            return "redirect:/member/signup";
        }
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        model.addAttribute("loginError", error != null);
        return "member/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("loginId") String loginId,
            @RequestParam("password") String password, HttpServletRequest request) {
        MemberDTO member = memberService.login(loginId, password);
        if (member == null) {
            HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                existingSession.removeAttribute("loginMember");
            }
            return "redirect:/member/login?error";
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute("loginMember",
                new LoginMember(member.getMemberId(), member.getLoginId(), member.getNickname()));
        return "redirect:/main";
    }

    @GetMapping("/login-success")
    public String loginSuccess(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        LoginMember loginMember = session == null ? null : (LoginMember) session.getAttribute("loginMember");
        if (loginMember == null) {
            return "redirect:/member/login";
        }
        model.addAttribute("loginMember", loginMember);
        return "member/login-success";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/main";
    }
}
