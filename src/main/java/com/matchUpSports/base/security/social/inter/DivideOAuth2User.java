package com.matchUpSports.base.security.social.inter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class DivideOAuth2User implements SocialUserInterface {

    Map<String, Object> attributes;
    Collection<? extends GrantedAuthority> authorities;
    protected OAuth2User oAuth2User;


    public DivideOAuth2User(OAuth2User oAuth2User, Map<String, Object> attributes) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        this.authorities = grantedAuthorities;
        this.oAuth2User = oAuth2User;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

//    @Override
//    public String getPassword() {
//        return UUID.randomUUID().toString().substring(0, 7);
//    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }


}