package com.example.social.security.oauth2;

import com.example.social.service.OAuth2VerificationService;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


public class CustomAccessTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

	private static final String INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response";

	@Autowired
	OAuth2VerificationService oAuth2VerificationService;

	@Override
	public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
		ClientRegistration clientRegistration = authorizationGrantRequest.getClientRegistration();

		// Build the authorization code grant request for the token endpoint
		AuthorizationCode authorizationCode = new AuthorizationCode(
				authorizationGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
		URI redirectUri = toURI(authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
		AuthorizationGrant authorizationCodeGrant = new AuthorizationCodeGrant(authorizationCode, redirectUri);
		URI tokenUri = toURI(clientRegistration.getProviderDetails().getTokenUri());

		// Set the credentials to authenticate the client at the token endpoint
		ClientID clientId = new ClientID(clientRegistration.getClientId());
		Secret clientSecret = new Secret(clientRegistration.getClientSecret());
		ClientAuthentication clientAuthentication;
		if (ClientAuthenticationMethod.POST.equals(clientRegistration.getClientAuthenticationMethod())) {
			clientAuthentication = new ClientSecretPost(clientId, clientSecret);
		} else {
			clientAuthentication = new ClientSecretBasic(clientId, clientSecret);
		}

		com.nimbusds.oauth2.sdk.TokenResponse tokenResponse;
		try {
			// Send the Access Token request
			TokenRequest tokenRequest = new TokenRequest(tokenUri, clientAuthentication, authorizationCodeGrant);
			HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
			httpRequest.setAccept(MediaType.APPLICATION_JSON_VALUE);
			httpRequest.setConnectTimeout(30000);
			httpRequest.setReadTimeout(30000);
			HTTPResponse httpResponse = httpRequest.send();
			boolean verified = oAuth2VerificationService.verify(httpResponse, clientRegistration);
			if (!verified) {
				throw new InternalAuthenticationServiceException("Invalid access token!");
			}
			tokenResponse = com.nimbusds.oauth2.sdk.TokenResponse.parse(httpResponse);
		} catch (ParseException | IOException ex) {
			OAuth2Error oauth2Error = new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE,
					"An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: " + ex.getMessage(), null);
			throw new OAuth2AuthorizationException(oauth2Error, ex);
		}

		if (!tokenResponse.indicatesSuccess()) {
			TokenErrorResponse tokenErrorResponse = (TokenErrorResponse) tokenResponse;
			ErrorObject errorObject = tokenErrorResponse.getErrorObject();
			OAuth2Error oauth2Error;
			if (errorObject == null) {
				oauth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR);
			} else {
				oauth2Error = new OAuth2Error(
						errorObject.getCode() != null ? errorObject.getCode() : OAuth2ErrorCodes.SERVER_ERROR,
								errorObject.getDescription(),
								errorObject.getURI() != null ? errorObject.getURI().toString() : null);
			}
			throw new OAuth2AuthorizationException(oauth2Error);
		}

		AccessTokenResponse accessTokenResponse = (AccessTokenResponse) tokenResponse;

		String accessToken = accessTokenResponse.getTokens().getAccessToken().getValue();
		OAuth2AccessToken.TokenType accessTokenType = null;
		if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(accessTokenResponse.getTokens().getAccessToken().getType().getValue())) {
			accessTokenType = OAuth2AccessToken.TokenType.BEARER;
		}
		long expiresIn = accessTokenResponse.getTokens().getAccessToken().getLifetime();

		// As per spec, in section 5.1 Successful Access Token Response
		// https://tools.ietf.org/html/rfc6749#section-5.1
		// If AccessTokenResponse.scope is empty, then default to the scope
		// originally requested by the client in the Authorization Request
		Set<String> scopes;
		if (CollectionUtils.isEmpty(accessTokenResponse.getTokens().getAccessToken().getScope())) {
			scopes = new LinkedHashSet<>(
					authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getScopes());
		} else {
			scopes = new LinkedHashSet<>(
					accessTokenResponse.getTokens().getAccessToken().getScope().toStringList());
		}

		String refreshToken = null;
		if (accessTokenResponse.getTokens().getRefreshToken() != null) {
			refreshToken = accessTokenResponse.getTokens().getRefreshToken().getValue();
		}

		Map<String, Object> additionalParameters = new LinkedHashMap<>(accessTokenResponse.getCustomParameters());

		return OAuth2AccessTokenResponse.withToken(accessToken)
				.tokenType(accessTokenType)
				.expiresIn(expiresIn)
				.scopes(scopes)
				.refreshToken(refreshToken)
				.additionalParameters(additionalParameters)
				.build();
	}

	private static URI toURI(String uriStr) {
		try {
			return new URI(uriStr);
		} catch (Exception ex) {
			throw new IllegalArgumentException("An error occurred parsing URI: " + uriStr, ex);
		}
	}

}
