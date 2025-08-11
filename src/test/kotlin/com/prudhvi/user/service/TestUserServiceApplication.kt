package com.prudhvi.user.service

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<UserServiceApplication>().with(TestcontainersConfiguration::class).run(*args)
}
