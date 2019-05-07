/*
 * ******************************************************************************
 *      Cloud Foundry
 *      Copyright (c) [2009-2016] Pivotal Software, Inc. All Rights Reserved.
 *
 *      This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *      You may not use this product except in compliance with the License.
 *
 *      This product includes a number of subcomponents with
 *      separate copyright notices and license terms. Your use of these
 *      subcomponents is subject to the terms and conditions of the
 *      subcomponent's license, as noted in the LICENSE file.
 * ******************************************************************************
 */

package org.cloudfoundry.identity.uaa.scim.event;

import org.cloudfoundry.identity.uaa.audit.AuditEvent;
import org.cloudfoundry.identity.uaa.audit.AuditEventType;
import org.cloudfoundry.identity.uaa.audit.event.AbstractUaaEvent;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.util.JsonUtils;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.springframework.security.core.Authentication;

public class UserModifiedEvent extends AbstractUaaEvent {

    private static final long serialVersionUID = 8139998613071093676L;
    private String userId;
    private String username;
    private String email;
    private AuditEventType eventType;


    protected UserModifiedEvent(String userId, String username, AuditEventType type, Authentication authentication, String zoneId) {
        super(authentication, zoneId);
        this.userId = userId;
        this.username = username;
        this.eventType = type;
    }

    protected UserModifiedEvent(String userId, String username, String email, AuditEventType type, Authentication authentication, String zoneId) {
        super(authentication, zoneId);
        this.userId = userId;
        this.username = username;
        this.eventType = type;
        this.email = email;
    }

    public static UserModifiedEvent userCreated(String userId, String username) {
        return new UserModifiedEvent(
            userId,
            username,
            AuditEventType.UserCreatedEvent,
            getContextAuthentication(), IdentityZoneHolder.getCurrentZoneId());
    }

    public static UserModifiedEvent userModified(String userId, String username) {
        return new UserModifiedEvent(
            userId,
            username,
            AuditEventType.UserModifiedEvent,
            getContextAuthentication(), IdentityZoneHolder.getCurrentZoneId());
    }

    public static UserModifiedEvent userDeleted(String userId, String username) {
        return new UserModifiedEvent(
            userId,
            username,
            AuditEventType.UserDeletedEvent,
            getContextAuthentication(), IdentityZoneHolder.getCurrentZoneId());
    }

    public static UserModifiedEvent userVerified(String userId, String username) {
        return new UserModifiedEvent(
            userId,
            username,
            AuditEventType.UserVerifiedEvent,
            getContextAuthentication(), IdentityZoneHolder.getCurrentZoneId());
    }

    public static UserModifiedEvent emailChanged(String userId, String username, String email) {
        return new UserModifiedEvent(
            userId,
            username,
            email,
            AuditEventType.EmailChangedEvent,
            getContextAuthentication(), IdentityZoneHolder.getCurrentZoneId());
    }

    @Override
    public AuditEvent getAuditEvent() {
        String data = JsonUtils.writeValueAsString(buildDetails());
        return createAuditRecord(
            userId,
            eventType,
            getOrigin(getAuthentication()),
            data);
    }

    private String[] buildDetails() {
        if (AuditEventType.UserCreatedEvent.equals(this.eventType)) {

            // Was this created by a user?
            if(getContextAuthentication().getPrincipal() instanceof UaaPrincipal) {
                UaaPrincipal uaaPrincipal = (UaaPrincipal) getContextAuthentication().getPrincipal();

                return new String[]{
                        "user_id=" + userId,
                        "username=" + username,
                        "createdBy_UserID=" + uaaPrincipal.getId(),
                        "createdBy_UserName=" + uaaPrincipal.getName()
                };
            }

            return new String[]{
                    "user_id=" + userId,
                    "username=" + username,
                    "createdBy_ClientID=" + getContextAuthentication().getPrincipal()
            };
        }
        return new String[]{
                "user_id=" + userId,
                "username=" + username
        };
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

}
