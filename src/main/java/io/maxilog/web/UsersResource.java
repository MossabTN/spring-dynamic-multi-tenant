package io.maxilog.web;

import io.maxilog.dto.UserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UsersResource {


    @GetMapping("/me")
    public UserDTO me() {
        return new UserDTO();
    }

}
