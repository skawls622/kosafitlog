package com.kosa.fitlog.main.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.workout.service.WorkoutService;

@Controller
public class MainController {

    private final WorkoutService workoutService;

    public MainController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String main(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        LoginMember loginMember = session == null
                ? null : (LoginMember) session.getAttribute("loginMember");
        if (loginMember != null) {
            model.addAttribute("workoutDates",
                    workoutService.getCompletedWorkoutDates(loginMember.getMemberId()));
        }
        return "main";
    }
}
