package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.UserClusterResult;
import com.neuedu.movieapi.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user-profile")
@CrossOrigin(origins = "*")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @PostMapping("/cluster")
    public Result<Map<String, Object>> performClustering() {
        return userProfileService.performClustering();
    }

    @GetMapping("/cluster-result")
    public Result<Map<String, Object>> getClusterResult() {
        return userProfileService.getClusterResult();
    }

    @GetMapping("/cluster/{clusterId}/users")
    public Result<PageResult<UserClusterResult>> getClusterUsers(
            @PathVariable int clusterId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return userProfileService.getClusterUsers(clusterId, pageNum, pageSize);
    }
}
