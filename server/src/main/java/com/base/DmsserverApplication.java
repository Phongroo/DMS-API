package com.base;

import com.base.model.Role;
import com.base.model.User;
import com.base.model.UserRole;
import com.base.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class DmsserverApplication implements CommandLineRunner {
	@Autowired
	private UserService userService;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(DmsserverApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Starting code");
		System.out.println("Starting code2");
		User existingUser = this.userService.getUser("yenna");
		System.out.println("Starting code1");
		if (existingUser == null) {

			User user = new User();
			user.setFirstName("Yen");
			user.setLastName("Na");
			user.setUsername("yenna");
			user.setPassword(this.bCryptPasswordEncoder.encode("123456"));
			user.setEmail("hongphong.12012001@gmail.com");
			user.setProfile("avatar.png");

			Role role = new Role();
			role.setRoleId(44L);
			role.setRoleName("ADMIN");

			Set<UserRole> userRoleSet = new HashSet<>();

			UserRole userRole = new UserRole();
			userRole.setRole(role);
			userRole.setUser(user);

			userRoleSet.add(userRole);

			this.userService.createUser(user, userRoleSet);

			System.out.println("User created");
		} else {
			System.out.println("User already exists");
		}
	}
}
