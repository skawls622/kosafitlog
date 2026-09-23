package com.kosa.fitlog.bodycomposition.controller;

import java.time.LocalDate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.kosa.fitlog.bodycomposition.dto.BodyCompositionDTO;
import com.kosa.fitlog.bodycomposition.service.BodyCompositionService;
import com.kosa.fitlog.member.dto.LoginMember;

@Controller
@RequestMapping("/body-composition")
public class BodyCompositionController {

    private final BodyCompositionService bodyCompositionService;

    public BodyCompositionController(BodyCompositionService bodyCompositionService) {
        this.bodyCompositionService = bodyCompositionService;
    }

    @GetMapping
    public String index(HttpServletRequest request, Model model) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        model.addAttribute("bodyCompositionList",
                bodyCompositionService.findByMemberId(loginMember.getMemberId()));
        model.addAttribute("today", LocalDate.now());
        return "bodycomposition/index";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute BodyCompositionDTO bodyComposition,
            BindingResult bindingResult,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("recordError", "입력값 형식을 확인해 주세요.");
            return "redirect:/body-composition";
        }

        try {
            bodyCompositionService.register(loginMember.getMemberId(), bodyComposition);
            redirectAttributes.addFlashAttribute("recordSuccess", "체성분 기록을 저장했습니다.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("recordError", exception.getMessage());
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("recordError", "체성분 기록 저장 중 오류가 발생했습니다.");
        }
        return "redirect:/body-composition";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("bodyCompositionId") Long bodyCompositionId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        LoginMember loginMember = getLoginMember(request);
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        try {
            boolean deleted = bodyCompositionService.delete(
                    bodyCompositionId, loginMember.getMemberId());
            if (!deleted) {
                redirectAttributes.addFlashAttribute("deleteError", "삭제할 기록을 찾을 수 없습니다.");
            }
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("deleteError", "체성분 기록 삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/body-composition";
    }

    private LoginMember getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (LoginMember) session.getAttribute("loginMember");
    }
}
