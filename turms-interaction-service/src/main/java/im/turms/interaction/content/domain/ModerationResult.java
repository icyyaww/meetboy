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

package im.turms.interaction.content.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 内容审核结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {

    /**
     * 审核引擎
     */
    private String engine;

    /**
     * 审核版本
     */
    private String version;

    /**
     * 总体得分 (0-1, 越高越安全)
     */
    private Double overallScore;

    /**
     * 各项检测结果
     */
    private Map<String, ModerationCheck> checks;

    /**
     * 审核标签
     */
    private List<String> labels;

    /**
     * 审核建议
     */
    private String recommendation;

    /**
     * 审核时间
     */
    private Instant moderatedAt;

    /**
     * 审核耗时 (毫秒)
     */
    private Long processingTime;

    /**
     * 单项审核检查
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModerationCheck {
        
        /**
         * 检查类型
         */
        private String type;
        
        /**
         * 检查得分 (0-1)
         */
        private Double score;
        
        /**
         * 检查结果
         */
        private CheckResult result;
        
        /**
         * 检查详情
         */
        private String details;
        
        /**
         * 检查结果枚举
         */
        public enum CheckResult {
            PASS,       // 通过
            WARNING,    // 警告
            BLOCK       // 阻止
        }
    }
}