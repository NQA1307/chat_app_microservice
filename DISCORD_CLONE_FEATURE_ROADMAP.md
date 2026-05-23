# Discord Clone Feature Roadmap

File nay luu danh sach tinh nang can co cho du an chat realtime mo phong Discord, trang thai hien tai, va thu tu nen phat trien tiep theo.

## Trang Thai Tong Quan

Neu so voi mot Discord clone day du, du an hien tai dat khoang 30-35%.

Neu chi tinh MVP chat app gom auth, user, friend, server, invite, DM, text channel chat, du an hien tai dat khoang 65-70%.

## 1. Authentication & Account

- [x] Dang ky
- [x] Dang nhap
- [ ] Dang xuat hoan chinh tren backend/client
- [x] Refresh token co ban
- [x] JWT authentication
- []  Verify email
- [ ] Quen mat khau
- [ ] Reset password
- [ ] Doi mat khau
- [ ] 2FA/OTP
- [ ] Quan ly session/device

## 2. User System

- [x] Profile user
- [x] Avatar
- [ ] Custom status
- [ ] Online/offline
- [ ] Last seen
- [x] Friend system co ban
- [ ] Block user
- [x] Search user bang Meilisearch
- [x] User settings UI co ban

## 3. Server/Guild System

- [x] Tao server
- [ ] Xoa server
- [ ] Edit server
- [x] Server icon co ban
- [ ] Server banner
- [x] Moi thanh vien bang direct invite
- [ ] Join bang invite link public
- [ ] Leave server
- [ ] Kick member
- [ ] Ban member
- [x] Role co ban: OWNER, ADMIN, GUEST
- [ ] Permission system day du
- [ ] Server settings
- [x] Danh sach member trong server
- [x] Incoming server invites UI
- [x] Accept invite va server xuat hien tren sidebar

## 4. Channel System

- [x] Text channel
- [x] Tao channel backend co ban
- [ ] Edit channel
- [ ] Delete channel
- [ ] Public/private channel
- [ ] Permission theo role
- [ ] Pinned messages
- [ ] Channel topic
- [x] Voice channel entity/UI placeholder
- [ ] Join voice
- [ ] Leave voice
- [ ] Mute/unmute
- [ ] Deafen/undeafen
- [ ] Speaking indicator
- [ ] Voice room limit
- [ ] Voice permission

## 5. Messaging System

### Direct Message

- [x] 1-1 chat co ban
- [x] DM conversation list co ban
- [ ] Unread count
- [ ] Last message preview
- [ ] Search DM

### Group Chat

- [ ] Group conversation
- [ ] Add/remove member
- [ ] Group avatar/name
- [ ] Group owner/admin

### Message Features

- [x] Send text message co ban
- [ ] Edit message
- [ ] Delete message
- [ ] Reply message
- [ ] Forward message
- [ ] Copy message
- [ ] Pin message
- [ ] Mention user
- [ ] Mention role
- [ ] Mention everyone
- [ ] Markdown/basic formatting
- [x] Message timestamp co ban

## 6. Message Status & Presence

- [ ] Sending
- [ ] Sent
- [ ] Delivered
- [ ] Read
- [ ] Failed
- [ ] Typing indicator
- [ ] Online presence
- [ ] Idle/Away status

## 7. Media & File Sharing

- [ ] Upload image trong message
- [ ] Upload video
- [ ] Upload file
- [ ] Image preview
- [ ] GIF support
- [ ] Sticker support
- [ ] Emoji support
- [ ] Voice message
- [ ] Drag & drop upload
- [ ] Progress upload
- [ ] Download file

## 8. Emoji / Sticker System

- [ ] Unicode emoji
- [ ] Custom emoji
- [ ] Sticker pack
- [ ] Recent emoji
- [ ] Favorite emoji
- [ ] GIF picker
- [ ] Reaction emoji

## 9. Reaction System

- [ ] React message
- [ ] Remove reaction
- [ ] Count reaction
- [ ] Multiple reactions
- [ ] Realtime reaction update

## 10. Notification System

- [ ] Push notification
- [ ] Mention notification
- [ ] DM notification
- [ ] Unread badge
- [ ] Desktop notification
- [ ] Email notification
- [ ] Mute server/channel
- [ ] Notification settings

## 11. Search System

- [ ] Search message
- [x] Search user
- [ ] Search server
- [ ] Search channel
- [ ] Filter image
- [ ] Filter link
- [ ] Filter file
- [ ] Filter user
- [ ] Filter date

