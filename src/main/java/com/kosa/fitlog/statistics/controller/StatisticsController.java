package com.kosa.fitlog.statistics.controller;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.statistics.dto.StatisticsDTO;
import com.kosa.fitlog.statistics.service.StatisticsService;

@Controller
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public String statistics(HttpSession session, Model model) {

        LoginMember loginMember =
                (LoginMember) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        Long memberId = loginMember.getMemberId();

        List<StatisticsDTO> statisticsList =
                statisticsService.findExerciseStatistics(memberId);

        model.addAttribute("statisticsList", statisticsList);

        return "statistics/index";
    }
}
