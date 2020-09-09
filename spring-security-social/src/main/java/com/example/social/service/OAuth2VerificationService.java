package com.example.social.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.social.domain.AuthProvider;
import com.example.social.exceptions.OAuth2AuthenticationProcessingException;
import com.example.social.payload.FacebookAuthResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

@Service
public class OAuth2VerificationService {

	//	@Value("${spring.security.oauth2.client.registration.google.clientId}")
	private final static String GOOGLE_CLIENT_ID = "525487446074-g9oflohi4bksdr7nv3dq2porvnpkidrp.apps.googleusercontent.com";

	private final static String FACEBOOK_APP_ACCESS_TOKEN = "2229296987181438|GaJeC4DQ69in0x7yy--GMah5wlM";

	private final static String GITHUB_CLIENT_ID = "Iv1.8ece6096b59eccc8";
	private final static String GITHUB_CLIENT_SECRET = "87f4d2d6fef075271f0d4af8f7fa6c8a20b32087";

	private final JacksonFactory jacksonFactory = new JacksonFactory();

	public boolean verify(HTTPResponse response, ClientRegistration clientRegistration) throws IOException, ParseException {
		AuthProvider provider = AuthProvider.valueOf(clientRegistration.getRegistrationId());
		if (provider.equals(AuthProvider.google)) {
			return verifyGoogle(response);
		}
		else if (provider.equals(AuthProvider.facebook)) {
			return verifyFacebook(response);
		}
		else if (provider.equals(AuthProvider.github)) {
			return verifyGithub(response);
		}
		return false;
	}

	// Google login server-side authentication
	private boolean verifyGoogle(HTTPResponse response) throws ParseException {
		String tokenValue = response.getContentAsJSONObject().getAsString("id_token");
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), jacksonFactory)
				// Specify the CLIENT_ID of the app that accesses the backend:
				.setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
				.setIssuer("https://accounts.google.com")
				.build();
		// (Receive idTokenString by HTTPS POST)
		try {
			GoogleIdToken idToken = verifier.verify(tokenValue);
			if (idToken!=null) {
				return true;
			}
		} catch (GeneralSecurityException e) {
			throw new OAuth2AuthenticationProcessingException("Invalid Google access token!");
		} catch (IOException e) {
			throw new OAuth2AuthenticationProcessingException("Invalid Google access token!");
		}
		return false;
	}

	// Facebook login server-side authentication
	private boolean verifyFacebook(HTTPResponse httpResponse) throws IOException, ParseException {
		String tokenValue = httpResponse.getContentAsJSONObject().getAsString("access_token");

		ResponseEntity<FacebookAuthResponse> response = null;
		RestTemplate restTemplate = new RestTemplate();
		String url = "https://graph.facebook.com/debug_token";
		url += "?input_token=" + tokenValue;
		url += "&access_token=" + FACEBOOK_APP_ACCESS_TOKEN;
		response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(new HttpHeaders()), FacebookAuthResponse.class);
		boolean is_valid = response.getBody().getData().getIs_valid();
		if (is_valid) {
			return true;
		}
		return false;
	}

	// Github login server-side authentication
	private boolean verifyGithub(HTTPResponse httpResponse) throws ParseException {
		String tokenValue = httpResponse.getContentAsJSONObject().getAsString("access_token");
		ResponseEntity<String> response = null;
		RestTemplate restTemplate = new RestTemplate();
		String credentials = GITHUB_CLIENT_ID + ":" + GITHUB_CLIENT_SECRET;
		String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", "Basic " + encodedCredentials);
		String url = "https://api.github.com/applications/";
		url += GITHUB_CLIENT_ID;
		url += "/tokens/" + tokenValue;
		response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
		if (response.getStatusCode().is2xxSuccessful()) {
			return true;
		}
		return false;	
	}
}
