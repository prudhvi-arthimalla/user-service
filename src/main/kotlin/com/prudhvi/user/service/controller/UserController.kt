package com.prudhvi.user.service.controller

import com.prudhvi.user.service.dto.RegisterUserRequest
import com.prudhvi.user.service.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/v1")
class UserController(
    val userService: UserService
) {
    @PostMapping("/auth/register", consumes = ["application/json"])
    @ResponseStatus(HttpStatus.CREATED)
    fun registerUser(@RequestBody @Valid user: RegisterUserRequest): Mono<Void> {
        return userService.registerUser(user)
    }

    @PostMapping("/auth/verify-email")
    @ResponseStatus(HttpStatus.OK)
    fun verifyEmail(@RequestParam token: String): Mono<Void> {
        return userService.verifyEmail(token)
    }
}