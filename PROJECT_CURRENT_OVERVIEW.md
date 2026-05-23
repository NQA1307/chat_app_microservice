# Discord Clone Project - Current Overview

> Cap nhat: 2026-05-21  
> Backend workspace: `D:\Project\Discord_clone\.claude\worktrees\vibrant-hellman`  
> Frontend workspace: `D:\Project\rediscord-main\rediscord-main`

## 1. Muc tieu project

Project nay la ung dung chat realtime mo phong Discord, gom:

- Dang ky, dang nhap, JWT auth.
- Quan ly user, profile, avatar.
- Friend system va Direct Message.
- Server/Guild system: tao server, quan ly member, role, invite, channel.
- Text channel chat trong server.
- DM chat.
- Voice channel bang LiveKit SFU.
- Media upload qua media service.
- Notification service dang o muc co database/RabbitMQ listener/API, chua phai push realtime day du.
- API Gateway + Eureka service discovery.

## 2. Cong nghe dang su dung

### Backend

- Java 21.
- Spring Boot 3.4.3.
- Spring Cloud Gateway.
- Spring Cloud Netflix Eureka.
- Spring Data JPA.
- PostgreSQL.
- Redis.
- RabbitMQ.
- WebSocket/STOMP cho chat message service.
- LiveKit cho voice channel SFU.
- Gradle multi-module.
- Spring Security.
- JWT.
- Meilisearch cho search user.
- Cloudinary config trong user service, nhung huong moi la day media upload ve `media_service`.
- Swagger/Springdoc da duoc them vao cac service de xem API truc quan.

### Frontend

- Next.js 13 App Router.
- React 18.
- TypeScript.
- Tailwind CSS.
- Zustand state management.
- Radix UI Dialog/Popover/Tooltip.
- React Icons.
- STOMP/SockJS client cho chat realtime.
- LiveKit client cho voice channel.
- Polling tam thoi cho mot so state chua co push event day du, vi du unread DM va channel list update.

### Infrastructure local

`docker-compose.yml` hien co:

- `postgres-user` -> `localhost:5532`, database `user_db`.
- `postgres-server` -> `localhost:5533`, database `server_db`.
- `postgres-message` -> `localhost:5534`, database `message_db`.
- `postgres-media` -> `localhost:5535`, database `media_db`.
- `postgres-notification` -> `localhost:5536`, database `notification_db`.
- `redis` -> `localhost:6380`.
- `rabbitmq` -> AMQP `localhost:5773`, management UI `localhost:15773`.
- `meilisearch` -> `localhost:7700`.
- `livekit` -> `localhost:7880`, RTC TCP `7881`, UDP `7882`.

## 3. Backend module structure

Backend la Gradle multi-module trong `settings.gradle`:

```text
discord-clone
├─ common
├─ config_server
├─ eureka_server
├─ api_gateway
├─ user_service
├─ server_service
├─ message_service
├─ media_service
├─ notification_service
└─ voice_service
```

### `common`

Dung chung giua cac service:

- `ApiResponse`: response wrapper chung.
- `AppException`, `GlobalExceptionHandler`.
- `UlidGenerator`: tao id dang ULID cho message/invite/media khi can.
- `InviteType`, `InviteStatus`.
- `PermissionCode`.
- `MessageSentEvent`: event publish qua RabbitMQ sau khi gui message.

### `api_gateway`

Port mac dinh: `8080`.

Vai tro:

- Entry point cua frontend.
- Route request den cac service qua Eureka:
  - `/api/auth/**` -> user service public.
  - `/api/users/**` -> user service, co JWT.
  - `/api/friends/**` -> user service, co JWT.
  - `/api/servers/**` -> server service, co JWT.
  - `/api/messages/**` -> message service, co JWT.
  - `/api/dm/**` -> message service, co JWT.
  - `/ws/**` -> message service WebSocket.
  - `/api/media/**` -> media service.
  - `/api/notifications/**` -> notification service.
  - `/api/voice/**` -> voice service.
- `JwtAuthGatewayFilterFactory` validate JWT va forward user id qua header `X-User-Id`.
- CORS cho frontend `http://localhost:3000`.

