package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ims.com/user")
public class EmployeeApiController {

    @GetMapping("/view-stocks")
    public String viewFreeSessions() {
        return "Stock will be visible to USER";
    }

    @GetMapping("/add-category")
    public String viewSchedule() {
        return "Category is added";
    }
  
}