## 12. Realtime Features

- [x] Realtime messaging nen tang WebSocket/STOMP
- [ ] Realtime typing
- [ ] Realtime presence
- [ ] Realtime reaction
- [ ] Realtime read receipt
- [ ] Realtime voice state
- [ ] Realtime notification

## 13. Voice & Video

- [x] Voice channel placeholder
- [ ] WebRTC audio
- [ ] Noise suppression
- [ ] Echo cancellation
- [ ] Push-to-talk
- [ ] Screen sharing
- [ ] Camera streaming
- [ ] Video call
- [ ] Stream quality

## 14. Moderation & Security

- [ ] Role-based permission day du
- [ ] Admin panel
- [ ] Kick/ban
- [ ] Timeout member
- [ ] Message report
- [ ] Anti-spam
- [ ] Rate limit
- [ ] Audit log
- [ ] Profanity filter

## 15. Admin / Management

- [ ] Manage members
- [ ] Manage roles
- [ ] Manage permissions
- [ ] Manage channels
- [ ] Server analytics
- [ ] Storage usage
- [ ] Logs

## 16. Backend Infrastructure

- [x] API Gateway
- [x] User Service
- [x] Server Service
- [x] Message/Chat Service
- [ ] Notification Service
- [ ] Media Service
- [ ] Voice Service
- [ ] Redis cache ung dung ro rang
- [ ] Message Queue
- [x] Docker cho DB/dev infra
- [x] Eureka service discovery
- [x] Common module
- [x] PostgreSQL
- [x] Meilisearch

## 17. Database Design

- [x] users
- [x] friendships
- [x] servers
- [x] server_members
- [ ] roles table rieng
- [ ] permissions table rieng
- [x] channels
- [x] conversations
- [ ] conversation_members cho group chat
- [x] messages
- [ ] message_receipts
- [ ] reactions
- [ ] stickers
- [ ] attachments
- [ ] notifications
- [x] refresh_tokens

## 18. Realtime Architecture

- [x] WebSocket/STOMP co ban
- [ ] WebSocket Gateway rieng
- [ ] Redis Pub/Sub
- [ ] Kafka/RabbitMQ
- [ ] Presence service
- [ ] Voice signaling service

## 19. Performance Features

- [x] Pagination co ban cho messages
- [ ] Cursor pagination
- [ ] Redis cache
- [ ] Optimistic UI hoan chinh
- [ ] Lazy loading day du
- [ ] Virtualized message list
- [ ] CDN media delivery

## 20. Security Features

- [x] JWT
- [ ] HTTPS/WSS trong production
- [ ] Refresh token HttpOnly Cookie
- [ ] Rate limiting
- [ ] Permission validation day du
- [ ] File validation
- [ ] Input sanitization day du
- [ ] Encrypted storage
- [ ] CSRF protection

## 21. Deployment & DevOps

- [ ] Docker Compose full stack
- [ ] Kubernetes
- [ ] CI/CD
- [ ] Nginx reverse proxy
- [ ] Monitoring
- [ ] Logging tap trung
- [ ] Tracing
- [ ] Backup database

## Viec Nen Lam Tiep Theo

Uu tien gan nhat:

1. Hoan thien text channel chat
2. Them permission validation cho message
3. Hien thi avatar/username trong message list
4. Fix/kiem tra realtime WebSocket giua 2 user
5. Them edit/delete message
6. Them unread count va last message preview
7. Them unfriend/block user
8. Sau do moi mo rong group chat, notification, reaction

## Roadmap Gan Nhat

### Phase 1: Text Channel Chat On Dinh

- [x] REST get/send message chay on tren frontend
- [x] Message list hien avatar + username
- [ ] Message input co sending/error state
- [x] WebSocket nhan realtime giua 2 tab/user
- [x] Khong bi duplicate message khi REST va WebSocket cung tra ve

### Phase 2: Permission Cho Message

- [x] Server service expose endpoint validate channel access
- [x] Message service goi server service truoc khi get/send message
- [x] Chan user khong thuoc server doc/gui message

### Phase 3: Message CRUD Co Ban

- [ ] Edit message
- [ ] Delete message soft-delete
- [ ] UI menu message action
- [ ] WebSocket broadcast message updated/deleted

### Phase 4: Friend Controls

- [ ] Unfriend
- [ ] Block user
- [ ] Unblock user
- [ ] Blocked users tab
- [ ] Chan friend request/DM neu bi block