### `eureka_server`

Port mac dinh: `8761`.

Vai tro:

- Service discovery.
- Cac service dang ky vao Eureka.
- Gateway route theo `lb://service-name`.

### `config_server`

Co module rieng nhung hien tai phan config chu yeu van nam trong tung `application.yml` cua service.

### `user_service`

Port mac dinh: `8081`. Database: `user_db`.

Thanh phan chinh:

```text
user_service
├─ controller
│  ├─ AuthController
│  ├─ UserController
│  └─ FriendshipController
├─ service
│  ├─ UserService
│  ├─ RefreshTokenService
│  ├─ FriendshipService
│  ├─ MeiliSearchService
│  └─ CloudinaryService
├─ entity
│  ├─ User
│  └─ Friendship
├─ repository
│  ├─ UserRepository
│  └─ FriendshipRepository
└─ security
   ├─ SecurityConfig
   └─ JwtUtil
```

Chuc nang:

- Register/login.
- JWT access token va refresh token config.
- Lay thong tin current user.
- Cap nhat profile co ban: display name/avatar URL.
- Tim user.
- Friend request, accept/decline, friend list.
- Meilisearch index user.

Trang thai hien tai:

- Auth da dung duoc.
- Profile UI da rut gon: chi sua avatar va display name.
- Friend system da co nen tang.
- Block/remove friend can tiep tuc hoan thien neu can.

### `server_service`

Port mac dinh: `8082`. Database: `server_db`.

Thanh phan chinh:

```text
server_service
├─ controller
│  └─ ServerController
├─ service
│  ├─ ServerService
│  └─ InviteService
├─ entity
│  ├─ Server
│  ├─ ServerMember
│  ├─ Channel
│  └─ Invite
├─ repository
│  ├─ ServerRepository
│  ├─ ServerMemberRepository
│  ├─ ChannelRepository
│  └─ InviteRepository
└─ dto
```

Chuc nang:

- Tao server.
- Khi tao server, tu dong tao:
  - 1 text channel `general`.
  - 1 voice channel `General Voice`.
  - Owner member role `OWNER`.
- Lay danh sach server cua user.
- Lay chi tiet server.
- Lay danh sach member trong server.
- Moi user vao server bang invite.
- Incoming invites, accept invite, decline invite.
- Update/delete server cho owner.
- Cap role member: `OWNER`, `ADMIN`, `MODERATOR`, `GUEST`.
- Xoa member ra khoi server cho owner.
- Tao/sua/xoa channel cho owner.
- Permission validation cho channel:
  - `canAccessChannel(channelId, userId)`.
  - `hasChannelPermission(channelId, userId, PermissionCode)`.

Role/permission hien tai:

- `OWNER`: full quyen.
- `ADMIN`: co mot so permission nhu manage messages/channels/invite members theo switch trong service.
- `MODERATOR`: manage messages/invite members.
- `GUEST`: khong co quyen quan tri.

Ghi chu mo rong:

- Hien tai permission van hardcode theo role trong `ServerService`.
- Neu muon mo rong chuyen nghiep, can tach bang `roles`, `permissions`, `role_permissions`, `member_roles`.

### `message_service`

Port mac dinh: `8084`. Database: `message_db`.

Thanh phan chinh:

```text
message_service
├─ controller
│  ├─ MessageController
│  └─ DirectMessageController
├─ service
│  ├─ MessageService
│  ├─ DirectMessageService
│  ├─ PermissionService
│  ├─ PermissionCacheService
│  └─ MessageEventPublisher
├─ entity
│  ├─ Message
│  ├─ DirectMessage
│  ├─ Conversation
│  └─ ChannelReadState
├─ repository
│  ├─ MessageRepository
│  ├─ DirectMessageRepository
│  ├─ ConversationRepository
│  └─ ChannelReadStateRepository
├─ client
│  └─ ServerServiceClient
└─ config
   ├─ WebSocketConfig
   └─ RabbitEventConfig
```

Chuc nang Text Channel:

