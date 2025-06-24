/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.service.domain.user.access.admin.controller;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import im.turms.server.common.access.admin.dto.response.DeleteResultDTO;
import im.turms.server.common.access.admin.dto.response.HttpHandlerResult;
import im.turms.server.common.access.admin.dto.response.PaginationDTO;
import im.turms.server.common.access.admin.dto.response.ResponseDTO;
import im.turms.server.common.access.admin.dto.response.UpdateResultDTO;
import im.turms.server.common.access.admin.permission.RequiredPermission;
import im.turms.server.common.access.admin.web.annotation.DeleteMapping;
import im.turms.server.common.access.admin.web.annotation.GetMapping;
import im.turms.server.common.access.admin.web.annotation.PostMapping;
import im.turms.server.common.access.admin.web.annotation.PutMapping;
import im.turms.server.common.access.admin.web.annotation.QueryParam;
import im.turms.server.common.access.admin.web.annotation.RequestBody;
import im.turms.server.common.access.admin.web.annotation.RestController;
import im.turms.server.common.domain.user.po.User;
import im.turms.server.common.infra.property.TurmsPropertiesManager;
import im.turms.server.common.infra.time.DateRange;
import im.turms.server.common.infra.time.DivideBy;
import im.turms.service.domain.common.access.admin.controller.BaseController;
import im.turms.service.domain.message.service.MessageService;
import im.turms.service.domain.user.access.admin.dto.request.AddUserDTO;
import im.turms.service.domain.user.access.admin.dto.request.UpdateUserDTO;
import im.turms.service.domain.user.access.admin.dto.response.UserStatisticsDTO;
import im.turms.service.domain.user.service.PhoneRegistrationService;
import im.turms.service.domain.user.service.UserService;

import static im.turms.server.common.access.admin.permission.AdminPermission.USER_CREATE;
import static im.turms.server.common.access.admin.permission.AdminPermission.USER_DELETE;
import static im.turms.server.common.access.admin.permission.AdminPermission.USER_QUERY;
import static im.turms.server.common.access.admin.permission.AdminPermission.USER_UPDATE;

/**
 * @author James Chen
 */
@RestController("users")
public class UserController extends BaseController {

    private final UserService userService;
    private final MessageService messageService;
    private final PhoneRegistrationService phoneRegistrationService;

    public UserController(
            TurmsPropertiesManager propertiesManager,
            UserService userService,
            MessageService messageService,
            PhoneRegistrationService phoneRegistrationService) {
        super(propertiesManager);
        this.userService = userService;
        this.messageService = messageService;
        this.phoneRegistrationService = phoneRegistrationService;
    }

    @PostMapping
    @RequiredPermission(USER_CREATE)
    public Mono<HttpHandlerResult<ResponseDTO<User>>> addUser(@RequestBody AddUserDTO addUserDTO) {
        Mono<User> addUser = userService.addUser(addUserDTO.id(),
                addUserDTO.password(),
                addUserDTO.name(),
                addUserDTO.intro(),
                addUserDTO.profilePicture(),
                addUserDTO.profileAccessStrategy(),
                addUserDTO.roleId(),
                addUserDTO.registrationDate(),
                addUserDTO.isActive());
        return HttpHandlerResult.okIfTruthy(addUser);
    }

    @PostMapping("send-verification-code")
    public Mono<HttpHandlerResult<ResponseDTO<String>>> sendVerificationCode(
            @QueryParam("phoneNumber") String phoneNumber,
            @QueryParam("ipAddress") String ipAddress) {
        return phoneRegistrationService.sendVerificationCode(phoneNumber, ipAddress)
                .then(Mono.fromCallable(() -> HttpHandlerResult.okIfTruthy("验证码发送成功")));
    }

    @PostMapping("register-with-phone")
    public Mono<HttpHandlerResult<ResponseDTO<User>>> registerWithPhone(
            @QueryParam("phoneNumber") String phoneNumber,
            @QueryParam("verificationCode") String verificationCode,
            @QueryParam("password") String password,
            @QueryParam("nickname") String nickname) {
        return phoneRegistrationService.registerWithPhone(
                phoneNumber, verificationCode, password, nickname)
                .map(HttpHandlerResult::okIfTruthy);
    }

    // 交互数据管理端点
    @GetMapping("interaction/moments/page")
    public Mono<HttpHandlerResult<ResponseDTO<PaginationDTO<Object>>>> queryMomentsPage(
            @QueryParam(defaultValue = "0") int page,
            @QueryParam(required = false) Integer size) {
        size = getPageSize(size);
        
        // 模拟朋友圈数据
        List<Object> moments = List.of(
            createMomentData("1", "user1", "今天天气真好！", "public"),
            createMomentData("2", "user2", "和朋友们一起吃饭", "friends"),
            createMomentData("3", "user3", "工作中的小确幸", "public")
        );
        
        Mono<Long> count = Mono.just(156L);
        Flux<Object> momentsFlux = Flux.fromIterable(moments);
        return HttpHandlerResult.page(count, momentsFlux);
    }
    
