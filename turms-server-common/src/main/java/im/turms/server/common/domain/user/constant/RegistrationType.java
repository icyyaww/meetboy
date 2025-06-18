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

package im.turms.server.common.domain.user.constant;

/**
 * @author James Chen
 */
public enum RegistrationType {
    /**
     * Created by administrator
     */
    ADMIN(0),
    
    /**
     * Self-registered with phone number
     */
    PHONE(1),
    
    /**
     * Self-registered with email
     */
    EMAIL(2),
    
    /**
     * Registered via third-party service
     */
    THIRD_PARTY(3);

    private final int code;

    RegistrationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RegistrationType fromCode(int code) {
        return switch (code) {
            case 0 -> ADMIN;
            case 1 -> PHONE;
            case 2 -> EMAIL;
            case 3 -> THIRD_PARTY;
            default -> throw new IllegalArgumentException("Unknown registration type code: " + code);
        };
    }
}