- Gui message vao text channel.
- Lay lich su message.
- Sua message.
- Xoa message.
- Check permission truoc khi gui/lay/sua/xoa message.
- Cache permission bang Redis:
  - Message service check Redis permission cache.
  - Neu miss thi goi HTTP sang server service.
  - Cache ket qua.
- WebSocket broadcast message trong channel.
- Publish RabbitMQ `MessageSentEvent`.
- Channel read state:
  - unread count.
  - mention count.
  - last read message.

Chuc nang DM:

- Tao/lay conversation.
- Gui direct message.
- Lay lich su DM.
- Sua/xoa DM.
- Polling frontend de cap nhat tin nhan DM gan realtime.
- DM unread badge hien dua tren notification/unread data frontend lay tu notification service.

Ghi chu:

- ID message va direct message da chuyen sang string/ULID o muc entity/code, can dam bao schema DB cung la varchar neu migrate thu cong.
- Permission validation hien co dung HTTP + Redis cache; RabbitMQ dung cho event bat dong bo, khong nen dung cho request-response permission realtime.

### `media_service`

Port mac dinh: `8085`. Database: `media_db`.

Thanh phan chinh:

```text
media_service
├─ controller
│  └─ MediaController
├─ service
│  └─ MediaService
├─ entity
│  └─ MediaFile
├─ repository
│  └─ MediaFileRepository
└─ dto
   └─ MediaUploadResponse
```

Chuc nang:

- Upload file/image.
- Luu file local trong `media_uploads`.
- Tra ve public URL qua gateway.
- Frontend dang dung media service de upload server icon.

Ghi chu:

- Hien tai storage local, sau nay co the doi sang Cloudinary/S3/MinIO.
- Nen them validate file type, file size, virus scan neu can production.

### `notification_service`

Port mac dinh: `8087`. Database: `notification_db`.

Thanh phan chinh:

```text
notification_service
├─ controller
│  └─ NotificationController
├─ service
│  └─ NotificationService
├─ listener
│  └─ MessageSentEventListener
├─ entity
│  └─ Notification
├─ repository
│  └─ NotificationRepository
└─ config
   └─ RabbitEventConfig
```

Chuc nang:

- Luu notification.
- API lay notification va unread count.
- Rabbit listener nhan `MessageSentEvent`.

Trang thai hien tai:

- Co nen tang DB/API/listener.
- Chua phai push realtime day du.
- DM unread badge frontend dang lay thong tin notification theo polling.
- De notification service thuc su day realtime, can them WebSocket/SSE endpoint va publish event den user online.

### `voice_service`

Port mac dinh: `8086`.

Thanh phan chinh:

```text
voice_service
├─ controller
│  └─ VoiceController
├─ service
│  ├─ VoiceService
│  └─ LiveKitTokenService
├─ client
│  └─ ServerServiceClient
├─ dto
│  ├─ JoinVoiceRequest
│  └─ VoiceTokenResponse
└─ security
   └─ SecurityConfig
```

Chuc nang:

- Tao token join LiveKit room.
- Validate user co quyen access channel qua server service.
- Room naming theo dang `server-{serverId}-channel-{channelId}`.
- Gioi han y tuong ban dau: 10 participants/channel.
- Frontend dung `livekit-client` ket noi vao LiveKit.

LiveKit config:

- File `livekit.yaml`.
- Docker service `livekit`.
- Local URL: `ws://localhost:7880`.

## 4. Frontend structure

Frontend nam tai `D:\Project\rediscord-main\rediscord-main`.

Tong quan:

```text
src
├─ app
│  ├─ login
│  ├─ (main)
│  │  ├─ (dm)
│  │  │  ├─ channels/me
│  │  │  └─ channels/[id]
│  │  └─ servers
│  │     ├─ [serverId]
│  │     └─ [serverId]/channels/[channelId]
│  └─ api/token
├─ components
│  ├─ layout
│  ├─ ui
│  └─ islets
├─ hooks
├─ lib
│  ├─ api
│  ├─ entities
│  ├─ ws
│  └─ utils
└─ state
```

### `src/lib/api`

Client wrapper goi API Gateway:

- `client.ts`: base API client, attach auth token.
- `auth.ts`: login/register.
- `users.ts`: current user, profile.
- `friends.ts`: friend API.
- `servers.ts`: server/channel/member/invite API.
- `messages.ts`: text channel message API va unread state.
- `dm.ts`: direct message API.
- `media.ts`: upload media.
- `notifications.ts`: notification/unread count.
- `voice.ts`: voice token API.

### `src/state`

Zustand stores:

- `auth.ts`: auth token/user.
- `user.ts`: current user info.
- `channel-list.ts`: DM channel list.
- `friend-list.ts`, `friendRequest-list.ts`, `friends-tab.ts`.
- `voice.ts`: LiveKit room, participants, mute/deafen, connected channel.

### Layout/UI

- `layout/sidemenu`: server icon sidebar.
- `layout/sidebar`: left sidebar.
- `layout/page`: main page layout.
- `ui/avatar`, `ui/dialog`, `ui/input`, `ui/list`, `ui/badge`, `ui/popover`, etc.

### Main feature components

- `auth-form`: login/register UI.
- `dm-layout`: DM shell.
- `dm-channel-list`: DM friend/conversation list + unread badge.
- `dm-channel/direct-message-view`: DM chat screen.
- `dm-chat`: DM message bubble UI.
- `server-list`: server creation/list.
- `server-view`: server page, member list, selected channel content.
- `server-channel-list`: server sidebar, channels, invite button, settings, create/edit/delete channel.
- `chat-room`: text channel chat.
- `emoji-picker-button`: emoji picker shared by DM and server text chat.
- `voice-status-footer`: voice connected footer controls.
- `server-invites-panel`: incoming server invites UI.
- `user-settings-modal`: simplified profile/account settings UI.

## 5. Cach cac chuc nang chinh hoat dong

### 5.1 Authentication

1. Frontend login/register qua `/api/auth/**`.
2. API Gateway route public den user service.
3. User service tao JWT.
4. Frontend luu token trong auth store.
5. Cac request protected di qua Gateway.
6. Gateway validate JWT va forward `X-User-Id`.
7. Service doc `X-User-Id` de biet current user.

### 5.2 Friend system va DM

1. User tim/ket ban qua user service/friendship API.
2. Frontend DM list lay friend list.
3. Khi vao DM voi user:
   - Frontend goi `getOrCreateConversation(targetUserId)`.
   - Lay lich su message cua conversation.
4. Gui DM:
   - Frontend POST den message service.
   - Message service luu direct message.
   - Frontend optimistic/upsert message vao UI.
   - DM view polling moi 2.5s de thay tin moi neu user khac gui.
5. DM unread:
   - DM list loader lay notification/unread data.
   - Badge do hien `unreadCount`.
   - Loader polling moi 10s.

### 5.3 Server/Guild

1. User tao server.
2. Server service tao record `servers`.
3. Tao owner trong `server_members` role `OWNER`.
4. Tao channel mac dinh:
   - `general` TEXT.
   - `General Voice` VOICE.
5. Frontend sidebar them server icon.
6. Click server icon se vao text channel mac dinh thay vi trang overview.

### 5.4 Invite vao server

1. Owner/member co UI `Moi ban be`.
2. Frontend lay friend list, search locally.
3. Gui invite den server service.
4. Invite luu trong `invites` status `PENDING`.
5. Receiver xem incoming invites.
6. Accept invite:
   - Check invite pending.
   - Add receiver vao `server_members` role `GUEST`.
   - Update invite status `ACCEPTED`.
   - Frontend them server vao sidebar.

### 5.5 Member list va role

1. Server view goi `GET /api/servers/{id}/members`.
2. Frontend lay user info tung member de hien avatar/display name.
3. Owner co UI doi role member.
4. Owner co UI xoa member khoi server.
5. Service chan:
   - Khong doi/xoa owner.
   - Khong assign owner role.
   - Chi owner moi quan ly role/member.

### 5.6 Channel CRUD