    @GetMapping("interaction/likes/page") 
    public Mono<HttpHandlerResult<ResponseDTO<PaginationDTO<Object>>>> queryLikesPage(
            @QueryParam(defaultValue = "0") int page,
            @QueryParam(required = false) Integer size) {
        size = getPageSize(size);
        
        // 模拟点赞数据
        List<Object> likes = List.of(
            createLikeData("1", "user1", "moment", "moment1"),
            createLikeData("2", "user2", "comment", "comment1"),
            createLikeData("3", "user3", "moment", "moment2")
        );
        
        Mono<Long> count = Mono.just(12450L);
        Flux<Object> likesFlux = Flux.fromIterable(likes);
        return HttpHandlerResult.page(count, likesFlux);
    }
    
    @GetMapping("interaction/comments/page")
    public Mono<HttpHandlerResult<ResponseDTO<PaginationDTO<Object>>>> queryCommentsPage(
            @QueryParam(defaultValue = "0") int page,
            @QueryParam(required = false) Integer size) {
        size = getPageSize(size);
        
        // 模拟评论数据
        List<Object> comments = List.of(
            createCommentData("1", "user1", "这是一个很棒的分享！", "moment1"),
            createCommentData("2", "user2", "同意你的观点", "moment1"),
            createCommentData("3", "user3", "很有意思的内容", "moment2")
        );
        
        Mono<Long> count = Mono.just(8920L);
        Flux<Object> commentsFlux = Flux.fromIterable(comments);
        return HttpHandlerResult.page(count, commentsFlux);
    }

    @GetMapping
    @RequiredPermission(USER_QUERY)
    public Mono<HttpHandlerResult<ResponseDTO<Collection<User>>>> queryUsers(
            @QueryParam(required = false) Set<Long> ids,
            @QueryParam(required = false) Date registrationDateStart,
            @QueryParam(required = false) Date registrationDateEnd,
            @QueryParam(required = false) Date deletionDateStart,
            @QueryParam(required = false) Date deletionDateEnd,
            @QueryParam(required = false) Boolean isActive,
            @QueryParam(required = false) Integer size) {
        size = getPageSize(size);
        Flux<User> usersFlux = userService.queryUsers(ids,
                DateRange.of(registrationDateStart, registrationDateEnd),
                DateRange.of(deletionDateStart, deletionDateEnd),
                isActive,
                0,
                size,
                true);
        return HttpHandlerResult.okIfTruthy(usersFlux);
    }

    @GetMapping("page")
    @RequiredPermission(USER_QUERY)
    public Mono<HttpHandlerResult<ResponseDTO<PaginationDTO<User>>>> queryUsers(
            @QueryParam(required = false) Set<Long> ids,
            @QueryParam(required = false) Date registrationDateStart,
            @QueryParam(required = false) Date registrationDateEnd,
            @QueryParam(required = false) Date deletionDateStart,
            @QueryParam(required = false) Date deletionDateEnd,
            @QueryParam(required = false) Boolean isActive,
            int page,
            @QueryParam(required = false) Integer size) {
        size = getPageSize(size);
        Mono<Long> count = userService.countUsers(ids,
                DateRange.of(registrationDateStart, registrationDateEnd),
                DateRange.of(deletionDateStart, deletionDateEnd),
                isActive);
        Flux<User> usersFlux = userService.queryUsers(ids,
                DateRange.of(registrationDateStart, registrationDateEnd),
                DateRange.of(deletionDateStart, deletionDateEnd),
                isActive,
                page,
                size,
                true);
        return HttpHandlerResult.page(count, usersFlux);
    }

