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

package im.turms.interaction.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * 内容审核结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
    private Double score;
    private String reason;
    private List<String> labels;
    private Instant processedAt;
    private String moderator;
    
    public ModerationResult(Double score, String reason, List<String> labels) {
        this.score = score;
        this.reason = reason;
        this.labels = labels;
        this.processedAt = Instant.now();
    }
}