1. Owner bam `+` trong TEXT/VOICE CHANNELS.
2. Frontend mo dialog tao channel.
3. POST `/api/servers/{serverId}/channels`.
4. Owner hover channel de sua/xoa.
5. PATCH `/api/servers/{serverId}/channels/{channelId}` de rename.
6. DELETE `/api/servers/{serverId}/channels/{channelId}` de xoa.
7. Frontend update local state ngay.
8. User khac dang o server se thay channel moi sau polling `getServerById` moi 8s.

### 5.7 Server text chat

1. User vao text channel.
2. Frontend `ChatRoom` dung hook `use-channel-messages`.
3. Lay lich su message tu message service.
4. Gui message:
   - Message service check permission.
   - Permission service doc Redis cache.
   - Neu cache miss, goi server service check access.
   - Save message vao DB.
   - Broadcast WebSocket den channel.
   - Publish RabbitMQ `MessageSentEvent`.
5. UI hien message, avatar, edit/delete.
6. Edit:
   - Chi chu tin nhan duoc sua.
7. Delete:
   - Chu tin nhan hoac role co quyen manage messages.
8. Emoji:
   - DM va server text chat dung chung `EmojiPickerButton`.
   - Picker khong dong sau moi lan chon, co the bam emoji lien tuc.

### 5.8 Unread text channel

1. Message service co `ChannelReadState`.
2. Frontend sidebar lay unread state tung text channel.
3. Badge:
   - Mention count mau do.
   - Unread count mau xam.
4. Khi user doc channel, frontend dispatch event `discordclone:channel-read` de clear badge local.

### 5.9 Media upload va server icon

1. Owner vao server settings.
2. Chon file image.
3. Frontend upload qua media service.
4. Media service luu file local va tra URL.
5. Frontend update server `imageUrl` qua server service.
6. Sidebar server icon dung image URL, object-cover de anh vua khung.

### 5.10 Voice channel

1. User click voice channel trong server sidebar.
2. Frontend goi voice service lay LiveKit token.
3. Voice service:
   - Goi server service check user co access channel.
   - Tao token LiveKit voi room `server-{serverId}-channel-{channelId}`.
4. Frontend dung `livekit-client` join room.
5. Voice footer hien:
   - connected channel.
   - participant count.
   - mute/unmute.
   - deafen/undeafen.
   - leave voice.
6. Local test 2 user cung may co the dung 2 browser/session khac nhau.

### 5.11 Notification

Hien tai:

- Message service publish `MessageSentEvent`.
- Notification service co Rabbit listener.
- Notification service co API lay notification va unread count.
- Frontend polling de hien unread badge.

Can hoan thien them:

- Xac dinh recipientIds day du cho event.
- Tao notification cho DM va mention/server message.
- Mark notification read khi user doc DM/channel.
- Push realtime qua WebSocket/SSE thay vi chi polling.

## 6. Cach cac service giao tiep voi nhau

### Sync HTTP

- Frontend -> API Gateway -> service.
- Message service -> Server service de validate permission/access.
- Voice service -> Server service de validate access voice channel.

### Service discovery

- Cac service dang ky Eureka.
- Gateway route bang `lb://service-name`.

### Async event

- Message service publish RabbitMQ event:
  - exchange `discord.events`.
  - routing key `message.sent`.
- Notification service consume event tu queue.

### Cache

- Redis dung cho permission cache trong message service.
- TTL mac dinh: 300 seconds.

### Realtime

- Message text channel: WebSocket/STOMP.
- DM hien chu yeu polling gan realtime.
- Voice: LiveKit SFU.
- Server channel list update: frontend polling `getServerById` moi 8s.

## 7. Local ports

| Component | Port |
|---|---:|
| Frontend Next.js | 3000 |
| API Gateway | 8080 |
| User Service | 8081 |
| Server Service | 8082 |
| Message Service | 8084 |
| Media Service | 8085 |
| Voice Service | 8086 |
| Notification Service | 8087 |
| Eureka | 8761 |
| PostgreSQL user_db | 5532 |
| PostgreSQL server_db | 5533 |
| PostgreSQL message_db | 5534 |
| PostgreSQL media_db | 5535 |
| PostgreSQL notification_db | 5536 |
| Redis | 6380 |
| RabbitMQ AMQP | 5773 |
| RabbitMQ UI | 15773 |
| Meilisearch | 7700 |
| LiveKit | 7880 |

