<template>
    <div class="interaction-moderation">
        <a-tabs v-model:activeKey="activeTab" type="card">
            <a-tab-pane key="pending" tab="待审核内容">
                <content-template
                    :name="'moderation-pending'"
                    :url="'/interaction/admin/moderation/pending'"
                    :filters="pendingFilters"
                    :actions="moderationActions"
                    :table="pendingTable"
                    :deletion="deletion"
                />
            </a-tab-pane>
            <a-tab-pane key="rules" tab="审核规则">
                <content-template
                    :name="'moderation-rules'"
                    :url="'/interaction/admin/moderation/rules'"
                    :filters="rulesFilters"
                    :actions="rulesActions"
                    :table="rulesTable"
                    :deletion="deletion"
                />
            </a-tab-pane>
            <a-tab-pane key="logs" tab="审核日志">
                <content-template
                    :name="'moderation-logs'"
                    :url="'/interaction/admin/moderation/logs'"
                    :filters="logsFilters"
                    :actions="[]"
                    :table="logsTable"
                    :deletion="deletion"
                />
            </a-tab-pane>
            <a-tab-pane key="statistics" tab="审核统计">
                <moderation-statistics />
            </a-tab-pane>
        </a-tabs>
    </div>
</template>

<script>
import ContentTemplate from '../../content/template/content-template.vue';
import ModerationStatistics from './moderation-statistics.vue';

