import type { ComponentCustomProperties } from '@vue/runtime-core';

export default {
    // 点赞管理 API
    fetchLikes(params: any) {
        return this.$http.get('/interaction/admin/likes', { params });
    },

    fetchLikesPage(params: any) {
        return this.$http.get('/interaction/admin/likes/page', { params });
    },

    createLike(data: any) {
        return this.$http.post('/interaction/likes', data);
    },

    updateLike(id: string, data: any) {
        return this.$http.put(`/interaction/likes/${id}`, data);
    },

    deleteLikes(ids: string[]) {
        const params = ids.map(id => `ids=${id}`).join('&');
        return this.$http.delete(`/interaction/likes?${params}`);
    },

    // 点赞统计 API
    fetchLikeStats(params: any) {
        return this.$http.get('/interaction/likes/stats', { params });
    },

    fetchLikeCountsByTarget(targetType: string, targetIds: string[]) {
        const params = { targetType, targetIds: targetIds.join(',') };
        return this.$http.get('/interaction/likes/counts', { params });
    },

    // 评论管理 API
    fetchComments(params: any) {
        return this.$http.get('/interaction/admin/comments', { params });
    },

    fetchCommentsPage(params: any) {
        return this.$http.get('/interaction/admin/comments/page', { params });
    },

    createComment(data: any) {
        return this.$http.post('/interaction/comments', data);
    },

    updateComment(id: string, data: any) {
        return this.$http.put(`/interaction/comments/${id}`, data);
    },

    deleteComments(ids: string[]) {
        const params = ids.map(id => `ids=${id}`).join('&');
        return this.$http.delete(`/interaction/comments?${params}`);
    },

    // 评论审核 API
    approveComments(ids: string[], reason?: string) {
        return this.$http.post('/interaction/comments/approve', { ids, reason });
    },

    rejectComments(ids: string[], reason: string) {
        return this.$http.post('/interaction/comments/reject', { ids, reason });
    },

    // 评论统计 API
    fetchCommentStats(params: any) {
        return this.$http.get('/interaction/comments/stats', { params });
    },

    fetchCommentCountsByArticle(articleIds: string[]) {
        const params = { articleIds: articleIds.join(',') };
        return this.$http.get('/interaction/comments/counts', { params });
    },

    // 朋友圈管理 API
    fetchMoments(params: any) {
        return this.$http.get('/content/moments', { params });
    },

    fetchMomentsPage(params: any) {
        return this.$http.get('/content/moments', { params });
    },

    createMoment(data: any) {
        return this.$http.post('/interaction/moments', data);
    },

    updateMoment(id: string, data: any) {
        return this.$http.put(`/interaction/moments/${id}`, data);
    },

    deleteMoments(ids: string[]) {
        const params = ids.map(id => `ids=${id}`).join('&');
        return this.$http.delete(`/interaction/moments?${params}`);
    },

    // 朋友圈附件管理 API
    fetchMomentAttachments(momentId: string) {
        return this.$http.get(`/interaction/moments/${momentId}/attachments`);
    },

    uploadMomentAttachment(momentId: string, file: File) {
        const formData = new FormData();
        formData.append('file', file);
        return this.$http.post(`/interaction/moments/${momentId}/attachments`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
    },

    deleteMomentAttachment(momentId: string, attachmentId: string) {
        return this.$http.delete(`/interaction/moments/${momentId}/attachments/${attachmentId}`);
    },

    // 内容审核 API
    fetchModerationPending(params: any) {
        return this.$http.get('/interaction/admin/moderation/pending', { params });
    },

    fetchModerationPendingPage(params: any) {
        return this.$http.get('/interaction/admin/moderation/pending/page', { params });
    },

    approveModerationContent(ids: string[], reason?: string) {
        return this.$http.post('/interaction/admin/moderation/approve', { ids, reason });
    },

    rejectModerationContent(ids: string[], reason: string) {
        return this.$http.post('/interaction/admin/moderation/reject', { ids, reason });
    },

    // 审核规则管理 API
    fetchModerationRules(params: any) {
        return this.$http.get('/interaction/admin/moderation/rules', { params });
    },

    fetchModerationRulesPage(params: any) {
        return this.$http.get('/interaction/admin/moderation/rules/page', { params });
    },

    createModerationRule(data: any) {
        return this.$http.post('/interaction/admin/moderation/rules', data);
    },

    updateModerationRule(id: string, data: any) {
        return this.$http.put(`/interaction/admin/moderation/rules/${id}`, data);
    },

    deleteModerationRules(ids: string[]) {
        const params = ids.map(id => `ids=${id}`).join('&');
        return this.$http.delete(`/interaction/admin/moderation/rules?${params}`);
    },

    enableModerationRule(id: string) {
        return this.$http.post(`/interaction/admin/moderation/rules/${id}/enable`);
    },

    disableModerationRule(id: string) {
        return this.$http.post(`/interaction/admin/moderation/rules/${id}/disable`);
    },

    // 审核日志 API
    fetchModerationLogs(params: any) {
        return this.$http.get('/interaction/admin/moderation/logs', { params });
    },

    fetchModerationLogsPage(params: any) {
        return this.$http.get('/interaction/admin/moderation/logs/page', { params });
    },

    // 审核统计 API
    fetchModerationStats(params: any) {
        return this.$http.get('/interaction/admin/moderation/stats', { params });
    },

    fetchModerationTrend(params: any) {
        return this.$http.get('/interaction/admin/moderation/trend', { params });
    },

    // 系统监控 API
    fetchInteractionHealth() {
        return this.$http.get('/interaction/admin/health');
    },

    fetchInteractionMetrics() {
        return this.$http.get('/interaction/admin/metrics');
    },

    fetchInteractionEvents(params: any) {
        return this.$http.get('/interaction/events', { params });
    },

    fetchInteractionEventsPage(params: any) {
        return this.$http.get('/interaction/events/page', { params });
    },

    fetchInteractionErrors(params: any) {
        return this.$http.get('/interaction/errors', { params });
    },

    fetchInteractionErrorsPage(params: any) {
        return this.$http.get('/interaction/errors/page', { params });
    },

    // 性能监控 API
    fetchPerformanceMetrics(params: any) {
        return this.$http.get('/interaction/performance/metrics', { params });
    },

    fetchDatabaseMetrics() {
        return this.$http.get('/interaction/performance/database');
    },

    fetchCacheMetrics() {
        return this.$http.get('/interaction/performance/cache');
    },

    fetchJvmMetrics() {
        return this.$http.get('/interaction/performance/jvm');
    },

    fetchServiceMetrics() {
        return this.$http.get('/interaction/performance/services');
    },

    // 缓存管理 API
    clearLikeCache(targetType?: string, targetId?: string) {
        const params: any = {};
        if (targetType) params.targetType = targetType;
        if (targetId) params.targetId = targetId;
        return this.$http.post('/interaction/cache/likes/clear', null, { params });
    },

    clearCommentCache(articleId?: string) {
        const params: any = {};
        if (articleId) params.articleId = articleId;
        return this.$http.post('/interaction/cache/comments/clear', null, { params });
    },

    refreshCache() {
        return this.$http.post('/interaction/cache/refresh');
    },

    // 数据同步 API
    syncLikeData() {
        return this.$http.post('/interaction/sync/likes');
    },

    syncCommentData() {
        return this.$http.post('/interaction/sync/comments');
    },

    fetchSyncStatus() {
        return this.$http.get('/interaction/sync/status');
    },

    // 批量操作 API
    batchUpdateLikes(data: any) {
        return this.$http.post('/interaction/likes/batch', data);
    },

    batchUpdateComments(data: any) {
        return this.$http.post('/interaction/comments/batch', data);
    },

    batchUpdateMoments(data: any) {
        return this.$http.post('/interaction/moments/batch', data);
    },

    // 导出 API
    exportLikes(params: any) {
        return this.$http.get('/interaction/likes/export', { 
            params, 
            responseType: 'blob' 
        });
    },

    exportComments(params: any) {
        return this.$http.get('/interaction/comments/export', { 
            params, 
            responseType: 'blob' 
        });
    },

    exportMoments(params: any) {
        return this.$http.get('/interaction/moments/export', { 
            params, 
            responseType: 'blob' 
        });
    },

    exportModerationLogs(params: any) {
        return this.$http.get('/interaction/admin/moderation/logs/export', { 
            params, 
            responseType: 'blob' 
        });
    }
} as ComponentCustomProperties & Record<string, any>;