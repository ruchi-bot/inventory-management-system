package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ims.com/admin")
public class AdminApiController {

    @GetMapping("/all-users")
    public String viewAllUsers() {
        return "Admin can view all users";
    }

    @GetMapping("/all-data")
    public String viewAllData() {
        return "Admin has access to everything";
    }
}