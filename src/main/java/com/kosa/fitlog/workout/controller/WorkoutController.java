package com.kosa.fitlog.workout.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.workout.dto.WorkoutExerciseDTO;
import com.kosa.fitlog.workout.dto.WorkoutLogDTO;
import com.kosa.fitlog.workout.service.WorkoutService;
import com.kosa.fitlog.workout.service.WorkoutSetService;

@Controller
@RequestMapping("/workout")
public class WorkoutController {

    private final WorkoutService workoutService;
    private final WorkoutSetService workoutSetService;

    public WorkoutController(WorkoutService workoutService,
            WorkoutSetService workoutSetService) {
        this.workoutService = workoutService;
        this.workoutSetService = workoutSetService;
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

        List<WorkoutExerciseDTO> workoutExercises =
                workoutService.getWorkoutExercises(workoutLogId);
        model.addAttribute("workoutLog", workoutLog);
        model.addAttribute("workoutExercises", workoutExercises);
        model.addAttribute("workoutSetsByExerciseId",
                workoutSetService.getSetsByWorkoutExercises(workoutExercises));
        return "workout/record";
    }

    @PostMapping("/complete")
    public String complete(@RequestParam("workoutLogId") Long workoutLogId,
            HttpServletRequest request) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        boolean completed = workoutService.completeWorkout(
                workoutLogId, loginMember.getMemberId());
        if (!completed) {
            return "redirect:/workout/list?notFound";
        }
        return "redirect:/workout/list";
    }

    @GetMapping("/list")
    public String list(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request, Model model) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        Long memberId = loginMember.getMemberId();
        model.addAttribute("workoutLogs", date == null
                ? workoutService.getWorkoutLogs(memberId)
                : workoutService.getCompletedWorkoutLogsByDate(memberId, date));
        model.addAttribute("selectedDate", date);
        return "workout/list";
    }

    @PostMapping("/set/add")
    public String addSet(
            @RequestParam("workoutLogId") Long workoutLogId,
            @RequestParam("workoutExerciseId") Long workoutExerciseId,
            @RequestParam(value = "weight", required = false) BigDecimal weight,
            @RequestParam("reps") Integer reps,
            @RequestParam(value = "memo", required = false) String memo,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        try {
            boolean added = workoutSetService.add(
                    loginMember.getMemberId(), workoutLogId,
                    workoutExerciseId, weight, reps, memo);
            if (!added) {
                return "redirect:/routine/list?notFound";
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("setError", exception.getMessage());
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute(
                    "setError", "세트 저장 중 오류가 발생했습니다.");
        }
        return "redirect:/workout/record?workoutLogId=" + workoutLogId;
    }

    @PostMapping("/set/remove")
    public String removeSet(
            @RequestParam("workoutLogId") Long workoutLogId,
            @RequestParam("workoutExerciseId") Long workoutExerciseId,
            @RequestParam("workoutSetId") Long workoutSetId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        try {
            boolean removed = workoutSetService.remove(
                    loginMember.getMemberId(), workoutLogId,
                    workoutExerciseId, workoutSetId);
            if (!removed) {
                return "redirect:/routine/list?notFound";
            }
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute(
                    "setError", "세트 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/workout/record?workoutLogId=" + workoutLogId;
    }

    private LoginMember getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null
                ? null
                : (LoginMember) session.getAttribute("loginMember");
    }
}
