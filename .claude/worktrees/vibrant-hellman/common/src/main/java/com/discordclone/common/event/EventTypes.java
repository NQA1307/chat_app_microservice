package com.discordclone.common.event;

public final class EventTypes {
    private EventTypes() {}

    public static final String DM_MESSAGE_SENT = "dm.message.sent";
    public static final String MESSAGE_SENT = "message.sent";
    public static final String NOTIFICATION_CREATED = "notification.created";
    public static final String NOTIFICATION_READ = "notification.read";
    public static final String AUTH_EMAIL_REQUESTED = "auth.email.requested";
    public static final String MESSAGE_REACTION_UPDATED = "message.reaction.updated";
    public static final String SERVER_INVITE_SENT = "server.invite.sent";
    public static final String SERVER_MEMBER_KICKED = "server.member.kicked";
    public static final String SERVER_MEMBER_LEFT = "server.member.left";
    public static final String FRIEND_REQUEST_SENT = "friend.request.sent";
    public static final String FRIEND_REQUEST_ACCEPTED = "friend.request.accepted";
    public static final String USER_BLOCKED = "user.blocked";
}
