package com.BaseNode.BaseNode;


//			###############################
//			##      powered by AI        ##
//			###############################


import com.BaseNode.BaseNode.controller.WebControllerImpl;
import com.BaseNode.BaseNode.model.UserEntity;
import com.BaseNode.BaseNode.repository.UserRepository;
import com.BaseNode.BaseNode.request.LoginRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BaseNodeApplicationTests {

	private UserRepository userRepository;
	private HttpSession session;
	private WebControllerImpl controller;
	private PasswordEncoder encoder;
	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		session = mock(HttpSession.class);
		encoder = new BCryptPasswordEncoder();

		controller = new WebControllerImpl(userRepository, encoder);
	}

	@Test
	void loginSuccess_redirectsToHome() {

		Model model = new ConcurrentModel();

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername("testUser");
		loginRequest.setPassword("testPassword");

		BindingResult bindingResult =
				new BeanPropertyBindingResult(loginRequest, "loginRequest");

		UserEntity user =
				new UserEntity("testUser", encoder.encode("testPassword"), "USER");

		when(userRepository.findByUsername("testUser"))
				.thenReturn(Optional.of(user));

		String result =
				controller.processLogin(loginRequest, bindingResult, model, session);

		assertEquals("redirect:/", result);

		verify(session).setAttribute("loggedInUser", "testUser");
	}

	@Test
	void loginFail_wrongPassword() {

		Model model = new ConcurrentModel();

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername("testUser");
		loginRequest.setPassword("wrongPassword");

		BindingResult bindingResult =
				new BeanPropertyBindingResult(loginRequest, "loginRequest");

		UserEntity user =
				new UserEntity("testUser", encoder.encode("correctPassword"), "USER");

		when(userRepository.findByUsername("testUser"))
				.thenReturn(Optional.of(user));

		String result =
				controller.processLogin(loginRequest, bindingResult, model, session);

		assertEquals("login", result);
	}

	@Test
	void loginFail_userNotFound() {

		Model model = new ConcurrentModel();

		LoginRequest loginRequest = new LoginRequest();
		loginRequest.setUsername("unknownUser");
		loginRequest.setPassword("anyPassword");

		BindingResult bindingResult =
				new BeanPropertyBindingResult(loginRequest, "loginRequest");

		when(userRepository.findByUsername("unknownUser"))
				.thenReturn(Optional.empty());

		String result =
				controller.processLogin(loginRequest, bindingResult, model, session);

		assertEquals("login", result);
	}
}