package com.dormhub.controller;

import com.dormhub.model.SeniorResidence;
import com.dormhub.service.SeniorResidenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SeniorResidenceController {

    @Autowired
    private SeniorResidenceService seniorResidenceService;

    @GetMapping("/admin/senior-residence")
    public String listSeniorResidence(Model model) {
        List<SeniorResidence> seniorResidenceList = seniorResidenceService.getAllSeniorResidence();

        model.addAttribute("seniorResidenceList", seniorResidenceList);

        return "admin/SeniorResidence/index";
    }

    @PostMapping("/admin/senior-residence/delete")
    public String deleteSeniorResidence(@RequestParam("id") int id) {
        seniorResidenceService.deleteSeniorResidence(id);

        return "redirect:/admin/senior-residence";
    }
}