    @GetMapping("count")
    @RequiredPermission(USER_QUERY)
    public Mono<HttpHandlerResult<ResponseDTO<UserStatisticsDTO>>> countUsers(
            @QueryParam(required = false) Date registeredStartDate,
            @QueryParam(required = false) Date registeredEndDate,
            @QueryParam(required = false) Date deletedStartDate,
            @QueryParam(required = false) Date deletedEndDate,
            @QueryParam(required = false) Date sentMessageStartDate,
            @QueryParam(required = false) Date sentMessageEndDate,
            @QueryParam(defaultValue = "NOOP") DivideBy divideBy) {
        List<Mono<?>> counts = new LinkedList<>();
        UserStatisticsDTO.UserStatisticsDTOBuilder builder = UserStatisticsDTO.builder();
        if (divideBy == null || divideBy == DivideBy.NOOP) {
            if (deletedStartDate != null || deletedEndDate != null) {
                counts.add(userService
                        .countDeletedUsers(DateRange.of(deletedStartDate, deletedEndDate))
                        .doOnNext(builder::deletedUsers));
            }
            if (sentMessageStartDate != null || sentMessageEndDate != null) {
                counts.add(messageService
                        .countUsersWhoSentMessage(DateRange.of(sentMessageStartDate,
                                sentMessageEndDate), null, false)
                        .doOnNext(builder::usersWhoSentMessages));
            }
            if (counts.isEmpty() || registeredStartDate != null || registeredEndDate != null) {
                counts.add(userService
                        .countRegisteredUsers(DateRange.of(registeredStartDate, registeredEndDate),
                                true)
                        .doOnNext(builder::registeredUsers));
            }
        } else {
            if (deletedStartDate != null && deletedEndDate != null) {
                counts.add(checkAndQueryBetweenDate(DateRange.of(deletedStartDate, deletedEndDate),
                        divideBy,
                        userService::countDeletedUsers).doOnNext(builder::deletedUsersRecords));
            }
            if (sentMessageStartDate != null && sentMessageEndDate != null) {
                counts.add(checkAndQueryBetweenDate(
                        DateRange.of(sentMessageStartDate, sentMessageEndDate),
                        divideBy,
                        messageService::countUsersWhoSentMessage,
                        null,
                        false).doOnNext(builder::usersWhoSentMessagesRecords));
            }
            if (registeredStartDate != null && registeredEndDate != null) {
                counts.add(checkAndQueryBetweenDate(
                        DateRange.of(registeredStartDate, registeredEndDate),
                        divideBy,
                        dateRange -> userService.countRegisteredUsers(dateRange, true))
                        .doOnNext(builder::registeredUsersRecords));
            }
            if (counts.isEmpty()) {
                return Mono.empty();
            }
        }
        return HttpHandlerResult.okIfTruthy(Mono.when(counts)
                .then(Mono.fromCallable(builder::build)));
    }

    @PutMapping
    @RequiredPermission(USER_UPDATE)
    public Mono<HttpHandlerResult<ResponseDTO<UpdateResultDTO>>> updateUser(
            Set<Long> ids,
            @RequestBody UpdateUserDTO updateUserDTO) {
        Mono<UpdateResultDTO> updateMono = userService
                .updateUsers(ids,
                        updateUserDTO.password(),
                        updateUserDTO.name(),
                        updateUserDTO.intro(),
                        updateUserDTO.profilePicture(),
                        updateUserDTO.profileAccessStrategy(),
                        updateUserDTO.roleId(),
                        updateUserDTO.registrationDate(),
                        updateUserDTO.isActive(),
                        null)
                .map(UpdateResultDTO::get);
        return HttpHandlerResult.okIfTruthy(updateMono);
    }

    @DeleteMapping
    @RequiredPermission(USER_DELETE)
    public Mono<HttpHandlerResult<ResponseDTO<DeleteResultDTO>>> deleteUsers(
            Set<Long> ids,
            @QueryParam(required = false) Boolean deleteLogically) {
        Mono<DeleteResultDTO> deleteMono = userService.deleteUsers(ids, deleteLogically)
                .map(DeleteResultDTO::get);
        return HttpHandlerResult.okIfTruthy(deleteMono);
    }
    
    // 交互数据创建辅助方法
    private Object createMomentData(String id, String userId, String content, String visibility) {
        return new Object() {
            public String getId() { return id; }
            public String getUserId() { return userId; }
            public String getContent() { return content; }
            public String getVisibility() { return visibility; }
            public Date getCreatedAt() { return new Date(); }
            public int getLikesCount() { return (int)(Math.random() * 50); }
            public int getCommentsCount() { return (int)(Math.random() * 20); }
            public boolean getHasAttachments() { return false; }
        };
    }
    
    private Object createLikeData(String id, String userId, String targetType, String targetId) {
        return new Object() {
            public String getId() { return id; }
            public String getUserId() { return userId; }
            public String getTargetType() { return targetType; }
            public String getTargetId() { return targetId; }
            public Date getCreatedAt() { return new Date(); }
            public String getDeviceType() { return "web"; }
            public String getIpAddress() { return "192.168.1.100"; }
        };
    }
    
    private Object createCommentData(String id, String userId, String content, String targetId) {
        return new Object() {
            public String getId() { return id; }
            public String getUserId() { return userId; }
            public String getUsername() { return "用户" + userId; }
            public String getContent() { return content; }
            public String getTargetId() { return targetId; }
            public String getStatus() { return "approved"; }
            public Date getCreatedAt() { return new Date(); }
            public int getLikesCount() { return (int)(Math.random() * 10); }
        };
    }

}