## 8. Cach chay local

### Backend infrastructure

```powershell
docker compose up -d
```

Neu LiveKit loi, kiem tra:

```powershell
docker logs discord_clone_livekit
```

### Backend services

Chay lan luot:

```powershell
.\gradlew.bat :eureka_server:bootRun
.\gradlew.bat :api_gateway:bootRun
.\gradlew.bat :user_service:bootRun
.\gradlew.bat :server_service:bootRun
.\gradlew.bat :message_service:bootRun
.\gradlew.bat :media_service:bootRun
.\gradlew.bat :notification_service:bootRun
.\gradlew.bat :voice_service:bootRun
```

Kiem tra compile rieng:

```powershell
.\gradlew.bat :server_service:compileJava
.\gradlew.bat :message_service:compileJava
```

### Frontend

Trong `D:\Project\rediscord-main\rediscord-main`:

```powershell
npm run dev
```

Typecheck:

```powershell
npx.cmd tsc --noEmit
```

## 9. Database core tables hien tai

### user_db

- `users`
- `friendships`
- refresh token/session tables neu da tao tu entity.

### server_db

- `servers`
- `server_members`
- `channels`
- `invites`

### message_db

- `messages`
- `direct_messages`
- `conversations`
- `channel_read_states`

### media_db

- `media_files`

### notification_db

- `notifications`

## 10. Nhung viec da lam tot

- Tach microservices kha ro.
- Co API Gateway va JWT protected route.
- Co service discovery.
- Co server/member/channel/invite flow.
- Co text chat va DM chat.
- Co permission validation cho message.
- Co Redis permission cache.
- Co RabbitMQ event cho message sent.
- Co LiveKit voice channel hoat dong.
- Co media service cho upload.
- Frontend da co UI tuong doi giong Discord:
  - server sidebar.
  - DM list.
  - server channel list.
  - member list.
  - chat room.
  - voice footer.
  - settings modal.

## 11. Nhung diem con yeu/can hoan thien

### Realtime

- Server channel list dang polling, nen chuyen sang WebSocket/SSE event.
- DM dang polling, nen chuyen sang WebSocket cho DM.
- Notification service chua push realtime.

### Permission

- Permission con hardcode theo enum role.
- Nen xay bang role/permission rieng:
  - `roles`
  - `permissions`
  - `role_permissions`
  - `server_member_roles`

### Notification

- Can xac dinh recipient cho message event.
- Can mark read ro rang cho DM/channel.
- Can notification settings: mute server/channel, mention only, desktop/email.

### Media

- Can validate MIME type.
- Can resize avatar/server icon.
- Can xoa file cu khi update avatar/server icon.
- Can chuyen local storage sang object storage neu deploy.

### Security

- Refresh token rotation chua nen tang hoan chinh.
- CSRF/cookie flow can ro neu dung HttpOnly cookie.
- Rate limiting chua co o gateway.
- File validation can bo sung.
- Secrets dang nam trong yml dev, production can dung env/secret manager.

### Observability

- Chua co central logging/tracing/metrics.
- Chua co health dashboard rieng.

## 12. De xuat uu tien tiep theo

1. Hoan thien notification realtime:
   - Notification DB + event recipient.
   - WebSocket/SSE push den user online.
   - Mark read khi doc DM/channel.

2. Hoan thien permission system:
   - Tao bang role/permission.
   - Migration tu hardcode sang DB permission.
   - UI cap quyen role.

3. Hoan thien DM realtime:
   - WebSocket topic theo conversation.
   - Sender/receiver update message ngay.
   - Read receipt.

4. Hoan thien moderation:
   - Kick/ban/leave server.
   - Audit log.
   - Delete message theo permission.

5. Hoan thien media:
   - Avatar user qua media service.
   - Attachments trong message.
   - Image preview.

6. Them rate limiting va protected route chat hon:
   - Gateway rate limit Redis.
   - Endpoint-level permission policy.
   - Swagger grouping/documentation.

