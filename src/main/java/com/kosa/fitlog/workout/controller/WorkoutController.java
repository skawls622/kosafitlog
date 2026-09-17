package com.kosa.fitlog.workout.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.workout.dto.WorkoutLogDTO;
import com.kosa.fitlog.workout.service.WorkoutService;

@Controller
@RequestMapping("/workout")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping("/start")
    public String start(@RequestParam("routineId") Long routineId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        try {
            Long workoutLogId = workoutService.startWorkout(
                    loginMember.getMemberId(), routineId);
            if (workoutLogId == null) {
                return "redirect:/routine/list?notFound";
            }
            return "redirect:/workout/record?workoutLogId=" + workoutLogId;
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("startError", exception.getMessage());
            return "redirect:/routine/read?routineId=" + routineId;
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute(
                    "startError", "운동 시작 중 오류가 발생했습니다.");
            return "redirect:/routine/read?routineId=" + routineId;
        }
    }

    @GetMapping("/record")
    public String record(@RequestParam("workoutLogId") Long workoutLogId,
            HttpServletRequest request, Model model) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        WorkoutLogDTO workoutLog = workoutService.getWorkoutLog(
                workoutLogId, loginMember.getMemberId());
        if (workoutLog == null) {
            return "redirect:/routine/list?notFound";
        }

        model.addAttribute("workoutLog", workoutLog);
        model.addAttribute("workoutExercises",
                workoutService.getWorkoutExercises(workoutLogId));
        return "workout/record";
    }

    private LoginMember getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null
                ? null
                : (LoginMember) session.getAttribute("loginMember");
    }
}
