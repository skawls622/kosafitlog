package com.kosa.fitlog.routine.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.routine.dto.RoutineDTO;
import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.service.RoutineService;
import com.kosa.fitlog.routine.service.RoutineExerciseService;

@Controller
@RequestMapping("/routine")
public class RoutineController {

    private final RoutineService routineService;
    private final RoutineExerciseService routineExerciseService;

    public RoutineController(RoutineService routineService,
            RoutineExerciseService routineExerciseService) {
        this.routineService = routineService;
        this.routineExerciseService = routineExerciseService;
    }

    @GetMapping("/register")
    public String registerForm(HttpServletRequest request) {
        if (getLoginMember(request) == null) {
            return "redirect:/member/login";
        }
        return "routine/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam("routineName") String routineName,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        try {
            routineService.register(loginMember.getMemberId(), routineName, description);
            return "redirect:/routine/list";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("registerError", exception.getMessage());
            return "redirect:/routine/register";
        }
    }

    @GetMapping("/list")
    public String list(HttpServletRequest request, Model model) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        model.addAttribute("routines", routineService.getRoutines(loginMember.getMemberId()));
        return "routine/list";
    }

    @GetMapping("/read")
    public String read(@RequestParam("routineId") Long routineId, HttpServletRequest request, Model model) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        RoutineDTO routine = routineService.getRoutine(routineId, loginMember.getMemberId());
        if (routine == null) {
            return "redirect:/routine/list?notFound";
        }

        model.addAttribute("routine", routine);
        model.addAttribute("routineExercises",
                routineExerciseService.getRoutineExercises(routineId));
        return "routine/read";
    }

    @GetMapping("/modify")
    public String modifyForm(@RequestParam("routineId") Long routineId,
            HttpServletRequest request, Model model) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        RoutineDTO routine = routineService.getRoutine(routineId, loginMember.getMemberId());
        if (routine == null) {
            return "redirect:/routine/list?notFound";
        }

        model.addAttribute("routine", routine);
        return "routine/modify";
    }

    @PostMapping("/modify")
    public String modify(
            @RequestParam("routineId") Long routineId,
            @RequestParam("routineName") String routineName,
            @RequestParam(value = "description", required = false) String description,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        try {
            boolean modified = routineService.modify(
                    loginMember.getMemberId(), routineId, routineName, description);
            if (!modified) {
                return "redirect:/routine/list?notFound";
            }
            return "redirect:/routine/read?routineId=" + routineId;
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("modifyError", exception.getMessage());
            return "redirect:/routine/modify?routineId=" + routineId;
        }
    }

    @PostMapping("/remove")
    public String remove(@RequestParam("routineId") Long routineId, HttpServletRequest request) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        routineService.remove(routineId, loginMember.getMemberId());
        return "redirect:/routine/list";
    }

    @GetMapping("/exercise/add")
    public String exerciseAddForm(@RequestParam("routineId") Long routineId,
            HttpServletRequest request, Model model) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        RoutineDTO routine = routineService.getRoutine(routineId, loginMember.getMemberId());
        if (routine == null) {
            return "redirect:/routine/list?notFound";
        }

        model.addAttribute("routine", routine);
        model.addAttribute("exercises", routineExerciseService.getAllExercises());
        Set<Long> addedExerciseIds = routineExerciseService.getRoutineExercises(routineId).stream()
                .map(RoutineExerciseDTO::getExerciseId)
                .collect(Collectors.toSet());
        model.addAttribute("addedExerciseIds", addedExerciseIds);
        return "routine/exercise-add";
    }

    @PostMapping("/exercise/add")
    public String addExercise(
            @RequestParam("routineId") Long routineId,
            @RequestParam(value = "exerciseIds", required = false) List<Long> exerciseIds,
            @RequestParam Map<String, String> formValues,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        Map<Long, String> memos = new HashMap<>();
        if (exerciseIds != null) {
            for (Long exerciseId : exerciseIds) {
                memos.put(exerciseId, formValues.get("memos[" + exerciseId + "]"));
            }
        }

        try {
            boolean added = routineExerciseService.add(
                    loginMember.getMemberId(), routineId, exerciseIds, memos);
            if (!added) {
                return "redirect:/routine/list?notFound";
            }
            return "redirect:/routine/read?routineId=" + routineId;
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("exerciseError", exception.getMessage());
            return "redirect:/routine/exercise/add?routineId=" + routineId;
        } catch (DataAccessException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("exerciseError", "운동 추가에 실패했습니다. 다시 시도해 주세요.");
            return "redirect:/routine/exercise/add?routineId=" + routineId;
        }
    }

    @PostMapping("/exercise/remove")
    public String removeExercise(
            @RequestParam("routineId") Long routineId,
            @RequestParam("routineExerciseId") Long routineExerciseId,
            HttpServletRequest request) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        boolean removed = routineExerciseService.remove(
                loginMember.getMemberId(), routineId, routineExerciseId);
        if (!removed) {
            return "redirect:/routine/list?notFound";
        }
        return "redirect:/routine/read?routineId=" + routineId;
    }

    private LoginMember getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (LoginMember) session.getAttribute("loginMember");
    }
}
