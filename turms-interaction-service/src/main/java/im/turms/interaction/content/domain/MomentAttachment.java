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

/**
 * 朋友圈多媒体附件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentAttachment {

    /**
     * 附件类型
     */
    private AttachmentType type;

    /**
     * 文件URL
     */
    private String url;

    /**
     * 缩略图URL (图片/视频)
     */
    private String thumbnailUrl;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 宽度 (图片/视频)
     */
    private Integer width;

    /**
     * 高度 (图片/视频)
     */
    private Integer height;

    /**
     * 时长 (视频/音频，秒)
     */
    private Integer duration;

    /**
     * 附件描述
     */
    private String description;

    /**
     * 附件类型枚举
     */
    public enum AttachmentType {
        IMAGE,      // 图片
        VIDEO,      // 视频
        AUDIO,      // 音频
        DOCUMENT,   // 文档
        LINK        // 链接
    }
}