export default {
    name: 'interaction-moderation',
    components: {
        ContentTemplate,
        ModerationStatistics
    },
    data() {
        return {
            activeTab: 'pending',
            pendingFilters: [
                {
                    name: 'contentType',
                    type: 'SELECT',
                    model: 'ALL',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: 'allContentTypes'
                        }],
                        values: [
                            { id: 'COMMENT', label: 'comment' },
                            { id: 'MOMENT', label: 'moment' }
                        ]
                    }
                },
                {
                    name: 'userId',
                    type: 'NUMBER',
                    model: null
                },
                {
                    name: 'priority',
                    type: 'SELECT',
                    model: 'ALL',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: 'allPriorities'
                        }],
                        values: [
                            { id: 'HIGH', label: 'high' },
                            { id: 'MEDIUM', label: 'medium' },
                            { id: 'LOW', label: 'low' }
                        ]
                    }
                },
                {
                    name: 'createdAt',
                    type: 'DATE-RANGE',
                    model: null
                }
            ],
            moderationActions: [
                {
                    name: 'APPROVE',
                    fields: [
                        {
                            name: 'reason',
                            type: 'TEXTAREA'
                        }
                    ]
                },
                {
                    name: 'REJECT',
                    fields: [
                        {
                            name: 'reason',
                            type: 'TEXTAREA',
                            required: true
                        }
                    ]
                }
            ],
            pendingTable: {
                columns: [
                    {
                        key: 'id',
                        type: 'TEXT',
                        width: 150
                    },
                    {
                        key: 'contentType',
                        type: 'ENUM',
                        width: 100
                    },
                    {
                        key: 'contentId',
                        type: 'TEXT',
                        width: 150
                    },
                    {
                        key: 'userId',
                        type: 'NUMBER',
                        width: 100
                    },
                    {
                        key: 'content',
                        type: 'TEXT',
                        width: 250
                    },
                    {
                        key: 'priority',
                        type: 'ENUM',
                        width: 80
                    },
                    {
                        key: 'score',
                        type: 'NUMBER',
                        width: 80
                    },
                    {
                        key: 'flags',
                        type: 'TEXT',
                        width: 120
                    },
                    {
                        key: 'createdAt',
                        type: 'DATE',
                        width: 180
                    },
                    {
                        key: 'operation',
                        type: 'OPERATION',
                        width: 150
                    }
                ]
            },
            rulesFilters: [
                {
                    name: 'category',
                    type: 'SELECT',
                    model: 'ALL',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: 'allCategories'
                        }],
                        values: [
                            { id: 'SPAM', label: 'spam' },
                            { id: 'INAPPROPRIATE', label: 'inappropriate' },
                            { id: 'VIOLENCE', label: 'violence' },
                            { id: 'HATE_SPEECH', label: 'hateSpeech' }
                        ]
                    }
                },
                {
                    name: 'enabled',
                    type: 'SELECT',
                    model: 'ALL',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: 'allStatuses'
                        }],
                        values: [
                            { id: 'true', label: 'enabled' },
                            { id: 'false', label: 'disabled' }
                        ]
                    }
                }
            ],
            rulesActions: [
                {
                    name: 'CREATE',
                    fields: [
                        {
                            name: 'name',
                            type: 'TEXT',
                            required: true
                        },
                        {
                            name: 'category',
                            type: 'SELECT',
                            required: true,
                            options: [
                                { id: 'SPAM', label: 'spam' },
                                { id: 'INAPPROPRIATE', label: 'inappropriate' },
                                { id: 'VIOLENCE', label: 'violence' },
                                { id: 'HATE_SPEECH', label: 'hateSpeech' }
                            ]
                        },
                        {
                            name: 'pattern',
                            type: 'TEXTAREA',
                            required: true
                        },
                        {
                            name: 'threshold',
                            type: 'NUMBER',
                            required: true
                        },
                        {
                            name: 'action',
                            type: 'SELECT',
                            required: true,
                            options: [
                                { id: 'AUTO_APPROVE', label: 'autoApprove' },
                                { id: 'AUTO_REJECT', label: 'autoReject' },
                                { id: 'MANUAL_REVIEW', label: 'manualReview' }
                            ]
                        },
                        {
                            name: 'enabled',
                            type: 'BOOLEAN'
                        }
                    ]
                },
                {
                    name: 'UPDATE',
                    fields: [
                        {
                            name: 'name',
                            type: 'TEXT'
                        },
                        {
                            name: 'pattern',
                            type: 'TEXTAREA'
                        },
                        {
                            name: 'threshold',
                            type: 'NUMBER'
                        },
                        {
                            name: 'action',
                            type: 'SELECT',
                            options: [
                                { id: 'AUTO_APPROVE', label: 'autoApprove' },
                                { id: 'AUTO_REJECT', label: 'autoReject' },
                                { id: 'MANUAL_REVIEW', label: 'manualReview' }
                            ]
                        },
                        {
                            name: 'enabled',
                            type: 'BOOLEAN'
                        }
                    ]
                }
            ],
            rulesTable: {
                columns: [
                    {
                        key: 'id',
                        type: 'NUMBER',
                        width: 100
                    },
                    {
                        key: 'name',
                        type: 'TEXT',
                        width: 150
                    },
                    {
                        key: 'category',
                        type: 'ENUM',
                        width: 120
                    },
                    {
                        key: 'pattern',
                        type: 'TEXT',
                        width: 200
                    },
                    {
                        key: 'threshold',
                        type: 'NUMBER',
                        width: 80
                    },
                    {
                        key: 'action',
                        type: 'ENUM',
                        width: 120
                    },
                    {
                        key: 'enabled',
                        type: 'BOOLEAN',
                        width: 80
                    },
                    {
                        key: 'hitCount',
                        type: 'NUMBER',
                        width: 80
                    },
                    {
                        key: 'createdAt',
                        type: 'DATE',
                        width: 180
                    },
                    {
                        key: 'operation',
                        type: 'OPERATION',
                        width: 120
                    }
                ]
            },
            logsFilters: [
                {
                    name: 'contentType',
                    type: 'SELECT',
                    model: 'ALL',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: 'allContentTypes'
                        }],
                        values: [
                            { id: 'COMMENT', label: 'comment' },
                            { id: 'MOMENT', label: 'moment' }
                        ]
                    }
                },
                {
                    name: 'moderatorId',
                    type: 'NUMBER',
                    model: null
                },
                {
                    name: 'action',
                    type: 'SELECT',
                    model: 'ALL',
                    options: {
                        base: [{
                            id: 'ALL',
                            label: 'allActions'
                        }],
                        values: [
                            { id: 'APPROVED', label: 'approved' },
                            { id: 'REJECTED', label: 'rejected' },
                            { id: 'AUTO_APPROVED', label: 'autoApproved' },
                            { id: 'AUTO_REJECTED', label: 'autoRejected' }
                        ]
                    }
                },
                {
                    name: 'createdAt',
                    type: 'DATE-RANGE',
                    model: null
                }
            ],
            logsTable: {
                columns: [
                    {
                        key: 'id',
                        type: 'NUMBER',
                        width: 100
                    },
                    {
                        key: 'contentType',
                        type: 'ENUM',
                        width: 100
                    },
                    {
                        key: 'contentId',
                        type: 'TEXT',
                        width: 150
                    },
                    {
                        key: 'moderatorId',
                        type: 'NUMBER',
                        width: 100
                    },
                    {
                        key: 'action',
                        type: 'ENUM',
                        width: 120
                    },
                    {
                        key: 'reason',
                        type: 'TEXT',
                        width: 200
                    },
                    {
                        key: 'ruleId',
                        type: 'NUMBER',
                        width: 100
                    },
                    {
                        key: 'score',
                        type: 'NUMBER',
                        width: 80
                    },
                    {
                        key: 'processTime',
                        type: 'NUMBER',
                        width: 100
                    },
                    {
                        key: 'createdAt',
                        type: 'DATE',
                        width: 180
                    }
                ]
            },
            deletion: {
                refresh: true
            }
        };
    }
};
</script>

<style lang="scss">
.interaction-moderation {
    .ant-tabs-content-holder {
        padding: 0;
    }
}
</style>