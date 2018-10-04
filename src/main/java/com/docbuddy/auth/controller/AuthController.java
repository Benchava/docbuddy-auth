package com.docbuddy.auth.controller;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.docbuddy.auth.controller.request.LoginRequest;
import com.docbuddy.auth.controller.response.LoginResponse;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;

import docbuddy.users.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;

@RestController
public class AuthController {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private EurekaClient eurekaClient;

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.token.lifetime}")
	private String tokenExpiredDays;

	@RequestMapping("/login")
	@PostMapping
	public LoginResponse login(@RequestBody LoginRequest loginRequest) {

		Application usersApp = eurekaClient.getApplication("USERS");

		InstanceInfo instanceInfo = usersApp.getInstances().get(0);

		List<String> toConcatenate = Arrays.asList("http://", instanceInfo.getIPAddr(), ":",
				new Integer(instanceInfo.getPort()).toString(), "/find");

		String url = String.join("", toConcatenate);

		User userResponse = restTemplate.postForObject(url, loginRequest, User.class);

		LoginResponse loginResponse = new LoginResponse();

		loginResponse.setUserId(userResponse.getId());
		loginResponse.setToken(generateToken(userResponse));

		return loginResponse;

	}

	private String generateToken(User user) {
		Date now = Calendar.getInstance().getTime();

		final String jwt = Jwts.builder().setSubject(user.getUserName()).setIssuedAt(now)
				.signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode(secret)).compact();

		return jwt;

	}

	@RequestMapping("/validateToken")
	@PostMapping
	public boolean validateToken(@RequestBody String token) {

		Claims claims = Jwts.parser().setSigningKey(TextCodec.BASE64.encode(secret)).parseClaimsJws(token).getBody();

		// Validate subject
		if (claims.getSubject() == null) {
			return false;
		}

		// Validate expiraton date

		Calendar calendar = Calendar.getInstance();

		Date generatedDate = claims.getIssuedAt();

		Date today = calendar.getTime();

		calendar.setTime(generatedDate);

		calendar.add(Calendar.DATE, Integer.parseInt(tokenExpiredDays));

		Date expirationDate = calendar.getTime();

		if (expirationDate.before(today)) {
			return false;
		}

		return true;
	}

}
