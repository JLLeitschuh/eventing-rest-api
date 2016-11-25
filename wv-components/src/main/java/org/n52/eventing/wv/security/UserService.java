/*
 * Copyright (C) 2016-2016 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as publishedby the Free
 * Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of the
 * following licenses, the combination of the program with the linked library is
 * not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed under
 * the aforementioned licenses, is permitted by the copyright holders if the
 * distribution is compliant with both the GNU General Public License version 2
 * and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package org.n52.eventing.wv.security;

import org.n52.eventing.wv.model.UserWrapper;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.n52.eventing.rest.users.User;
import org.n52.eventing.security.NotAuthenticatedException;
import org.n52.eventing.security.SecurityService;
import org.n52.eventing.wv.dao.DatabaseException;
import org.n52.eventing.wv.model.Group;
import org.n52.eventing.wv.model.WvUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.n52.eventing.wv.dao.GroupDao;
import org.n52.eventing.wv.dao.UserDao;

/**
 * @since 4.0.0
 *
 */
public class UserService implements AuthenticationProvider, Serializable, SecurityService {
    private static final long serialVersionUID = -3207103212342510378L;

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public User resolveCurrentUser() throws NotAuthenticatedException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            String username;

            if (principal instanceof UserPrinciple) {
                username = ((UserPrinciple)principal).getUser().getName();
            } else {
                username = principal.toString();
            }

            try {
                Optional<WvUser> result = userDao.retrieveUserByName(username);
                if (result.isPresent()) {
                    return new UserWrapper(result.get(), containsAdminGroup(result.get().getGroups()));
                }
            } catch (DatabaseException ex) {
                LOG.info("User '{}' not retrievable", username, ex);
            }
        }
        throw new NotAuthenticatedException("No valid user object found");
    }

    @Override
    public UsernamePasswordAuthenticationToken authenticate(Authentication authentication)
            throws AuthenticationException {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) authentication;
        WvUser user = authenticate((String) auth.getPrincipal(), (String) auth.getCredentials());
        return new UsernamePasswordAuthenticationToken(new UserPrinciple(user,
                containsAdminGroup(user.getGroups())), null, createPrincipals(user.getGroups()));
    }

    public WvUser authenticate(final String username, final String password) throws AuthenticationException {
        if (username == null || password == null) {
            throw new BadCredentialsException("Invalid Credentials");
        }

        Optional<WvUser> user;
        try {
            user = userDao.retrieveUserByName(username);
        } catch (DatabaseException ex) {
            LOG.warn("Could not retrieve user: {}", ex.getMessage());
            throw new AuthenticationServiceException(ex.getMessage(), ex);
        }


        if (user == null || !user.isPresent()) {
            throw new BadCredentialsException("Invalid Credentials");
        }

        if (!getPasswordEncoder().matches(password, user.get().getPassword())) {
            throw new BadCredentialsException("Invalid Credentials");
        }

        return user.get();
    }

    @Override
    public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }


    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    private boolean containsAdminGroup(Set<Group> groups) {
        if (groups == null) {
            return false;
        }

        return groups.stream().filter((Group g) -> {
            return "admin".equals(g.getName());
        }).count() > 0;
    }

    private Collection<? extends GrantedAuthority> createPrincipals(Set<Group> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }
        return groups.stream().map((Group t) -> new GroupPrinciple(t)).collect(Collectors.toList());
    }

    public void setGroupDao(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

}
