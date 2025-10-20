package com.example.pp.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class CustomOAuth2UserService implements ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        final ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultReactiveOAuth2UserService();

        return delegate.loadUser(userRequest)
                .flatMap(oAuth2User -> {
                    String registrationId = userRequest.getClientRegistration().getRegistrationId();
                    String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                            .getUserInfoEndpoint().getUserNameAttributeName();

                    OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

                    User user = saveOrUpdate(attributes);

                    return Mono.just(new DefaultOAuth2User(
                            Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                            attributes.getAttributes(),
                            attributes.getNameAttributeKey()));
                });
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .map(entity -> entity.update(attributes.getName(), attributes.getEmail()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }
}
